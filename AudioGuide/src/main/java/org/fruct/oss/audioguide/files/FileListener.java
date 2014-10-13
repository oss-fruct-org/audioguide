package org.fruct.oss.audioguide.files;


public interface FileListener {
	void itemLoaded(String fileUrl);
	void itemDownloadProgress(String fileUrl, int current, int max);
}
