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
package waazdoh.testing;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import waazdoh.client.Binary;
import waazdoh.client.MBinaryID;
import waazdoh.client.MBinarySource;
import waazdoh.client.MBinaryStorage;
import waazdoh.cutils.MPreferences;
import waazdoh.cutils.MStringID;
import waazdoh.cutils.xml.JBean;
import waazdoh.service.CMService;
import waazdoh.service.ReportingService;

public final class TestPBinarySource implements MBinarySource {
	private MPreferences preferences;
	private MBinaryStorage storage;
	private CMService service;
	private Map<String, JBean> beans = new HashMap<String, JBean>();

	public TestPBinarySource(MPreferences p) {
		this.preferences = p;
	}

	@Override
	public Set<MStringID> getLocalObjectIDs() {
		Set<MStringID> ret = new HashSet<MStringID>();
		Set<String> keys = beans.keySet();
		for (final String string : keys) {
			ret.add(new MStringID(string));
		}
		return ret;
	}

	@Override
	public boolean reload(Binary binary) {
		return storage.reload(binary) != null;
	}

	public void addBinary(Binary stream) {
		storage.addNewBinary(stream);
	}

	@Override
	public File getBinaryFile(Binary b) {
		return storage.getBinaryFile(b);
	}

	@Override
	public Binary newBinary(final String string, String extension) {
		return storage.newBinary(string, extension);
	}

	@Override
	public String getMemoryUsageInfo() {
		// TODO Auto-generated method stub
		return "info";
	}

	@Override
	public void setDownloadEverything(boolean b) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setReportingService(ReportingService rservice) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearMemory(int suggestedmemorytreshold) {
		storage.clearMemory(suggestedmemorytreshold);
	}

	@Override
	public String getInfoText() {
		return "TEST";
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
	}

	@Override
	public void addBean(final String id, JBean response) {
		beans.put(id, response);
	}

	@Override
	public JBean getBean(final String id) {
		return beans.get(id);
	}

	private Binary get(MBinaryID fsid) {
		return storage.getBinary(fsid);
	}

	@Override
	public Binary getOrDownload(MBinaryID samplesid) {
		Binary b = storage.getBinary(samplesid);
		if (b == null) {
			b = new Binary(samplesid, service);
			storage.addNewBinary(b);
		}
		return b;
	}

	@Override
	public CMService getService() {
		return service;
	}

	@Override
	public void setService(CMService service) {
		this.service = service;
		this.storage = new MBinaryStorage(preferences, service);
	}

	@Override
	public void saveBinaries() {
		storage.saveBinaries();
	}

	@Override
	public boolean isRunning() {
		return true;
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void waitUntilReady() {
		//
	}
}