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
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

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

	@Override
	public MarkovModel importCorpus() {
		long start = System.currentTimeMillis();

		log.info("Starting corpus text import...");

		MarkovModel model = new MarkovModel(order);
		parseFiles(Paths.get(corpusDirectory), model);

		log.info("Time elapsed: " + (System.currentTimeMillis() - start) + "ms");

		model.postProcess(minCount);

		return model;
	}

	protected MarkovModel parseFiles(Path path, MarkovModel model) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path entry : stream) {
				if (Files.isDirectory(entry)) {
					parseFiles(entry, model);
				} else {
					String ext = entry.toString().substring(entry.toString().lastIndexOf('.'));

					if (!ext.equals(EXTENSION)) {
						log.info("Skipping file with unexpected file extension: " + entry.toString());

						continue;
					}

					parseFile(entry, model);
				}
			}
		} catch (IOException ioe) {
			log.error("Unable to parse files due to:" + ioe.getMessage(), ioe);
		}

		return model;
	}

	protected void parseFile(Path path, MarkovModel model) {
		log.debug("Importing file " + path.toString());

		try {
			String content = new String(Files.readAllBytes(path));

			content = content.replaceAll(WHITESPACE_AND_INTER_SENTENCE_PUNC, "").toLowerCase();

			for (int i = 0; i < content.length() - order; i++) {
				String kGramString = content.substring(i, i + order);

				if (PATTERN.matcher(kGramString + content.charAt(i + order)).matches()) {
					continue;
				}

				Character symbol = content.charAt(i + order);

				model.addTransition(kGramString, symbol);
			}
		} catch (IOException ioe) {
			log.error("Unable to parse file: " + path.toString(), ioe);
		}
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
