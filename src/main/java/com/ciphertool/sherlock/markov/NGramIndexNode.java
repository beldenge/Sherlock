package com.ciphertool.sherlock.markov;

public class NGramIndexNode {
	private static final int	MINIMUM_ASCII_VALUE	= 97;
	private static final int	MAXIMUM_ASCII_VALUE	= 122;

	private int					level				= 0;
	private long				frequencyCount		= 0L;
	private double				ratio				= 0.0;
	private char				letter;
	private NGramIndexNode[]	transitions			= new NGramIndexNode[26];

	/**
	 * @param level
	 *            the level to set
	 */
	public NGramIndexNode(int level) {
		this.level = level;
	}

	public boolean containsChild(char c) {
		return this.transitions[resolveIndex(c)] != null;
	}

	public NGramIndexNode getChild(char c) {
		return this.transitions[resolveIndex(c)];
	}

	public synchronized void addOrIncrementChildAsync(Character firstLetter, int level) {
		if (!this.containsChild(firstLetter)) {
			this.putChild(firstLetter, new NGramIndexNode(level));
		}

		this.getChild(firstLetter).increment();
	}

	public void putChild(char c, NGramIndexNode child) {
		if ((int) c < MINIMUM_ASCII_VALUE || (int) c > MAXIMUM_ASCII_VALUE) {
			throw new IllegalArgumentException(
					"Attempted to add a character to the Markov Model which is outside the range of ["
							+ (char) MINIMUM_ASCII_VALUE + "-" + (char) MINIMUM_ASCII_VALUE + "]");
		}

		child.setLetter(c);

		this.transitions[resolveIndex(c)] = child;
	}

	private static int resolveIndex(char c) {
		return ((int) c) - MINIMUM_ASCII_VALUE;
	}

	public void increment() {
		this.frequencyCount += 1L;
	}

	/**
	 * @return the count
	 */
	public Long getCount() {
		return frequencyCount;
	}

	/**
	 * @return the level
	 */
	public int getLevel() {
		return level;
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
	 * @return the letter
	 */
	public char getLetter() {
		return letter;
	}

	/**
	 * @param letter
	 *            the letter to set
	 */
	public void setLetter(char letter) {
		this.letter = letter;
	}

	/**
	 * @return the transitions array
	 */
	public NGramIndexNode[] getTransitions() {
		return transitions;
	}
}
