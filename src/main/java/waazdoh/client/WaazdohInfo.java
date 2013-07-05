package waazdoh.client;

public class WaazdohInfo {
	public final static String version = "0.2.3";
	public static final int DOWNLOADER_MAX_REQUESTED_PIECELENGTH = 200000; // should be larger than WHOHAS_RESPONSE_MAX_PIECE_SIZE 
	public static final int WHOHAS_RESPONSE_MAX_PIECE_SIZE = 100000;
	public static final Integer RESPONSECOUNT_DOWNLOADTRIGGER = 20;
	public static final int MAX_RESPONSE_WAIT_TIME = 40000;

}
