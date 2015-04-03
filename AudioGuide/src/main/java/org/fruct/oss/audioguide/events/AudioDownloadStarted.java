package org.fruct.oss.audioguide.events;

public class AudioDownloadStarted {
	private String url;

	public AudioDownloadStarted(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
}
