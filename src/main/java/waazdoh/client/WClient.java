package waazdoh.client;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import waazdoh.client.model.User;
import waazdoh.common.BeanStorage;
import waazdoh.common.UserID;
import waazdoh.common.WLogger;
import waazdoh.common.WObject;
import waazdoh.common.WPreferences;
import waazdoh.common.client.ServiceClient;
import waazdoh.common.service.ObjectsService;
import waazdoh.common.vo.AppLoginVO;
import waazdoh.common.vo.StorageAreaVO;
import waazdoh.common.vo.UserVO;

public class WClient {
	private ServiceClient service;
	private WPreferences preferences;
	private boolean running = true;
	//
	private Set<WClientListener> listeners = new HashSet<WClientListener>();
	private final BeanStorage beanstorage;
	private final BinarySource source;
	private UserID userid;

	private WLogger logger = WLogger.getLogger(this);
	private List<Filter> filters = new LinkedList<WClient.Filter>();

	public WClient(WPreferences p, BinarySource binarysource, BeanStorage beanstorage, ServiceClient nservice) {
		this.preferences = p;
		this.source = binarysource;
		this.beanstorage = beanstorage;
		this.service = nservice;
	}

	public boolean isRunning() {
		if (source == null || !source.isRunning()) {
			return false;
		}

		if (service == null || !isLoggedIn()) {
			return false;
		}

		return running;
	}

	public boolean isLoggedIn() {
		return this.userid != null && getService().getAuthenticationToken() != null;
	}

	public UserID getUserID() {
		return this.userid;
	}

	public BinarySource getBinarySource() {
		return source;
	}

	public ServiceClient getService() {
		return service;
	}

	public String getInfoText() {
		return source.getInfoText();
	}

	public WPreferences getPreferences() {
		return preferences;
	}

	public boolean trySavedSession() {
		return setSession(getPreferences().get(WPreferences.PREFERENCES_SESSION, ""));
	}

	public boolean setSession(final String session) {
		if (userid == null) {
			try {
				service.setAuthenticationToken(session);
				UserVO user = service.getUsers().checkSession();
				if (user != null && user.isSuccess()) {
					this.userid = new UserID(user.getUserid());
					source.setClient(this);
					getPreferences().set(WPreferences.PREFERENCES_SESSION, session);
					loggedIn();
					return true;
				} else {
					return false;
				}
			} catch (Exception e) {
				logger.error(e);
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
		for (WClientListener clientListener : listeners) {
			clientListener.loggedIn();
		}
	}

	public AppLoginVO requestAppLogin() {
		AppLoginVO b = getService().getUsers().requestAppLogin();
		return b;
	}

	public AppLoginVO checkAppLogin(String id) {
		AppLoginVO b = getService().getUsers().checkAppLogin(id);
		if (b != null && b.getSessionid() != null) {
			setSession(b.getSessionid());
		}
		return b;
	}

	public String readStorageArea(String string) {
		return getService().getStorageArea()
				.read(new StorageAreaVO(getService().getUser().getUsername(), string, null));
	}

	public List<String> search(String searchitem, int index, int count) {
		return getService().getObjects().search(searchitem, index, count);
	}

	public List<UserVO> searchUsers(String searchitem, int index) {
		return getService().getUsers().search(searchitem, index);
	}

	public BeanStorage getBeanStorage() {
		return this.beanstorage;
	}

	public User getUser(UserID userID) {
		UserVO r = getService().getUsers().getUser(userID.toString());
		if (r != null && r.isSuccess()) {
			return new User(r);
		} else {
			return null;
		}
	}

	@Override
	public String toString() {
		UserVO user = getService().getUser();
		String username = user != null ? user.getUsername() : "unknown";
		return "WClient[" + username + "][connected:" + isRunning() + "][bsource:" + source + "]";
	}

	public ObjectsService getObjects() {
		return getService().getObjects();
	}

	public boolean filter(WObject o) {
		for (Filter filter : filters) {
			if (!filter.check(o)) {
				return false;
			}
		}

		return true;
	}

	public void addObjectFilter(Filter f) {
		filters.add(f);
	}

	public static interface Filter {
		boolean check(WObject o);

	}
}
