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
package waazdoh.client.model;

import waazdoh.util.MLogger;

public final class User {

	private String name;
	private String img;

	public User(WData bean) {
		/*
		 * <response> <user> <uid>1b32558c-827d-4f4c-83bf-b9ea4a313db6</uid>
		 * <profile>
		 * <pictureURL>https://twimg0-a.akamaihd.net/profile_images/2297908262
		 * /rhp37rm35mul5uf0zom6_reasonably_small.jpeg</pictureURL>
		 * <name>Juuso</name> <info>me!!!</info> </profile> <name>jeukku</name>
		 * </user> <success>true</success> </response>
		 */
		WData buser = bean.get("user");
		MLogger.getLogger(this).info("user : " + buser);
		name = buser.getValue("name");
		WData profile = buser.get("profile");
		img = profile.getValue("pictureURL");
	}

	public String getName() {
		return name;
	}

}
