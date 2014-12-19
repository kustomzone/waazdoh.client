package waazdoh.client;

import waazdoh.client.model.BookmarksListener;
import waazdoh.client.model.WBookmarkGroup;
import waazdoh.client.model.WBookmarks;
import waazdoh.testing.StaticService;

public class TestBookmarks extends WCTestCase {
	public void testListener() {
		StaticService service = new StaticService();
		WBookmarks ms = new WBookmarks(service);

		final StringBuilder st = new StringBuilder();

		final String string = "listener triggered";
		ms.addListener(new BookmarksListener() {
			@Override
			public void groupAdded(WBookmarkGroup group) {
				st.append(string);
			}
		});

		ms.addGroup("testgroup");
		
		assertEquals(string, st.toString());
	}
}
