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

import com.ciphertool.sherlock.enumerations.TerminalType;

public class TerminalInfo {
	private int				level	= 0;
	private long			count	= 0L;
	// TODO: consider using BigDecimal for better precision but with potentially significant memory overhead
	private double			ratio	= 0.0;
	private TerminalType	terminalType;

	/**
	 * Default no-args constructor
	 */
	public TerminalInfo() {
	}

	/**
	 * @param level
	 *            the level to set
	 * @param terminalType
	 *            the TerminalType to set
	 */
	public TerminalInfo(int level, TerminalType terminalType) {
		this.level = level;
		this.terminalType = terminalType;
	}

	public void increment() {
		this.count += 1L;
	}

	/**
	 * @return the count
	 */
	public long getCount() {
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
	 * @return the terminalType
	 */
	public TerminalType getTerminalType() {
		return terminalType;
	}

	/**
	 * @param terminalType
	 *            the terminalType to set
	 */
	public void setTerminalType(TerminalType terminalType) {
		this.terminalType = terminalType;
	}
}
