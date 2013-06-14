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
package waazdoh.cp2p.impl.handlers;


import waazdoh.cp2p.impl.MMessage;
import waazdoh.cp2p.impl.MessageResponseListener;
import waazdoh.cp2p.impl.Node;
import waazdoh.cp2p.impl.SimpleMessageHandler;
import waazdoh.cutils.MLogger;

public class PingHandler extends SimpleMessageHandler {
	private MLogger log = MLogger.getLogger(this);

	@Override
	public boolean handle(MMessage childb, Node n) {
		 final long st = System.currentTimeMillis();
		//
		MMessage response = getFactory().newResponseMessage(childb,
				"pingresponse");
		final long senttime = System.currentTimeMillis();
		n.addMessage(response, new MessageResponseListener() {
			@Override
			public void messageReceived(Node n, MMessage message) {
				long responsetime = System.currentTimeMillis();
				log.info("ping in " + (responsetime - senttime));
			}

			@Override
			public boolean isDone() {
				return (System.currentTimeMillis()-st)>50000;
			}
		});
		return true;
	}
}
