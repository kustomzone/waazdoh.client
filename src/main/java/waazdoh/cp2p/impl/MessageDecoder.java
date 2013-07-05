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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.MessageList;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import waazdoh.cutils.MLogger;

public class MessageDecoder extends ByteToMessageDecoder {
	private MLogger log = MLogger.getLogger(this);
	private ByteArrayOutputStream baos;

	@Override
	protected void decode(ChannelHandlerContext arg0, ByteBuf cb,
			MessageList<Object> msgs) throws Exception {
		log.info("decoding messages " + cb.capacity());
		if (cb.readableBytes() < 8)
			return;
		int length = cb.getInt(0);
		log.info("decoding messages " + cb.capacity() + " length:" + length);
		if (cb.readableBytes() < length + 8)
			return;
		int size = cb.readInt();
		byte[] bs = new byte[size];
		cb.readBytes(bs);
		log.info("bytes size " + size);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bs));
		//
		msgs.add(parse(dis));
		// cb.clear();
	}

	private List<MMessage> parse(DataInputStream dis) throws IOException {
		//
		List<MMessage> ret = new LinkedList<MMessage>();
		int messagecount = dis.readInt();
		log.debug("messagecount : " + messagecount);
		for (int i = 0; i < messagecount; i++) {
			MMessage m = readMessage(dis);
			ret.add(m);
		}
		return ret;
	}

	private MMessage readMessage(DataInputStream dis) throws IOException {
		int messagelength = dis.readInt();
		log.info("decoder reading " + messagelength);
		byte messagebytes[] = new byte[messagelength];
		dis.read(messagebytes, 0, messagelength);
		MMessage m = new MMessage(messagebytes);
		log.debug("channelread " + m);
		return m;
	}
}
