/**
 * Copyright 2015 George Belden
 * 
 * This file is part of Sherlock.
 * 
 * Sherlock is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * Sherlock is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Sherlock. If not, see <http://www.gnu.org/licenses/>.
 */

package com.ciphertool.sherlock.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import com.ciphertool.sherlock.entities.Word;
import com.ciphertool.sherlock.enumerations.PartOfSpeechType;

public class BasicWordMapDao implements WordMapDao {
	private static Logger							log					= LoggerFactory.getLogger(BasicWordMapDao.class);

	private Map<PartOfSpeechType, ArrayList<Word>>	partOfSpeechWordMap	= new HashMap<PartOfSpeechType, ArrayList<Word>>();
	private Map<Integer, ArrayList<Word>>			lengthWordMap		= new HashMap<Integer, ArrayList<Word>>();
	private WordDao									wordDao;
	private Integer									topWords;

	/**
	 * @param wordDao
	 *            the WordDao to use for populating the internal Maps
	 * @param top
	 *            the top number of words
	 */
	@PostConstruct
	public void init() {
		if (wordDao == null) {
			throw new IllegalArgumentException("Error constructing BasicWordMapDao.  WordDao cannot be null.");
		}

		if (topWords == null || topWords == 0) {
			throw new IllegalArgumentException(
					"Error constructing BasicWordMapDao.  Top cannot be 0.  Please ensure top is either set to a positive number, or to -1 to be unbounded.");
		}

		ArrayList<Word> allWords = new ArrayList<Word>();

		log.info("Beginning fetching of words from database.");

		long start = System.currentTimeMillis();

		allWords.addAll(wordDao.findTopByFrequency(topWords));

		log.info("Finished fetching words from database in " + (System.currentTimeMillis() - start) + "ms.");

		partOfSpeechWordMap = mapByPartOfSpeech(allWords);

		lengthWordMap = mapByWordLength(allWords);
	}

	@Override
	public Word findRandomWordByPartOfSpeech(PartOfSpeechType pos) {
		ArrayList<Word> wordList = partOfSpeechWordMap.get(pos);

		int randomIndex = (int) (ThreadLocalRandom.current().nextDouble() * wordList.size());

		return wordList.get(randomIndex);
	}

	@Override
	public Word findRandomWordByLength(Integer length) {
		ArrayList<Word> wordList = lengthWordMap.get(length);

		int randomIndex = (int) (ThreadLocalRandom.current().nextDouble() * wordList.size());

		return wordList.get(randomIndex);
	}

	/**
	 * @param allWords
	 *            the List of all Words pulled in from the constructor
	 * @return a Map of all Words keyed by their PartOfSpeech
	 */
	protected static HashMap<PartOfSpeechType, ArrayList<Word>> mapByPartOfSpeech(List<Word> allWords) {
		if (allWords == null || allWords.isEmpty()) {
			throw new IllegalArgumentException(
					"Error mapping Words by PartOfSpeech.  The supplied List of Words cannot be null or empty.");
		}

		HashMap<PartOfSpeechType, ArrayList<Word>> byPartOfSpeech = new HashMap<PartOfSpeechType, ArrayList<Word>>();
		for (Word w : allWords) {
			PartOfSpeechType pos = w.getPartOfSpeech();

			// Add the part of speech to the map if it doesn't exist
			if (!byPartOfSpeech.containsKey(pos)) {
				byPartOfSpeech.put(pos, new ArrayList<Word>());
			}

			/*
			 * Add the word to the map by reference a number of times equal to the frequency value
			 */
			for (int i = 0; i < w.getFrequencyWeight(); i++) {
				byPartOfSpeech.get(pos).add(w);
			}
		}
		return byPartOfSpeech;
	}

	/**
	 * @param allWords
	 *            the List of all Words pulled in from the constructor
	 * @return a Map of all Words keyed by their length
	 */
	protected static HashMap<Integer, ArrayList<Word>> mapByWordLength(List<Word> allWords) {
		if (allWords == null || allWords.isEmpty()) {
			throw new IllegalArgumentException(
					"Error mapping Words by length.  The supplied List of Words cannot be null or empty.");
		}

		HashMap<Integer, ArrayList<Word>> byWordLength = new HashMap<Integer, ArrayList<Word>>();

		for (Word w : allWords) {
			Integer length = w.getWord().length();

			// Add the part of speech to the map if it doesn't exist
			if (!byWordLength.containsKey(length)) {
				byWordLength.put(length, new ArrayList<Word>());
			}

			/*
			 * Add the word to the map by reference a number of times equal to the frequency value
			 */
			for (int i = 0; i < w.getFrequencyWeight(); i++) {
				byWordLength.get(length).add(w);
			}
		}

		return byWordLength;
	}

	@Override
	public Map<PartOfSpeechType, ArrayList<Word>> getPartOfSpeechWordMap() {
		return Collections.unmodifiableMap(partOfSpeechWordMap);
	}

	@Override
	public Map<Integer, ArrayList<Word>> getLengthWordMap() {
		return Collections.unmodifiableMap(lengthWordMap);
	}

	/**
	 * @param wordDao
	 *            the wordDao to set
	 */
	@Required
	public void setWordDao(WordDao wordDao) {
		this.wordDao = wordDao;
	}

	/**
	 * @param topWords
	 *            the topWords to set
	 */
	@Required
	public void setTopWords(Integer topWords) {
		this.topWords = topWords;
	}
}
