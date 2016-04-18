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
import waazdoh.common.vo.ReturnVO;

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

	private String lastpublishedid;

	public ServiceObject(final String tagname, final WClient env, final ServiceObjectData data, final String version,
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
		if (oid != null) {
			ObjectVO response = env.getService().getObjects().read(oid.toString());
			if (response != null && response.isSuccess() && env.filter(response.getObject())) {
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
		bt.addValue("license", "GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html");
		if (copyof != null) {
			bt.addValue("copyof", copyof.toString());
		}

		return bt;
	}

	@Override
	public String toString() {
		return "ServiceObject[" + tagname + "][" + id + "]";
	}

	public long getModifytime() {
		return modifytime;
	}

	public long getCreationtime() {
		return creationtime;
	}

	public boolean publish() {
		save();
		String sid = id.toString();
		if (lastpublishedid == null || !lastpublishedid.equals(sid)) {
			long st = System.currentTimeMillis();
			log.info("publishing " + st + " id:" + id);
			lastpublishedid = sid;
			ReturnVO ret = env.getService().getObjects().publish(sid);
			log.info("published " + ret + " dtime:" + (System.currentTimeMillis() - st));
			return ret.isSuccess();
		} else {
			return true;
		}
	}

	@Override
	public String getHash() {
		return data.getObject().getContentHash();
	}

	public void save() {
		if (!env.getUserID().equals(creatorid)) {
			copyof = getID().getStringID();
			id = new ObjectID(this, prefix);
			creatorid = env.getUserID();
		}

		WObject current = data.getObject();
		String sid = id.toString();
		current.setAttribute("id", sid);
		if (!storedbean.equals(current)) {

			log.info("" + id + " stored " + storedbean.toText());
			log.info("" + id + " current " + current.toText());
			log.info("" + id + " stored " + storedbean.getContentHash());
			log.info("" + id + " current " + current.getContentHash());

			modified();
			sid = id.toString();
			WObject storing = data.getObject();
			storing.setAttribute("id", sid);
			log.info("" + id + " storing " + storing.toText());
			//
			storedbean = storing;

			log.info("writing to service " + sid);
			env.getService().getObjects().write(sid, storing.toText());
			log.info("stored " + sid);
		}
	}

	public void modified() {
		modifytime = System.currentTimeMillis();
		log.info("modified " + id);
		//
		List<ServiceObjectListener> ls = new LinkedList<ServiceObjectListener>(listeners);
		for (ServiceObjectListener trackListener : ls) {
			trackListener.modified();
		}
	}

	public void addListener(ServiceObjectListener trackListener) {
		listeners.add(trackListener);
	}

	public boolean hasChanged() {
		return lastpublishedid == null || !lastpublishedid.equals(id.toString());
	}

	public MStringID getCopyOf() {
		return copyof;
	}
}
