package org.fruct.oss.audioguide.track.gets.parsers;

import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.gets.IContent;

import java.util.List;

public class TracksContent implements IContent {
	private List<Track> tracks;

	public TracksContent(List<Track> tracks) {
		this.tracks = tracks;
	}

	public List<Track> getTracks() {
		return tracks;
	}
}
