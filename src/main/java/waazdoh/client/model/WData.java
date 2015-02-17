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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.codec.binary.Base64;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import waazdoh.util.BytesHash;
import waazdoh.util.MLogger;
import waazdoh.util.MStringID;
import waazdoh.util.XML;

public final class WData implements Comparable<WData> {
	private String name, text;
	private List<WData> children = new LinkedList<WData>();
	private Map<String, String> attributes = new HashMap<String, String>();
	private MLogger log = MLogger.getLogger(this);
	private WData parent;
	private List<String> doctypes;

	public List<String> getChildNames() {
		List<String> ret = new LinkedList<String>();
		List<WData> lchildren = children;
		for (WData jBean : lchildren) {
			ret.add(jBean.getName());
		}
		return ret;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WData) {
			WData b = (WData) obj;
			return toXML().equals(b.toXML());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return toXML().hashCode();
	}

	public void toXML(StringBuilder sb) {
		toXML(0, sb);
	}

	public void toXML(int indent, StringBuilder sb) {
		appendDoctypes(sb);
		//
		indent(indent, sb);
		sb.append("<" + getName());
		appendAttributes(sb);
		//
		if (text == null && getChildren().isEmpty()) {
			sb.append(" />\n");
		} else {
			sb.append(">");
			if (text != null && getChildren().isEmpty()) {
				sb.append(text);
			} else {
				sb.append("\n");
			}
			//
			List<WData> children = new ArrayList<WData>(getChildren());
			java.util.Collections.sort(children);
			for (WData jbean : children) {
				jbean.toXML(indent + 1, sb);
			}
			//
			if (text == null) {
				indent(indent, sb);
			}
			//
			sb.append("</" + getName() + ">\n");
		}
	}

	private void appendAttributes(StringBuilder sb) {
		for (final String key : attributes.keySet()) {
			String value = attributes.get(key);
			char q = '\"';
			if (value.indexOf(q) >= 0) {
				q = '\'';
			}
			sb.append(" " + key + "=" + q + value + q);
		}
	}

	private void appendDoctypes(StringBuilder sb) {
		if (doctypes != null) {
			for (String line : doctypes) {
				sb.append(line);
				sb.append("\n");
			}
		}
	}

	public String toText() {
		XML xml = toXML();
		String ret = xml.toString();
		ret = ret.replaceAll("<", "[");
		ret = ret.replaceAll(">", "]");
		return ret;
	}

	private void indent(int indent, StringBuilder sb) {
		for (int i = 0; i < indent; i++) {
			sb.append('\t');
		}
	}

	public void parseXml(XML oxml) throws SAXException {
		if (oxml != null) {
			String xml = "" + oxml.toString().trim();
			if (xml.indexOf("<!xml") >= 0) {
				StringTokenizer st = new StringTokenizer(xml, "\n");
				st.nextToken();
				xml = "";
				while (st.hasMoreTokens()) {
					xml += st.nextToken() + "\n";
				}
			}
			//
			XMLReader r;
			try {

				r = XMLReaderFactory.createXMLReader();
				r.setContentHandler(new WDataContentHandler(this));
				InputSource input = new InputSource(new StringReader(xml));
				r.parse(input);
				//
				BufferedReader br = new BufferedReader(new StringReader(xml));
				while (true) {
					String line = br.readLine();
					if (line == null) {
						break;
					}
					if (line.indexOf("DOCTYPE") > 0) {
						addDoctype(line);
					}
				}
			} catch (IOException e) {
				log.info("tried to parse " + oxml);
				log.error(e);
				throw new IllegalArgumentException(e);
			}
		}
	}

	private void addDoctype(String line) {
		if (doctypes == null) {
			doctypes = new LinkedList<String>();
		}
		doctypes.add(line);
	}

	private WData() {
		//
	}

	public WData(final String name) {
		setName(name);
	}

	public WData(XML xml) throws SAXException {
		parseXml(xml);
	}

	public List<WData> getChildren() {
		return new LinkedList<WData>(children);
	}

	public String getValue(final String string) {
		WData child = get(string);
		if (child != null) {
			return child.getText();
		} else {
			return null;
		}
	}

	public String getText() {
		if (text != null) {
			return text.length() > 0 ? text : null;
		} else {
			return null;
		}
	}

	public WData add(WData b) {
		if (b == null) {
			throw new NullPointerException("Childbean cannot be null");
		}
		children.add(b);
		b.setParent(this);
		return b;
	}

	private void setParent(WData jBean) {
		this.parent = jBean;
	}

	public WData add(final String string, WData bean) {
		bean.setName(string);
		add(bean);
		return bean;
	}

	public WData get(final String name) {
		List<WData> lc = children;
		for (WData jBean : lc) {
			if (jBean.getName().equals(name)) {
				return jBean;
			}
		}
		// if child not found
		if (this.name != null && this.name.equals(name)) {
			return this;
		} else {
			return null;
		}
	}

