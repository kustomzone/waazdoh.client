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

import waazdoh.common.vo.UserVO;

public final class User {

	private String name;
	private String img;

	public User(UserVO r) {
		this.name = r.getUsername();
	}

	public String getName() {
		return name;
	}

}
