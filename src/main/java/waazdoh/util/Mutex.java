package waazdoh.util;

public class Mutex {

	private Object o;
	private MLogger log = MLogger.getLogger(this);

	public Mutex(Object o) {
		this.o = o;
	}

	public void sleep(long time) {
		synchronized (this) {
			try {
				if (time < 10) {
					time = 10;
				}
				this.wait(time);
			} catch (InterruptedException e) {
				log.error(e);
			}
		}
	}

}
