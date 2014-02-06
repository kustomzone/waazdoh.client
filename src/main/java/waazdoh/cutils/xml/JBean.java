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
package waazdoh.cutils.xml;

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

import waazdoh.cutils.BytesHash;
import waazdoh.cutils.MID;
import waazdoh.cutils.MLogger;
import waazdoh.cutils.MStringID;
import waazdoh.cutils.UserID;

public final class JBean implements Comparable<JBean> {
	private String name, text;
	private List<JBean> children = new LinkedList<JBean>();
	private Map<String, String> attributes = new HashMap<String, String>();
	private MLogger log = MLogger.getLogger(this);
	private JBean parent;

	public List<String> getChildNames() {
		List<String> ret = new LinkedList<String>();
		List<JBean> lchildren = children;
		for (JBean jBean : lchildren) {
			ret.add(jBean.getName());
		}
		return ret;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof JBean) {
			JBean b = (JBean) obj;
			return toXML().equals(b.toXML());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return toXML().hashCode();
	}

	public void toXML(StringBuffer sb) {
		toXML(0, sb);
	}

	public void toXML(int indent, StringBuffer sb) {
		indent(indent, sb);
		sb.append("<" + getName());
		for (final String key : attributes.keySet()) {
			String value = attributes.get(key);
			sb.append(" " + key + "=\"" + value + "\"");
		}
		sb.append(">");
		//
		if (text != null && getChildren().size() == 0) {
			sb.append(text);
		} else {
			sb.append("\n");
		}
		//
		List<JBean> children = new ArrayList<JBean>(getChildren());
		java.util.Collections.sort(children);
		for (JBean jbean : children) {
			jbean.toXML(indent + 1, sb);
		}
		//
		if (text == null) {
			indent(indent, sb);
		}
		//
		sb.append("</" + getName() + ">\n");
	}

	public String toText() {
		XML xml = toXML();
		String ret = xml.toString();
		ret = ret.replaceAll("<", "[");
		ret = ret.replaceAll(">", "]");
		return ret;
	}

	private void indent(int indent, StringBuffer sb) {
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
			// xml = xml.replace("\n", "");
			// xml = xml.replace("\t", "");
			//
			XMLReader r;
			try {
				r = XMLReaderFactory.createXMLReader();
				r.setContentHandler(new JBeanContentHandler(this));
				InputSource input = new InputSource(new StringReader(xml));
				r.parse(input);
			} catch (IOException e) {
				e.printStackTrace();
				log.info("tried to parse " + oxml);
				log.error(e);
				throw new IllegalArgumentException(e);
			}
		}
	}

	private JBean() {
		//
	}

	public JBean(final String name) {
		setName(name);
		add("license").setValue(
				"GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html");
	}

	public JBean(XML xml) throws SAXException {
		parseXml(xml);
	}

	public List<JBean> getChildren() {
		return new LinkedList<JBean>(children);
	}

	public String getValue(final String string) {
		JBean child = get(string);
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

	public JBean add(JBean b) {
		if (b == null) {
			throw new NullPointerException("Childbean cannot be null");
		}
		children.add(b);
		b.setParent(this);
		return b;
	}

	private void setParent(JBean jBean) {
		this.parent = jBean;
	}

	public JBean add(final String string, JBean bean) {
		bean.setName(string);
		add(bean);
		return bean;
	}

	public JBean get(final String name) {
		List<JBean> lc = children;
		for (JBean jBean : lc) {
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
		StringBuffer sb = new StringBuffer();
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
		children.remove(get(string));
	}

	public JBean setValue(final String string2) {
		text = string2.trim();
		return this;
	}

	public int compareTo(JBean o) {
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

	public JBean add(final String beanname) {
		return add(beanname, new JBean());
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

	public Boolean getBooleanValue(final String aname) {
		String s = getValue(aname);
		if (s != null) {
			return Boolean.parseBoolean(s);
		} else {
			return null;
		}
	}

	public JBean getFirst() {
		return getChildren().get(0);
	}

	public void addValue(final String string, int num) {
		addValue(string, "" + num);
	}

	public void addValue(final String string, long num) {
		addValue(string, "" + num);
	}

	public JBean addList(final String string, Set<String> list) {
		JBean b = add(string);
		for (final String item : list) {
			b.add("item").setValue(item);
		}
		return b;
	}

	public void addValue(final String string, boolean bvalue) {
		this.addValue(string, "" + bvalue);
	}

	public JBean find(final String string) {
		if (name.equals(string)) {
			return this;
		} else {
			return findChild(string);
		}
	}

	private JBean findChild(final String string) {
		List<JBean> cs = this.children;
		for (JBean cb : cs) {
			JBean findcb = cb.find(string);
			if (findcb != null) {
				return findcb;
			}
		}
		return null;
	}

	public MStringID getIDValue(final String string) {
		String sid = getValue(string);
		if (sid == null || sid.equals("null")) {
			return null;
		} else {
			return new MStringID(sid);
		}
	}

	public void addChildValue(final String string, MID id) {
		this.addValue(string, id.toString());
	}

	public UserID getUserAttribute(final String string) {
		return new UserID(getValue(string));
	}

	public void addValue(final String name, float value) {
		this.addValue(name, "" + value);
	}

	public void addChildren(JBean nb) {
		for (JBean c : nb.getChildren()) {
			if (c.getChildren().size() > 0) {
				JBean ncb = new JBean(c.getName());
				add(ncb);
				ncb.addChildren(c);
			} else {
				addValue(c.getName(), c.getText());
			}
		}

	}

	public JBean getRoot() {
		if (parent != null) {
			return parent.getRoot();
		} else {
			return this;
		}
	}

	public void setValue(MID id) {
		setValue(id.toString());
	}

	public void setAttribute(final String qName, String value) {
		attributes.put(qName, value);
	}

	public String getContentHash() {
		return new BytesHash(toXML().toString().getBytes()).toString();
	}

	public void addValue(final String string, MID id) {
		this.addValue(string, id.toString());
	}

	public String getAttribute(final String string) {
		return attributes.get(string);
	}

	public void setBase64Value(final String valuename, String value) {
		byte[] bytes64 = Base64.encodeBase64(value.getBytes());
		addValue(valuename, new String(bytes64));
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
