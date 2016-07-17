package waazdoh.cp2p.network;

import java.util.List;

import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.messaging.MMessage;

public interface WNode {

	MNodeID getID();

	void close();

	void startClosing();

	void sendMessage(MMessage message);

	boolean isConnected();

	void addInfoTo(MMessage message);

	boolean isClosed();

	List<MMessage> getOutgoingMessages();

}
