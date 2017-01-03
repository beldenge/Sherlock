package com.ciphertool.sherlock.dto;

public class ParseResults {
	private long	total;
	private long	orderTotal;
	private long	unique;

	/**
	 * @param total
	 *            the total count
	 * @param orderTotal
	 *            the orderTotal count
	 * @param unique
	 *            the unique count
	 */
	public ParseResults(long total, long orderTotal, long unique) {
		this.total = total;
		this.orderTotal = orderTotal;
		this.unique = unique;
	}

	/**
	 * @return the total
	 */
	public long getTotal() {
		return total;
	}

	/**
	 * @return the orderTotal
	 */
	public long getOrderTotal() {
		return orderTotal;
	}

	/**
	 * @return the unique
	 */
	public long getUnique() {
		return unique;
	}
}
