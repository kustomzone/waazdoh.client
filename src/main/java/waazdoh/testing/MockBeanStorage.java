package waazdoh.testing;

import java.util.HashMap;
import java.util.Map;

import waazdoh.common.BeanStorage;
import waazdoh.common.MStringID;
import waazdoh.common.WData;

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

	@Override
	public Iterable<MStringID> getLocalSetIDs(String search) {
		return beans.keySet();
	}
}
