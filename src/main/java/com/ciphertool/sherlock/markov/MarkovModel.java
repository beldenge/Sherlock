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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ciphertool.sherlock.etl.importers.MarkovImporterImpl;

public class MarkovModel {
	private static Logger	log			= LoggerFactory.getLogger(MarkovImporterImpl.class);

	private KGramIndexNode	rootNode	= new KGramIndexNode();
	private int				order;

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

	public static void populateMap(KGramIndexNode currentNode, String kGramString) {
		Character firstLetter = kGramString.charAt(0);

		if (!currentNode.containsChild(firstLetter)) {
			currentNode.putChild(firstLetter, new KGramIndexNode());
		}

		currentNode.getChild(firstLetter).increment();

		if (kGramString.length() > 1) {
			populateMap(currentNode.getChild(firstLetter), kGramString.substring(1));
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		for (Character key : rootNode.getTransitionMap().keySet()) {
			appendTransitions("", key, rootNode.getTransitionMap().get(key), sb);
		}

		return sb.toString();
	}

	public void appendTransitions(String parent, Character symbol, KGramIndexNode node, StringBuffer sb) {
		sb.append("\n[" + parent + "] ->" + symbol + " | " + node.getCount());

		if (node.getTransitionMap() == null || node.getTransitionMap().isEmpty()) {
			return;
		}

		for (Character key : node.getTransitionMap().keySet()) {
			appendTransitions(parent + key.toString(), key, rootNode.getTransitionMap().get(key), sb);
		}
	}

	/**
	 * @param kGram
	 *            the K-gram String to search by
	 * @return the Map of transitions
	 */
	public KGramIndexNode find(String kGram) {
		return findMatch(rootNode, kGram);
	}

	public static KGramIndexNode findMatch(KGramIndexNode node, String kGramString) {
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
}
