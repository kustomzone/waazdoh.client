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

import java.net.ConnectException;
import java.util.List;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import waazdoh.cutils.MLogger;
import waazdoh.cutils.MTimedFlag;
import waazdoh.cutils.xml.JBean;

public class TCPNode {
	private static final long MAX_GIVEUP_TIME = 3000;

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
						ChannelFutureListener.CLOSE_ON_FAILURE);
				//
				int bytecount = 0;
				for (MMessage mMessage : smessages) {
					bytecount += mMessage.getByteCount();
				}
				log.info("messages written");
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
		return channel != null && channel.isConnected();
	}

	private synchronized void checkConnection() {
		// if (future != null) {
		// channel = future.getChannel();
		// }
		//
		if (!closed && !offline && channel == null
				&& (connectionwaiter == null || connectionwaiter.isTriggered())) {
			log.debug("creating connection " + this + " trigger "
					+ connectionwaiter);
			channel = TCPNode.connectionfactory.connect(this, host, port);

			connectionwaiter = new MTimedFlag(10000);
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
		closed = true;
		new Thread(new Runnable() {
			@Override
			public void run() {
				closeChannel();
			}
		}, "Node closing").start();
	}

	public boolean shouldGiveUp() {
		return !isConnected()
				&& (System.currentTimeMillis() - lastmessage) > MAX_GIVEUP_TIME;
	}

	public void logChannel() {
		if (channel != null) {
			log.info("channel " + host + ":" + port + " channel " + channel
					+ " connected:" + channel.isConnected() + " writable:"
					+ channel.isWritable() + " readable:"
					+ channel.isReadable() + " open:" + channel.isOpen()
					+ " local:" + channel.getLocalAddress() + " remote:"
					+ channel.getRemoteAddress());
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

	public void channelDisconnected() {
		channel = null;
		trigger();
	}

	public synchronized void channelException(ChannelHandlerContext ctx, ExceptionEvent e) {
		if (!(e.getCause() instanceof ConnectException)) {
			log.info("Exception with " + host + ":" + port + " e:" + e);
			//log.error(e.getCause());
		} else {
			log.debug("Connection failed " + ctx);
		}
		close();
		channel = null;
		trigger();
	}

	public void messageReceived(MessageEvent e) {
		touch();
		//
		List<MMessage> messages = (List<MMessage>) e.getMessage();
		List<MMessage> response = node.incomingMessages(messages);
		sendMessages(response);

	}

	private synchronized void closeChannel() {
		if (channel != null) {
			log.info("closing channel " + channel);
			channel.close();
			channel = null;
			if (connectionwaiter != null) {
				connectionwaiter.trigger();
			}
		}
	}
}
