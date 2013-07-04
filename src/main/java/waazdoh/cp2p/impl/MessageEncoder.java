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
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import waazdoh.cutils.MLogger;

public class MessageEncoder extends MessageToByteEncoder<List<MMessage>> {
	private MLogger log = MLogger.getLogger(this);

	protected void encode(ChannelHandlerContext arg0, List<MMessage> list,
			ByteBuf bb) throws Exception {
		log.info("encoding message " + list);
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			//
			bb.writeInt(list.size());
			for (MMessage bean : list) {
				int bytes = writeMessage(bb, bean);
			}
		} catch (IOException e1) {
			log.error(e1);
		}
	}

	public int writeMessage(ByteBuf bb, MMessage bean) throws IOException {
		log.debug("channelwrite " + bean);
		//
		byte bs[] = bean.getAsBytes();
		bb.writeInt(bs.length);
		bb.writeBytes(bs);
		return bs.length;
	}
}
