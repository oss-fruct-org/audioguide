package org.fruct.oss.audioguide.events;

public class AudioDownloadFinished {
	private String url;

	public AudioDownloadFinished(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
}
