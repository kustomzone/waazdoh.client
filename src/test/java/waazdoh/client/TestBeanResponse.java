package waazdoh.client;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import waazdoh.client.model.JBeanResponse;

public class TestBeanResponse extends TestCase {
	public void testIDList() throws SAXException {
		StringBuilder sb = new StringBuilder();
		sb.append("<response><" + JBeanResponse.IDLIST + ">");

		sb.append("<" + JBeanResponse.IDLIST_ITEM + ">");
		sb.append("" + Math.random());
		sb.append("</" + JBeanResponse.IDLIST_ITEM + ">");

		sb.append("<" + JBeanResponse.IDLIST_ITEM + ">");
		sb.append("" + System.currentTimeMillis());
		sb.append("</" + JBeanResponse.IDLIST_ITEM + ">");

		sb.append("</" + JBeanResponse.IDLIST + ">");
		sb.append("<success>true</success>");
		sb.append("</response>");

		JBeanResponse r = new JBeanResponse(sb.toString());

		assertTrue(r.isSuccess());

		assertEquals(2, r.getIDList().size());
	}

	public void testTrue() {
		JBeanResponse r = JBeanResponse.getTrue();
		assertTrue(r.isSuccess());
	}

	public void testFalse() {
		JBeanResponse r = JBeanResponse.getFalse();
		assertFalse(r.isSuccess());
	}

	public void testError() {
		JBeanResponse r = JBeanResponse.getError("test");
		assertFalse(r.isSuccess());
		assertEquals("test", r.getBean().getValue("error"));
	}
}
