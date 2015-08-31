package waazdoh.client.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import waazdoh.common.WLogger;

public class ThreadChecker {

	private IChecker checker;
	private WLogger log = WLogger.getLogger(this);

	public ThreadChecker(final IChecker checker) {
		this.checker = checker;
		start(checker, 20000);
	}

	public ThreadChecker(final IChecker checker, long timeout) {
		this.checker = checker;
		start(checker, timeout);
	}

	private void start(final IChecker checker, final long timeout) {
		new Thread(new Runnable() {
			public void run() {
				synchronized (this) {
					long st = System.currentTimeMillis();

					try {
						loop(checker, timeout, st);
					} catch (InterruptedException e) {
						log.error(e);
					}
				}
			}
		}).start();
	}

	private synchronized void loop(final IChecker checker, final long timeout,
			long st) throws InterruptedException {
		while (System.currentTimeMillis() - st < timeout) {
			if (!checker.check()) {
				break;
			}

			this.wait(timeout / 3);
			printOutLockedThreads();
		}
	}

	private void printOutLockedThreads() {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		long[] threadIds = bean.findDeadlockedThreads();
		if (threadIds != null) {
			ThreadInfo[] infos = bean.getThreadInfo(threadIds);

			for (ThreadInfo info : infos) {
				StackTraceElement[] stack = info.getStackTrace();
				for (StackTraceElement e : stack) {
					log.info("Thread " + info + " (" + info.getLockName()
							+ ") " + e);
				}
			}
		}
	}

	public interface IChecker {
		public boolean check();
	}
}
