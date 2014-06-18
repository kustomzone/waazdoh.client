package waazdoh.client.model;

import waazdoh.util.MStringID;


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
