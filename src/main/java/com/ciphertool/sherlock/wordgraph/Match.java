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

package com.ciphertool.sherlock.wordgraph;

public class Match {
	private int		beginPos;
	private int		endPos;
	private String	word;

	public Match(int beginPos, int endPos, String word) {
		this.beginPos = beginPos;
		this.endPos = endPos;
		this.word = word;
	}

	public int getBeginPos() {
		return beginPos;
	}

	public final int getEndPos() {
		return endPos;
	}

	public final String getWord() {
		return word;
	}

	@Override
	public String toString() {
		return "Match [beginPos=" + beginPos + ", endPos=" + endPos + ", word=" + word + "]";
	}
}
