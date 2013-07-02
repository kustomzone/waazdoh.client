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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import waazdoh.cutils.MLogger;

public class ZipEncoder extends SimpleChannelHandler {
	private MLogger log = MLogger.getLogger(this);

	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) {
		try {
			List<MMessage> list = (List<MMessage>) e.getMessage();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ZipOutputStream zzos = new ZipOutputStream(baos);
			zzos.putNextEntry(new ZipEntry("data"));
			DataOutputStream zos = new DataOutputStream(zzos);
			//
			zos.writeInt(list.size());
			int totalbytes = 0;
			for (MMessage bean : list) {
				int bytes = writeMessage(zos, bean);
				totalbytes += bytes;
			}
			//
			zos.close();
			log.debug("zip encoded " + totalbytes + " bytes to " + baos.size());
			byte[] zipbytes = baos.toByteArray();
			baos = new ByteArrayOutputStream();
			DataOutputStream dis = new DataOutputStream(baos);
			dis.writeInt(zipbytes.length);
			dis.write(zipbytes);
			dis.close();
			//
			byte[] sendbytes = baos.toByteArray();
			ChannelBuffer buf = ChannelBuffers.copiedBuffer(sendbytes);
			Channels.write(ctx, e.getFuture(), buf);
		} catch (IOException e1) {
			log.error(e1);
			e.getFuture().setFailure(e1);
		}
	}

	public int writeMessage(DataOutputStream zos, MMessage bean)
			throws IOException {
		log.debug("channelwrite " + bean);
		//
		byte bs[] = bean.getAsBytes();
		zos.writeInt(bs.length);
		zos.write(bs);
		return bs.length;
	}
}
