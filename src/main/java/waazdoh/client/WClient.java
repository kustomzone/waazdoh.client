package waazdoh.client;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import waazdoh.client.model.User;
import waazdoh.client.model.UserID;
import waazdoh.client.model.WData;
import waazdoh.client.model.WResponse;
import waazdoh.client.model.objects.Bookmarks;
import waazdoh.client.service.WService;
import waazdoh.client.storage.BeanStorage;
import waazdoh.util.MPreferences;
import waazdoh.util.MStringID;

public class WClient {
	private WService service;
	private MPreferences preferences;
	private boolean running = true;
	//
	private Set<WClientListener> listeners = new HashSet<WClientListener>();
	private Bookmarks bookmarks;
	private final BeanStorage beanstorage;
	private final BinarySource source;

	public WClient(MPreferences p, BinarySource binarysource,
			BeanStorage beanstorage, WService nservice) {
		this.preferences = p;
		this.source = binarysource;
		this.beanstorage = beanstorage;
		this.service = nservice;
	}

	public boolean isRunning() {
		if (source == null || !source.isRunning()) {
			return false;
		}

		if (service == null || !service.isLoggedIn()) {
			return false;
		}

		return running;
	}

	public Bookmarks getBookmarks() {
		return bookmarks;
	}

	public UserID getUserID() {
		return service.getUserID();
	}

	public BinarySource getBinarySource() {
		return source;
	}

	public WService getService() {
		return service;
	}

	public String getInfoText() {
		return source.getInfoText();
	}

	public MPreferences getPreferences() {
		return preferences;
	}

	public boolean trySavedSession() {
		return setSession(getPreferences().get(
				MPreferences.PREFERENCES_SESSION, ""));
	}

	public boolean setSession(final String session) {
		if (!service.isLoggedIn()) {
			if (service.setSession(session)) {
				source.setService(service);
				getPreferences().set(MPreferences.PREFERENCES_SESSION, session);
				loggedIn();
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	public void stop() {
		running = false;
		source.close();
	}

	public void addListener(WClientListener listener) {
		listeners.add(listener);
	}

	private void loggedIn() {
		bookmarks = new Bookmarks(service);

		for (WClientListener clientListener : listeners) {
			clientListener.loggedIn();
		}
	}

	public WClientAppLogin requestAppLogin() {
		WData b = getService().requestAppLogin();
		return new WClientAppLogin(b);
	}

	public WClientAppLogin checkAppLogin(MStringID id) {
		WData b = getService().checkAppLogin(id);
		WClientAppLogin applogin = new WClientAppLogin(b);
		if (applogin.getSessionId() != null) {
			setSession(applogin.getSessionId());
		}
		return applogin;
	}

	public String readStorageArea(String string) {
		return getService().readStorageArea(string);
	}

	public List<MStringID> search(String searchitem, int index, int count) {
		WResponse bresult = getService().search(searchitem, index, count);
		List<MStringID> idlist = bresult.getIDList();
		return idlist;
	}

	public BeanStorage getBeanStorage() {
		return this.beanstorage;
	}

	public User getUser(UserID userID) {
		WResponse r = getService().getUser(userID);
		if (r != null && r.getBean() != null && r.isSuccess()) {
			return new User(r.getBean());
		} else {
			return null;
		}
	}
}
