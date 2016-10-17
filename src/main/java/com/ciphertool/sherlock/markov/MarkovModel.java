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
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ciphertool.sherlock.etl.importers.MarkovImporterImpl;

public class MarkovModel {
	private static Logger						log		= LoggerFactory.getLogger(MarkovImporterImpl.class);

	private Map<KGram, ArrayList<Transition>>	model	= new HashMap<KGram, ArrayList<Transition>>();
	private int									order;

	public MarkovModel(int order) {
		this.order = order;
	}

	public void addTransition(Character[] characters, Character symbol) {
		if (characters.length != order) {
			log.error("Expected k-gram of order " + order + ", but a k-gram of order " + characters.length
					+ " was found.  Unable to add transition.");
		}

		KGram kGram = new KGram(characters);

		if (!model.containsKey(kGram)) {
			model.put(kGram, new ArrayList<Transition>());
		}

		ArrayList<Transition> transitions = model.get(kGram);

		Transition transition = new Transition(symbol);

		if (!transitions.contains(transition)) {
			transitions.add(transition);
		} else {
			transition = transitions.get(transitions.indexOf(transition));
		}

		transition.increment();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		for (KGram key : model.keySet()) {
			sb.append(key.toString());

			for (Transition transition : model.get(key)) {
				sb.append("\n\t -> " + transition.getSymbol() + ": " + transition.getCount());
			}

			sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * @return the model
	 */
	public Map<KGram, ArrayList<Transition>> getModel() {
		return model;
	}

	/**
	 * @return the order
	 */
	public int getOrder() {
		return order;
	}
}
