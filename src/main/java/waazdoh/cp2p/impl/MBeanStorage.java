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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import waazdoh.cutils.JBeanResponse;
import waazdoh.cutils.MLogger;
import waazdoh.cutils.MPreferences;
import waazdoh.cutils.MStringID;
import waazdoh.cutils.xml.JBean;
import waazdoh.cutils.xml.XML;

public final class MBeanStorage {
	private MLogger log = MLogger.getLogger(this);
	private String path;

	public MBeanStorage(MPreferences preferences) {
		this.path = preferences.get(MPreferences.LOCAL_PATH,
				MPreferences.LOCAL_PATH_DEFAULT) + File.separator + "beans";
		File file = new File(path);
		file.mkdirs();
	}

	public JBeanResponse getBean(String id) {
		try {
			File f = getFile(id);
			if (f.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(f));
				StringBuffer sb = new StringBuffer();
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
				JBeanResponse resp = new JBeanResponse();
				resp.setBean(new JBean(xml));
				//
				return resp;
			} else {
				return null;
			}
		} catch (IOException e) {
			log.error(e);
			return null;
		}
	}

	private File getFile(String id) {
		File f = new File(path + File.separator + id);
		return f;
	}

	public void addBean(String id, JBeanResponse response) {
		try {
			File f = getFile(id);
			FileWriter fw;
			fw = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(response.getBean().toXML().toString());
			bw.close();
		} catch (IOException e) {
			log.error(e);
		}
	}

	public Set<MStringID> getLocalSetIDs() {
		File f = new File(path);
		String[] list = f.list();
		Set<MStringID> ret = new HashSet<MStringID>();
		for (String string : list) {
			try {
				ret.add(new MStringID(string));
			} catch (Exception e) {
				log.error("Exteption " + e + " with " + string + " in " + path);
			}
		}
		return ret;
	}

}
