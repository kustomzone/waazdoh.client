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

import waazdoh.common.HashSource;
import waazdoh.common.MStringID;
import waazdoh.common.ObjectID;
import waazdoh.common.UserID;
import waazdoh.common.WLogger;
import waazdoh.common.WObject;
import waazdoh.common.vo.ObjectVO;

public final class ServiceObject implements HashSource {
	private UserID creatorid;

	private ObjectID id;
	private long modifytime = System.currentTimeMillis();
	private long creationtime = System.currentTimeMillis();

	private WClient env;

	private ServiceObjectData data;

	private WLogger log = WLogger.getLogger(this);

	private String tagname;
	private MStringID copyof;
	private WObject storedbean = new WObject();

	private List<ServiceObjectListener> listeners = new LinkedList<ServiceObjectListener>();

	private String version;

	private String prefix;

	public ServiceObject(final String tagname, final WClient env,
			final ServiceObjectData data, final String version,
			final String nprefix) {
		this.tagname = tagname;
		this.creatorid = env.getUserID();
		this.data = data;
		this.env = env;
		this.creationtime = System.currentTimeMillis();
		this.version = version;
		this.prefix = nprefix;
		id = new ObjectID(this, prefix);
	}

	public boolean load(MStringID oid) {
		log.info("loading " + oid);
		if (oid != null) {
			ObjectVO response = env.getService().getObjects()
					.read(oid.toString());
			if (response != null && response.isSuccess()) {
				id = new ObjectID(oid, this);
				return parseObject(response.getObject());
			} else {
				log.info("loading " + tagname + " bean failed " + oid);
				return false;
			}
		} else {
			return false;
		}
	}

	private boolean parseObject(WObject o) {
		id = new ObjectID(o.getAttribute("id"), this);

		creatorid = o.getUserAttribute("creator");
		creationtime = o.getLongValue("creationtime");
		modifytime = o.getLongValue("modified");
		version = o.getValue("version");
		copyof = o.getIDValue("copyof");
		//
		return data.parse(o);
	}

	public WClient getEnvironment() {
		return env;
	}

	public ObjectID getID() {
		return id;
	}

	public WObject getBean() {
		WObject bt = new WObject(tagname);
		bt.addValue("creationtime", getCreationtime());
		bt.addValue("modified", getModifytime());
		bt.addValue("creator", creatorid.toString());
		bt.addValue("version", version);
		bt.addValue("license",
				"GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html");
		if (copyof != null) {
			bt.addValue("copyof", copyof.toString());
		}

		return bt;
	}

	public long getModifytime() {
		return modifytime;
	}

	public long getCreationtime() {
		return creationtime;
	}

	public boolean publish() {
		log.info("publishing " + id);
		save();
		return env.getService().getObjects().publish(id.toString()).isSuccess();
	}

	@Override
	public String getHash() {
		return data.getObject().getContentHash();
	}

	public void save() {
		log.info("possibly saving " + id);

		if (!env.getUserID().equals(creatorid)) {
			copyof = getID().getStringID();
			id = new ObjectID(this, prefix);
			creatorid = env.getUserID();
		}

		WObject current = data.getObject();
		current.setAttribute("id", id.toString());
		if (!storedbean.equals(current)) {

			log.info("" + id + " stored " + storedbean.toText());
			log.info("" + id + " current " + current.toText());
			log.info("" + id + " stored " + storedbean.getContentHash());
			log.info("" + id + " current " + current.getContentHash());

			modified();
			WObject storing = data.getObject();
			storing.setAttribute("id", id.toString());
			log.info("" + id + " storing " + storing.toText());
			//
			storedbean = storing;
			log.info("adding bean" + id);

			env.getService().getObjects()
					.write(id.getStringID().toString(), storing.toText());
		}
	}

	public void modified() {
		modifytime = System.currentTimeMillis();
		log.info("modified " + id);
		//
		List<ServiceObjectListener> ls = new LinkedList<ServiceObjectListener>(
				listeners);
		for (ServiceObjectListener trackListener : ls) {
			trackListener.modified();
		}
	}

	public void addListener(ServiceObjectListener trackListener) {
		listeners.add(trackListener);
	}
}
