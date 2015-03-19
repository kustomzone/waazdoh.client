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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.compression.JZlibDecoder;
import io.netty.handler.codec.compression.JZlibEncoder;

import java.util.LinkedList;
import java.util.List;

import waazdoh.common.WLogger;
import waazdoh.common.WPreferences;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MessageDecoder;
import waazdoh.cp2p.messaging.MessageEncoder;

public final class TCPListener {
	public static final int DEFAULT_PORT = 7900;

	private int port;
	//
	private WLogger log = WLogger.getLogger(this);
	private WMessenger messenger;
	private ThreadGroup tg;

	private ServerBootstrap bootstrap;

	private ChannelFuture bind;

	private boolean closed;

	private WPreferences preferences;

	public TCPListener(ThreadGroup tg, WMessenger mMessager, WPreferences p) {
		this.messenger = mMessager;
		this.tg = tg;
		this.preferences = p;
	}

	public void start() {
		if (!isClosed()) {
			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			bootstrap = new ServerBootstrap();
			bootstrap
					.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch)
								throws Exception {
							if (!isClosed()) {
								log.info("initChannel " + ch);
								ChannelPipeline pipe = ch.pipeline();
								pipe.addLast("zipencoder", new JZlibEncoder());
								pipe.addLast("zipdecoder", new JZlibDecoder());
								pipe.addLast("messageencoder",
										new MessageEncoder());
								pipe.addLast("messagedecoder",
										new MessageDecoder());
								pipe.addLast("server", new MServerHandler());
								//
								List<MMessage> mlist = new LinkedList<MMessage>();
								mlist.add(messenger.getMessage("hello"));
								ch.writeAndFlush(mlist);
							} else {
								log.info("InitChannel on closed listener. Closing channel.");
								ch.close();
							}
						}
					}).option(ChannelOption.SO_BACKLOG, 128)
					.childOption(ChannelOption.SO_KEEPALIVE, true);
			//
			port = preferences.getInteger(WPreferences.NETWORK_SERVER_PORT,
					DEFAULT_PORT);
			//
			try {
				while (!isClosed() && bind == null && port < 65000) {
					startListening(bootstrap);
					if (bind == null) {
						port++;
					}
				}
			} catch (InterruptedException e) {
				log.error(e);
			}

			log.info("listening " + port + " messager:" + this.messenger);
			//
			MMessage b = this.messenger.getMessage("newnode");
			b.addAttribute("port", port);
			//
			this.messenger.broadcastMessage(b);
		}
	}

	private synchronized void startListening(final ServerBootstrap bootstrap)
			throws InterruptedException {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				bind = bootstrap.bind(port);
			}
		});
		t.start();
		while (!isClosed() && t.isAlive() && bind == null) {
			synchronized (t) {
				t.wait(100);
			}
		}
	}

	private boolean isClosed() {
		return closed;
	}

	public synchronized void close() {
		try {
			closed = true;
			log.info("closing");
			//
			if (bind != null) {
				bind.channel().disconnect();
				bind.channel().closeFuture().sync().awaitUninterruptibly()
						.addListener(new ChannelFutureListener() {

							@Override
							public void operationComplete(ChannelFuture arg0)
									throws Exception {
								log.info("close operation complete " + arg0
										+ " " + TCPListener.this);
								bind = null;
								bootstrap = null;
							}
						});
			}
			//
			while (bind != null) {
				wait(200);
			}
			log.info("closing complete");
		} catch (InterruptedException e) {
			log.error(e);
		}
	}

	public void startClosing() {
		closed = true;
	}

	class MServerHandler extends SimpleChannelInboundHandler<List<MMessage>> {
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, List<MMessage> ms)
				throws Exception {
			log.info("messageReceived " + ms);
			if (!closed) {
				List<MMessage> response = messenger.handle(ms);
				if (response != null) {
					log.debug("sending back response " + response);
					ctx.writeAndFlush(response).addListener(
							ChannelFutureListener.CLOSE_ON_FAILURE);
				} else {
					log.info("Response null for " + ms + ". Closing " + ctx);
					ctx.close();
				}
			} else {
				ctx.close();
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			log.info("got exception " + cause);
			log.error(cause);
		}
	}

	public int getPort() {
		return port;
	}

}
