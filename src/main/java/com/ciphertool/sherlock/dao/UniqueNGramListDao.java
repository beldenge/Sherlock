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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import com.ciphertool.sherlock.entities.NGram;

public class UniqueNGramListDao implements NGramListDao {
	private static Logger				log				= LoggerFactory.getLogger(UniqueNGramListDao.class);

	private NGramDao					nGramDao;
	private Integer						topTwoGrams;
	private Integer						topThreeGrams;
	private Integer						topFourGrams;
	private Integer						topFiveGrams;

	private List<NGram>					twoGramList;
	private List<NGram>					threeGramList;
	private List<NGram>					fourGramList;
	private List<NGram>					fiveGramList;

	private Map<Integer, List<NGram>>	mapOfNGramLists	= new HashMap<Integer, List<NGram>>(4);

	/**
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@PostConstruct
	public void init() throws InterruptedException, ExecutionException {
		if (this.nGramDao == null) {
			throw new IllegalArgumentException("Error constructing UniqueNGramListDao.  NGramDao cannot be null.");
		}

		if (this.topTwoGrams == null) {
			throw new IllegalArgumentException(
					"Error constructing UniqueNGramListDao.  Top cannot be null.  Please ensure top is either set to a positive number, or to -1 to be unbounded.");
		}

		if (this.topThreeGrams == null) {
			throw new IllegalArgumentException(
					"Error constructing UniqueNGramListDao.  Top cannot be null.  Please ensure top is either set to a positive number, or to -1 to be unbounded.");
		}

		if (this.topFourGrams == null) {
			throw new IllegalArgumentException(
					"Error constructing UniqueNGramListDao.  Top cannot be null.  Please ensure top is either set to a positive number, or to -1 to be unbounded.");
		}

		if (this.topFiveGrams == null) {
			throw new IllegalArgumentException(
					"Error constructing UniqueNGramListDao.  Top cannot be null.  Please ensure top is either set to a positive number, or to -1 to be unbounded.");
		}

		twoGramList = new ArrayList<NGram>(topTwoGrams);
		threeGramList = new ArrayList<NGram>(topThreeGrams);
		fourGramList = new ArrayList<NGram>(topFourGrams);
		fiveGramList = new ArrayList<NGram>(topFiveGrams);

		mapOfNGramLists.put(2, twoGramList);
		mapOfNGramLists.put(3, threeGramList);
		mapOfNGramLists.put(4, fourGramList);
		mapOfNGramLists.put(5, fiveGramList);

		log.info("Beginning fetching of n-grams from database.");

		long start = System.currentTimeMillis();

		Future<List<NGram>> twoGrams = nGramDao.findTopMostFrequentByNumWordsAsync(2, topTwoGrams);
		Future<List<NGram>> threeGrams = nGramDao.findTopMostFrequentByNumWordsAsync(3, topThreeGrams);
		Future<List<NGram>> fourGrams = nGramDao.findTopMostFrequentByNumWordsAsync(4, topFourGrams);
		Future<List<NGram>> fiveGrams = nGramDao.findTopMostFrequentByNumWordsAsync(5, topFiveGrams);

		fiveGramList.addAll(fiveGrams.get());
		fourGramList.addAll(fourGrams.get());
		threeGramList.addAll(threeGrams.get());
		twoGramList.addAll(twoGrams.get());

		log.info("Finished fetching n-grams from database in " + (System.currentTimeMillis() - start) + "ms.");

		twoGramList.sort(new FrequencyComparator());
		threeGramList.sort(new FrequencyComparator());
		fourGramList.sort(new FrequencyComparator());
		fiveGramList.sort(new FrequencyComparator());
	}

	private class FrequencyComparator implements Comparator<NGram> {
		@Override
		public int compare(NGram nGram1, NGram nGram2) {
			if (nGram1.getFrequencyWeight() < nGram2.getFrequencyWeight()) {
				return 1;
			} else if (nGram1.getFrequencyWeight() > nGram2.getFrequencyWeight()) {
				return -1;
			}

			return 0;
		}
	}

	@Override
	public NGram findRandomNGram() {
		int randomMapIndex = (int) (ThreadLocalRandom.current().nextDouble() * mapOfNGramLists.size());

		List<NGram> nGramList = mapOfNGramLists.get(randomMapIndex);

		int randomIndex = (int) (ThreadLocalRandom.current().nextDouble() * nGramList.size());

		return nGramList.get(randomIndex);
	}

	@Override
	public Map<Integer, List<NGram>> getMapOfNGramLists() {
		return Collections.unmodifiableMap(mapOfNGramLists);
	}

	/**
	 * @param nGramDao
	 *            the nGramDao to set
	 */
	@Required
	public void setnGramDao(NGramDao nGramDao) {
		this.nGramDao = nGramDao;
	}

	/**
	 * @param topTwoGrams
	 *            the topTwoGrams to set
	 */
	@Required
	public void setTopTwoGrams(Integer topTwoGrams) {
		this.topTwoGrams = topTwoGrams;
	}

	/**
	 * @param topThreeGrams
	 *            the topThreeGrams to set
	 */
	@Required
	public void setTopThreeGrams(Integer topThreeGrams) {
		this.topThreeGrams = topThreeGrams;
	}

	/**
	 * @param topFourGrams
	 *            the topFourGrams to set
	 */
	@Required
	public void setTopFourGrams(Integer topFourGrams) {
		this.topFourGrams = topFourGrams;
	}

	/**
	 * @param topFiveGrams
	 *            the topFiveGrams to set
	 */
	@Required
	public void setTopFiveGrams(Integer topFiveGrams) {
		this.topFiveGrams = topFiveGrams;
	}
}
