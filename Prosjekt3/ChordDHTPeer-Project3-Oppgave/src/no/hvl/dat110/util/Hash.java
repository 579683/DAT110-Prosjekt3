package no.hvl.dat110.util;

/**
 * project 3
 * @author tdoy
 *
 */

import no.hvl.dat110.middleware.Message;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {

	public static int mbit = 128;            // using SHA-1 compresses/hashes to 160bits

	public static int sbit = 4;            // we use this for the size of the finger table

	private static BigInteger hashint;

	public static BigInteger hashOf(String entity) {

		// Task: Hash a given string using MD5 and return the result as a BigInteger.

	    MessageDigest md = null;

	    try {
	        md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
        }

		// we use MD5 with 128 bits digest
		// compute the hash of the input 'entity'
	    byte[] digest = md.digest(entity.getBytes());

		// convert the hash into hex format
	    String hex = toHex(digest);

		// convert the hex into BigInteger
        hashint = new BigInteger(hex, 16);


		return hashint;
	}


	public static BigInteger addressSize() {

		// Task: compute the address size of MD5
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

		// get the digest length
		// compute the number of bits = digest length * 8
		int bits = md.getDigestLength() * 8;

		// compute the address size = 2 ^ number of bits
		BigInteger adrStr = BigInteger.valueOf(2).pow(bits);

		// return the address size
		return adrStr;
	}

	public static int bitSize() {

		int digestlen = 0;

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            digestlen = md.getDigestLength();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


		return digestlen*8;
	}

	public static String toHex(byte[] digest) {
		StringBuilder strbuilder = new StringBuilder();
		for(byte b : digest) {
			strbuilder.append(String.format("%02x", b&0xff));
		}
		return strbuilder.toString();
	}

}
