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

import java.io.ByteArrayOutputStream;
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
		// if (cb.readableBytes() >= 4) {
		log.info("decoding messages " + cb.capacity());
		msgs.add(parse(cb));
		// cb.clear();
		// } else {
		// return;
		// }
	}

	private List<MMessage> parse(ByteBuf bb) throws IOException {
		//
		List<MMessage> ret = new LinkedList<MMessage>();
		if (bb.capacity() > 0) {
			int messagecount = bb.readInt();
			log.debug("messagecount : " + messagecount);
			for (int i = 0; i < messagecount; i++) {
				MMessage m = readMessage(bb);
				ret.add(m);
			}
		} else {
			log.info("returning empty list of messages");
		}
		return ret;
	}

	private MMessage readMessage(ByteBuf cb) throws IOException {
		int messagelength = cb.readInt();
		byte messagebytes[] = new byte[messagelength];
		cb.readBytes(messagebytes);
		MMessage m = new MMessage(messagebytes);
		log.debug("channelread " + m);
		return m;
	}
}
