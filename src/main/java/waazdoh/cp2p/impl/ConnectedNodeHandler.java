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

public class ConnectedNodeHandler extends SimpleMessageHandler {
	private P2PServer source;

	public ConnectedNodeHandler(P2PServer server) {
		super();
		this.source = server;
	}

	@Override
	public boolean handle(MMessage childb, Node node) {
		// TODO Auto-generated method stub
		return false;
	}
}
