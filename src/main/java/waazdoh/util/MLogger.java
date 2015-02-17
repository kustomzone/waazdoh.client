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
package waazdoh.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class MLogger {
	private static long starttime;
	private Logger log;
	private Object o;

	static {
		MLogger.starttime = System.currentTimeMillis();
	}

	public MLogger(Object o) {
		this.o = o;
	}

	public static MLogger getLogger(Object o) {
		MLogger logger = new MLogger(o);
		return logger;
	}

	public void info(final String string) {
		getLog().info(getLine(string));
	}

	public void debug(final String string) {
		getLog().finest(getLine(string));
	}

	private String getLine(String string) {
		if (string != null) {
			string = string.replace('\n', '-');
		}
		return "" + o + " - "
				+ (System.currentTimeMillis() - MLogger.starttime) + " -- "
				+ string;
	}

	private Logger getLog() {
		if (log == null) {
			log = Logger.getLogger(o.getClass().getName());
		}
		return log;
	}

	public void error(Exception e) {
		getLog().severe(e.toString());
		getLog().throwing("" + o, "", e);
	}

	public void error(Throwable cause) {
		getLog().severe(cause.toString());
		getLog().log(Level.SEVERE, "", cause);
	}

	public void error(final String string) {
		info("ERROR " + string);
	}

	public static void resetStartTime() {
		MLogger.starttime = System.currentTimeMillis();
	}
}
