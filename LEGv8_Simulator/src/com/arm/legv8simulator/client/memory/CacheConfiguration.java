package com.arm.legv8simulator.client.memory;

public class CacheConfiguration {
	private int size;
	private int blocksize;
	public CacheConfiguration(int size, int blocksize) {
		this.size = size;
		this.blocksize = blocksize;
	}
	public int getSize() {
		return size;
	}
	public int getBlocksize() {
		return blocksize;
	}
}
