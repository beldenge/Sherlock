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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.task.TaskExecutor;

import com.ciphertool.sherlock.markov.MarkovModel;

public class WordNGramMarkovImporter implements MarkovImporter {
	private static Logger		log							= LoggerFactory.getLogger(WordNGramMarkovImporter.class);

	private static final String	EXTENSION					= ".txt";
	private static final String	WHITESPACE					= "[\\s]+";
	private static final String	NON_WHITESPACE_AND_ALPHA	= "[^a-z\\s]";

	private String				corpusDirectory;
	private Integer				minCount;
	private TaskExecutor		taskExecutor;
	private MarkovModel			wordkMarkovModel;

	@Override
	@PostConstruct
	public MarkovModel importCorpus() {
		long start = System.currentTimeMillis();

		log.info("Starting corpus text import...");

		List<FutureTask<Long>> futures = parseFiles(Paths.get(this.corpusDirectory));

		long total = 0;

		for (FutureTask<Long> future : futures) {
			try {
				total += future.get();
			} catch (InterruptedException ie) {
				log.error("Caught InterruptedException while waiting for ParseFileTask ", ie);
			} catch (ExecutionException ee) {
				log.error("Caught ExecutionException while waiting for ParseFileTask ", ee);
			}
		}

		log.info("Imported " + total + " word N-Grams in " + (System.currentTimeMillis() - start) + "ms");

		this.wordkMarkovModel.postProcess(this.minCount, false, false);

		return this.wordkMarkovModel;
	}

	/**
	 * A concurrent task for parsing a file into a Markov model.
	 */
	protected class ParseFileTask implements Callable<Long> {
		private Path path;

		/**
		 * @param path
		 *            the Path to set
		 */
		public ParseFileTask(Path path) {
			this.path = path;
		}

		@Override
		public Long call() throws Exception {
			log.debug("Importing file {}", this.path.toString());

			int order = wordkMarkovModel.getOrder();
			long total = 0;
			StringBuilder concatenated;

			try {
				String content = new String(Files.readAllBytes(this.path));

				content = content.toLowerCase().replaceAll(NON_WHITESPACE_AND_ALPHA, "");

				String[] words = content.split(WHITESPACE);

				for (int i = 0; i < words.length; i++) {
					for (int j = 1; j <= order; j++) {
						concatenated = new StringBuilder();

						for (int k = 0; k < Math.min(j, words.length - i); k++) {
							concatenated.append(words[i + k]);
						}

						if (concatenated.length() != 0) {
							wordkMarkovModel.addTransition(concatenated.toString(), false);
						}
					}

					total++;
				}
			} catch (IOException ioe) {
				log.error("Unable to parse file: " + this.path.toString(), ioe);
			}

			return total;
		}
	}

	protected List<FutureTask<Long>> parseFiles(Path path) {
		List<FutureTask<Long>> tasks = new ArrayList<FutureTask<Long>>();
		FutureTask<Long> task;
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

					task = new FutureTask<Long>(new ParseFileTask(entry));
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
	 * @param wordkMarkovModel
	 *            the wordkMarkovModel to set
	 */
	@Required
	public void setWordMarkovModel(MarkovModel wordkMarkovModel) {
		this.wordkMarkovModel = wordkMarkovModel;
	}
}
