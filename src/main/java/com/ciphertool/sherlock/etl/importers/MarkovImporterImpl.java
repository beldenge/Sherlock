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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.task.TaskExecutor;

import com.ciphertool.sherlock.markov.MarkovModel;

public class MarkovImporterImpl implements MarkovImporter {
	private static Logger			log									= LoggerFactory.getLogger(MarkovImporterImpl.class);

	private static final String		EXTENSION							= ".txt";
	private static final String		NON_ALPHA							= ".*[^a-z].*";
	private static final String		WHITESPACE_AND_INTER_SENTENCE_PUNC	= "[\\s-,;()`'\"]";
	private static final Pattern	PATTERN								= Pattern.compile(NON_ALPHA);

	private String					corpusDirectory;
	private Integer					order;
	private Integer					minCount;
	private TaskExecutor			taskExecutor;

	@Override
	public MarkovModel importCorpus() {
		long start = System.currentTimeMillis();

		log.info("Starting corpus text import...");

		MarkovModel model = new MarkovModel(order);

		List<FutureTask<Void>> futures = parseFiles(Paths.get(corpusDirectory), model);

		for (FutureTask<Void> future : futures) {
			try {
				future.get();
			} catch (InterruptedException ie) {
				log.error("Caught InterruptedException while waiting for ParseFileTask ", ie);
			} catch (ExecutionException ee) {
				log.error("Caught ExecutionException while waiting for ParseFileTask ", ee);
			}
		}

		log.info("Time elapsed: " + (System.currentTimeMillis() - start) + "ms");

		model.postProcess(minCount);

		return model;
	}

	/**
	 * A concurrent task for parsing a file into a Markov model.
	 */
	protected class ParseFileTask implements Callable<Void> {
		private MarkovModel	model;
		private Path		path;

		/**
		 * @param model
		 *            the MarkovModel to set
		 * @param path
		 *            the Path to set
		 */
		public ParseFileTask(MarkovModel model, Path path) {
			this.model = model;
			this.path = path;
		}

		@Override
		public Void call() throws Exception {
			log.debug("Importing file " + this.path.toString());

			try {
				String content = new String(Files.readAllBytes(this.path));

				content = content.replaceAll(WHITESPACE_AND_INTER_SENTENCE_PUNC, "").toLowerCase();

				for (int i = 0; i < content.length() - order; i++) {
					String kGramString = content.substring(i, i + order);

					if (PATTERN.matcher(kGramString + content.charAt(i + order)).matches()) {
						continue;
					}

					Character symbol = content.charAt(i + order);

					this.model.addTransition(kGramString, symbol);
				}
			} catch (IOException ioe) {
				log.error("Unable to parse file: " + this.path.toString(), ioe);
			}

			return null;
		}
	}

	protected List<FutureTask<Void>> parseFiles(Path path, MarkovModel model) {
		List<FutureTask<Void>> tasks = new ArrayList<FutureTask<Void>>();
		FutureTask<Void> task;

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path entry : stream) {
				if (Files.isDirectory(entry)) {
					tasks.addAll(parseFiles(entry, model));
				} else {
					String ext = entry.toString().substring(entry.toString().lastIndexOf('.'));

					if (!ext.equals(EXTENSION)) {
						log.info("Skipping file with unexpected file extension: " + entry.toString());

						continue;
					}

					task = new FutureTask<Void>(new ParseFileTask(model, entry));
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
	 * @param order
	 *            the order to set
	 */
	@Required
	public void setOrder(Integer order) {
		this.order = order;
	}

	/**
	 * @param minCount
	 *            the minCount to set
	 */
	@Required
	public void setMinCount(Integer minCount) {
		this.minCount = minCount;
	}
}
