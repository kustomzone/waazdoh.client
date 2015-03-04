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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.net.ConnectException;
import java.util.LinkedList;
import java.util.List;

import waazdoh.client.model.WData;
import waazdoh.cp2p.common.MHost;
import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MessageResponseListener;
import waazdoh.util.MLogger;
import waazdoh.util.MStringID;
import waazdoh.util.MTimedFlag;

public final class TCPNode implements WNode {
	private static final long MAX_GIVEUP_TIME = 6000;

	private Channel channel;
	private MLogger log = MLogger.getLogger(this);
	private long created = System.currentTimeMillis();

	private MTimedFlag connectionwaiter;
	private boolean offline;

	private MHost host;
	private int port;

	private long touch = System.currentTimeMillis();
	private boolean closed;

	private MNodeID id;
	final private MStringID localid = new MStringID();

	private MMessager source;

	private boolean isactive;

	public final static NodeConnectionFactory connectionfactory = new NodeConnectionFactory();

	public TCPNode(MHost host2, int port2, MMessager nsource) {
		this.host = host2;
		this.port = port2;
		this.source = nsource;
	}

	public synchronized int sendMessages(List<MMessage> smessages) {
		if (checkConnection()) {
			if (smessages != null && !smessages.isEmpty()) {
				log.debug("writing messages " + smessages);

				int bytecount = writeMessages(smessages);
				return bytecount;
			} else {
				log.info("not writing zero messages");
				return 0;
			}
		} else {
			log.info("Not sending. Connection not ok.");
			return 0;
		}
	}

	private int writeMessages(List<MMessage> smessages) {
		for (MMessage m : smessages) {
			MessageResponseListener listener = m.getResponseListener();
			this.source.addResponseListener(m.getID(), listener);
		}
		
		channel.writeAndFlush(smessages).addListener(
				ChannelFutureListener.CLOSE_ON_FAILURE); //
		int bytecount = 0;
		for (MMessage mMessage : smessages) {
			bytecount += mMessage.getByteCount();
		}
		log.debug("messages written " + bytecount + " bytes");
		return bytecount;
	}

	@Override
	public void sendMessage(MMessage message) {
		List<MMessage> ms = new LinkedList<MMessage>();
		ms.add(message);
		writeMessages(ms);
	}

	public synchronized boolean checkConnection() {
		// if closed and connectionwaiter is triggered, create new connection
		if (!closed && !offline && channel == null
				&& isConnectionWaiterTriggered()) {
			log.info("creating connection " + this + " trigger "
					+ connectionwaiter);
			TCPNode.connectionfactory.connect(this, host, port);
			connectionwaiter = new MTimedFlag(10000);
			touch();
		}

		return channel != null;
	}

	private boolean isConnectionWaiterTriggered() {
		return connectionwaiter == null || connectionwaiter.isTriggered();
	}

	@Override
	public String toString() {
		return "TCPNode[" + host + ":" + port + "]["
				+ (System.currentTimeMillis() - touch) + "][closed:" + closed
				+ ",id:" + getID() + ", channel:" + channel + "][localid:"
				+ localid + "]";
	}

	private void touch() {
		touch = System.currentTimeMillis();
		isactive = true;
	}

	public synchronized void close() {
		log.info("closing " + this);
		closed = true;
		closeChannel();
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public List<MMessage> getOutgoingMessages() {
		return null;
	}

	@Override
	public boolean isConnected() {
		checkConnection();
		return !closed && isactive && channel != null && getID() != null;
	}

	public void logChannel() {
		if (channel != null) {
			log.info("channel " + host + ":" + port + " channel " + channel
					+ " connected:" + channel.isOpen() + " writable:"
					+ channel.isWritable() + " open:" + channel.isOpen()
					+ " local:" + channel.localAddress() + " remote:"
					+ channel.remoteAddress());
		} else {
			log.info("channel is null");
		}
	}

	public void addInfoTo(MMessage message) {
		if (host != null) {
			WData nodeinfo = message.add("nodeinfo");
			WData nodeid = nodeinfo.add(getID().toString());
			nodeid.add("host").setValue(host.toString());
			nodeid.add("port").setValue("" + port);
		}
	}

	public MNodeID getID() {
		return id;
	}

	public void trigger() {
		if (connectionwaiter != null) {
			connectionwaiter.trigger();
		}
	}

	public synchronized void channelDisconnected() {
		log.info("Channel disconnected");
		channel = null;
		trigger();
	}

	public synchronized void channelException(ChannelHandlerContext ctx,
			Throwable e) {
		if (!(e.getCause() instanceof ConnectException)) {
			log.info("Exception with " + host + ":" + port + " e:" + e);
			log.error(e);
		} else {
			log.debug("Connection failed " + ctx);
		}
		close();
		channel = null;
		trigger();
	}

	void messagesReceived(Channel channel, List<MMessage> messages) {
		log.debug("got " + messages.size() + " messages");
		this.channel = channel;

		touch();

		if (messages.size() > 0) {
			MMessage m = messages.get(0);
			MNodeID nid = m.getLastHandler();
			if (nid == null) {
				nid = m.getSentBy();
			}

			if (this.source.getID().equals(nid)) {
				log.info("Connected to self  " + nid);
				close();
			} else {
				id = nid;
				List<MMessage> response = this.source.handle(messages);
				sendMessages(response);
			}

		}
	}

	private void closeChannel() {
		final Channel cc = this.channel;
		if (cc != null) {
			synchronized (this) {
				log.info("closing channel " + cc);
				try {
					cc.disconnect();
					this.wait(100);
					cc.close().addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture arg0)
								throws Exception {
							log.info("channel closed " + cc);
						}
					});
				} catch (InterruptedException e) {
					log.error(e);
				}
				//
				channel = null;
				if (connectionwaiter != null) {
					connectionwaiter.trigger();
				}
			}
		}
	}

	void channelActive(Channel c) {
		log.info("channelActive " + c);
		touch = System.currentTimeMillis();
		isactive = true;
		touch();
		channel = c;
	}

	void channelUnregistered(Channel c) {
		//
	}

	void channelRegistered(Channel c) {
		log.info("channelRegistered " + c);
		channel = c;

		// sendMessage(this.source.getMessage(HelloHandler.HELLO));
	}

	void channelInactive(Channel ctx) {
		isactive = false;
	}

	@Override
	public int hashCode() {
		return localid.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TCPNode) {
			TCPNode n = (TCPNode) obj;
			return n.localid.equals(localid);
		} else {
			return false;
		}
	}
}
