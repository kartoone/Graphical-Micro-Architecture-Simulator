package com.arm.legv8simulator.client.memory;

import java.util.HashMap;

/**
 * <code>Cache</code> is the class used to implement a LEGv8 cache
 * <p>
 * <code>Cache</code> is just an array of CacheEntries where each entry has a tag, valid bit, and the block data is stored in Big Endian as an array of longs.
 * <p>
 *
 * Because ARM/LEGv8 allows unaligned memory addresses, a single load or store might span TWO entries and might span TWO (at most) longs within the block since a doubleword is the biggest data we could be reading.
 * We are storing the block as an array of longs to keep with the same philosophy as Memory.java where typical register access is a long so presumably most memory reads will be for an entire long.
 * So to keep things as simple as possible (it's still pretty ridiculous), let's require the blocksize to be a multiple of 8
 * @author Brian Toone, Samford University, 2023
 *
 */


public class Cache { 

	private class CacheEntry {

		long tag; 	
		boolean valid; 
		long[] block; // these are the individual longs within the block 

		// cache entry constructor can do the work of calculating tag 
		CacheEntry(long address, int blocksize) {
			tag = 0L;
			valid = false;
			block = new long[blocksize/8]; // since blocksize is specified as bytes but we are storing this as longs (doublewords), we need the blocksize to be a multiple of 8 ... to remove this limitation store the block as an array of bytes instead
		}

	}

	public static final int DEFAULT_BLOCKSIZE = Memory.DOUBLEWORD_SIZE; // measured in bytes .... for at least a little bit of simplicity the blocksize needs to be a multiple of 8
	public static final int DEFAULT_SIZE = 10*DEFAULT_BLOCKSIZE; 		// also measured in bytes .... so the default number of entries in the cache is DEFAULT_SIZE / DEFAULT_BLOCKSIZE
	
	/**
	 * Cache constructor with a specified configuration
	 * @param size	the total number of bytes in the cache ... MUST be a multiple of blocksize
	 * @param blocksize	the total number of bytes in the each block ... MUST be a multiple of 8 for our simulator but doesn't have to be for actual LEGv8
	 */
	public Cache(CacheConfiguration config) throws CacheConfigurationException {
		if (config.getBlocksize() % 8 != 0)
			throw new CacheConfigurationException(config);
		if (config.getSize() % config.getBlocksize() != 0)
			throw new CacheConfigurationException(config);
		this.size = config.getSize();
		this.blocksize = config.getBlocksize();
		initCache();
	}

	public Cache() {
		this.size = DEFAULT_SIZE;
		this.blocksize = DEFAULT_BLOCKSIZE;
		initCache();
	}

	protected void initCache() {
		cache = new CacheEntry[size/blocksize]; // should be evenly divisible and blocksize ... UI should not let user select something that isn't evenly divisible as that doesn't even make sense from a hardware perspective... or we need to introduce some sort of CacheConfigurationException
	}

	// for all of thse memory reads, we first figure out which CacheEntry(s) are involved
	// if only one CacheEntry, then simply request the data from the CacheEntry
	public long loadDoubleword(long address) {
		return 0L;
	}

	public int loadSignedword(long address) {
		return 0;
	}

	public int loadHalfword(long address) {
		return 0;
	}

	public int loadByte(long address) {
		return 0;
	}

	public void storeDoubleword(long address, long value) {
	}

	public void storeWord(long address, int value) {
	}

	public void storeHalfword(long address, short value) {
	}

	public void storeByte(long address, byte value) {
	}

	private int size;
	private int blocksize;
	private CacheEntry[] cache; // the actual cache ... just an array of size/blocksize entries
}
