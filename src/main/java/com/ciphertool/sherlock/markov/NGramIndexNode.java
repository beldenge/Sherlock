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

package com.ciphertool.sherlock.markov;

import java.util.HashMap;
import java.util.Map;

public class NGramIndexNode<T> {
	private int							level		= 0;
	private long						count		= 0L;
	private double						ratio		= 0.0;
	private boolean						isTerminal;
	private Map<T, NGramIndexNode<T>>	transitions	= new HashMap<T, NGramIndexNode<T>>();

	/**
	 * @param isTerminal
	 *            whether this is a terminal node
	 */
	public NGramIndexNode(boolean isTerminal) {
		this.isTerminal = isTerminal;
	}

	/**
	 * @param isTerminal
	 *            whether this is a terminal node
	 * @param level
	 *            the level to set
	 */
	public NGramIndexNode(boolean isTerminal, int level) {
		this.isTerminal = isTerminal;
		this.level = level;
	}

	public boolean containsChild(T state) {
		return this.transitions.containsKey(state);
	}

	public NGramIndexNode<T> getChild(T state) {
		return this.transitions.get(state);
	}

	public synchronized void addOrIncrementChildAsync(T firstState, int level) {
		if (!this.containsChild(firstState)) {
			this.putChild(firstState, new NGramIndexNode<T>(true, level));
		}

		this.getChild(firstState).increment();
	}

	public void putChild(T state, NGramIndexNode<T> child) {
		this.transitions.put(state, child);
	}

	public void increment() {
		this.count += 1L;
	}

	/**
	 * @return the count
	 */
	public Long getCount() {
		return this.count;
	}

	/**
	 * @return the level
	 */
	public int getLevel() {
		return this.level;
	}

	/**
	 * @return the ratio
	 */
	public double getRatio() {
		return this.ratio;
	}

	/**
	 * All current usages of this method are thread-safe, but since it's used in a multi-threaded way, this is a
	 * defensive measure in case future usage changes are not thread-safe.
	 * 
	 * @param ratio
	 *            the ratio to set
	 */
	public synchronized void setRatio(double ratio) {
		this.ratio = ratio;
	}

	/**
	 * @return the transitions array
	 */
	public Map<T, NGramIndexNode<T>> getTransitions() {
		return this.transitions;
	}

	/**
	 * @return the isTerminal
	 */
	public boolean isTerminal() {
		return isTerminal;
	}

	/**
	 * @param isTerminal
	 *            the isTerminal to set
	 */
	public void setIsTerminal(boolean isTerminal) {
		this.isTerminal = isTerminal;
	}
}