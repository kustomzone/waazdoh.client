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
package waazdoh.cp2p.impl;

import java.util.LinkedList;
import java.util.List;

import waazdoh.cutils.MID;
import waazdoh.cutils.MLogger;

public class Node {
	private static final long MAX_DIE_TIME = 30100;
	private static final int WARNING_TRESHOLD = 5;
	private static final long MAX_PINGDELAY = 10000;
	private static final int MAX_MESSAGES_COUNT = 20;
	private MID id;
	//
	private MLogger log = MLogger.getLogger(this);
	private long lastping;
	private P2PServer source;
	private long touch;
	private int warning;

	private TCPNode tcpnode;
	private int outputbytecount;

	private List<MMessage> outgoingmessages = new LinkedList<MMessage>();

	public Node(MID id, MHost host, int port, P2PServer nsource) {
		this.id = id;
		tcpnode = new TCPNode(host, port, this);
		this.source = nsource;
		touch();
	}

	/*
	 * public Node(MID id2, MHost string, int port2, P2PServer source) { this.id
	 * = id2; this.host = string; this.port = port2; this.source = source;
	 * touch(); }
	 */

	public Node(MID id2, P2PServer source) {
		this.id = id2;
		this.source = source;
		touch();
	}

	public String getMemoryUsageInfo() {
		return " messages[" + outgoingmessages.size() + "]";
	}

	@Override
	public String toString() {
		return "Node[" + tcpnode + "][" + getID() + "]";
	}

	public void addMessage(MMessage b) {
		if (this.outgoingmessages.size() < MAX_MESSAGES_COUNT
				&& !findMessage(b)) {
			synchronized (outgoingmessages) {
				log.info("addMessage " + b);
				b.setLastHandler(source.getID());
				this.outgoingmessages.add(b);
			}
			source.notifyNewMessages();
		} else {
			log.info("Message queue full. Not sending message." + b);
		}
	}

	private boolean findMessage(MMessage b) {
		for (MMessage m : outgoingmessages) {
			if (m.getID().equals(b.getID())) {
				throw new RuntimeException("message already on outgoing list");
			}
		}
		return false;
	}

	public void addMessage(MMessage message,
			MessageResponseListener messageResponseListener) {
		if (message != null) {
			source.addResponseListener(message.getID(), messageResponseListener);
			addMessage(message);
		}
	}

	public void sendMessages() {
		List<MMessage> smessages;
		synchronized (outgoingmessages) {
			smessages = getMessages();
			outgoingmessages.clear();
		}
		tcpnode.sendMessages(smessages);
	}

	public MID getID() {
		return id;
	}

	public synchronized int getMessagesSize() {
		return outgoingmessages.size();
	}

	public List<MMessage> getMessages() {
		synchronized (outgoingmessages) {
			if (outgoingmessages.size() > 0) {
				LinkedList<MMessage> ret = new LinkedList<MMessage>(
						outgoingmessages);
				outgoingmessages = new LinkedList<MMessage>();
				return ret;
			} else {
				return outgoingmessages;
			}
		}
	}

	public void messagesAsResponse(List<MMessage> messages)
			throws NodeException {
		log.info("Handling node response " + messages);
		touch();
		//
		for (MMessage message : messages) {
			if (this.id == null) {
				this.id = message.getSentBy();
			}
		}
		try {
			List<MMessage> returnmessages = source.handle(messages);
			synchronized (outgoingmessages) {
				outgoingmessages.addAll(returnmessages);
			}
		} catch (Exception e) {
			log.error(e);
			throw (new NodeException(e));
		}
	}

	public synchronized void clearMessages() {
		outgoingmessages.clear();
	}

	public synchronized boolean checkPing() {
		if (System.currentTimeMillis() - lastping > MAX_PINGDELAY
				&& tcpnode != null) {
			log.info("should ping");
			lastping = System.currentTimeMillis();
			return true;
		} else {
			return false;
		}
	}

	public void touch() {
		this.touch = System.currentTimeMillis();
	}

	public void warning() {
		this.warning++;
	}

	public boolean shouldDie() {
		return (System.currentTimeMillis() - touch) > MAX_DIE_TIME
				|| warning > WARNING_TRESHOLD;
	}

	public void close() {
		log.info("close");
		getMessages();

		if (tcpnode != null) {
			tcpnode.close();
			tcpnode = null;
		}
	}

	public void addInfoTo(MMessage message) {
		if (tcpnode != null) {
			tcpnode.addInfoTo(message);
		}
	}

	public void check() {
		final TCPNode checknode = tcpnode;
		if (checknode != null) {
			synchronized (checknode) {
				if (checknode.isConnected() && getMessagesSize() > 0) {
					log.info("node ok and has messages " + tcpnode);
					outputbytecount += checknode.sendMessages(getMessages());
				}

				if (checknode != null & checknode.shouldGiveUp()) {
					checknode.close();
					tcpnode = null;
				}
			}
		}
	}

	public List<MMessage> incomingMessages(List<MMessage> messages) {
		if (messages.size() > 0) {
			updatePing();

			if (id == null) {
				this.id = new MID(messages.get(0).getAttribute("sentby"));
			}

			List<MMessage> retmessages = source
					.handle(new LinkedList<MMessage>(messages));
			synchronized (messages) {
				messages.addAll(retmessages);
			}
		}
		return getMessages();
	}

	private void updatePing() {
		lastping = System.currentTimeMillis();
	}
}
