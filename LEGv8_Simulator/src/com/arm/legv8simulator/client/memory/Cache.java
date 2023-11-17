package com.arm.legv8simulator.client.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <code>Cache</code> is the class used to implement a LEGv8 cache
 * <p>
 * <code>Cache</code> is just an array of CacheEntries where each entry has a tag, valid bit, and the block data is stored in Big Endian as an array of longs.
 * <p>
 *
 * Instruction cache and data cache design is different. Instructions are only 32 bits long so we will ALWAYS be reading exactly one word from the cache. Furthermore, instruction memory accesses are always aligned. So it makes sense for instruction caches
 * to store an array of ints (4 bytes = 32 bits) as the unit within the CacheEntry block
 *
 * A data cache in ARM/LEGv8 is much trickier because many read/store instructions allow unaligned memory addresses. Furthermore, the amount of data being read can vary from a single byte (LDURB) all the way to a double-word (LDUR).
 * So we could have a read that spans multiple cache entries if it begins near the end of a cache block and extends into the next cache block. If we use an array of ints or longs for each block, we might need to combine part of one int (or long)
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
	protected Logger rootLogger;

	private class CacheEntry {

		long tag; 	
		boolean valid; 
		byte[] block; // these are the individual bytes within the block ... also, because the blocksize can never change while we are running it's fine to use an array instead of a more dynamic data structure

		// cache entry constructor is only called during initial setup of cache (so no data ready for the block yet)
		CacheEntry(long tag, boolean valid, int blocksize) {
			this.tag = tag;
			this.valid = valid;
			block = new byte[blocksize];
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < block.length; i++) {
				sb.append(toHex(block[i],2));
				if (i < block.length - 1) {
					sb.append(".");
				}
			}
			int hexDigitsRequired = ceilDiv(tagsizebits, 4);
			String hexString = "0x" + toHex(tag, hexDigitsRequired);
			String validString = valid ? "Y" : "N";
			return hexString + "\t" + validString + "\t" + sb.toString();
		}

		public void setBlock(byte[] block) {
			this.block = block;
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
		this.entries = size / blocksize;
		this.blocksizebits = log2(blocksize);
		this.tagsizebits = 64 - log2(entries) - blocksizebits;

		if (size % blocksize != 0)
			throw new CacheConfigurationException(config);

		if (!isPowerOfTwo(entries))
			throw new CacheConfigurationException(config);

		initCache();
		rootLogger = Logger.getLogger("");
	}

	public Cache() throws CacheConfigurationException {
		this(new CacheConfiguration(DEFAULT_SIZE, DEFAULT_BLOCKSIZE));
	}

	protected void initCache() {
		cache = new CacheEntry[entries];
		// now create the actual cacheentry (note that this will initialize all of the entries with their invalid "bit" set to false)
		for (int i = 0; i<cache.length; i++) {
			cache[i] = new CacheEntry(0L, false, blocksize);
		}
	}

	/**
	 * This is super complicated because of unaligned memory accesses. We might have a request to access a memory location that
	 * spans TWO (or more) cache entries if you access data near the end of one block and then have tiny blocksizes so that you
	 * can request for example 8 bytes when the blocksize is only 2 bytes, then you are going to have to service several cache entries!
	 * 
	 * We could combine a little bit of code from this function and checkCacheW but the checkCacheW has an extra param and it needs to call a different service function ... sorry for the code redundancy!
	 * 
	 * @param address specifies which address we are trying to read/write
	 * @param numbytes how many bytes this access was trying to access (need this for data cache to see if the access spanned more than one block entry)
	 * @param memory "main memory" ... we can grab the data from here to service misses 
	 * @return true => hit, false => miss
	 */
	public boolean checkCache(long address, int numbytes, Memory memory) {
		int[] indices = calculateIndices(address, numbytes);
		long tag = calculateTag(address);
		boolean hit = true; // it's only a hit if we make it all the way through all the potential cache entries for this access! 
		for (Integer index: indices) {
			int i = (int) index;
			hit = hit && cache[i].valid && cache[i].tag == tag;
		}
		if (hit) {
			hits++;
		} else {
			misses++;
			serviceMiss(address, indices, memory);
		}
		return hit;
	}

	/**
	 * @param address specifies which address we are trying to read/write
	 * @param numbytes how many bytes this access was trying to access (need this for data cache to see if the access spanned more than one block entry)
	 * @param newval  for writes
	 * @param memory "main memory" ... we can grab the data from here to service misses 
	 * @return true => hit, false => miss
	 */
	public boolean checkCacheW(long address, int numbytes, long newval, Memory memory) throws SegmentFaultException {
		// first whether or not we have a hit or miss, since we are write-through cache only, let's go ahead and write the required number of bytes out to memory
		switch (numbytes) {
			case 1: memory.storeByte(address, newval); break;
			case 2: memory.storeHalfword(address, newval); break;
			case 4: memory.storeWord(address, newval); break;
			case 8: memory.storeDoubleword(address, newval); break;
		}

		int[] indices = calculateIndices(address, numbytes);
		long tag = calculateTag(address);
		boolean hit = true; // it's only a hit if we make it all the way through all potential cache entries this ONE request might span! 
		for (Integer index: indices) {
			int i = (int) index;
			hit = hit && cache[i].valid && cache[i].tag == tag;
		}
		if (hit) {
			hits++;
		} else {
			misses++;
			serviceMiss(address, indices, memory); // this will fetch entire block(s) for those requests that span multiple cache entries
		}
		return hit;
	}

	/**
	 * This is super complicated because of unaligned memory accesses. We might have a request to access a memory location that
	 * spans TWO (or more) cache entries if you access data near the end of one block and then have tiny blocksizes so that you
	 * can request for example 8 bytes when the blocksize is only 2 bytes, then you are going to have to service several entries!
	 * To simplify the implementation the easiest way to service is a miss is simply retrieve all necessary blocks
	 * based on how many cache entries (indices) are needed).
	 * 
	 * This is super complicated because of unaligned memory accesses. We might have a request to access a memory location that
	 * spans TWO (or more) cache entries if you access data near the end of one block and then have tiny blocksizes so that you
	 * can request for example 8 bytes when the blocksize is only 2 bytes, then you are going to have to service several entries!
	 * 
	 * Note that this method is called for both reads and writes ... writes have already "written-through" to main memory
	 * so safe to retrieve the freshly updated value from memory into the cache.
	 * 
	 * 
	 * 
	 * @param indices[] - array of indices of cache entry blocks that must be retrieved from main memory
	 * @param memory
	 */
	protected void serviceMiss(long address, int[] indices, Memory memory) {
		rootLogger.log(Level.SEVERE, "service miss: " + toHex(address, 16));
		long tagAddr = address >> (64 - tagsizebits);
		tagAddr = tagAddr << (64 - tagsizebits); // now we just need to add the index 
		rootLogger.log(Level.SEVERE, "tagaddr: " + toHex(tagAddr, 16));
		for (int i: indices) {
			cache[i] = new CacheEntry(calculateTag(address), true, blocksize);
			cache[i].valid = true;
			// now retrieve the blocks from memory
			long indexAddr = tagAddr + (i << blocksizebits);
			cache[i].setBlock(memory.retrieveBlock(indexAddr, blocksize)); 			
			rootLogger.log(Level.SEVERE, "index: " + i + " " + cache[i].toString());
		}
	}

	protected int[] calculateIndices(long address, int numbytes) {
		rootLogger.log(Level.SEVERE, "calculateIndices: " + toHex(address, 16) + ", " + numbytes + ", " + tagsizebits + ", " + (64-tagsizebits-blocksizebits) + ", " + blocksizebits);
		ArrayList<Integer> indices = new ArrayList<>();

		while (numbytes > 0) {
			// skip blocksize bits
			long step1 = address >> blocksizebits; // shift the blocksize bits out of the way

			// now shift tagsizebits to the left to get rid of the tag
			long step2 = step1 << (tagsizebits + blocksizebits);

			// finally return the index shifted back correct number of places which is the blocksize + the tagsize
			// safely cast to int b/c valid CacheConfiguration only takes in ints when specifying size and blocksize
			long step3 = step2 >>> (tagsizebits + blocksizebits);
		//	rootLogger.log(Level.SEVERE,"step2: " + toHex(step2,16) + ", step3: " + toHex(step3,16));
			indices.add((int)step3);

			// now see how many bytes we "consumed" with that index
			long blockend = (step1 << blocksizebits) + blocksize;
			numbytes = numbytes - (int) (blockend - address); // if the address is towards the beginning of the block, then this could become negative ... but that doesn't matter because our loop ends when numbytes is no longer positive
			address = blockend;
		}
		int placeholder[] = new int[indices.size()];
		for (int i=0; i<indices.size(); i++) {
			placeholder[i] = indices.get(i);
		}
		rootLogger.log(Level.SEVERE, "calculateIndices: indices: " + java.util.Arrays.toString(placeholder));
		return placeholder;
	}

	protected long calculateTag(long address) {
		long tag = address >> (64-tagsizebits);
		rootLogger.log(Level.SEVERE, "calculateTag: address: " + toHex(address, 16) + ", " + toHex(tag, 16));
		return tag; // since the tag is on the far left of the address, simply shift it to the right correct number of times and we have the tag!
	}

	protected static int log2(long num) {
		return (int) (Math.log(num) / Math.log(2));
	}

	protected static int ceilDiv(int x, int y) {
		return (x + y - 1) / y;
	}

	// GWT doesn't support String.format() so this is easiest way to convert to hex
	public static String toHex(long num, int digits) {
		char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
		char[] hexChars = new char[digits];
		for (int i = digits - 1; i >= 0; i--) {
			hexChars[i] = hexDigits[(int) (num & 0x0F)];
			num >>= 4;
		}
		return new String(hexChars);
	}

	// for all of thse memory reads, we first figure out which CacheEntry(s) are involved
	// if only one CacheEntry, then simply request the data from the CacheEntry
	// public long loadDoubleword(long address) {
	// 	return 0L;
	// }

	// public int loadSignedword(long address) {
	// 	return 0;
	// }

	// public int loadHalfword(long address) {
	// 	return 0;
	// }

	// public int loadByte(long address) {
	// 	return 0;
	// }

	// public void storeDoubleword(long address, long value) {
	// }

	// public void storeWord(long address, int value) {
	// }

	// public void storeHalfword(long address, short value) {
	// }

	// public void storeByte(long address, byte value) {
	// }

	public String toString() {
		String str = size + " bytes total with " + blocksize + " bytes per block\n";
		str += "tag\t\t\t\t\tV\tblock data\n";
		for (CacheEntry ce : cache) {
			str += ce.toString() + "\n";
		}
		return str;
	}

	public String getStatsStr() {
		return hits + " hits, " + misses + " misses";
	}

	private int hits;
	private int misses;
	private int size;
	private int blocksize;
	private int tagsizebits;
	private int blocksizebits; 	// the number of bits to skip when calculating index
	private int entries;  		// used to determine size of CacheEntry[] array ... also more convenient: icache.cache.length simply becomes icache.entries
	private CacheEntry[] cache; // the actual cache ... just an array of size/blocksize entries ... also, because any changes to the cache configuration require "re-running" the program which means re-creating the cache, an array is perfect instead of a more dynamic data structure
}
