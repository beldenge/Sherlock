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

public class NGramIndexNode {
	private static final Pattern			LOWERCASE_LETTERS	= Pattern.compile("[a-z]");
	private Map<Character, NGramIndexNode>	transitions			= new HashMap<Character, NGramIndexNode>();

	public NGramIndexNode() {
	}

	public boolean containsChild(Character c) {
		return this.transitions.containsKey(c);
	}

	public NGramIndexNode getChild(Character c) {
		return this.transitions.get(c);
	}

	public synchronized void addOrIncrementChildAsync(Character firstLetter, int level, boolean isTerminal) {
		NGramIndexNode child = this.getChild(firstLetter);

		if (child == null) {
			this.putChild(firstLetter, isTerminal ? new TerminalNGramIndexNode(level) : new NGramIndexNode());

			child = this.getChild(firstLetter);
		}

		if (isTerminal) {
			if (!(child instanceof TerminalNGramIndexNode)) {
				TerminalNGramIndexNode newChild = new TerminalNGramIndexNode(level);

				for (Map.Entry<Character, NGramIndexNode> entry : child.getTransitions().entrySet()) {
					newChild.putChild(entry.getKey(), entry.getValue());
				}

				this.putChild(firstLetter, newChild);

				child = newChild;
			}

			((TerminalNGramIndexNode) child).increment();
		}
	}

	public void putChild(Character c, NGramIndexNode child) {
		if (!LOWERCASE_LETTERS.matcher(c.toString()).matches()) {
			throw new IllegalArgumentException(
					"Attempted to add a character to the Markov Model which is outside the range of "
							+ LOWERCASE_LETTERS);
		}

		this.transitions.put(c, child);
	}

	/**
	 * @return the transitions array
	 */
	public Map<Character, NGramIndexNode> getTransitions() {
		return this.transitions;
	}
}
