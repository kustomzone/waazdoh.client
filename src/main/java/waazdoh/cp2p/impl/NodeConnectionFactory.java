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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import waazdoh.cutils.MLogger;

public class NodeConnectionFactory {
	private ClientBootstrap bootstrap;
	private Map<Channel, TCPNode> nodes = new HashMap<Channel, TCPNode>();

	private MLogger log = MLogger.getLogger(this);

	public synchronized ClientBootstrap getBootstrap() {
		if (bootstrap == null) {
			NioClientSocketChannelFactory factory = new NioClientSocketChannelFactory(
					Executors.newCachedThreadPool(),
					Executors.newCachedThreadPool());
			bootstrap = new ClientBootstrap(factory);
			bootstrap.setPipelineFactory(new NodePipelineFactory());
		}
		return bootstrap;
	}

	public Channel connect(TCPNode node, MHost host, int port) {
		ClientBootstrap bs = getBootstrap();
		ChannelFuture future = bs.connect(new InetSocketAddress(
				host.toString(), port));
		future.awaitUninterruptibly(200);
		Channel c = future.getChannel();
		nodes.put(c, node);
		return c;
	}

	private class NodeHandler extends SimpleChannelHandler {
		@Override
		public void channelConnected(ChannelHandlerContext ctx,
				ChannelStateEvent e) throws Exception {
			Channel channel = e.getChannel();
			TCPNode node = nodes.get(channel);
			if (node != null) {
				node.channelConnected();
			} else {
				log.error("ChannelConnected node null with channel " + channel);
			}
		}

		@Override
		public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
				throws Exception {
			super.channelClosed(ctx, e);
			log.info("channel closed " + e);
			Channel channel = e.getChannel();
			TCPNode node = nodes.get(channel);
			if (node != null) {
				node.channelClosed();
			} else {
				log.error("node null with channel " + channel);
			}
		}

		@Override
		public void channelDisconnected(ChannelHandlerContext ctx,
				ChannelStateEvent e) throws Exception {
			super.channelDisconnected(ctx, e);
			log.info("channel disconnected " + e);
			Channel channel = e.getChannel();
			TCPNode node = nodes.get(channel);
			if (node != null) {
				node.channelDisconnected();
			} else {
				log.error("ChannelDisconnected node null with channel "
						+ channel);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
				throws Exception {
			Channel channel = e.getChannel();
			TCPNode node = nodes.get(channel);
			if (node != null) {
				node.channelException(ctx, e);
			} else {
				log.error("ChannelDisconnected node null with channel "
						+ channel);
			}
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
				throws Exception {
			log.info("TCPNode messageReceived " + e);
			Channel channel = e.getChannel();
			TCPNode node = nodes.get(channel);
			if (node != null) {
				node.messageReceived(e);
			} else {
				log.error("ChannelDisconnected node null with channel "
						+ channel);
			}
					}

	}

	private class NodePipelineFactory implements ChannelPipelineFactory {
		@Override
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeline = Channels.pipeline();
			pipeline.addLast("zipencoder", new ZipEncoder());
			pipeline.addLast("zipdecoder", new ZipDecoder());
			pipeline.addLast("channels", new NodeHandler());
			return pipeline;
		}
	}

}
