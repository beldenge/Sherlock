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

import java.util.Map;
import java.util.Random;

import com.ciphertool.sherlock.etl.importers.MarkovImporterImpl;

public class MarkovModelTest {
	private static final int			ORDER	= 6;

	private static MarkovImporterImpl	importer;
	private static MarkovModel			model;

	// @BeforeClass
	public static void setUp() {
		importer = new MarkovImporterImpl();
		importer.setCorpusDirectory("src/main/data/corpus");
		importer.setOrder(ORDER);

		model = importer.importCorpus();
	}

	// @Test
	public void generate() {
		StringBuffer sb = new StringBuffer();
		String root = "happyh";
		sb.append(root);

		for (int i = 0; i < 100; i++) {
			Map<Character, KGramIndexNode> transitions = model.getTransitions(root);

			if (transitions == null || transitions.isEmpty()) {
				System.out.println("Could not find transition for root: " + root);

				break;
			}

			Random rand = new Random();
			int randomIndex = rand.nextInt(transitions.size());

			Character nextSymbol = (Character) transitions.keySet().toArray()[randomIndex];
			sb.append(nextSymbol);

			root = root.substring(1) + nextSymbol;
		}

		System.out.println(sb.toString());
	}
}
