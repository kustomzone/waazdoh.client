package waazdoh.client;

import java.net.MalformedURLException;

import org.xml.sax.SAXException;

import waazdoh.client.model.objects.BookmarkGroup;
import waazdoh.client.model.objects.Bookmarks;
import waazdoh.client.model.objects.BookmarksListener;
import waazdoh.testing.StaticService;

public class TestBookmarks extends WCTestCase {
	public void testListener() {
		StaticService service = new StaticService(getRandomUserName());
		Bookmarks ms = new Bookmarks(service);

		final StringBuilder st = new StringBuilder();

		final String string = "listener triggered";
		ms.addListener(new BookmarksListener() {
			@Override
			public void groupAdded(BookmarkGroup group) {
				st.append(string);
			}
		});

		ms.addGroup("testgroup");

		assertEquals(string, st.toString());
	}

	public void testRead() throws MalformedURLException, SAXException {
		WClient c = getClient(getRandomUserName(), false);
		BookmarkGroup bg = c.getBookmarks().addGroup("testbookmark");
		bg.add("testvalue", "value");
		assertNotNull(bg);
		BookmarkGroup bgb = c.getBookmarks().get("testbookmark");
		assertEquals(bg.get("testvalue"), bgb.get("testvalue"));
	}
}
