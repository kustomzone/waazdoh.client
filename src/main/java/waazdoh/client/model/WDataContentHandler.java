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

import java.util.ArrayDeque;
import java.util.Deque;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public final class WDataContentHandler implements ContentHandler {
	private Deque<WData> stack = new ArrayDeque<WData>();
	private WData org;

	public WDataContentHandler(WData b) {
		org = b;
	}

	private WData getCurrent() {
		if (!stack.isEmpty()) {
			return stack.peek();
		} else {
			return null;
		}
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		// nothing to do
	}

	@Override
	public void startDocument() throws SAXException {
		// nothing to do
	}

	@Override
	public void endDocument() throws SAXException {
		// nothing to do
	}

	@Override
	public void startPrefixMapping(final String prefix, String uri)
			throws SAXException {
		// nothing to do
	}

	@Override
	public void endPrefixMapping(final String prefix) throws SAXException {
		// TODO Auto-generated method stub
	}

	@Override
	public void startElement(final String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		WData b;
		if (stack.isEmpty()) {
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

	@Override
	public void endElement(final String uri, String localName, String qName)
			throws SAXException {
		stack.pop();
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		String text = getCurrent().getText();
		if (text == null) {
			text = "";
		}
		text = text + new String(ch, start, length);
		getCurrent().setValue(text);
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		// nothing to do
	}

	@Override
	public void processingInstruction(final String target, String data)
			throws SAXException {
		// nothing to do
	}

	@Override
	public void skippedEntity(final String name) throws SAXException {
		// nothing to do
	}
}
