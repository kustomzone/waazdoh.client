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

import java.util.List;

public interface MMessager {

	MMessage getMessage(String string);

	void broadcastMessage(MMessage b);

	List<MMessage> handle(List<MMessage> messages);

	Node addNode(MHost mHost, int nport);
}
