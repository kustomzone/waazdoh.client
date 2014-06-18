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
package waazdoh.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import waazdoh.util.MLogger;

public final class MConverter {
	private byte[] bytes;
	private MLogger log = MLogger.getLogger(this);

	public MConverter(float[] fs) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream das = new DataOutputStream(baos);
		try {
			for (float f : fs) {
				das.writeFloat(f);
			}
			das.close();
		} catch (IOException e) {
			log.error(e);
		}
		bytes = baos.toByteArray();
	}

	@Override
	public String toString() {
		String s = new String(bytes);
		return s;
	}

	public MConverter(byte[] attachment) {
		this.bytes = attachment;
	}

	public byte[] toByteArray() {
		return bytes;
	}

	public List<Float> toFloatList() {
		List<Float> ret = new ArrayList<Float>();
		DataInputStream dis = getDataInputStream();
		try {
			while (dis.available() > 0) {
				ret.add(dis.readFloat());
			}
		} catch (IOException e) {
			log.error(e);
		}
		return ret;
	}

	private DataInputStream getDataInputStream() {
		return new DataInputStream(new ByteArrayInputStream(bytes));
	}
}
