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

import waazdoh.cutils.LogObjectHandler;
import waazdoh.cutils.MLogger;

public final class MLoggerMessage implements LogObjectHandler {
	@Override
	public void handle(String title, Object message2, MLogger log) {
		MMessage message = (MMessage) message2;
		log.info("#MMessage:" + title + "[" + message.getID() + "("
				+ message.getResponseTo() + ")][" + message.getName()
				+ "][atts:" + message.getAttachments().size() + "][sentby:"
				+ message.getSentBy() + "]");
	}
}
