package waazdoh.client.model;

import waazdoh.util.MStringID;


public final class BinaryID extends MStringID {

	public BinaryID(final MStringID idValue) {
		super(idValue.toString());
	}

	public BinaryID() {
		super();
	}

	public BinaryID(final String sid) {
		super(sid);
	}
}
