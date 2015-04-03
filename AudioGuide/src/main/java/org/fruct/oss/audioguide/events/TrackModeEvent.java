package org.fruct.oss.audioguide.events;

import org.fruct.oss.audioguide.track.Track;

public class TrackModeEvent {
	private Track track;

	public TrackModeEvent(Track track) {
		this.track = track;
	}

	public Track getTrack() {
		return track;
	}
}
