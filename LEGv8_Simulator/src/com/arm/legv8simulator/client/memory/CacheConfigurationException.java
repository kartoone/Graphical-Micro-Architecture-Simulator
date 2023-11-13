package com.arm.legv8simulator.client.memory;

/**
 * Thrown when blocksize isn't a multiple of 8 or size isn't evenly divisible by blocksize
 * 
 * @author Brian Toone, 2023
 */
public class CacheConfigurationException extends Exception {
	
	private static final long serialVersionUID = 2L;
	
	/**
	 * @param size 		Total cache size in bytes (must be evenly divisible by blocksize)	
	 * @param blocksize	Number of bytes in each block (must be multiple of 8)
	 */
	public CacheConfigurationException(CacheConfiguration config) {
		super();
		this.size = config.getSize();
		this.blocksize = config.getBlocksize();
	}
	
	@Override
	public String getMessage() {
		if (blocksize % 8 != 0)
			return "Blocksize " + blocksize + " bytes is not a multiple of 8";
		if (size % blocksize != 0)
			return "Total cache size " + size + " bytes is not a multiple of " + blocksize + " bytes.";
		return "check your code, this does not appear to be an exception";
	}
	
	private int size;
	private int blocksize;
}
