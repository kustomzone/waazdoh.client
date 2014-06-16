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
import waazdoh.cutils.MLogger;
import waazdoh.cutils.MPreferences;

public final class TCPListener {
	public static final int DEFAULT_PORT = 7900;

	private int port = DEFAULT_PORT;
	//
	private MLogger log = MLogger.getLogger(this);
	private MMessager messager;
	private ThreadGroup tg;

	private ServerBootstrap bootstrap;

	private ChannelFuture bind;

	private boolean closed;

	private MPreferences preferences;

	public TCPListener(ThreadGroup tg, MMessager mMessager, MPreferences p) {
		this.messager = mMessager;
		this.tg = tg;
		this.preferences = p;
	}

	public void start() {
		if (!closed) {
			EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch)
								throws Exception {
							log.info("initChannel " + ch);
							ChannelPipeline pipe = ch.pipeline();
							pipe.addLast("zipencoder", new JZlibEncoder());
							pipe.addLast("zipdecoder", new JZlibDecoder());
							pipe.addLast("messageencoder", new MessageEncoder());
							pipe.addLast("messagedecoder", new MessageDecoder());
							pipe.addLast("server", new MServerHandler());
						}
					}).option(ChannelOption.SO_BACKLOG, 128) // (5)
					.childOption(ChannelOption.SO_KEEPALIVE, true); // (6);
			//
			port = preferences.getInteger(P2PServer.PREFERENCES_PORT,
					DEFAULT_PORT);
			//
			try {
				while (bind == null && port < 65000) {
					startListening(bootstrap);
					if (bind == null) {
						port++;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			log.info("listening " + port + " messager:" + this.messager);
			//
			MMessage b = this.messager.getMessage("newnode");
			b.addAttribute("port", port);
			//
			this.messager.broadcastMessage(b);
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
		while (t.isAlive() && bind == null) {
			synchronized (t) {
				t.wait(100);
			}
		}
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
							}
						});
			}
		} catch (InterruptedException e) {
			log.error(e);
		}
		bind = null;
	}

	class MServerHandler extends SimpleChannelInboundHandler<MMessageList> {
		@Override
		protected void messageReceived(ChannelHandlerContext ctx,
				MMessageList ms) throws Exception {
			log.info("messageReceived " + ms);
			if (!closed) {
				MMessageList response = messager.handle(ms);
				if (response != null) {
					log.debug("sending back response " + response);
					ctx.writeAndFlush(response).addListener(
							ChannelFutureListener.CLOSE_ON_FAILURE);
				} else {
					log.debug("closing " + ctx);
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
