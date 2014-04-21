package org.fruct.oss.audioguide.files;


public interface FileListener {
	void itemLoaded(String url);
	void itemDownloadProgress(String url, int current, int max);
}
