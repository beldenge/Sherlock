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
import java.util.regex.Pattern;

public class LetterNGramIndexNode {
	private static final Pattern					LOWERCASE_LETTERS	= Pattern.compile("[a-z]");
	private int										level				= 0;
	private long									count				= 0L;
	private double									ratio				= 0.0;
	private boolean									isTerminal;
	private Map<Character, LetterNGramIndexNode>	transitions			= new HashMap<Character, LetterNGramIndexNode>();

	/**
	 * @param isTerminal
	 *            whether this is a terminal node
	 */
	public LetterNGramIndexNode(boolean isTerminal) {
		this.isTerminal = isTerminal;
	}

	/**
	 * @param isTerminal
	 *            whether this is a terminal node
	 * @param level
	 *            the level to set
	 */
	public LetterNGramIndexNode(boolean isTerminal, int level) {
		this.isTerminal = isTerminal;
		this.level = level;
	}

	public boolean containsChild(Character c) {
		return this.transitions.containsKey(c);
	}

	public LetterNGramIndexNode getChild(Character c) {
		return this.transitions.get(c);
	}

	public synchronized void addOrIncrementChildAsync(Character firstLetter, int level) {
		if (!this.containsChild(firstLetter)) {
			this.putChild(firstLetter, new LetterNGramIndexNode(true, level));
		}

		this.getChild(firstLetter).increment();
	}

	public void putChild(Character c, LetterNGramIndexNode child) {
		if (!LOWERCASE_LETTERS.matcher(c.toString()).matches()) {
			throw new IllegalArgumentException(
					"Attempted to add a character to the Markov Model which is outside the range of "
							+ LOWERCASE_LETTERS);
		}

		this.transitions.put(c, child);
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
	public Map<Character, LetterNGramIndexNode> getTransitions() {
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
