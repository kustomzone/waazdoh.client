package waazdoh.cp2p.impl;

import waazdoh.cutils.MStringID;

public class MNodeID extends MStringID {

	public MNodeID(String attribute) {
		super(attribute);
	}

	public MNodeID(MStringID idAttribute) {
		super(idAttribute.toString());
	}

}
