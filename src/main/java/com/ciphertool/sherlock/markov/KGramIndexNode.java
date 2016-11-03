package com.ciphertool.sherlock.markov;

public class KGramIndexNode {
	private static final int	MINIMUM_ASCII_VALUE	= 97;
	private static final int	MAXIMUM_ASCII_VALUE	= 122;

	private int					level				= 0;
	private long				frequencyCount		= 0L;
	private double				ratio				= 0.0;
	private char				letter;
	private KGramIndexNode[]	transitions			= new KGramIndexNode[26];

	/**
	 * @param level
	 *            the level to set
	 */
	public KGramIndexNode(int level) {
		this.level = level;
	}

	public boolean containsChild(char c) {
		return this.transitions[resolveIndex(c)] != null;
	}

	public KGramIndexNode getChild(char c) {
		return this.transitions[resolveIndex(c)];
	}

	public void putChild(char c, KGramIndexNode child) {
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
	 * @param ratio
	 *            the ratio to set
	 */
	public void setRatio(double ratio) {
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
	public KGramIndexNode[] getTransitions() {
		return transitions;
	}
}