	public void setName(final String nname) {
		if (nname == null || nname.indexOf(' ') >= 0
				|| nname.indexOf('\n') >= 0) {
			throw new IllegalArgumentException("JBean name illegal (" + nname
					+ ")");
		} else {
			this.name = nname;
		}
	}

	public String getName() {
		return name;
	}

	public XML toXML() {
		StringBuilder sb = new StringBuilder();
		toXML(sb);
		return new XML(sb.toString());
	}

	public void addValue(final String string, String string2) {
		if (string2 != null) {
			string2 = string2.trim();
			if (string2.length() > 0) {
				while (get(string) != null) {
					delete(string);
				}
				add(string).setValue(string2);
			}
		} else {
			children.remove(string);
		}
	}

	private void delete(final String string) {
		WData value = get(string);
		if (value != null) {
			children.remove(value);
		}
	}

	public WData setValue(final String string2) {
		text = string2.trim();
		return this;
	}

	public int compareTo(WData o) {
		if (o == null) {
			return Integer.MIN_VALUE;
		} else if (o.getName() == null) {
			return Integer.MIN_VALUE;
		} else if (getName() == null) {
			return Integer.MAX_VALUE;
		} else {
			return o.getName().compareTo(getName());
		}
	}

	@Override
	public String toString() {
		return "JBean:" + toXML();
	}

	public WData add(final String beanname) {
		return add(beanname, new WData());
	}

	public int getIntValue(final String aname) {
		String s = getValue(aname);
		if (s != null) {
			return Integer.parseInt(s);
		} else {
			return 0;
		}
	}

	public long getLongValue(final String aname) {
		String s = getValue(aname);
		if (s != null) {
			return Long.parseLong(s);
		} else {
			return 0;
		}
	}

	public float getFloatValue(final String aname) {
		String s = getValue(aname);
		if (s != null) {
			return Float.parseFloat(s);
		} else {
			return 0;
		}
	}

	public double getDoubleValue(String aname) {
		String s = getValue(aname);
		if (s != null) {
			return Double.parseDouble(s);
		} else {
			return 0;
		}
	}

	public Boolean getBooleanValue(final String aname) {
		String s = getValue(aname);
		return "true".equals(s);
	}

	public WData getFirst() {
		return getChildren().get(0);
	}

	public void addValue(final String string, int num) {
		addValue(string, "" + num);
	}

	public void addValue(final String string, long num) {
		addValue(string, "" + num);
	}

	public WData addList(final String string, Set<String> list) {
		WData b = add(string);
		for (final String item : list) {
			b.add("item").setValue(item);
		}
		return b;
	}

	public void addValue(final String string, boolean bvalue) {
		this.addValue(string, "" + bvalue);
	}

	public WData find(final String string) {
		if (name.equals(string)) {
			return this;
		} else {
			return findChild(string);
		}
	}

	private WData findChild(final String string) {
		List<WData> cs = this.children;
		for (WData cb : cs) {
			WData findcb = cb.find(string);
			if (findcb != null) {
				return findcb;
			}
		}
		return null;
	}

	public MStringID getIDValue(final String string) {
		String sid = getValue(string);
		if (sid == null || sid.equals("null")) {
			sid = getAttribute(string);
		}

		if (sid == null || sid.equals("null")) {
			return null;
		} else {
			return new MStringID(sid);
		}
	}

	public void addChildValue(final String string, ObjectID id) {
		this.addValue(string, id.toString());
	}

	public UserID getUserAttribute(final String string) {
		return new UserID(getValue(string));
	}

	public void addValue(final String name, double value) {
		this.addValue(name, "" + value);
	}

	public void addChildren(WData nb) {
		for (WData c : nb.getChildren()) {
			if (!c.getChildren().isEmpty()) {
				WData ncb = new WData(c.getName());
				add(ncb);
				ncb.addChildren(c);
			} else {
				addValue(c.getName(), c.getText());
			}
		}

	}

	public WData getRoot() {
		if (parent != null) {
			return parent.getRoot();
		} else {
			return this;
		}
	}

	public void setValue(ObjectID id) {
		setValue(id.toString());
	}

	public void setAttribute(final String qName, String value) {
		attributes.put(qName, value);
	}

	public String getContentHash() {
		return new BytesHash(toXML().toString().getBytes()).toString();
	}

	public void addValue(final String string, ObjectID id) {
		this.addValue(string, id.toString());
	}

	public String getAttribute(final String string) {
		return attributes.get(string);
	}

	public void setBase64Value(final String valuename, String value) {
		if (value != null) {
			byte[] bytes64 = Base64.encodeBase64(value.getBytes());
			addValue(valuename, new String(bytes64));
		} else {
			delete(valuename);
		}
	}

	public String getBase64Value(final String string) {
		String value64 = getValue(string);
		if (value64 != null) {
			byte[] bytes = Base64.decodeBase64(value64.getBytes());
			return new String(bytes);
		} else {
			return null;
		}
	}
}
