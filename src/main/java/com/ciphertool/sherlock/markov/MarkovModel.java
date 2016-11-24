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
import java.util.Map;
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

	private NGramIndexNode					rootNode			= new NGramIndexNode();
	private boolean							postProcessed		= false;
	private Integer							letterOrder;
	private Integer							wordOrder;
	private TaskExecutor					taskExecutor;

	/**
	 * A concurrent task for normalizing a Markov model node.
	 */
	protected class NormalizeTask implements Callable<Void> {
		private NGramIndexNode node;

		/**
		 * @param node
		 *            the NGramIndexNode to set
		 */
		public NormalizeTask(NGramIndexNode node) {
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
		private Character		key;
		private NGramIndexNode	node;

		/**
		 * @param key
		 *            the Character key to set
		 * @param node
		 *            the NGramIndexNode to set
		 */
		public LinkChildTask(Character key, NGramIndexNode node) {
			this.key = key;
			this.node = node;
		}

		@Override
		public Void call() throws Exception {
			linkChild(this.node, String.valueOf(this.key));

			return null;
		}
	}

	public boolean addLetterTransition(String nGramString, boolean alwaysTerminal) {
		return populateLetter(rootNode, nGramString, alwaysTerminal);
	}

	protected boolean populateLetter(NGramIndexNode currentNode, String nGramString, boolean alwaysTerminal) {
		Character firstLetter = nGramString.charAt(0);

		boolean isNew = currentNode.addOrIncrementChildAsync(firstLetter, letterOrder - (nGramString.length()
				- 1), alwaysTerminal || nGramString.length() == 1);

		if (nGramString.length() > 1) {
			return populateLetter(currentNode.getChild(firstLetter), nGramString.substring(1), alwaysTerminal);
		}

		return isNew;
	}

	public boolean addWordTransition(String nGramString, boolean alwaysTerminal, int level) {
		return populateWord(rootNode, nGramString, alwaysTerminal, level);
	}

	protected boolean populateWord(NGramIndexNode currentNode, String nGramString, boolean alwaysTerminal, int level) {
		Character firstLetter = nGramString.charAt(0);

		boolean isNew = currentNode.addOrIncrementChildAsync(firstLetter, level, alwaysTerminal
				|| nGramString.length() == 1);

		if (nGramString.length() > 1) {
			return populateWord(currentNode.getChild(firstLetter), nGramString.substring(1), alwaysTerminal, level);
		}

		return isNew;
	}

	public void postProcess(int minCount, boolean normalize, boolean linkChildren) {
		if (postProcessed) {
			return;
		}

		long start = System.currentTimeMillis();

		log.info("Starting Markov model post-processing...");

		Map<Character, NGramIndexNode> initialTransitions = this.rootNode.getTransitions();

		List<FutureTask<Void>> futures = new ArrayList<FutureTask<Void>>(26);
		FutureTask<Void> task;

		if (normalize) {
			for (Map.Entry<Character, NGramIndexNode> entry : initialTransitions.entrySet()) {
				if (entry.getValue() != null) {
					task = new FutureTask<Void>(new NormalizeTask(entry.getValue()));
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
		}

		if (minCount > 1) {
			removeOutliers(this.getRootNode(), minCount);
		}

		if (linkChildren) {
			futures = new ArrayList<FutureTask<Void>>(26);

			for (Map.Entry<Character, NGramIndexNode> entry : initialTransitions.entrySet()) {
				if (entry.getValue() != null) {
					task = new FutureTask<Void>(new LinkChildTask(entry.getKey(), entry.getValue()));
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
		}

		postProcessed = true;

		log.info("Time elapsed: " + (System.currentTimeMillis() - start) + "ms");
	}

	protected void removeOutliers(NGramIndexNode node, int minCount) {
		Map<Character, NGramIndexNode> transitions = node.getTransitions();

		List<Character> keysToRemove = new ArrayList<Character>();
		TerminalInfo terminalInfo;

		for (Map.Entry<Character, NGramIndexNode> entry : transitions.entrySet()) {
			if (entry.getValue() == null) {
				continue;
			}

			terminalInfo = entry.getValue().getTerminalInfo();

			if (terminalInfo != null && terminalInfo.getCount() < minCount) {
				keysToRemove.add(entry.getKey());

				continue;
			}

			removeOutliers(entry.getValue(), minCount);
		}

		for (Character key : keysToRemove) {
			transitions.remove(key);
		}
	}

	protected void normalize(NGramIndexNode node) {
		Map<Character, NGramIndexNode> transitions = node.getTransitions();

		if (transitions == null || transitions.isEmpty()) {
			return;
		}

		Long total = 0L;
		TerminalInfo terminalInfo;

		for (Map.Entry<Character, NGramIndexNode> entry : transitions.entrySet()) {
			terminalInfo = entry.getValue().getTerminalInfo();

			if (entry.getValue() != null && terminalInfo != null) {
				total += terminalInfo.getCount();
			}
		}

		for (Map.Entry<Character, NGramIndexNode> entry : transitions.entrySet()) {
			terminalInfo = entry.getValue().getTerminalInfo();

			if (terminalInfo != null) {
				terminalInfo.setRatio((double) terminalInfo.getCount() / (double) total);

				normalize(entry.getValue());
			}
		}
	}

	protected void linkChild(NGramIndexNode node, String nGram) {
		Map<Character, NGramIndexNode> transitions = node.getTransitions();

		if (nGram.length() == letterOrder) {
			for (Character letter : LOWERCASE_LETTERS) {
				NGramIndexNode match = this.findLongest(nGram.substring(1) + letter.toString());

				if (match != null) {
					node.putChild(letter, match);
				}
			}

			return;
		}

		for (Map.Entry<Character, NGramIndexNode> entry : transitions.entrySet()) {
			NGramIndexNode nextNode = entry.getValue();

			if (nextNode != null) {
				linkChild(nextNode, nGram + String.valueOf(entry.getKey()));
			}
		}
	}

	/**
	 * @param nGram
	 *            the N-Gram String to search by
	 * @return the longest matching NGramIndexNode
	 */
	public NGramIndexNode findLongest(String nGram) {
		return findLongestMatch(rootNode, nGram, null);
	}

	protected static NGramIndexNode findLongestMatch(NGramIndexNode node, String nGramString, NGramIndexNode longestMatch) {
		NGramIndexNode nextNode = node.getChild(nGramString.charAt(0));

		if (nextNode == null) {
			return longestMatch;
		}

		if (nextNode.getTerminalInfo() != null) {
			longestMatch = nextNode;
		}

		if (nGramString.length() == 1) {
			return longestMatch;
		}

		return findLongestMatch(nextNode, nGramString.substring(1), longestMatch);
	}

	/**
	 * @param nGram
	 *            the N-gram String to search by
	 * @return the longest matching String
	 */
	public String findLongestAsString(String nGram) {
		return findLongestMatchAsString(rootNode, nGram, "");
	}

	protected static String findLongestMatchAsString(NGramIndexNode node, String nGramString, String longestMatch) {
		NGramIndexNode nextNode = node.getChild(nGramString.charAt(0));

		if (nextNode == null) {
			return longestMatch;
		}

		if (nextNode.getTerminalInfo() != null) {
			longestMatch = longestMatch + nGramString.charAt(0);
		}

		if (nGramString.length() == 1) {
			return longestMatch;
		}

		return findLongestMatchAsString(nextNode, nGramString.substring(1), longestMatch);
	}

	/**
	 * @return the rootNode
	 */
	public NGramIndexNode getRootNode() {
		return rootNode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		Map<Character, NGramIndexNode> transitions = rootNode.getTransitions();

		for (Map.Entry<Character, NGramIndexNode> entry : transitions.entrySet()) {
			if (entry.getValue() != null) {
				appendTransitions("", entry.getKey(), entry.getValue(), sb);
			}
		}

		return sb.toString();
	}

	protected void appendTransitions(String parent, Character symbol, NGramIndexNode node, StringBuilder sb) {
		sb.append("\n[" + parent + "] ->" + symbol + " | "
				+ (node.getTerminalInfo() != null ? node.getTerminalInfo().getCount() : 0));

		Map<Character, NGramIndexNode> transitions = node.getTransitions();

		if (transitions == null || transitions.isEmpty()) {
			return;
		}

		for (Map.Entry<Character, NGramIndexNode> entry : transitions.entrySet()) {
			if (entry.getValue() != null) {
				appendTransitions(parent + entry.getKey(), entry.getKey(), entry.getValue(), sb);
			}
		}
	}

	/**
	 * @return the letterOrder
	 */
	public Integer getLetterOrder() {
		return letterOrder;
	}

	/**
	 * @param letterOrder
	 *            the letterOrder to set
	 */
	@Required
	public void setLetterOrder(Integer letterOrder) {
		this.letterOrder = letterOrder;
	}

	/**
	 * @return the wordOrder
	 */
	public Integer getWordOrder() {
		return wordOrder;
	}

	/**
	 * @param wordOrder
	 *            the wordOrder to set
	 */
	@Required
	public void setWordOrder(Integer wordOrder) {
		this.wordOrder = wordOrder;
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
