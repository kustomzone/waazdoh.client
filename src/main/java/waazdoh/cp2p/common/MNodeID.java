package waazdoh.cp2p.common;

import waazdoh.common.MStringID;

public final class MNodeID extends MStringID {

	public MNodeID(final String attribute) {
		super(attribute);
	}

	public MNodeID(MStringID idAttribute) {
		super(idAttribute.toString());
	}

}
