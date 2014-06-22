package waazdoh.cp2p;

import waazdoh.client.model.MBinaryID;

public interface WhoHasListener {

	void binaryRequested(MBinaryID streamid, Integer count);

}
