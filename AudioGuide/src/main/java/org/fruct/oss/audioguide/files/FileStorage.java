package org.fruct.oss.audioguide.files;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface FileStorage {
	String storeFile(String fileUrl, FileSource.Variant variant, InputStream inputStream) throws IOException;
	String getFile(String fileUrl, FileSource.Variant variant);
	void pullFile(FileStorage otherStorage, String fileUrl, FileSource.Variant variant) throws IOException;
	List<String> retainUrls(List<String> keepUrls);
}
