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
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import waazdoh.cutils.MID;
import waazdoh.cutils.xml.JBean;
import waazdoh.cutils.xml.XML;

public final class MMessage {
	private JBean bean;
	private Map<String, byte[]> attachments = new HashMap<String, byte[]>();
	private int bytecount;

	public MMessage(JBean bean) {
		this.bean = bean;
		//
		bytecount = bean.toXML().toString().length();
		addCreatedTimestamp();
	}

	private void addCreatedTimestamp() {
		if (bean.getAttribute("timestamp") == null) {
			bean.addAttribute("timestamp", System.currentTimeMillis());
		}
	}

	public MMessage(String string, MID sentby) {
		bean = new JBean(string);
		bean.addAttribute("sentby", sentby.toString());
		setID(new MID());
		//
		bytecount = bean.toXML().toString().length();
		addCreatedTimestamp();
	}

	public MMessage(byte[] messagebytes) throws IOException {
		this.bytecount = messagebytes.length;
		//
		ByteArrayInputStream bais = new ByteArrayInputStream(messagebytes);
		DataInputStream dis = new DataInputStream(bais);
		int length = dis.readInt();
		byte bs[] = new byte[length];
		dis.read(bs);
		String sxml = new String(bs);
		XML xml = new XML(sxml);
		bean = new JBean(xml);

		addCreatedTimestamp();

		int attachmentcount = dis.readInt();
		for (int i = 0; i < attachmentcount; i++) {
			int namelength = dis.readInt();
			bs = new byte[namelength];
			dis.read(bs);
			String name = new String(bs);
			//
			int alength = dis.readInt();
			bs = new byte[alength];
			dis.read(bs);
			addAttachment(name, bs);
		}
	}

	@Override
	public String toString() {
		return "MMessage:" + bean.toXML().toString();
	}

	private void setID(MID mid) {
		this.addAttribute("messageid", "" + mid);
	}

	public MID getID() {
		return new MID(getAttribute("messageid"));
	}

	public String getAttribute(String string) {
		return bean.getAttribute(string);
	}

	public JBean get(String string) {
		return bean.get(string);
	}

	public void addAttribute(String string, String string2) {
		this.bean.addAttribute(string, string2);
	}

	public void addAttribute(String string, int start) {
		this.bean.addAttribute(string, start);
	}

	public String getName() {
		return this.bean.getName();
	}

	public void addAttachment(String string, byte[] byteArray) {
		attachments.put(string, byteArray);
	}

	public int getAttributeInt(String string) {
		return this.bean.getAttributeInt(string);
	}

	public JBean add(String string) {
		return this.bean.add(string);
	}

	public XML toXML() {
		return bean.toXML();
	}

	public Set<String> getAttachments() {
		return attachments.keySet();
	}

	public byte[] getAttachment(String string) {
		return attachments.get(string);
	}

	public MID getSentBy() {
		return getIDAttribute("sentby");
	}

	public MID getResponseTo() {
		return getIDAttribute("responseto");
	}

	public List<JBean> getChildren() {
		return bean.getChildren();
	}

	public void setLastHandler(MID networkid) {
		bean.addAttribute("lasthandler", networkid.toString());
	}

	public MID getLastHandler() {
		return getIDAttribute("lasthandler");
	}

	public byte[] getAsBytes() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		//
		byte[] bytes = bean.toXML().toString().getBytes();
		try {
			dos.writeInt(bytes.length);
			for (byte b : bytes) {
				dos.write(b);
			}
			//
			Set<String> akeys = getAttachments();
			dos.writeInt(akeys.size());
			for (String string : akeys) {
				byte[] a = getAttachment(string);
				byte[] namebytes = string.getBytes();
				dos.writeInt(namebytes.length);
				dos.write(namebytes);
				dos.writeInt(a.length);
				dos.write(a);
			}
			byte[] byteArray = baos.toByteArray();
			bytecount = byteArray.length;
			return byteArray;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public MID getIDAttribute(String string) {
		String sid = getAttribute(string);
		if (sid != null) {
			return new MID(sid);
		} else {
			return null;
		}
	}

	public void addIDAttribute(String string, MID id) {
		addAttribute(string, id.toString());
	}

	public int getSentCount() {
		return getAttributeInt("sentcount");
	}

	public int getByteCount() {
		return this.bytecount;
	}
}
