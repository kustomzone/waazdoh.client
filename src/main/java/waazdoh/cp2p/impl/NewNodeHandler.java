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

public class NewNodeHandler implements MMessageHandler {
	private MMessageFactory factory;
	private P2PServer server;

	public NewNodeHandler(P2PServer p2pServer) {
		this.server = p2pServer;
	}

	@Override
	public boolean handle(MMessage childb, Node node) {
		return false;
	}
	
	@Override
	public void setFactory(MMessageFactory factory) {
		this.factory = factory;
	}
}
