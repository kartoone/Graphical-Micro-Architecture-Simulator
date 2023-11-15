package com.arm.legv8simulator.client.memory;

import java.util.HashMap;

/**
 * <code>Cache</code> is the class used to implement a LEGv8 cache
 * <p>
 * <code>Cache</code> is just an array of CacheEntries where each entry has a tag, valid bit, and the block data is stored in Big Endian as an array of longs.
 * <p>
 *
 * Instruction cache and data cache design is different. Instructions are only 32 bits long so we will ALWAYS be reading exactly one word from the cache. Furthermore, instruction memory accesses are always aligned. So it makes sense for instruction caches
 * to store an array of ints (4 bytes = 32 bits) for each "block" (i.e., CacheEntry).
 *
 * A data cache in ARM/LEGv8 is much trickier because many read/store instructions allow unaligned memory addresses. Furthermore, the amount of data being read can vary from a single byte (LDURB) all the way to a double-word (LDUR).
 * So we could have a read that spans multiple cache entries if it begins near the end of a cache block and extends into the next cache block. If use an array of ints or longs for each block, we might need to combine part of one int (or long)
 * with another int or long. To avoid that particular nasty section of shifting/anding/oring/combining two ints together to get the desired data, it makes more sense to store the entire block as an array of bytes.
 *
 * Even though an I-cache make sense to store block data as an array of ints since we will always be grabbing a memory-aligned int with each read, our compromise to use one class for both I-cache and D-cache
 * is store each block as an array of bytes and add methods to the cache for grabbing the desired amount of data starting from a particular address. Blocksize then needs to be a power of 2, which then determines our block index. 
 * We no longer think of the address as having a byte offset since we are storing the block as individual bytes. Instead, the index is simply log2 total cache size / blocksize which gives us the total number of entries in the cache (WHICH IS WHAT YOU NEED FOR THE INDEX).
 * 
 * @author Brian Toone, Samford University, 2023
 *
 */


public class Cache { 

	private class CacheEntry {

		long tag; 	
		boolean valid; 
		byte[] block; // these are the individual bytes within the block

		// cache entry constructor is only called during initial setup of cache (so no data ready for the block yet)
		CacheEntry(long tag, int blocksize) {
			tag = 0L;
			valid = false;
			block = new byte[blocksize]; 
		}

	}

	// restrictions on these defaults ... the total number of entries in the cache (SIZE/BLOCKSIZE) has to be a power of 2 to be properly be indexed
	public static final int DEFAULT_BLOCKSIZE = Memory.DOUBLEWORD_SIZE; // measured in bytes .... 
	public static final int DEFAULT_SIZE = 16*DEFAULT_BLOCKSIZE; 		// also measured in bytes .... 
	public static boolean isPowerOfTwo(int value) {
	    return value > 0 && (value & (value - 1)) == 0;
	}
	
	/**
	 * Cache constructor with a specified configuration
	 * @param size	the total number of bytes in the cache 
	 * @param blocksize	the total number of bytes in the each block 
	 */
	public Cache(CacheConfiguration config) throws CacheConfigurationException {
		this.size = config.getSize();
		this.blocksize = config.getBlocksize();
		this.entries = this.size / this.blocksize;

		if (this.size % this.blocksize != 0)
			throw new CacheConfigurationException(config);

		if (!isPowerOfTwo(this.entries))
			throw new CacheConfigurationException(config);

		initCache();
	}

	public Cache() throws CacheConfigurationException {
		this(new CacheConfiguration(DEFAULT_SIZE, DEFAULT_BLOCKSIZE));
	}

	protected void initCache() {
		cache = new CacheEntry[entries];
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

	public String toString() {
		return size + " bytes total with " + blocksize + " bytes per block";
	}

	private int size;
	private int blocksize;
	private int entries;  		// used to determine size of CacheEntry[] array ... also more convenient: icache.cache.length simply becomes iache.entries
	private CacheEntry[] cache; // the actual cache ... just an array of size/blocksize entries
}
