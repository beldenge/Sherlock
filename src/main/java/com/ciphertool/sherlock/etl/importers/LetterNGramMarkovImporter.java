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
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.task.TaskExecutor;

import com.ciphertool.sherlock.dto.ParseResults;
import com.ciphertool.sherlock.markov.MarkovModel;

public class LetterNGramMarkovImporter implements MarkovImporter {
	private static Logger			log									= LoggerFactory.getLogger(LetterNGramMarkovImporter.class);

	private static final String		EXTENSION							= ".txt";
	private static final String		NON_ALPHA							= ".*[^a-z].*";
	private static final String		WHITESPACE_AND_INTER_SENTENCE_PUNC	= "[\\s-,;()`'\"]";
	private static final Pattern	PATTERN								= Pattern.compile(NON_ALPHA);

	private String					corpusDirectory;
	private Integer					minCount;
	private TaskExecutor			taskExecutor;
	private MarkovModel				markovModel;

	@Override
	@PostConstruct
	public MarkovModel importCorpus() {
		long start = System.currentTimeMillis();

		log.info("Starting corpus text import...");

		List<FutureTask<ParseResults>> futures = parseFiles(Paths.get(this.corpusDirectory));
		ParseResults parseResults;
		long total = 0L;
		long unique = 0L;

		for (FutureTask<ParseResults> future : futures) {
			try {
				parseResults = future.get();
				total += parseResults.getTotal();
				unique += parseResults.getUnique();
			} catch (InterruptedException ie) {
				log.error("Caught InterruptedException while waiting for ParseFileTask ", ie);
			} catch (ExecutionException ee) {
				log.error("Caught ExecutionException while waiting for ParseFileTask ", ee);
			}
		}

		log.info("Imported " + unique + " distinct letter N-Grams out of " + total + " total in "
				+ (System.currentTimeMillis() - start) + "ms");

		this.markovModel.postProcess(this.minCount, false, true);

		return this.markovModel;
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

			int order = markovModel.getLetterOrder();
			long total = 0;
			long unique = 0;

			try {
				String content = new String(Files.readAllBytes(this.path));

				content = content.toLowerCase().replaceAll(WHITESPACE_AND_INTER_SENTENCE_PUNC, "");

				for (int i = 0; i < content.length(); i++) {
					String nGramString = content.substring(i, i + Math.min(order, content.length() - i));

					if (PATTERN.matcher(nGramString).matches()) {
						continue;
					}

					unique += markovModel.addLetterTransition(nGramString, true) ? 1 : 0;
					total++;
				}
			} catch (IOException ioe) {
				log.error("Unable to parse file: " + this.path.toString(), ioe);
			}

			return new ParseResults(total, unique);
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
	 * @param markovModel
	 *            the markovModel to set
	 */
	@Required
	public void setMarkovModel(MarkovModel markovModel) {
		this.markovModel = markovModel;
	}
}
