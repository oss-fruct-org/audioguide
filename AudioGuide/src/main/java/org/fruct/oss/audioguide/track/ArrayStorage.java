package org.fruct.oss.audioguide.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArrayStorage implements ILocalStorage {
	private HashMap<String, Track> tracks = new HashMap<String, Track>();

	public ArrayStorage insert(Track track) {
		storeLocalTrack(track);
		return this;
	}

	@Override
	public void storeLocalTrack(Track track) {
		// TODO: this method updates all local tracks instead of update only changed
		if (tracks.containsKey(track.getName()))
			tracks.remove(track.getName());

		tracks.put(track.getName(), track);
	}

	@Override
	public void initialize() {
	}

	@Override
	public void close() {
	}

	@Override
	public void load() {
	}

	@Override
	public List<Track> getTracks() {
		return new ArrayList<Track>(tracks.values());
	}
}
