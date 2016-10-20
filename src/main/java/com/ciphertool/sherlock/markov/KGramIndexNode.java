package com.ciphertool.sherlock.markov;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class KGramIndexNode {
	private Long							frequencyCount;
	private BigDecimal						frequencyRatio;
	private Map<Character, KGramIndexNode>	transitionMap	= new HashMap<Character, KGramIndexNode>();

	/**
	 * Default no-args constructor
	 */
	public KGramIndexNode() {
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
		this.frequencyCount = (this.frequencyCount == null) ? 1L : this.frequencyCount + 1L;
	}

	/**
	 * @return the count
	 */
	public Long getCount() {
		return frequencyCount;
	}

	/**
	 * @return the frequencyRatio
	 */
	public BigDecimal getFrequencyRatio() {
		return frequencyRatio;
	}

	/**
	 * @return the transitionMap
	 */
	public Map<Character, KGramIndexNode> getTransitionMap() {
		return transitionMap;
	}

	/**
	 * @param frequencyRatio
	 *            the frequencyRatio to set
	 */
	public void setFrequencyRatio(BigDecimal frequencyRatio) {
		this.frequencyRatio = frequencyRatio;
	}
}
