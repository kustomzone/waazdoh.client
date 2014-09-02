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
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MMessageList;
import waazdoh.cp2p.messaging.MessageDecoder;
import waazdoh.cp2p.messaging.MessageEncoder;
import waazdoh.util.MLogger;
import waazdoh.util.MPreferences;

public final class TCPListener {
	public static final int DEFAULT_PORT = 7900;

	private int port;
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
		if (!isClosed()) {
			EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup)
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
							} else {
								log.info("InitChannel on closed listener. Closing channel.");
								ch.close();
							}
						}
					}).option(ChannelOption.SO_BACKLOG, 128) // (5)
					.childOption(ChannelOption.SO_KEEPALIVE, true); // (6);
			//
			port = preferences.getInteger(MPreferences.NETWORK_SERVER_PORT,
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