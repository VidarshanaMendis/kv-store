package edu.gmu.cs475.internal;

import org.junit.runners.model.Statement;

/**
 * JUnit's FailOnTimeout... but actually thread safe.
 */
public class FailOnTimeoutWithoutRacing extends Statement {
    private final Statement fOriginalStatement;

    private final long fTimeout;

    public FailOnTimeoutWithoutRacing(Statement originalStatement, long timeout) {
        fOriginalStatement = originalStatement;
        fTimeout = timeout;
    }

    @Override
    public void evaluate() throws Throwable {
        StatementThread thread = evaluateStatement();
        if (!thread.fFinished) {
            throwExceptionForUnfinishedThread(thread);
        }
    }

    private StatementThread evaluateStatement() throws InterruptedException {
        StatementThread thread = new StatementThread(fOriginalStatement);
        thread.start();
        thread.join(fTimeout);
        if (!thread.fFinished) {
            thread.recordStackTrace();
        }
        thread.interrupt();
        return thread;
    }

    private void throwExceptionForUnfinishedThread(StatementThread thread)
            throws Throwable {
        if (thread.fExceptionThrownByOriginalStatement != null) {
            throw thread.fExceptionThrownByOriginalStatement;
        } else {
            throwTimeoutException(thread);
        }
    }

    private void throwTimeoutException(StatementThread thread) throws Exception {
        Exception exception = new Exception(String.format(
                "test timed out after %d milliseconds", fTimeout));
        exception.setStackTrace(thread.getRecordedStackTrace());
        throw exception;
    }

    private static class StatementThread extends Thread {
        private final Statement fStatement;

        private volatile boolean fFinished = false;

        private volatile Throwable fExceptionThrownByOriginalStatement = null;

        private volatile StackTraceElement[] fRecordedStackTrace = null;

        public StatementThread(Statement statement) {
            fStatement = statement;
        }

        public void recordStackTrace() {
            fRecordedStackTrace = getStackTrace();
        }

        public StackTraceElement[] getRecordedStackTrace() {
            return fRecordedStackTrace;
        }

        @Override
        public void run() {
            try {
                fStatement.evaluate();
                fFinished = true;
            } catch (InterruptedException e) {
                // don't log the InterruptedException
            } catch (Throwable e) {
                fExceptionThrownByOriginalStatement = e;
            }
        }
    }
}