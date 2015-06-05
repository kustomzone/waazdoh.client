/*******************************************************************************
 * Copyright (c) 2013 Juuso Vilmunen.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Juuso Vilmunen - initial API and implementation
 ******************************************************************************/
package waazdoh.cp2p.network;

import java.util.LinkedList;
import java.util.List;

import waazdoh.common.WLogger;
import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.common.WMessenger;
import waazdoh.cp2p.messaging.MMessage;

public final class PassiveNode implements WNode {
	private static final int MAX_MESSAGES_COUNT = 20;
	private MNodeID id;
	//
	private WLogger log = WLogger.getLogger(this);
	private WMessenger source;

	private int outputbytecount;

	private List<MMessage> outgoingmessages = new LinkedList<MMessage>();

	private boolean closed;

	public PassiveNode(MNodeID id2, WMessenger source) {
		this.id = id2;
		log.info("PassiveNode with id " + id);
		this.source = source;
	}

	@Override
	public String toString() {
		return "Node[" + getID() + "][" + outputbytecount + "B]";
	}

	@Override
	public void sendMessage(MMessage message) {
		addOutgoingMessage(message);
	}

	public void addOutgoingMessage(MMessage b) {
		if (this.outgoingmessages.size() > MAX_MESSAGES_COUNT) {
			log.info("Message queue full. Not sending message." + b);
		} else if (findMessage(b)) {
			log.info("Message already added to queue");
		} else if (isClosed()) {
			log.info("Node closed. Not adding message.");
		} else {
			addMessageToQueue(b);
		}
	}

	private void addMessageToQueue(MMessage b) {
		synchronized (outgoingmessages) {
			if (isConnected()) {
				// Not logging before if we are connected
				log.info("addMessage " + b);
			}
			b.setLastHandler(source.getID());
			this.outgoingmessages.add(b);
		}
		//
		source.addResponseListener(b.getID(), b.getResponseListener());
	}

	private boolean findMessage(MMessage b) {
		for (MMessage m : outgoingmessages) {
			if (m.getID().equals(b.getID())) {
				return true;
			}
		}
		return false;
	}

	public MNodeID getID() {
		return id;
	}

	public synchronized int getMessagesSize() {
		return outgoingmessages.size();
	}

	@Override
	public List<MMessage> getOutgoingMessages() {
		synchronized (outgoingmessages) {
			if (!outgoingmessages.isEmpty()) {
				List<MMessage> ret = new LinkedList<MMessage>(outgoingmessages);
				outgoingmessages = new LinkedList<MMessage>();
				return ret;
			} else {
				return outgoingmessages;
			}
		}
	}

	public synchronized void clearMessages() {
		outgoingmessages.clear();
	}

	public void close() {
		log.info("close");
		this.closed = true;
		// emptying messages
		getOutgoingMessages();

		source = null;
	}

	public boolean isConnected() {
		return !isClosed() && getID() != null;
	}

	public boolean isClosed() {
		return source == null || closed;
	}

	@Override
	public void addInfoTo(MMessage message) {
		// nothing to add
	}
}
