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
package waazdoh.service;

import java.util.Map;

import waazdoh.cutils.JBeanResponse;
import waazdoh.cutils.MID;
import waazdoh.cutils.MURL;
import waazdoh.cutils.UserID;
import waazdoh.cutils.xml.JBean;

public interface CMService {
	boolean setSession(String username, String session);
	
	JBeanResponse read(MID id);
	
	JBeanResponse write(MID id, JBean b);
	
	boolean publish(MID id);
	
	UserID getUserID();
	
	JBeanResponse search(
			String filter, int index, int i);
	
	MURL getURL(String service, String method, MID id);
	
	String getUsername();
	
	boolean isLoggedIn();
	
	String requestAppLogin(String username, String appname, MID appid);

	String getSessionID();

	String getInfoText();

	JBeanResponse reportDownload(MID id, boolean success);

	JBeanResponse getBookmarkGroup(String id);

	Map<String, String> getBookmarkGroups();

	JBeanResponse getUser(UserID userid);

}
