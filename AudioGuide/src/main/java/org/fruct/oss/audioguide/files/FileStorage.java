package org.fruct.oss.audioguide.files;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorage {
	String storeFile(String fileUrl, FileSource.Variant variant, InputStream inputStream) throws IOException;
	String getFile(String fileUrl, FileSource.Variant variant);
}
