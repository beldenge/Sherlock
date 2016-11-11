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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import com.ciphertool.sherlock.entities.Word;

public class FrequencyWordListDao implements WordListDao {
	private static Logger	log			= LoggerFactory.getLogger(FrequencyWordListDao.class);

	private List<Word>		wordList	= new ArrayList<Word>();
	private WordDao			wordDao;
	private Integer			topWords;

	/**
	 * Stacks the List based on Word frequency.
	 * 
	 * @param wordDao
	 *            the WordDao to use for populating the internal List
	 * @param top
	 *            the top number of words
	 */
	@PostConstruct
	public void init() {
		if (wordDao == null) {
			throw new IllegalArgumentException("Error constructing FrequencyWordListDao.  WordDao cannot be null.");
		}

		if (topWords == null) {
			throw new IllegalArgumentException(
					"Error constructing FrequencyWordListDao.  Top cannot be null.  Please ensure top is either set to a positive number, or to -1 to be unbounded.");
		}

		log.info("Beginning fetching of words from database.");

		long start = System.currentTimeMillis();

		wordList.addAll(wordDao.findTopUniqueWordsByFrequency(topWords));

		log.info("Finished fetching words from database in " + (System.currentTimeMillis() - start) + "ms.");

		List<Word> wordsToAdd = new ArrayList<Word>();

		for (Word w : this.wordList) {
			/*
			 * Add the word to the map by reference a number of times equal to the frequency value - 1 since it already
			 * exists in the list once.
			 */
			for (int i = 0; i < w.getFrequencyWeight() - 1; i++) {
				wordsToAdd.add(w);
			}
		}

		this.wordList.addAll(wordsToAdd);
	}

	@Override
	public Word findRandomWord() {
		int randomIndex = (int) (ThreadLocalRandom.current().nextDouble() * wordList.size());

		return wordList.get(randomIndex);
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
