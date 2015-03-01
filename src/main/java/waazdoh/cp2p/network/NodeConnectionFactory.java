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

import waazdoh.cp2p.common.MHost;
import waazdoh.cp2p.messaging.MMessageList;
import waazdoh.cp2p.messaging.MessageDecoder;
import waazdoh.cp2p.messaging.MessageEncoder;
import waazdoh.util.MLogger;

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
					log.info("init client channel " + ch);

					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast("zipencoder", new JZlibEncoder());
					pipeline.addLast("zipdecoder", new JZlibDecoder());
					pipeline.addLast("messageencoder", new MessageEncoder());
					pipeline.addLast("messagedecoder", new MessageDecoder());
					pipeline.addLast("channels", new NodeHandler());

					ch.writeAndFlush("<init>" + System.currentTimeMillis()
							+ "</init>");
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
		nodes.put(future.channel(), node);
		Channel c = future.channel();
		log.info("node " + node + " with channel " + c);
		return c;
	}

	private TCPNode getNode(ChannelHandlerContext ctx) {
		return nodes.get(ctx.channel());
	}

	private class NodeHandler extends SimpleChannelInboundHandler<MMessageList> {
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			TCPNode node = getNode(ctx);
			if (node != null) {
				super.channelActive(ctx);
				node.channelActive(ctx.channel());
			} else {
				ctx.close();
			}
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
			TCPNode node = getNode(ctx);
			if (node != null) {
				node.channelRegistered(ctx.channel());
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			getNode(ctx).channelException(ctx, cause);
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, MMessageList msgs)
				throws Exception {
			log.info("messageReceived size " + msgs.size());
			getNode(ctx).messagesReceived(ctx.channel(), msgs);
		}

	}

}
