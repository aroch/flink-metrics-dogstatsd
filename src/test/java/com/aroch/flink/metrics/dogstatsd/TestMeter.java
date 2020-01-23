package com.aroch.flink.metrics.dogstatsd;

import org.apache.flink.metrics.Meter;

/**
 * A dummy {@link Meter} implementation.
 */
public class TestMeter implements Meter {
	private final long countValue;
	private final double rateValue;

	public TestMeter() {
		this(100, 5);
	}

	public TestMeter(long countValue, double rateValue) {
		this.countValue = countValue;
		this.rateValue = rateValue;
	}

	@Override
	public void markEvent() {
	}

	@Override
	public void markEvent(long n) {
	}

	@Override
	public double getRate() {
		return rateValue;
	}

	@Override
	public long getCount() {
		return countValue;
	}
}
