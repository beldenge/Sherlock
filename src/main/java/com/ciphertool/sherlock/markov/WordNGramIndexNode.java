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

public class WordNGramIndexNode {
	private int									level		= 0;
	private long								count		= 0L;
	private double								ratio		= 0.0;
	private boolean								isTerminal;
	private Map<Character, WordNGramIndexNode>	transitions	= new HashMap<Character, WordNGramIndexNode>();

	/**
	 * Default no-args constructor
	 */
	public WordNGramIndexNode() {
	}

	/**
	 * @param isTerminal
	 *            whether this is a terminal node
	 */
	public WordNGramIndexNode(boolean isTerminal) {
		this.isTerminal = isTerminal;
	}

	/**
	 * @param isTerminal
	 *            whether this is a terminal node
	 * @param level
	 *            the level to set
	 */
	public WordNGramIndexNode(boolean isTerminal, int level) {
		this.isTerminal = isTerminal;
		this.level = level;
	}

	/**
	 * @param c
	 *            the Character to find
	 * @return whether the Character exists
	 */
	public boolean containsChild(Character c) {
		return this.transitions.containsKey(c);
	}

	/**
	 * @param c
	 *            the Character
	 * @return the child matching the given Character
	 */
	public WordNGramIndexNode getChild(Character c) {
		return this.transitions.get(c);
	}

	/**
	 * @param c
	 *            the Character
	 * @param child
	 *            the child node
	 */
	public void putChild(Character c, WordNGramIndexNode child) {
		this.transitions.put(c, child);
	}

	/**
	 * @return the level
	 */
	public int getLevel() {
		return level;
	}

	public void increment() {
		this.count += 1L;
	}

	/**
	 * @return the count
	 */
	public long getCount() {
		return count;
	}

	/**
	 * @return the ratio
	 */
	public double getRatio() {
		return ratio;
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
