package waazdoh.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class MStringID {

	private String sid;

	public MStringID() {
		Date date = Calendar.getInstance().getTime();
		SimpleDateFormat d = new SimpleDateFormat("yyyyMM");
		sid = d.format(date) + UUID.randomUUID().toString();
	}

	public MStringID(final String sid) {
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
