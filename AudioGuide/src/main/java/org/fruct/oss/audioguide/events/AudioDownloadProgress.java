package org.fruct.oss.audioguide.events;

public class AudioDownloadProgress {
	private String url;
	private int total;
	private int current;

	public AudioDownloadProgress(String url, int total, int current) {
		this.url = url;
		this.total = total;
		this.current = current;
	}

	public String getUrl() {
		return url;
	}

	public int getTotal() {
		return total;
	}

	public int getCurrent() {
		return current;
	}
}
