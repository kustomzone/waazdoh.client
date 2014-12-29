package waazdoh.cp2p;

import waazdoh.client.model.BinaryID;

public interface WhoHasListener {

	void binaryRequested(BinaryID streamid, Integer count);

}
