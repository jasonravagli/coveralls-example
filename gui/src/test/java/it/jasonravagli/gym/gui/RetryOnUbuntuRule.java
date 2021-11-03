package it.jasonravagli.gym.gui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.privatejgoodies.common.base.SystemUtils;

public class RetryOnUbuntuRule implements TestRule {
	
	private static final Logger LOGGER = LogManager.getLogger(RetryOnUbuntuRule.class);
	
    private int retryCount;

    public RetryOnUbuntuRule(int retryCount) {
        this.retryCount = retryCount;
    }

    public Statement apply(Statement base, Description description) {
        return statement(base, description);
    }

    private Statement statement(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Throwable caughtThrowable = null;

                // Retry to run the test only on Linux
                if(SystemUtils.IS_OS_LINUX) {
	                for (int i = 0; i < retryCount; i++) {
	                    try {
	                        base.evaluate();
	                        return;
	                    } catch (Throwable t) {
	                        caughtThrowable = t;
	                        LOGGER.warn(description.getDisplayName() + " - " + description.getMethodName() + ": run " + (i+1) + " failed");
	                        LOGGER.warn("Error: " + t.getMessage());
	                    }
	                }
	                LOGGER.error(description.getDisplayName() + " - " + description.getMethodName() + " : giving up after " + retryCount + " failures");
	                throw caughtThrowable;
                }
                else {
                	base.evaluate();
                }
            }
        };
    }
}
