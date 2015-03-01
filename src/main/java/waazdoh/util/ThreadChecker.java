package waazdoh.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Set;

public class ThreadChecker {

	private IChecker checker;
	private MLogger log = MLogger.getLogger(this);

	public ThreadChecker(final IChecker checker) {
		this.checker = checker;

		new Thread(new Runnable() {

			@Override
			public void run() {
				synchronized (this) {
					try {
						this.wait(20000);
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
