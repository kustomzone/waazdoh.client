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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import waazdoh.cutils.MLogger;

public class ZipDecoder extends FrameDecoder {
	private MLogger log = MLogger.getLogger(this);
	private ByteArrayOutputStream baos;
	private int expectedlength = -1;

	@Override
	protected Object decode(ChannelHandlerContext arg0, Channel arg1,
			ChannelBuffer cb) throws Exception {
		if (baos == null) {
			expectedlength = cb.readInt();
			baos = new ByteArrayOutputStream();
		}
		int toread = cb.readableBytes();
		if (toread + baos.size() > expectedlength) {
			toread = expectedlength - baos.size();
		}
		//
		cb.readBytes(baos, toread);
		if (baos.size() >= expectedlength) {
			byte[] bytes = Unzip();
			log.info("got " + bytes.length + " bytes");
			return parse(bytes);
		} else {
			log.info("expected " + expectedlength + " baos has:" + baos.size()
					+ " available:" + toread);
			return null;
		}
	}

	private byte[] Unzip() throws IOException {
		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(
				baos.toByteArray()));
		log.info("has read " + baos.size() + " bytes and encoding them");
		baos = null;
		zis.getNextEntry();
		//
		ByteArrayOutputStream unzippedbaos = new ByteArrayOutputStream();
		while (true) {
			int b = zis.read();
			if (b < 0) {
				break;
			}
			unzippedbaos.write(b);
		}
		return unzippedbaos.toByteArray();
	}

	private List<MMessage> parse(byte[] bytes) throws IOException {
		log.info("parsing " + bytes.length + " bytes");
		//
		List<MMessage> ret = new LinkedList<MMessage>();
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(
				bytes));
		int messagecount = dis.readInt();
		log.info("messagecount : " + messagecount);
		for (int i = 0; i < messagecount; i++) {
			MMessage m = readMessage(dis);
			ret.add(m);
		}
		return ret;
	}

	private MMessage readMessage(DataInputStream dis) throws IOException {
		int messagelength = dis.readInt();
		byte messagebytes[] = new byte[messagelength];
		dis.read(messagebytes);
		MMessage m = new MMessage(messagebytes);
		log.info("channelread " + m);
		return m;
	}
}
