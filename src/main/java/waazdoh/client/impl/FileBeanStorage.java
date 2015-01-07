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
package waazdoh.client.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.xml.sax.SAXException;

import waazdoh.client.BeanStorage;
import waazdoh.client.model.StringIDLocalPath;
import waazdoh.client.model.WData;
import waazdoh.util.MLogger;
import waazdoh.util.MPreferences;
import waazdoh.util.MStringID;
import waazdoh.util.xml.XML;

public final class FileBeanStorage implements BeanStorage {
	private MLogger log = MLogger.getLogger(this);
	private String path;

	public FileBeanStorage(MPreferences preferences) {
		this.path = preferences.get(MPreferences.LOCAL_PATH,
				MPreferences.LOCAL_PATH_DEFAULT) + File.separator + "beans";
		File file = new File(path);
		file.mkdirs();
	}

	public WData getBean(final String id) {
		try {
			File f = getFile(id);
			if (f.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(f));
				StringBuilder sb = new StringBuilder();
				while (true) {
					String line = br.readLine();
					if (line == null) {
						break;
					}
					sb.append(line);
				}
				br.close();
				//
				XML xml = new XML(sb.toString());

				return new WData(xml);
			} else {
				return null;
			}
		} catch (IOException e) {
			log.error(e);
			return null;
		} catch (SAXException e) {
			log.error(e);
			return null;
		}
	}

	private File getFile(final String id) {
		File f = new File(getFilePath(id));
		return f;
	}

	private String getFilePath(final String id) {
		String filepath = new StringIDLocalPath(this.path, id).getPath();
		File fpath = new File(filepath);
		if (!fpath.isDirectory()) {
			fpath.mkdirs();
		}
		//
		return filepath + id + ".xml";
	}

	public void addBean(final String id, WData response) {
		try {
			File f = getFile(id);
			FileWriter fw;
			fw = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(response.toXML().toString());
			bw.close();
		} catch (IOException e) {
			log.error(e);
		}
	}

	public Set<MStringID> getLocalSetIDs(String search) {
		Set<MStringID> ret = new HashSet<MStringID>();
		String listpath = path;

		searchLocalSetIDs(search, ret, listpath);
		return ret;
	}

	private void searchLocalSetIDs(String search, Set<MStringID> ret,
			String listpath) {
		File f = new File(listpath);
		String[] list = f.list();
		for (final String childname : list) {
			if (search.indexOf(childname) >= 0) {
				File child = new File(childname);
				if (child.isDirectory()) {
					searchLocalSetIDs(search, ret, listpath + File.separator
							+ childname);
				} else {
					ret.add(new MStringID(childname));
				}
			}
		}
	}

}
