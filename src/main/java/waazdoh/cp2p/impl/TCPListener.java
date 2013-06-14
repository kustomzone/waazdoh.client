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

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import waazdoh.cutils.MLogger;
import waazdoh.cutils.MPreferences;

public class TCPListener {
	public static final int DEFAULT_PORT = 7900;

	private int port = DEFAULT_PORT;
	//
	private MLogger log = MLogger.getLogger(this);
	private MMessager messager;
	private ThreadGroup tg;

	private ServerBootstrap bootstrap;

	private Channel bind;

	private boolean closed;

	private MPreferences preferences;

	public TCPListener(ThreadGroup tg, MMessager mMessager, MPreferences p) {
		this.messager = mMessager;
		this.tg = tg;
		this.preferences = p;
	}

	public void start() {
		if (!closed) {
			NioServerSocketChannelFactory factory = new NioServerSocketChannelFactory(
					Executors.newCachedThreadPool(),
					Executors.newCachedThreadPool());
			bootstrap = new ServerBootstrap(factory);
			bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
				public ChannelPipeline getPipeline() {
					ChannelPipeline pipe = Channels.pipeline();
					pipe.addLast("zipencoder", new ZipEncoder());
					pipe.addLast("zipdecoder", new ZipDecoder());
					pipe.addLast("server", new MServerHandler());
					return pipe;
				}
			});
			bootstrap.setOption("child.tcpNoDelay", false);
			bootstrap.setOption("child.keepAlive", false);
			//
			port = preferences.getInteger(P2PServer.PREFERENCES_PORT, DEFAULT_PORT);
			//
			while (bind == null && port < 65000) {
				try {
					bind = bootstrap.bind(new InetSocketAddress(port));
				} catch (ChannelException e) {
					//log.error(e);
					port++;
				}
			}
			log.info("listening " + port + " bound:" + bind.isBound()
					+ " messager:" + this.messager);
			//
			MMessage b = this.messager.getMessage("newnode");
			b.addAttribute("port", port);
			//
			this.messager.broadcastMessage(b);
		}
	}

	public synchronized void close() {
		try {
			closed = true;
			log.info("closing");
			if (bind != null) {
				bind.close();
				while (bind.isBound()) {
					log.info("waiting... ");
					synchronized (bind) {
						bind.wait(1000);
					}
				}
			}
		} catch (InterruptedException e) {
			log.error(e);
		}
		bind = null;
	}

	class MServerHandler extends SimpleChannelHandler {
		@Override
		public void channelConnected(ChannelHandlerContext ctx,
				ChannelStateEvent e) throws Exception {
			log.info("serverchannel connected " + e);
			super.channelConnected(ctx, e);
		}

		@Override
		public void channelDisconnected(ChannelHandlerContext ctx,
				ChannelStateEvent e) throws Exception {
			log.info("serverchannel disconnected");
			super.channelDisconnected(ctx, e);
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
				throws Exception {
			log.info("messageReceived " + e.getMessage() + " "
					+ e.getRemoteAddress());
			List<MMessage> messages = (List<MMessage>) e.getMessage();
			List<MMessage> response = messager.handle(messages);
			log.info("sending back response " + response);
			e.getChannel().write(response);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
				throws Exception {
			log.info("got exception " + e);
			//log.error(e.getCause());
		}
	}

	public void addDefaultNodes() {
		for (int i = 0; i < 10; i++) {
			int nport = port - 5 + i;
			if (nport == this.port) {
				//
			} else {
				messager.addNode(new MHost("localhost"), nport);
			}
		}
	}
}
