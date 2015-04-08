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
package waazdoh.testing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.xml.sax.SAXException;

import waazdoh.common.MStringID;
import waazdoh.common.UserID;
import waazdoh.common.WData;
import waazdoh.common.WLogger;
import waazdoh.common.XML;
import waazdoh.common.client.ServiceClient;
import waazdoh.common.service.ObjectsService;
import waazdoh.common.service.StorageAreaService;
import waazdoh.common.service.UsersService;
import waazdoh.common.vo.AppLoginVO;
import waazdoh.common.vo.LoginVO;
import waazdoh.common.vo.ObjectVO;
import waazdoh.common.vo.ProvileVO;
import waazdoh.common.vo.ReturnVO;
import waazdoh.common.vo.UserVO;

public final class StaticService implements ServiceClient {
	private UserID userid;
	private StorageAreaService storagearea;
	private UsersService users;
	private ObjectsService objects;
	private String session;
	private String username;

	private static Map<String, ObjectVO> data = new HashMap<String, ObjectVO>();
	private static Map<String, String> storage = new HashMap<>();
	private static Map<String, UserVO> userlist = new HashMap<>();

	private WLogger logger = WLogger.getLogger(this);

	public StaticService(String username1) {
		this.username = username1;
	}

	@Override
	public void setAuthenticationToken(String session) {
		this.session = session;
		this.userid = new UserID(new MStringID().toString());

		UserVO uservo = new UserVO();
		uservo.setUsername(username);
		uservo.setUserid(userid.toString());
		userlist.put(userid.toString(), uservo);
	}

	@Override
	public String getAuthenticationToken() {
		return session;
	}

	@Override
	public ObjectsService getObjects() {
		if (objects == null) {
			objects = new ObjectsService() {

				@Override
				public boolean write(String objectid, String testdata) {
					try {
						new WData(new XML(testdata));
						data.put(objectid, new ObjectVO(testdata));
						return true;
					} catch (SAXException e) {
						logger.error(e);
						return false;
					}
				}

				@Override
				public List<String> search(String search, int i, int j) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public ObjectVO read(String string) {
					return data.get(string);
				}

				@Override
				public boolean publish(String objectid) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public boolean delete(String objectid) {
					// TODO Auto-generated method stub
					return false;
				}
			};
		}

		return objects;
	}

	@Override
	public UsersService getUsers() {
		if (users == null) {
			users = new UsersService() {

				@Override
				public List<UserVO> search(String string, int i) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public boolean saveProfile(ProvileVO profile) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public AppLoginVO requestAppLogin() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void registerOAuthService(String string, String string2) {
					// TODO Auto-generated method stub

				}

				@Override
				public UserVO getWithName(String username) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public UserVO getUser(String userid) {
					return userlist.get(userid);
				}

				@Override
				public ProvileVO getProfile() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public ReturnVO denyApplication(String appid) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public UserVO checkSession() {
					return userlist.get(userid.toString());
				}

				@Override
				public boolean checkInvitatation(String string) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public AppLoginVO checkAppLogin(String id) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public LoginVO authenticateOAuth(LoginVO vo) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public ReturnVO acceptApplication(String appid) {
					// TODO Auto-generated method stub
					return null;
				}
			};
		}
		return users;
	}

	@Override
	public StorageAreaService getStorageArea() {
		if (storagearea == null) {
			storagearea = new StorageAreaService() {

				@Override
				public boolean write(String path, String name) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public String read(String path) {
					return storage.get(path);
				}

				@Override
				public List<String> list(String path) {
					// TODO Auto-generated method stub
					return null;
				}
			};
		}
		return storagearea;
	}

	public String createSession() {
		session = UUID.randomUUID().toString();
		return session;
	}

}
