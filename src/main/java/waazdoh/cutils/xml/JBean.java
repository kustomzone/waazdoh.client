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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import waazdoh.cutils.MID;
import waazdoh.cutils.MLogger;
import waazdoh.cutils.UserID;

public final class JBean implements Comparable<JBean> {
	private String name, text;
	private List<JBean> children = new LinkedList<JBean>();
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
		sb.append("<" + getName() + ">");
		//
		if (text != null) {
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

	public void parseXml(XML oxml) {
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
			} catch (SAXException e) {
				e.printStackTrace();
				log.info("tried to parse " + oxml);
				log.info("tried to parse " + xml);
				log.error(e);
				throw new IllegalArgumentException(e);
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

	public JBean(String name) {
		setName(name);
	}

	public JBean(XML xml) {
		parseXml(xml);
	}

	public List<JBean> getChildren() {
		return new LinkedList<JBean>(children);
	}

	public String getAttribute(String string) {
		JBean child = get(string);
		if (child != null) {
			return child.getValue();
		} else {
			return null;
		}
	}

	public String getValue() {
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

	public JBean add(String string, JBean bean) {
		bean.setName(string);
		add(bean);
		return bean;
	}

	public JBean get(String name) {
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

	public void setName(String nname) {
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

	public void addAttribute(String string, String string2) {
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

	private void delete(String string) {
		children.remove(get(string));
	}

	public JBean setValue(String string2) {
		this.text = string2.trim();
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

	public JBean add(String beanname) {
		return add(beanname, new JBean());
	}

	public int getAttributeInt(String aname) {
		String s = getAttribute(aname);
		if (s != null) {
			return Integer.parseInt(s);
		} else {
			return 0;
		}
	}

	public long getAttributeLong(String aname) {
		String s = getAttribute(aname);
		if (s != null) {
			return Long.parseLong(s);
		} else {
			return 0;
		}
	}

	public float getAttributeFloat(String aname) {
		String s = getAttribute(aname);
		if (s != null) {
			return Float.parseFloat(s);
		} else {
			return 0;
		}
	}

	public Boolean getAttributeBoolean(String aname) {
		String s = getAttribute(aname);
		if (s != null) {
			return Boolean.parseBoolean(s);
		} else {
			return null;
		}
	}

	public JBean getFirst() {
		return getChildren().get(0);
	}

	public void addAttribute(String string, int num) {
		addAttribute(string, "" + num);
	}

	public void addAttribute(String string, long num) {
		addAttribute(string, "" + num);
	}

	public JBean addList(String string, Set<String> list) {
		JBean b = add(string);
		for (String item : list) {
			b.add("item").setValue(item);
		}
		return b;
	}

	public void addAttribute(String string, boolean bvalue) {
		this.addAttribute(string, "" + bvalue);
	}

	public JBean find(String string) {
		if (name.equals(string)) {
			return this;
		} else {
			return findChild(string);
		}
	}

	private JBean findChild(String string) {
		List<JBean> cs = this.children;
		for (JBean cb : cs) {
			JBean findcb = cb.find(string);
			if (findcb != null) {
				return findcb;
			}
		}
		return null;
	}

	public MID getIDAttribute(String string) {
		String sid = getAttribute(string);
		if (sid == null || sid.equals("null")) {
			return null;
		} else {
			return new MID(sid);
		}
	}

	public void addAttribute(String string, MID id) {
		this.addAttribute(string, id.toString());
	}

	public UserID getUserAttribute(String string) {
		return new UserID(getAttribute(string));
	}

	public void addAttribute(String name, float value) {
		this.addAttribute(name, "" + value);
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
}
