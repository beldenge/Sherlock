/**
 * Copyright 2015 George Belden
 * 
 * This file is part of DecipherEngine.
 * 
 * DecipherEngine is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * DecipherEngine is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * DecipherEngine. If not, see <http://www.gnu.org/licenses/>.
 */

package com.ciphertool.sherlock.markov;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarkovModel {
	private static Logger					log					= LoggerFactory.getLogger(MarkovModel.class);

	private static final List<Character>	LOWERCASE_LETTERS	= Arrays.asList(new Character[] { 'a', 'b', 'c', 'd',
			'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y',
			'z' });

	private KGramIndexNode					rootNode			= new KGramIndexNode(0);
	private int								order;
	private boolean							postProcessed		= false;

	public MarkovModel(int order) {
		this.order = order;
	}

	public void addTransition(String kGramString, Character symbol) {
		if (kGramString.length() != order) {
			log.error("Expected k-gram of order " + order + ", but a k-gram of order " + kGramString.length()
					+ " was found.  Unable to add transition.");
		}

		populateMap(rootNode, kGramString + symbol);
	}

	protected void populateMap(KGramIndexNode currentNode, String kGramString) {
		Character firstLetter = kGramString.charAt(0);

		if (!currentNode.containsChild(firstLetter)) {
			currentNode.putChild(firstLetter, new KGramIndexNode(order - (kGramString.length() - 2)));
		}

		currentNode.getChild(firstLetter).increment();

		if (kGramString.length() > 1) {
			populateMap(currentNode.getChild(firstLetter), kGramString.substring(1));
		}
	}

	public void postProcess() {
		if (postProcessed) {
			return;
		}

		long start = System.currentTimeMillis();

		log.info("Starting corpus post-processing...");

		Map<Character, KGramIndexNode> transitions = this.getRootNode().getTransitionMap();

		normalize(this.getRootNode());

		for (Character c : transitions.keySet()) {
			KGramIndexNode node = transitions.get(c);

			if (node != null) {
				linkChild(node, c.toString());
			}
		}

		postProcessed = true;

		log.info("Time elapsed: " + (System.currentTimeMillis() - start) + "ms");
	}

	protected void normalize(KGramIndexNode node) {
		Map<Character, KGramIndexNode> transitions = node.getTransitionMap();

		if (transitions == null || transitions.isEmpty()) {
			return;
		}

		Long total = 0L;
		for (Character c : transitions.keySet()) {
			KGramIndexNode child = transitions.get(c);

			total += child.getCount();
		}

		for (Character c : transitions.keySet()) {
			KGramIndexNode child = transitions.get(c);

			child.setRatio(Double.parseDouble(child.getCount().toString()) / Double.parseDouble(total.toString()));

			normalize(child);
		}
	}

	protected void linkChild(KGramIndexNode node, String kGram) {
		Map<Character, KGramIndexNode> transitions = node.getTransitionMap();

		if (kGram.length() > order) {
			for (Character letter : LOWERCASE_LETTERS) {
				KGramIndexNode match = this.find(kGram.substring(1) + letter.toString());

				if (match != null) {
					node.putChild(letter, match);
				}
			}

			return;
		}

		for (Character c : transitions.keySet()) {
			KGramIndexNode nextNode = transitions.get(c);

			if (nextNode != null) {
				linkChild(nextNode, kGram + c.toString());
			}
		}
	}

	/**
	 * @param kGram
	 *            the K-gram String to search by
	 * @return the matching KGramIndexNode
	 */
	public KGramIndexNode find(String kGram) {
		return findMatch(rootNode, kGram);
	}

	protected static KGramIndexNode findMatch(KGramIndexNode node, String kGramString) {
		KGramIndexNode nextNode = node.getChild(kGramString.charAt(0));

		if (nextNode == null) {
			return null;
		}

		if (kGramString.length() == 1) {
			return nextNode;
		}

		return findMatch(nextNode, kGramString.substring(1));
	}

	/**
	 * @param kGram
	 *            the K-gram String to search by
	 * @return the longest matching KGramIndexNode
	 */
	public KGramIndexNode findLongest(String kGram) {
		return findLongestMatch(rootNode, kGram);
	}

	protected static KGramIndexNode findLongestMatch(KGramIndexNode node, String kGramString) {
		KGramIndexNode nextNode = node.getChild(kGramString.charAt(0));

		if (nextNode == null) {
			return node;
		}

		if (kGramString.length() == 1) {
			return nextNode;
		}

		return findLongestMatch(nextNode, kGramString.substring(1));
	}

	/**
	 * @return the rootNode
	 */
	public KGramIndexNode getRootNode() {
		return rootNode;
	}

	/**
	 * @return the order
	 */
	public int getOrder() {
		return order;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		for (Character key : rootNode.getTransitionMap().keySet()) {
			appendTransitions("", key, rootNode.getTransitionMap().get(key), sb);
		}

		return sb.toString();
	}

	protected void appendTransitions(String parent, Character symbol, KGramIndexNode node, StringBuffer sb) {
		sb.append("\n[" + parent + "] ->" + symbol + " | " + node.getCount());

		if (node.getTransitionMap() == null || node.getTransitionMap().isEmpty()) {
			return;
		}

		for (Character key : node.getTransitionMap().keySet()) {
			appendTransitions(parent + key.toString(), key, rootNode.getTransitionMap().get(key), sb);
		}
	}
}
