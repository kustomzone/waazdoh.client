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
package waazdoh.cp2p.messaging;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import waazdoh.common.WLogger;

public final class MessageEncoder extends MessageToByteEncoder<List<MMessage>> {
	private WLogger log = WLogger.getLogger(this);

	protected void encode(ChannelHandlerContext arg0, List<MMessage> list,
			ByteBuf bb) throws Exception {
		log.debug("encoding message " + list);
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			//
			dos.writeInt(list.size());
			for (MMessage bean : list) {
				int bytes = writeMessage(dos, bean);
			}
			//
			byte[] dosbb = baos.toByteArray();
			bb.writeInt(dosbb.length);
			bb.writeBytes(dosbb, 0, dosbb.length);
		} catch (IOException e1) {
			log.error(e1);
		}
	}

	public int writeMessage(DataOutputStream dos, MMessage m)
			throws IOException {
		log.debug("encoding " + m);
		//
		byte bs[] = m.getAsBytes();
		dos.writeInt(bs.length);
		dos.write(bs);
		return bs.length;
	}
}
