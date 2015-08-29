package waazdoh.client.utils;

import waazdoh.common.WLogger;

public class ConditionWaiter {
	private boolean done;

	public ConditionWaiter(final Condition c, final int nmaxtime) {
		try {
			loop(c, nmaxtime);
		} catch (InterruptedException e) {
			WLogger.getLogger(this).error(e);
		}
	}

	private synchronized void loop(final Condition c, final int nmaxtime)
			throws InterruptedException {
		int maxtime = nmaxtime;
		long st = System.currentTimeMillis();
		if (maxtime <= 0) {
			maxtime = Integer.MAX_VALUE;
		}
		//
		while ((System.currentTimeMillis() - st) < maxtime && !c.test()) {
			this.wait(100);
		}

		done = true;
	}

	public boolean isDone() {
		return done;
	}

	public static interface Condition {
		boolean test();
	}
}
