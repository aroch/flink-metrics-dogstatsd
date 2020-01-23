package com.aroch.flink.metrics.dogstatsd;

import org.apache.flink.metrics.Counter;

/**
 * A dummy {@link Counter} implementation.
 */
public class TestCounter implements Counter {
	private long countValue;

	public TestCounter() {
		this.countValue = 0;
	}

	public TestCounter(long countValue) {
		this.countValue = countValue;
	}

	@Override
	public void inc() {
		countValue++;
	}

	@Override
	public void inc(long n) {
		countValue += n;
	}

	@Override
	public void dec() {
		countValue--;
	}

	@Override
	public void dec(long n) {
		countValue -= n;
	}

	@Override
	public long getCount() {
		return countValue;
	}
}
