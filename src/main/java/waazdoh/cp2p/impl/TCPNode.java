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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.net.ConnectException;
import java.util.List;

import waazdoh.cutils.MLogger;
import waazdoh.cutils.MTimedFlag;
import waazdoh.cutils.xml.JBean;

public class TCPNode {
	private static final long MAX_GIVEUP_TIME = 6000;

	private Channel channel;
	private MLogger log = MLogger.getLogger(this);
	private long created = System.currentTimeMillis();

	private MTimedFlag connectionwaiter;
	private boolean offline;

	private MHost host;
	private int port;

	private Node node;

	private long lastmessage = System.currentTimeMillis();

	private boolean closed;

	private boolean isactive;

	public final static NodeConnectionFactory connectionfactory = new NodeConnectionFactory();

	public TCPNode(MHost host2, int port2, Node node) {
		this.host = host2;
		this.port = port2;
		this.node = node;
	}

	public int sendMessages(List<MMessage> smessages) {
		if (isConnected()) {
			if (smessages.size() > 0) {
				log.info("writing messages");

				channel.write(smessages).addListener(
						ChannelFutureListener.CLOSE_ON_FAILURE); //
				int bytecount = 0;
				for (MMessage mMessage : smessages) {
					bytecount += mMessage.getByteCount();
				}
				log.info("messages written " + bytecount);
				return bytecount;
			} else {
				log.info("not writing zero messages");
				return 0;
			}
		} else {
			log.info("not sending messages because not connected");
			return 0;
		}
	}

	public synchronized boolean isConnected() {
		checkConnection();
		return channel != null && channel.isOpen();
	}

	private synchronized void checkConnection() {
		// if closed and connectionwaiter is triggered, create new connection
		if (!closed && !offline && channel == null
				&& (connectionwaiter == null || connectionwaiter.isTriggered())) {
			log.debug("creating connection " + this + " trigger "
					+ connectionwaiter);
			TCPNode.connectionfactory.connect(this, host, port);
			connectionwaiter = new MTimedFlag(10000);
			touch();
		}
	}

	@Override
	public String toString() {
		return "TCPNode[" + host + ":" + port + "]["
				+ (System.currentTimeMillis() - lastmessage) + "]";
	}

	private void touch() {
		lastmessage = System.currentTimeMillis();
	}

	public synchronized void close() {
		log.info("closing " + this);
		closed = true;
		closeChannel();
	}

	public boolean shouldGiveUp() {
		return !isConnected()
				&& (System.currentTimeMillis() - lastmessage) > MAX_GIVEUP_TIME;
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
			JBean nodeinfo = message.add("nodeinfo");
			JBean nodeid = nodeinfo.add(node.getID().toString());
			nodeid.add("host").setValue(host.toString());
			nodeid.add("port").setValue("" + port);
		}
	}

	public void channelConnected() {
		if (!closed) {
			log.info("channel connected " + channel);
			//
			sendMessages(node.getMessages());
		} else {
			channel.close();
		}
	}

	public void channelClosed() {
		if (shouldGiveUp()) {
			offline = true;
		}

		trigger();
	}

	public void trigger() {
		if (connectionwaiter != null) {
			connectionwaiter.trigger();
		}
	}

	public synchronized void channelDisconnected() {
		channel = null;
		trigger();
	}

	public synchronized void channelException(ChannelHandlerContext ctx,
			Throwable e) {
		if (!(e.getCause() instanceof ConnectException)) {
			log.info("Exception with " + host + ":" + port + " e:" + e);
			log.error(e.getCause());
		} else {
			log.debug("Connection failed " + ctx);
		}
		close();
		channel = null;
		trigger();
	}

	public void messageReceived(List<MMessage> messages) {
		log.info("got " + messages.size() + " messages");
		touch();
		List<MMessage> response = node.incomingMessages(messages);
		sendMessages(response);

	}

	private void closeChannel() {
		if (channel != null) {
			synchronized (this) {
				log.info("closing channel " + channel);
				channel.disconnect();
				channel.close();
				channel = null;
				if (connectionwaiter != null) {
					connectionwaiter.trigger();
				}
			}
		}
	}

	public void channelActive(Channel c) {
		lastmessage = System.currentTimeMillis();
		isactive = true;
		touch();
		channel = c;
	}

	public void channelRegistered(ChannelHandlerContext ctx) {
		if (ctx.channel() != null) {
			channel = ctx.channel();
		}
		touch();
	}

	public synchronized void channelUnregistered(ChannelHandlerContext ctx) {
		channel = null;
	}

	public void channelInactive(ChannelHandlerContext ctx) {
		isactive = false;
	}

	public void channel(Channel c) {
		this.channel = c;
	}
}
