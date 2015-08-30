package waazdoh.client.utils;

import waazdoh.common.WLogger;

public class ConditionWaiter {
	private boolean done;
	private int maxtime;
	private Condition c;

	private ConditionWaiter(final Condition c, final int nmaxtime) {
		this.c = c;
		this.maxtime = nmaxtime;
		if (maxtime <= 0) {
			maxtime = Integer.MAX_VALUE;
		}
	}

	private synchronized void loop() throws InterruptedException {
		long st = System.currentTimeMillis();

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

	public static ConditionWaiter wait(Condition c, int timeout) {
		ConditionWaiter waiter = new ConditionWaiter(c, timeout);
		try {
			waiter.loop();
		} catch (InterruptedException e) {
			WLogger.getLogger(waiter).error(e);
		}
		
		return waiter;
	}
}
