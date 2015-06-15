package waazdoh.client.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Set;

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
					try {
						this.wait(timeout);
					} catch (Exception e1) {
						log.error(e1);
					}
				}

				while (true) {
					if (!checker.check()) {
						break;
					}

					ThreadMXBean bean = ManagementFactory.getThreadMXBean();
					long[] threadIds = bean.findDeadlockedThreads();
					if (threadIds != null) {
						ThreadInfo[] infos = bean.getThreadInfo(threadIds);

						for (ThreadInfo info : infos) {
							StackTraceElement[] stack = info.getStackTrace();
							for (StackTraceElement e : stack) {
								log.info("Thread " + info + " ("
										+ info.getLockName() + ") " + e);
							}
						}
					} else {
						log.info("No deadlocks.");
					}

					Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
					for (Thread t : threadSet) {
						StackTraceElement[] stackTrace = t.getStackTrace();
						logOutStacktrace(t, stackTrace);
					}

				}
			}

			private void logOutStacktrace(Thread t,
					StackTraceElement[] stackTrace) {
				for (StackTraceElement e : stackTrace) {
					log.info("Thread " + t + " (" + t.getState() + ") " + e);
				}
			}
		}).start();
	}

	public interface IChecker {
		public boolean check();
	}
}
