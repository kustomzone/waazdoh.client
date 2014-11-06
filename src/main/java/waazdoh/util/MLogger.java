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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.logging.Logger;

public final class MLogger {
	private Logger log;
	private Object o;

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
		return "" + o + " - " + System.currentTimeMillis() + " --" + string;
	}

	private Logger getLog() {
		if (log == null) {
			log = Logger.getLogger(o.getClass().getName());
			// o = "" + o;
		}
		return log;
	}

	public void error(Exception e) {
		getLog().severe(e.toString());
		getLog().throwing("" + o, "", e);
	}

	private PrintWriter getWriter() {
		return new PrintWriter(new Writer() {
			@Override
			public void write(char[] cbuf, int off, int len) throws IOException {
				log.info(new String(cbuf, off, len));
			}

			@Override
			public void flush() throws IOException {
				//
			}

			@Override
			public void close() throws IOException {
				log.info("writer closed");
			}
		});
	}

	public void error(Throwable cause) {
		getLog().severe(cause.toString());
		getLog().throwing("" + o, "", cause);
	}

	public void error(final String string) {
		info("ERROR " + string);
	}
}
