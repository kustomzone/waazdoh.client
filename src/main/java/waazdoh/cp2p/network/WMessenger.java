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

import java.util.List;
import java.util.Set;

import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MMessageHandler;
import waazdoh.cp2p.messaging.MessageID;
import waazdoh.cp2p.messaging.MessageResponseListener;

public interface WMessenger {
	MNodeID getID();

	List<MMessage> handle(List<MMessage> ms);

	MMessage getMessage(final String string);

	MMessage newResponseMessage(MMessage childb, String string);

	void addResponseListener(MessageID id,
			MessageResponseListener responseListener);

	void broadcastMessage(MMessage b);

	void broadcastMessage(MMessage notification,
			MessageResponseListener messageResponseListener);

	void broadcastMessage(MMessage message,
			MessageResponseListener messageResponseListener,
			Set<MNodeID> exceptions);

	String getInfoText();

	void close();

	long getLastMessageReceived();

	void addMessageHandler(String name, MMessageHandler h);

	boolean isClosed();

}
