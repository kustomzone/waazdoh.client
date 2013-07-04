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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MessageList;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.handler.codec.compression.JZlibDecoder;
import io.netty.handler.codec.compression.JZlibEncoder;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import waazdoh.cutils.MLogger;

public class NodeConnectionFactory {

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
		future.awaitUninterruptibly(1000);
		Channel c = future.channel();
		node.channel(c);
		nodes.put(c, node);
		log.info("node " + node + " with channel " + c);
		return c;
	}

	private class NodeHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			super.channelActive(ctx);
			Channel channel = ctx.channel();
			TCPNode node = nodes.get(channel);
			if (node != null) {
				node.channelActive(channel);
			} else {
				log.error("ChannelActive node null with channel "
						+ channel);
			}
		}
		
		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			super.channelInactive(ctx);
			Channel channel = ctx.channel();
			TCPNode node = nodes.get(channel);
			if (node != null) {
				node.channelInactive(ctx);
			} else {
				log.error("ChannelInactive node null with channel " + channel);
			}
		}

		@Override
		public void channelRegistered(ChannelHandlerContext ctx)
				throws Exception {
			super.channelRegistered(ctx);
			Channel channel = ctx.channel();
			TCPNode node = nodes.get(channel);
			if (node != null) {
				node.channelRegistered(ctx);
			} else {
				log.error("ChannelRegistered node null with channel " + channel + " local:" + channel.localAddress() + " " + channel.remoteAddress());
			}
		}

		@Override
		public void channelUnregistered(ChannelHandlerContext ctx)
				throws Exception {
			super.channelUnregistered(ctx);

			Channel channel = ctx.channel();
			TCPNode node = nodes.get(channel);
			if (node != null) {
				node.channelUnregistered(ctx);
			} else {
				log.error("ChannelUnregistered node null with channel "
						+ channel);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			Channel channel = ctx.channel();
			TCPNode node = nodes.get(channel);
			if (node != null) {
				node.channelException(ctx, cause);
			} else {
				log.error("ChannelDisconnected node null with channel "
						+ channel + " " + cause);
			}
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx,
				MessageList<Object> msgs) throws Exception {
			log.info("TCPNode messageReceived " + msgs);
			Channel channel = ctx.channel();
			TCPNode node = nodes.get(channel);
			if (node != null) {
				node.messageReceived((List<MMessage>) msgs.get(0));
			} else {
				log.error("ChannelMessageReceived node null with channel "
						+ channel);
			}
		}

	}
}
