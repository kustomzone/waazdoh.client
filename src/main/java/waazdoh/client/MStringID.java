package waazdoh.client;

import java.util.UUID;

public class MStringID {

	private String sid;

	public MStringID() {
		sid = UUID.randomUUID().toString();
	}

	public MStringID(String sid) {
		this.sid = sid.toString();
	}

	public String toString() {
		return sid;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (obj instanceof MStringID) {
			MStringID bid = (MStringID) obj;
			return bid.sid.equals(sid);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return sid.hashCode();
	}

}
