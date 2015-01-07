package waazdoh.testing;

import java.util.HashMap;
import java.util.Map;

import waazdoh.client.BeanStorage;
import waazdoh.client.model.WData;

public class MockBeanStorage implements BeanStorage {

	private Map<String, WData> beans = new HashMap<String, WData>();

	@Override
	public void addBean(final String id, WData response) {
		beans.put(id, response);
	}

	@Override
	public WData getBean(final String id) {
		return beans.get(id);
	}

}
