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
import io.netty.channel.socket.oio.OioSocketChannel;

import java.net.ConnectException;
import java.util.List;

import waazdoh.client.model.WData;
import waazdoh.cp2p.common.MHost;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MMessageList;
import waazdoh.util.MLogger;
import waazdoh.util.MTimedFlag;

public final class TCPNode {
	private static final long MAX_GIVEUP_TIME = 30000;

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

	public synchronized int sendMessages(MMessageList smessages) {
		if (isConnected()) {
			if (!smessages.isEmpty()) {
				log.debug("writing messages " + smessages);

				channel.writeAndFlush(smessages).addListener(
						ChannelFutureListener.CLOSE_ON_FAILURE); //
				int bytecount = 0;
				for (MMessage mMessage : smessages) {
					bytecount += mMessage.getByteCount();
				}
				log.debug("messages written " + bytecount + " bytes");
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
		OioSocketChannel oio = (OioSocketChannel) channel;
		return channel != null && isactive && channel.isOpen() && oio.localAddress()!=null && oio.remoteAddress()!=null;
	}

	private synchronized void checkConnection() {
		// if closed and connectionwaiter is triggered, create new connection
		if (!closed && !offline && channel == null
				&& (connectionwaiter == null || connectionwaiter.isTriggered())) {
			log.info("creating connection " + this + " trigger "
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
		if (!isConnected()
				&& (System.currentTimeMillis() - lastmessage) > MAX_GIVEUP_TIME) {
			log.info("Should give up " + lastmessage + " "
					+ (System.currentTimeMillis() - lastmessage));
			return true;
		} else {
			return false;
		}
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
			WData nodeid = nodeinfo.add(node.getID().toString());
			nodeid.add("host").setValue(host.toString());
			nodeid.add("port").setValue("" + port);
		}
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

	void messagesReceived(List<MMessage> messages) {
		log.debug("got " + messages.size() + " messages");
		touch();
		MMessageList response = node.incomingMessages(messages);
		sendMessages(response);
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
		lastmessage = System.currentTimeMillis();
		isactive = true;
		touch();
		channel = c;
	}

	void channelUnregistered(Channel c) {
		//
	}

	void cannelRegistered(Channel c) {
		channelActive(c);
	}

	void channelInactive(Channel ctx) {
		isactive = false;
	}

}
