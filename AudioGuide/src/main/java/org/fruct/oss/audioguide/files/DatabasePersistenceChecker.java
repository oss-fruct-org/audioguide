package org.fruct.oss.audioguide.files;

import org.fruct.oss.audioguide.track.Database;

import java.util.HashSet;
import java.util.Set;

public class DatabasePersistenceChecker implements PersistenceChecker {
	private final Database database;

	private Set<String> persistentUrls;

	public DatabasePersistenceChecker(Database database) {
		this.database = database;
	}

	@Override
	public synchronized boolean isPersistent(String url) {
		if (persistentUrls == null) {
			updatePersistentUrls();
		}
		return persistentUrls.contains(url);
	}

	public synchronized void updatePersistentUrls() {
		persistentUrls = new HashSet<>();
		persistentUrls.addAll(database.getPersistentUrls());
	}
}
