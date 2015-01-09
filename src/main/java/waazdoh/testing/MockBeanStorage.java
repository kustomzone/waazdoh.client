package waazdoh.testing;

import java.util.HashMap;
import java.util.Map;

import waazdoh.client.BeanStorage;
import waazdoh.client.model.WData;
import waazdoh.util.MStringID;

public class MockBeanStorage implements BeanStorage {

	private Map<MStringID, WData> beans = new HashMap<MStringID, WData>();

	@Override
	public void addBean(final MStringID id, WData response) {
		beans.put(id, response);
	}

	@Override
	public WData getBean(final MStringID id) {
		return beans.get(id);
	}

}
