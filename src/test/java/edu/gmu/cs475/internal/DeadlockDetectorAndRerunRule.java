package edu.gmu.cs475.internal;

import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class DeadlockDetectorAndRerunRule implements TestRule {
	private final int fMillis;
	public static final int N_ITERATIONS = Integer.valueOf(System.getProperty("nIterations", "10"));

	/**
	 * @param millis
	 *            the millisecond timeout
	 */
	public DeadlockDetectorAndRerunRule(int millis) {
		fMillis = Integer.valueOf(System.getProperty("timeout","20000"));
	}

	public Statement apply(Statement base, Description description) {
		System.out.println("DeadlockDetector and Rerun Failing Tests rule loaded for " + description.getDisplayName()+ "; we will time out after "+fMillis+ "ms (per test), and will re-run each passing test "+ N_ITERATIONS + " times.");
		return new RetryOnSuccess(new FailOnTimeoutIfDeadlocked(base, fMillis), description);
	}
	
	class RetryOnSuccess extends Statement{

		private final Description description;
		private final Statement parent;
		public RetryOnSuccess(Statement parent,Description description) {
			this.parent =parent;
			this.description = description;
		}
		@Override
		public void evaluate() throws Throwable {
			for (int i = 0; i < N_ITERATIONS; i++) {
				try {
					parent.evaluate();
				} catch (Throwable t) {
					System.err.println(description.getDisplayName() + " failed on run " + (i + 1));
					throw t;
				}
			}
		}

	}
}
