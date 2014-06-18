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

import waazdoh.client.model.MID;
import waazdoh.client.model.UserID;
import waazdoh.util.HashSource;
import waazdoh.util.MLogger;
import waazdoh.util.MStringID;
import waazdoh.util.xml.JBean;

public final class ServiceObject implements HashSource {
	private UserID creatorid;

	private MID id;
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

	private String prefix;

	@Deprecated
	public ServiceObject(final String string, WClient env,
			ServiceObjectData data, String version) {
		this(string, env, data, version, "WZH");
	}

	public ServiceObject(final String string, final WClient env,
			final ServiceObjectData data, final String version,
			final String nprefix) {
		this.tagname = string;
		this.creatorid = env.getUserID();
		this.data = data;
		this.env = env;
		this.created = System.currentTimeMillis();
		this.version = version;
		this.prefix = nprefix;
		id = new MID(this, prefix);
	}

	public boolean load(MStringID oid) {
		log.info("loading " + oid);
		JBean response = env.getService().read(oid);
		if (response != null && response.get("data").get(tagname) != null) {
			id = new MID(oid, this);
			return parseBean(response.get("data").get(tagname));
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
		bt.addValue("license",
				"GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html");

		return bt;
	}

	public long getModifytime() {
		return modifytime;
	}

	public boolean publish() {
		log.info("publishing " + id);
		save();
		return env.getService().publish(id);
	}

	@Override
	public String getHash() {
		return data.getBean().getContentHash();
	}

	public void save() {
		log.info("possibly saving " + id);

		if (!env.getUserID().equals(creatorid)) {
			copyof = getID().getStringID();
			id = new MID(this, prefix);
			creatorid = env.getUserID();
		}

		JBean current = data.getBean();
		current.setAttribute("id", id.toString());
		if (!storedbean.equals(current)) {

			log.info("" + id + " stored " + storedbean.toText());
			log.info("" + id + " current " + current.toText());
			log.info("" + id + " stored " + storedbean.getContentHash());
			log.info("" + id + " current " + current.getContentHash());

			modified();
			JBean storing = data.getBean();
			storing.setAttribute("id", id.toString());
			log.info("" + id + " storing " + storing.toText());
			//
			storedbean = storing;
			log.info("adding bean" + id);

			env.getService().addBean(id.getStringID(), storing);
		}
	}

	public void modified() {
		modifytime = System.currentTimeMillis();
		log.info("modified " + id);
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
