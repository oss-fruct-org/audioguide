package org.fruct.oss.audioguide.track;

public class TrackManager {
	private final ILocalStorage localStorage;
	private final IStorage remoteStorage;

	public TrackManager(ILocalStorage localStorage, IStorage remoteStorage) {
		this.localStorage = localStorage;
		this.remoteStorage = remoteStorage;
	}
}
