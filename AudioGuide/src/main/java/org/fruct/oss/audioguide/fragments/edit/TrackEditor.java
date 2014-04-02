package org.fruct.oss.audioguide.fragments.edit;

import org.fruct.oss.audioguide.overlays.EditOverlay;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;
import org.osmdroid.api.IGeoPoint;

public class TrackEditor implements EditOverlay.Listener<Point> {
	private final TrackManager trackManager;
	private final Track track;

	public TrackEditor(TrackManager trackManager, Track track) {
		this.trackManager = trackManager;
		this.track = track;
	}

	@Override
	public void pointMoved(Point point, IGeoPoint geoPoint) {
		//trackManager.storeLocal();
	}

	@Override
	public void pointPressed(Point point) {

	}
}
