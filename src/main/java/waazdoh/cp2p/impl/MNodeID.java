package waazdoh.cp2p.impl;

import waazdoh.cutils.MStringID;

public final class MNodeID extends MStringID {

	public MNodeID(final String attribute) {
		super(attribute);
	}

	public MNodeID(MStringID idAttribute) {
		super(idAttribute.toString());
	}

}
