package waazdoh.cutils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BytesHash {

	private byte[] hash;

	public BytesHash(byte[] bytes) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			hash = md.digest(bytes);
		} catch (NoSuchAlgorithmException e) {
			MLogger.getLogger(this).error(e);
		}
	}

	public String toString() {
		BigInteger bigInteger = new BigInteger(hash);
		String s = String.format("%040x", bigInteger);
		return s.replace('-', 'A');
	}

}
