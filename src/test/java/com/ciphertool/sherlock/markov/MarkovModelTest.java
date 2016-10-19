package com.ciphertool.sherlock.markov;

import java.util.ArrayList;
import java.util.Random;

import com.ciphertool.sherlock.etl.importers.MarkovImporterImpl;

public class MarkovModelTest {
	private static final int			ORDER	= 10;

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
		String begin = "happyhallo";
		sb.append(begin);

		Character[] kGramArray = new Character[ORDER];
		char[] kGram = new char[ORDER];
		begin.getChars(0, ORDER, kGram, 0);

		for (int i = 0; i < kGram.length; i++) {
			kGramArray[i] = kGram[i];
		}

		KGram root = new KGram(kGramArray);

		for (int i = 0; i < 100; i++) {
			ArrayList<Transition> transitions = model.getModel().get(root);

			if (transitions == null || transitions.isEmpty()) {
				System.out.println("Could not find transition for root: " + root);

				break;
			}

			Random rand = new Random();
			int randomIndex = rand.nextInt(transitions.size());
			Character nextSymbol = transitions.get(randomIndex).getSymbol();

			Character[] nextRoot = new Character[ORDER];
			Character[] currentRoot = root.getkGram();
			for (int j = 0; j < currentRoot.length - 1; j++) {
				nextRoot[j] = currentRoot[j + 1];
			}

			sb.append(nextSymbol);
			nextRoot[ORDER - 1] = nextSymbol;
			root = new KGram(nextRoot);
		}

		System.out.println(sb.toString());
	}
}
