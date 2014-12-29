package waazdoh.client;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import waazdoh.client.model.WResponse;

public class TestBeanResponse extends TestCase {
	public void testIDList() throws SAXException {
		StringBuilder sb = new StringBuilder();
		sb.append("<response><" + WResponse.IDLIST + ">");

		sb.append("<" + WResponse.IDLIST_ITEM + ">");
		sb.append("" + Math.random());
		sb.append("</" + WResponse.IDLIST_ITEM + ">");

		sb.append("<" + WResponse.IDLIST_ITEM + ">");
		sb.append("" + System.currentTimeMillis());
		sb.append("</" + WResponse.IDLIST_ITEM + ">");

		sb.append("</" + WResponse.IDLIST + ">");
		sb.append("<success>true</success>");
		sb.append("</response>");

		WResponse r = new WResponse(sb.toString());

		assertTrue(r.isSuccess());

		assertEquals(2, r.getIDList().size());
	}

	public void testTrue() {
		WResponse r = WResponse.getTrue();
		assertTrue(r.isSuccess());
	}

	public void testFalse() {
		WResponse r = WResponse.getFalse();
		assertFalse(r.isSuccess());
	}

	public void testError() {
		WResponse r = WResponse.getError("test");
		assertFalse(r.isSuccess());
		assertEquals("test", r.getBean().getValue("error"));
	}
}
