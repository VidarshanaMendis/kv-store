package edu.gmu.cs475.internal;

import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class OurTimeout implements TestRule {
	private final int fMillis;

	/**
	 * @param millis the millisecond timeout
	 */
	public OurTimeout(int millis) {
		fMillis = millis;
	}

	public Statement apply(Statement base, Description description) {
		return new FailOnTimeoutWithoutRacing(base, fMillis);
	}
}
