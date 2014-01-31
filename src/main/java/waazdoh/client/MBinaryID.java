package waazdoh.client;

import waazdoh.cutils.MStringID;

public final class MBinaryID extends MStringID {

	public MBinaryID(final MStringID idValue) {
		super(idValue.toString());
	}

	public MBinaryID() {
		super();
	}

	public MBinaryID(final String sid) {
		super(sid);
	}
}
