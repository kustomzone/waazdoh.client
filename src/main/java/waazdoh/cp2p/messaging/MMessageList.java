package waazdoh.cp2p.messaging;

import java.util.LinkedList;
import java.util.List;

public final class MMessageList extends LinkedList<MMessage> {

	public MMessageList(List<MMessage> outgoingmessages) {
		super(outgoingmessages);
	}

	public MMessageList() {
		super();
	}

}
