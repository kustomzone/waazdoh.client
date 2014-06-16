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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.handler.codec.compression.JZlibDecoder;
import io.netty.handler.codec.compression.JZlibEncoder;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import waazdoh.cutils.MLogger;

public final class NodeConnectionFactory {

	private static Bootstrap _bootstrap;
	private Map<Channel, TCPNode> nodes = new HashMap<Channel, TCPNode>();
	private EventLoopGroup workerGroup = new OioEventLoopGroup();
	private MLogger log = MLogger.getLogger(this);

	public synchronized Bootstrap getBootstrap() {
		if (NodeConnectionFactory._bootstrap == null) {
			NodeConnectionFactory._bootstrap = new Bootstrap(); // (1)
			_bootstrap.group(workerGroup); // (2)
			_bootstrap.channel(OioSocketChannel.class); // (3)
			_bootstrap.option(ChannelOption.SO_KEEPALIVE, true); // (4)
			_bootstrap.option(ChannelOption.TCP_NODELAY, true); // (4)

			_bootstrap.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast("zipencoder", new JZlibEncoder());
					pipeline.addLast("zipdecoder", new JZlibDecoder());
					pipeline.addLast("messageencoder", new MessageEncoder());
					pipeline.addLast("messagedecoder", new MessageDecoder());
					pipeline.addLast("channels", new NodeHandler());

				}
			});
		}
		return _bootstrap;
	}

	public Channel connect(TCPNode node, MHost host, int port) {
		log.info("creating connection to " + host + " port:" + port);
		Bootstrap bs = getBootstrap();
		ChannelFuture future = bs.connect(new InetSocketAddress(
				host.toString(), port));
		future.awaitUninterruptibly(100);
		nodes.put(future.channel(), node);
		Channel c = future.channel();
		node.channelActive(c);
		log.info("node " + node + " with channel " + c);
		return c;
	}

	private TCPNode getNode(ChannelHandlerContext ctx) {
		return nodes.get(ctx.channel());
	}

	private class NodeHandler extends SimpleChannelInboundHandler<MMessageList> {
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			super.channelActive(ctx);
			// getNode(ctx).nodeChannelActive(ctx.channel());
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			super.channelInactive(ctx);
			getNode(ctx).channelInactive(ctx.channel());
		}

		@Override
		public void channelRegistered(ChannelHandlerContext ctx)
				throws Exception {
			super.channelRegistered(ctx);
			// getNode(ctx).nodeChannelRegistered(ctx.channel());
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			getNode(ctx).channelException(ctx, cause);
		}

		@Override
		protected void messageReceived(ChannelHandlerContext ctx,
				MMessageList msgs) throws Exception {
			log.info("messageReceived size " + msgs.size());
			getNode(ctx).messagesReceived(msgs);
		}

	}

}
