package waazdoh.cp2p;

public class NodeStatus {
	public static final int WARNING_TRESHOLD = 5;
	public static final long MAX_PINGDELAY = 10000;
	public static final long MIN_PINGDELAY = 200;
	public static final long MAX_DIE_TIME = 6 * MAX_PINGDELAY;

	private long lastping = System.currentTimeMillis();
	private long currentpingdelay;
	private long touch = System.currentTimeMillis();
	private int warning;
	private int receivedmessages;

	public boolean checkPing() {
		long maxpingdelay = getPingDelay();
		if (System.currentTimeMillis() - lastping > maxpingdelay) {
			lastping = System.currentTimeMillis();
			return true;
		} else {
			return false;
		}

	}

	private long getPingDelay() {
		if (this.currentpingdelay < MIN_PINGDELAY) {
			currentpingdelay = MIN_PINGDELAY;
		}
		return this.currentpingdelay;
	}

	public void pingSent() {
		this.currentpingdelay = getPingDelay() * 2;
		if (currentpingdelay > NodeStatus.MAX_PINGDELAY) {
			currentpingdelay = NodeStatus.MAX_PINGDELAY;
		}
	}

	public void messageReceived(String name) {
		receivedmessages++;
		touch();

		if (name.toLowerCase().indexOf("ping") < 0) {
			// received ping messages do not effect time between pings.
			this.currentpingdelay /= 2;
		}
	}

	public void touch() {
		this.touch = System.currentTimeMillis();
	}

	public void warning() {
		this.warning++;
	}

	public boolean shouldDie() {
		return (System.currentTimeMillis() - touch) > MAX_DIE_TIME || warning > WARNING_TRESHOLD;
	}

	public int getReceivedMessages() {
		return receivedmessages;
	}
}
