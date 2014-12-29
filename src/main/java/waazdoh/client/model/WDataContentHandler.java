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
package waazdoh.client.model;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public final class WDataContentHandler implements ContentHandler {
	// private JLogger log = JLogger.getLogger(this);
	private Stack<WData> stack = new Stack<WData>();
	private WData last;
	private WData org;

	public WDataContentHandler(WData b) {
		org = b;
	}

	private WData getCurrent() {
		if (!stack.isEmpty()) {
			return stack.lastElement();
		} else {
			return null;
		}
	}

	public void setDocumentLocator(Locator locator) {
		// TODO Auto-generated method stub
	}

	public void startDocument() throws SAXException {
		// TODO Auto-generated method stub
	}

	public void endDocument() throws SAXException {
		// TODO Auto-generated method stub
	}

	public void startPrefixMapping(final String prefix, String uri)
			throws SAXException {
		// TODO Auto-generated method stub
	}

	public void endPrefixMapping(final String prefix) throws SAXException {
		// TODO Auto-generated method stub
	}

	public void startElement(final String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		// log.info("start " + localName + " qn:" + qName + " attrs:" + atts);
		WData b;
		if (stack.size() == 0) {
			b = org;
			b.setName(localName);
		} else {
			b = new WData(localName);
			getCurrent().add(b);
		}
		//
		for (int i = 0; i < atts.getLength(); i++) {
			b.setAttribute(atts.getQName(i), atts.getValue(i));
		}
		stack.push(b);
	}

	public void endElement(final String uri, String localName, String qName)
			throws SAXException {
		// log.info("end " + localName + " qn:" + qName);
		last = stack.pop();
		// log.info("current "+ (getCurrent() != null ? getCurrent().toXML() :
		// "null"));
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		String text = getCurrent().getText();
		if (text == null) {
			text = "";
		}
		text = text + new String(ch, start, length);
		getCurrent().setValue(text);
	}

	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		// TODO Auto-generated method stub
	}

	public void processingInstruction(final String target, String data)
			throws SAXException {
		// TODO Auto-generated method stub
	}

	public void skippedEntity(final String name) throws SAXException {
		// TODO Auto-generated method stub
	}
}
