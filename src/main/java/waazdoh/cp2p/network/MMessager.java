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
package waazdoh.cp2p.network;

import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MMessageList;
import waazdoh.cp2p.messaging.MessageID;
import waazdoh.cp2p.messaging.MessageResponseListener;
import waazdoh.util.MStringID;

public interface MMessager {

	MMessage getMessage(final String string);

	void broadcastMessage(MMessage b);

	MMessageList handle(MMessageList ms);

	void notifyNewMessages();

	void addResponseListener(MessageID id,
			MessageResponseListener responseListener);

	MStringID getID();

}
