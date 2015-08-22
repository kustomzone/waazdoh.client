package waazdoh.client.utils;

import waazdoh.common.WLogger;

public class ConditionWaiter {
	private boolean done;

	public ConditionWaiter(final Condition c, final int nmaxtime) {
		int maxtime = nmaxtime;
		long st = System.currentTimeMillis();
		if (maxtime <= 0) {
			maxtime = Integer.MAX_VALUE;
		}
		//
		while ((System.currentTimeMillis() - st) < maxtime && !c.test()) {
			doWait();
		}

		done = true;
	}

	public boolean isDone() {
		return done;
	}

	private void doWait() {
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				WLogger.getLogger(this).error(e);
			}
		}
	}

	public static interface Condition {
		boolean test();
	}
}
