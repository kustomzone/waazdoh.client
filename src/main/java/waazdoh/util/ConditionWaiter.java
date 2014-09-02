package waazdoh.util;

public class ConditionWaiter {
	public ConditionWaiter(Condition c, int maxtime) {
		long st = System.currentTimeMillis();
		if (maxtime <= 0) {
			maxtime = Integer.MAX_VALUE;
		}
		//
		while ((System.currentTimeMillis() - st) < maxtime && !c.test()) {
			doWait();
		}
	}

	private void doWait() {
		synchronized (this) {
			try {
				this.wait(100);
			} catch (InterruptedException e) {
				MLogger.getLogger(this).error(e);
			}
		}
	}

	public static interface Condition {
		boolean test();
	}
}