package com.ciphertool.sherlock.markov;

import java.util.HashMap;
import java.util.Map;

public class KGramIndexNode {
	private int								level			= 0;
	private long							frequencyCount	= 0L;
	private double							ratio			= 0.0;
	private Map<Character, KGramIndexNode>	transitionMap	= new HashMap<Character, KGramIndexNode>();

	/**
	 * @param level
	 *            the level to set
	 */
	public KGramIndexNode(int level) {
		this.level = level;
	}

	public boolean containsChild(Character c) {
		return this.transitionMap.containsKey(c);
	}

	public KGramIndexNode getChild(Character c) {
		return this.transitionMap.get(c);
	}

	public void putChild(Character c, KGramIndexNode child) {
		this.transitionMap.put(c, child);
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
	 * @return the transitionMap
	 */
	public Map<Character, KGramIndexNode> getTransitionMap() {
		return transitionMap;
	}
}
