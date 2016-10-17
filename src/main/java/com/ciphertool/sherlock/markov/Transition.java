package com.ciphertool.sherlock.markov;

import java.math.BigDecimal;

public class Transition {
	private Character	symbol;
	private Long		frequencyCount;
	private BigDecimal	frequencyRatio;

	/**
	 * @param symbol
	 *            the symbol to set
	 */
	public Transition(Character symbol) {
		this.symbol = symbol;
		this.frequencyCount = 0L;
	}

	public void increment() {
		this.frequencyCount += 1L;
	}

	/**
	 * @return the symbol
	 */
	public Character getSymbol() {
		return symbol;
	}

	/**
	 * @return the count
	 */
	public Long getCount() {
		return frequencyCount;
	}

	/**
	 * @return the frequencyRatio
	 */
	public BigDecimal getFrequencyRatio() {
		return frequencyRatio;
	}

	/**
	 * @param frequencyRatio
	 *            the frequencyRatio to set
	 */
	public void setFrequencyRatio(BigDecimal frequencyRatio) {
		this.frequencyRatio = frequencyRatio;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
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
		if (!(obj instanceof Transition)) {
			return false;
		}
		Transition other = (Transition) obj;
		if (symbol == null) {
			if (other.symbol != null) {
				return false;
			}
		} else if (!symbol.equals(other.symbol)) {
			return false;
		}
		return true;
	}
}
