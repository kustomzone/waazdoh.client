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
package waazdoh.cutils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MLogger {
	private Logger log;
	private Object o;
	final private static Map<Class, LogObjectHandler> objeccthandlers = new HashMap<Class, LogObjectHandler>();
	
	public MLogger(Object o) {
		this.o = o;
	}

	public static MLogger getLogger(Object o) {
		MLogger logger = new MLogger(o);
		return logger;
	}

	public void info(String string) {
		getLog().info(getLine(string));
	}

	public void debug(String string) {
		getLog().finer(getLine(string));
	}

	private String getLine(String string) {
		if (string != null) {
			string = string.replace('\n', '-');
		}
		return "" + o + " - " + System.currentTimeMillis() + " --" + string;
	}

	private Logger getLog() {
		if (log == null) {
			log = Logger.getLogger("" + o);
			//o = "" + o;
		}
		return log;
	}

	public void error(Exception e) {
		e.printStackTrace();
		getLog().severe(e.toString());
		e.printStackTrace(getWriter());
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
		cause.printStackTrace();
		getLog().severe(cause.toString());
	}

	/*
	public void logObject(String title, Object message) {
		if (message != null) {
			LogObjectHandler h = getHandler(message);
			if (h != null) {
				h.handle(title, message, this);
			}
		} else {
			getLog().info("logObject null with title " + title);
		}
	}
	*/
	
	private LogObjectHandler getHandler(Object message) {
		return MLogger.objeccthandlers.get(message.getClass());
	}

	public static void addObjectHandler(Class class1, LogObjectHandler w) {
		MLogger.objeccthandlers.put(class1, w);
	}

	public void error(String string) {
		info("ERROR " + string);
	}
}
