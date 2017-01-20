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

package com.ciphertool.sherlock.etl.importers;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.task.TaskExecutor;

import com.ciphertool.sherlock.MathConstants;
import com.ciphertool.sherlock.dto.ParseResults;
import com.ciphertool.sherlock.markov.MarkovModel;
import com.ciphertool.sherlock.markov.NGramIndexNode;
import com.ciphertool.sherlock.markov.TerminalInfo;

public class LetterNGramMarkovImporter implements MarkovImporter {
	private static Logger		log			= LoggerFactory.getLogger(LetterNGramMarkovImporter.class);

	private static final String	EXTENSION	= ".txt";
	private static final String	NON_ALPHA	= "[^a-zA-Z]";

	private String				corpusDirectory;
	private Integer				minCount;
	private TaskExecutor		taskExecutor;
	private MarkovModel			letterMarkovModel;

	@Override
	@PostConstruct
	public MarkovModel importCorpus() {
		long start = System.currentTimeMillis();

		log.info("Starting corpus text import...");

		List<FutureTask<ParseResults>> futures = parseFiles(Paths.get(this.corpusDirectory));
		ParseResults parseResults;
		long total = 0L;
		long orderTotal = 0L;
		long unique = 0L;

		for (FutureTask<ParseResults> future : futures) {
			try {
				parseResults = future.get();
				total += parseResults.getTotal();
				orderTotal += parseResults.getOrderTotal();
				unique += parseResults.getUnique();
			} catch (InterruptedException ie) {
				log.error("Caught InterruptedException while waiting for ParseFileTask ", ie);
			} catch (ExecutionException ee) {
				log.error("Caught ExecutionException while waiting for ParseFileTask ", ee);
			}
		}

		log.info("Imported " + unique + " distinct letter N-Grams out of " + total + " total in "
				+ (System.currentTimeMillis() - start) + "ms");

		this.letterMarkovModel.getRootNode().setTerminalInfo(new TerminalInfo(0, total));

		this.letterMarkovModel.postProcess(true, true);

		for (Map.Entry<Character, NGramIndexNode> entry : this.letterMarkovModel.getRootNode().getTransitions().entrySet()) {
			log.info(entry.getKey().toString() + ": "
					+ entry.getValue().getTerminalInfo().getConditionalProbability().toString().substring(0, Math.min(7, entry.getValue().getTerminalInfo().getConditionalProbability().toString().length())));
		}

		normalize(this.letterMarkovModel, orderTotal);

		if (minCount > 1) {
			removeOutliers(this.letterMarkovModel.getRootNode(), this.minCount);
		}

		return this.letterMarkovModel;
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

	/**
	 * A concurrent task for normalizing a Markov model node.
	 */
	protected class NormalizeTask implements Callable<Void> {
		private NGramIndexNode	node;
		private long			total;

		/**
		 * @param node
		 *            the NGramIndexNode to set
		 * @param total
		 *            the total to set
		 */
		public NormalizeTask(NGramIndexNode node, long total) {
			this.node = node;
			this.total = total;
		}

		@Override
		public Void call() throws Exception {
			normalizeTerminal(this.node, this.total);

			return null;
		}
	}

	protected void normalizeTerminal(NGramIndexNode node, long total) {
		if (node.getTerminalInfo() != null && node.getTerminalInfo().getLevel() == this.letterMarkovModel.getOrder()) {
			node.getTerminalInfo().setProbability(BigDecimal.valueOf(node.getTerminalInfo().getCount()).divide(BigDecimal.valueOf(total), MathConstants.PREC_10_HALF_UP));

			return;
		}

		Map<Character, NGramIndexNode> transitions = node.getTransitions();

		if (transitions == null || transitions.isEmpty()) {
			return;
		}

		for (Map.Entry<Character, NGramIndexNode> entry : transitions.entrySet()) {
			normalizeTerminal(entry.getValue(), total);
		}
	}

	protected void normalize(MarkovModel markovModel, long orderTotal) {
		List<FutureTask<Void>> futures = new ArrayList<FutureTask<Void>>(26);
		FutureTask<Void> task;

		for (Map.Entry<Character, NGramIndexNode> entry : markovModel.getRootNode().getTransitions().entrySet()) {
			if (entry.getValue() != null) {
				// Add one for unknown words
				task = new FutureTask<Void>(new NormalizeTask(entry.getValue(), orderTotal + 1));
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

	/**
	 * A concurrent task for parsing a file into a Markov model.
	 */
	protected class ParseFileTask implements Callable<ParseResults> {
		private Path path;

		/**
		 * @param path
		 *            the Path to set
		 */
		public ParseFileTask(Path path) {
			this.path = path;
		}

		@Override
		public ParseResults call() throws Exception {
			log.debug("Importing file {}", this.path.toString());

			int order = letterMarkovModel.getOrder();
			long total = 0;
			long orderTotal = 0;
			long unique = 0;

			try {
				String content = new String(Files.readAllBytes(this.path));
				String sentence;

				String[] sentences = content.split("(\n|\r|\r\n)+");

				for (int i = 0; i < sentences.length; i++) {
					sentence = sentences[i].replaceAll(NON_ALPHA, "").toLowerCase();

					for (int j = 0; j < sentence.length(); j++) {
						String nGramString = sentence.substring(j, j + Math.min(order, sentence.length() - j));

						unique += (letterMarkovModel.addLetterTransition(nGramString) ? 1 : 0);
						total++;

						if (nGramString.length() == order) {
							orderTotal++;
						}
					}
				}
			} catch (IOException ioe) {
				log.error("Unable to parse file: " + this.path.toString(), ioe);
			}

			return new ParseResults(total, orderTotal, unique);
		}
	}

	protected List<FutureTask<ParseResults>> parseFiles(Path path) {
		List<FutureTask<ParseResults>> tasks = new ArrayList<FutureTask<ParseResults>>();
		FutureTask<ParseResults> task;
		String filename;

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path entry : stream) {
				if (Files.isDirectory(entry)) {
					tasks.addAll(parseFiles(entry));
				} else {
					filename = entry.toString();
					String ext = filename.substring(filename.lastIndexOf('.'));

					if (!ext.equals(EXTENSION)) {
						log.info("Skipping file with unexpected file extension: " + filename);

						continue;
					}

					task = new FutureTask<ParseResults>(new ParseFileTask(entry));
					tasks.add(task);
					this.taskExecutor.execute(task);
				}
			}
		} catch (IOException ioe) {
			log.error("Unable to parse files due to:" + ioe.getMessage(), ioe);
		}

		return tasks;
	}

	/**
	 * @param taskExecutor
	 *            the taskExecutor to set
	 */
	@Required
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * @param fileName
	 *            the fileName to set
	 */
	@Required
	public void setCorpusDirectory(String corpusDirectory) {
		this.corpusDirectory = corpusDirectory;
	}

	/**
	 * @param minCount
	 *            the minCount to set
	 */
	@Required
	public void setMinCount(Integer minCount) {
		this.minCount = minCount;
	}

	/**
	 * @param letterMarkovModel
	 *            the letterMarkovModel to set
	 */
	@Required
	public void setLetterMarkovModel(MarkovModel letterMarkovModel) {
		this.letterMarkovModel = letterMarkovModel;
	}
}
