package com.hms.measurement.storage;

import com.hms.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Disk storage outside any web-served folder. Swap for an S3-style
 * implementation later.
 */
@Component
public class LocalImageStorage implements ImageStorage {

	private final Path root;

	public LocalImageStorage(@Value("${physio.measurement.storage-dir:./data/measurement-photos}") String dir) {
		this.root = Path.of(dir).toAbsolutePath().normalize();
	}

	@Override
	public void save(String ref, byte[] data) {
		try {
			Path p = resolve(ref);
			Files.createDirectories(p.getParent());
			Files.write(p, data);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not store the image", e);
		}
	}

	@Override
	public byte[] load(String ref) {
		try {
			return Files.readAllBytes(resolve(ref));
		} catch (NoSuchFileException e) {
			throw new BusinessException("The image file is no longer available", HttpStatus.NOT_FOUND);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read the image", e);
		}
	}

	@Override
	public void delete(String ref) {
		try {
			Files.deleteIfExists(resolve(ref));
		} catch (IOException e) {
			throw new UncheckedIOException("Could not delete the image", e);
		}
	}

	private Path resolve(String ref) {
		Path p = root.resolve(ref).normalize();
		if (!p.startsWith(root))
			throw new IllegalArgumentException("Invalid storage reference"); // path traversal guard
		return p;
	}
}