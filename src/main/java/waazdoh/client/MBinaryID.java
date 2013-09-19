package waazdoh.client;

public class MBinaryID extends MStringID {

	public MBinaryID(MStringID idValue) {
		super(idValue.toString());
	}

	public MBinaryID() {
		super();
	}

	public MBinaryID(String sid) {
		super(sid);
	}
}
