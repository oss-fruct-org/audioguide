package org.fruct.oss.audioguide.files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public interface FileSource {
	public enum Variant {
		PREVIEW, FULL
	}

	InputStream getInputStream(String fileUrl, Variant variant) throws IOException;
}
