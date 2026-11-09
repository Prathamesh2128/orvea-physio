package com.hms.measurement.storage;

public interface ImageStorage {
	void save(String ref, byte[] data);

	byte[] load(String ref);

	void delete(String ref);
}