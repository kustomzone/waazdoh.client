package waazdoh.client.model;

public final class WaazdohInfo {
	public static final String version = "1.1.4";
	public static final int DOWNLOADER_MAX_REQUESTED_PIECELENGTH = 200000; // should
																			// be
																			// larger
																			// than
																			// WHOHAS_RESPONSE_MAX_PIECE_SIZE
	public static final int WHOHAS_RESPONSE_MAX_PIECE_SIZE = 100000;
	public static final Integer RESPONSECOUNT_DOWNLOADTRIGGER = 20;
	public static final int MAX_RESPONSE_WAIT_TIME = 40000;
	public static final int DOWNLOAD_RESET_DELAY = 8000;

	private WaazdohInfo() {

	}
}
