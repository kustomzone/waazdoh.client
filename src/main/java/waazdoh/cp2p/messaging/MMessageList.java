package waazdoh.cp2p.messaging;

import java.util.LinkedList;
import java.util.List;

public final class MMessageList extends LinkedList<MMessage> {
	private static final long serialVersionUID = -4872410268546887589L;

	public MMessageList(List<MMessage> outgoingmessages) {
		super(outgoingmessages);
	}

	public MMessageList() {
		super();
	}

}
