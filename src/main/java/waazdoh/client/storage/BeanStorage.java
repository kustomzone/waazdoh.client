package waazdoh.client.storage;

import waazdoh.common.MStringID;
import waazdoh.common.WData;

public interface BeanStorage {

	WData getBean(MStringID id);

	void addBean(MStringID id, WData b);

	Iterable<MStringID> getLocalSetIDs(String search);

}
