package waazdoh.cp2p.impl;

import java.util.LinkedList;
import java.util.List;

public class MMessageList extends LinkedList<MMessage> {

	public MMessageList(List<MMessage> outgoingmessages) {
		super(outgoingmessages);
	}

	public MMessageList() {
		super();
	}

}
