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
