package waazdoh.client;

import waazdoh.client.model.BookmarksListener;
import waazdoh.client.model.BookmarkGroup;
import waazdoh.client.model.Bookmarks;
import waazdoh.testing.StaticService;

public class TestBookmarks extends WCTestCase {
	public void testListener() {
		StaticService service = new StaticService();
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
}
