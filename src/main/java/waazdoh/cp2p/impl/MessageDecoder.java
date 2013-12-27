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
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import org.xml.sax.SAXException;

import waazdoh.cutils.MLogger;

public final class MessageDecoder extends ByteToMessageDecoder {
	private MLogger log = MLogger.getLogger(this);
	private ByteArrayOutputStream baos;

	@Override
	protected void decode(ChannelHandlerContext arg0, ByteBuf cb,
			List<Object> msgs) throws Exception {
		if (cb.readableBytes() < 8)
			return;
		cb.markReaderIndex();
		int length = cb.readInt();

		if (cb.readableBytes() < length) {
			log.info("missing readablebytes " + cb.readableBytes() + " vs "
					+ length);
			cb.resetReaderIndex();
			return;
		}

		byte[] bs = new byte[length];
		cb.readBytes(bs);

		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bs));
		msgs.add(parse(dis));
	}

	private MMessageList parse(DataInputStream dis) throws IOException {
		//
		MMessageList ret = new MMessageList();
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
		log.debug("decoder reading " + messagelength);
		byte messagebytes[] = new byte[messagelength];
		dis.read(messagebytes, 0, messagelength);
		MMessage m;
		try {
			m = new MMessage(messagebytes);
			log.info("decoded " + m);
			return m;
		} catch (SAXException e) {
			log.error(e);
			return null;
		}
	}
}
