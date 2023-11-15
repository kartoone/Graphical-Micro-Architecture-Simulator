package com.arm.legv8simulator.client.memory;

/**
 * Thrown when blocksize isn't a multiple of 8 or size isn't evenly divisible by blocksize
 * 
 * @author Brian Toone, 2023
 */
public class CacheConfigurationException extends Exception {
	
	private static final long serialVersionUID = 2L;
	
	/**
	 * @param size 		Total cache size in bytes 
	 * @param blocksize	Number of bytes in each block 
	 */
	public CacheConfigurationException(CacheConfiguration config) {
		super();
		this.size = config.getSize();
		this.blocksize = config.getBlocksize();
	}
	
	@Override
	public String getMessage() {
		if (size % blocksize != 0)
			return "Total cache size " + size + " bytes is not a multiple of " + blocksize + " bytes.";
		if (!Cache.isPowerOfTwo(size / blocksize))
			return "Number of entries " + (size/blocksize) + " is not a power of 2, leading to invalid index";

		return ""; // this should never happen, it would only be if this exception is thrown on a VALID configuration
	}
	
	private int size;
	private int blocksize;
}
