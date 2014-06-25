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

import java.util.List;

import waazdoh.cp2p.common.MHost;
import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MMessageList;
import waazdoh.util.MLogger;

public final class Node {
	private static final long MAX_DIE_TIME = 30100;
	private static final int WARNING_TRESHOLD = 5;
	private static final long MAX_PINGDELAY = 10000;
	private static final int MAX_MESSAGES_COUNT = 20;
	private MNodeID id;
	//
	private MLogger log = MLogger.getLogger(this);
	private long lastping;
	private MMessager source;
	private long touch;
	private int warning;

	private TCPNode tcpnode;
	private int outputbytecount;

	private MMessageList outgoingmessages = new MMessageList();
	private int receivedmessages;
	private long currentpingdelay;

	public Node(MNodeID id, MHost host, int port, MMessager nsource) {
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

	public Node(MNodeID id2, MMessager source) {
		this.id = id2;
		this.source = source;
		touch();
	}

	public String getMemoryUsageInfo() {
		return " messages[" + outgoingmessages.size() + "]";
	}

	@Override
	public String toString() {
		return "Node[" + tcpnode + "][" + getID() + "][" + outputbytecount
				+ "B]";
	}

	public void addMessage(MMessage b) {
		synchronized (outgoingmessages) {
			if (this.outgoingmessages.size() < MAX_MESSAGES_COUNT
					&& !findMessage(b)) {
				if (isConnected()) {
					// Not logging before if we are connected
					log.info("addMessage " + b);
				}
				b.setLastHandler(source.getID());
				this.outgoingmessages.add(b);
				source.addResponseListener(b.getID(), b.getResponseListener());
				source.notifyNewMessages();
			} else {
				log.info("Message queue full. Not sending message." + b);
			}
		}
	}

	private boolean findMessage(MMessage b) {
		for (MMessage m : outgoingmessages) {
			if (m.getID().equals(b.getID())) {
				return true;
			}
		}
		return false;
	}

	public void sendMessages() {
		MMessageList smessages;
		synchronized (outgoingmessages) {
			smessages = getMessages();
			outgoingmessages.clear();
		}
		tcpnode.sendMessages(smessages);
	}

	public MNodeID getID() {
		return id;
	}

	public synchronized int getMessagesSize() {
		return outgoingmessages.size();
	}

	public MMessageList getMessages() {
		synchronized (outgoingmessages) {
			if (outgoingmessages.size() > 0) {
				MMessageList ret = new MMessageList(outgoingmessages);
				outgoingmessages = new MMessageList();
				return ret;
			} else {
				return outgoingmessages;
			}
		}
	}

	public synchronized void clearMessages() {
		outgoingmessages.clear();
	}

	public synchronized boolean checkPing() {
		long maxpingdelay = getPingDelay();
		if (System.currentTimeMillis() - lastping > maxpingdelay
				&& tcpnode != null) {
			log.debug("should ping " + (System.currentTimeMillis() - lastping)
					+ " > " + maxpingdelay);
			lastping = System.currentTimeMillis();
			return true;
		} else {
			return false;
		}
	}

	private long getPingDelay() {
		if (this.currentpingdelay < 10) {
			currentpingdelay = 10;
		}
		return this.currentpingdelay;
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
		source = null;
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
					log.debug("node ok and has messages " + tcpnode);
					outputbytecount += checknode.sendMessages(getMessages());
				}

				if (checknode != null & checknode.shouldGiveUp()) {
					checknode.close();
					tcpnode = null;
				}
			}
		}
	}

	public MMessageList incomingMessages(List<MMessage> incomingmessages) {
		if (source == null) {
			close();
		} else if (incomingmessages.size() > 0) {
			updatePing();
			receivedmessages += incomingmessages.size();

			log.info("incoming message size " + incomingmessages.size());

			if (id == null) {
				this.id = new MNodeID(incomingmessages.get(0).getAttribute(
						"sentby"));
				if (id.equals(source.getID())) {
					log.info("Source and target nodes has a same id. Closing node.");
					close();
					return null;
				}
			}

			List<MMessage> retmessages = source.handle(new MMessageList(
					incomingmessages));
			if (retmessages != null) {
				synchronized (outgoingmessages) {
					outgoingmessages.addAll(retmessages);
				}
			} else {
				close();
			}
		}
		return getMessages();
	}

	private void updatePing() {
		lastping = System.currentTimeMillis();
	}

	public int getReceivedMessages() {
		return receivedmessages;
	}

	public boolean isConnected() {
		return getReceivedMessages() > 0 && !isClosed();
	}

	private boolean isClosed() {
		return source == null;
	}

	public void messageReceived() {
		receivedmessages++;

		this.currentpingdelay /= 2;

		log.info("Message received " + getReceivedMessages()
				+ " current ping delay:" + this.currentpingdelay + "ms");
	}

	public void pingSent() {
		this.currentpingdelay = getPingDelay() * 2;
		if (currentpingdelay > Node.MAX_PINGDELAY) {
			currentpingdelay = Node.MAX_PINGDELAY;
		}
	}
}
