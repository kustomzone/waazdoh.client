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

import java.util.LinkedList;
import java.util.List;

import waazdoh.cutils.HashSource;
import waazdoh.cutils.JBeanResponse;
import waazdoh.cutils.MID;
import waazdoh.cutils.MLogger;
import waazdoh.cutils.MStringID;
import waazdoh.cutils.UserID;
import waazdoh.cutils.xml.JBean;

public final class ServiceObject implements HashSource {
	private UserID creatorid;

	private MID id = new MID(this);
	private long created = System.currentTimeMillis();
	private long modifytime;

	private WClient env;

	private ServiceObjectData data;

	private MLogger log = MLogger.getLogger(this);

	private String tagname;
	private MStringID copyof;
	private JBean storedbean = new JBean("temp");

	private List<ServiceObjectListener> listeners = new LinkedList<ServiceObjectListener>();

	private String version;

	public ServiceObject(String string, WClient env, ServiceObjectData data,
			String version) {
		this.tagname = string;
		this.creatorid = env.getUserID();
		this.data = data;
		this.env = env;
		this.created = System.currentTimeMillis();
		this.version = version;
	}

	public boolean load(MStringID oid) {
		JBeanResponse response = env.getService().read(oid);
		if (response != null && response.isSuccess()) {
			id = new MID(oid, this);
			return parseBean(response.getBean());
		} else {
			log.info("loading " + tagname + " bean failed " + oid);
			return false;
		}
	}

	private boolean parseBean(JBean bean) {
		id = new MID(bean.getAttribute("id"), this);

		creatorid = bean.getUserAttribute("creator");
		created = bean.getLongValue("created");
		modifytime = bean.getLongValue("modified");
		version = bean.getValue("version");
		//
		return data.parseBean(bean);
	}

	public WClient getEnvironment() {
		return env;
	}

	public MID getID() {
		return id;
	}

	public JBean getBean() {
		JBean bt = new JBean(tagname);
		bt.addValue("created", created);
		bt.addValue("modified", modifytime);
		bt.addValue("creator", creatorid.toString());
		bt.addValue("version", version);
		return bt;
	}

	public long getModifytime() {
		return modifytime;
	}

	public void publish() {
		save();
		env.getService().publish(id);
	}

	@Override
	public String getHash() {
		return data.getBean().getContentHash();
	}

	public boolean save() {
		if (!env.getUserID().equals(creatorid)) {
			copyof = getID().getStringID();
			id = new MID(this);
			creatorid = env.getUserID();
		}

		if (!storedbean.equals(data.getBean())) {
			modified();
			JBean databean = data.getBean();
			databean.setAttribute("id", id.toString());
			//
			storedbean = databean;
			return env.getService().write(getID().getStringID(), databean)
					.isSuccess();
		}
		{
			return true;
		}
	}

	public void modified() {
		modifytime = System.currentTimeMillis();
		//
		LinkedList<ServiceObjectListener> ls = new LinkedList<ServiceObjectListener>(
				listeners);
		for (ServiceObjectListener trackListener : ls) {
			trackListener.modified();
		}
	}

	public void addListener(ServiceObjectListener trackListener) {
		listeners.add(trackListener);
	}
}
