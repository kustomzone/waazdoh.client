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

public interface MMessager {

	MMessage getMessage(String string);

	void broadcastMessage(MMessage b);

	MMessageList handle(MMessageList ms);

	Node addNode(MHost mHost, int nport);
}
