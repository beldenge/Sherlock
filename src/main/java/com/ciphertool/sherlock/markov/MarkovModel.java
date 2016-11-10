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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.task.TaskExecutor;

public class MarkovModel {
	private static Logger					log					= LoggerFactory.getLogger(MarkovModel.class);

	private static final List<Character>	LOWERCASE_LETTERS	= Arrays.asList(new Character[] { 'a', 'b', 'c', 'd',
			'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y',
			'z' });

	private KGramIndexNode					rootNode			= new KGramIndexNode(0);
	private boolean							postProcessed		= false;
	private Integer							order;
	private TaskExecutor					taskExecutor;

	/**
	 * A concurrent task for normalizing a Markov model node.
	 */
	protected class NormalizeTask implements Callable<Void> {
		private KGramIndexNode node;

		public NormalizeTask() {
		}

		/**
		 * @param node
		 *            the KGramIndexNode to set
		 */
		public NormalizeTask(KGramIndexNode node) {
			this.node = node;
		}

		@Override
		public Void call() throws Exception {
			normalize(this.node);

			return null;
		}
	}

	/**
	 * A concurrent task for linking leaf nodes in a Markov model.
	 */
	protected class LinkChildTask implements Callable<Void> {
		private KGramIndexNode node;

		public LinkChildTask() {
		}

		/**
		 * @param node
		 *            the KGramIndexNode to set
		 */
		public LinkChildTask(KGramIndexNode node) {
			this.node = node;
		}

		@Override
		public Void call() throws Exception {
			linkChild(this.node, String.valueOf(this.node.getLetter()));

			return null;
		}
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

		currentNode.addOrIncrementChildAsync(firstLetter, order - (kGramString.length() - 2));

		if (kGramString.length() > 1) {
			populateMap(currentNode.getChild(firstLetter), kGramString.substring(1));
		}
	}

	public void postProcess(int minCount) {
		if (postProcessed) {
			return;
		}

		long start = System.currentTimeMillis();

		log.info("Starting Markov model post-processing...");

		KGramIndexNode[] initialTransitions = this.rootNode.getTransitions();

		List<FutureTask<Void>> futures = new ArrayList<FutureTask<Void>>(26);
		FutureTask<Void> task;

		for (int i = 0; i < initialTransitions.length; i++) {
			KGramIndexNode node = initialTransitions[i];

			if (node != null) {
				task = new FutureTask<Void>(new NormalizeTask(node));
				futures.add(task);
				this.taskExecutor.execute(task);
			}
		}

		for (FutureTask<Void> future : futures) {
			try {
				future.get();
			} catch (InterruptedException ie) {
				log.error("Caught InterruptedException while waiting for NormalizeTask ", ie);
			} catch (ExecutionException ee) {
				log.error("Caught ExecutionException while waiting for NormalizeTask ", ee);
			}
		}

		if (minCount > 1) {
			removeOutliers(this.getRootNode(), minCount);
		}

		futures = new ArrayList<FutureTask<Void>>(26);

		for (int i = 0; i < initialTransitions.length; i++) {
			KGramIndexNode node = initialTransitions[i];

			if (node != null) {
				task = new FutureTask<Void>(new LinkChildTask(node));
				futures.add(task);
				this.taskExecutor.execute(task);
			}
		}

		for (FutureTask<Void> future : futures) {
			try {
				future.get();
			} catch (InterruptedException ie) {
				log.error("Caught InterruptedException while waiting for LinkChildTask ", ie);
			} catch (ExecutionException ee) {
				log.error("Caught ExecutionException while waiting for LinkChildTask ", ee);
			}
		}

		postProcessed = true;

		log.info("Time elapsed: " + (System.currentTimeMillis() - start) + "ms");
	}

	protected void removeOutliers(KGramIndexNode node, int minCount) {
		KGramIndexNode[] transitions = node.getTransitions();

		for (int i = 0; i < transitions.length; i++) {
			if (transitions[i] == null) {
				continue;
			}

			if (transitions[i].getCount() < minCount) {
				transitions[i] = null;

				continue;
			}

			removeOutliers(transitions[i], minCount);
		}
	}

	protected void normalize(KGramIndexNode node) {
		KGramIndexNode[] transitions = node.getTransitions();

		if (transitions == null || transitions.length == 0) {
			return;
		}

		Long total = 0L;
		for (int i = 0; i < transitions.length; i++) {
			KGramIndexNode child = transitions[i];

			if (child != null) {
				total += child.getCount();
			}
		}

		for (int i = 0; i < transitions.length; i++) {
			KGramIndexNode child = transitions[i];

			if (child != null) {
				child.setRatio((double) child.getCount() / (double) total);

				normalize(child);
			}
		}
	}

	protected void linkChild(KGramIndexNode node, String kGram) {
		KGramIndexNode[] transitions = node.getTransitions();

		if (kGram.length() > order) {
			for (Character letter : LOWERCASE_LETTERS) {
				KGramIndexNode match = this.find(kGram.substring(1) + letter.toString());

				if (match != null) {
					node.putChild(letter, match);
				}
			}

			return;
		}

		for (int i = 0; i < transitions.length; i++) {
			KGramIndexNode nextNode = transitions[i];

			if (nextNode != null) {
				linkChild(nextNode, kGram + String.valueOf(nextNode.getLetter()));
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

		KGramIndexNode[] transitions = rootNode.getTransitions();

		for (int i = 0; i < transitions.length; i++) {
			if (transitions[i] != null) {
				appendTransitions("", transitions[i].getLetter(), transitions[i], sb);
			}
		}

		return sb.toString();
	}

	protected void appendTransitions(String parent, Character symbol, KGramIndexNode node, StringBuffer sb) {
		sb.append("\n[" + parent + "] ->" + symbol + " | " + node.getCount());

		KGramIndexNode[] transitions = node.getTransitions();

		if (transitions == null || transitions.length == 0) {
			return;
		}

		for (int i = 0; i < transitions.length; i++) {
			if (transitions[i] != null) {
				appendTransitions(parent + transitions[i].getLetter(), transitions[i].getLetter(), transitions[i], sb);
			}
		}
	}

	/**
	 * @param order
	 *            the order to set
	 */
	@Required
	public void setOrder(Integer order) {
		this.order = order;
	}

	/**
	 * @param taskExecutor
	 *            the taskExecutor to set
	 */
	@Required
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
}
