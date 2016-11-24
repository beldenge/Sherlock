package com.ciphertool.sherlock.dto;

public class ParseResults {
	private long	total;
	private long	unique;

	/**
	 * @param total
	 * @param unique
	 */
	public ParseResults(long total, long unique) {
		this.total = total;
		this.unique = unique;
	}

	/**
	 * @return the total
	 */
	public long getTotal() {
		return total;
	}

	/**
	 * @return the unique
	 */
	public long getUnique() {
		return unique;
	}
}
