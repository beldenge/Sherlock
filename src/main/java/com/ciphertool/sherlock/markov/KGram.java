package com.ciphertool.sherlock.markov;

import java.util.Arrays;

public class KGram {
	private Character[] kGram;

	/**
	 * @param kGram
	 */
	public KGram(Character[] kGram) {
		this.kGram = kGram;
	}

	public Integer size() {
		return this.kGram.length;
	}

	/**
	 * @return the kGram
	 */
	public Character[] getkGram() {
		return kGram;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(kGram);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof KGram)) {
			return false;
		}
		KGram other = (KGram) obj;
		if (!Arrays.equals(kGram, other.kGram)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "KGram: " + Arrays.toString(kGram);
	}
}
