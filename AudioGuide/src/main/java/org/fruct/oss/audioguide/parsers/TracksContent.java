package org.fruct.oss.audioguide.parsers;

import org.fruct.oss.audioguide.track.Track;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(strict = false)
public class TracksContent implements IContent {
	@ElementList(inline = true, name = "tracks", entry = "track", required = true, empty = false)
	private List<Track> tracks;

	public List<Track> getTracks() {
		return tracks;
	}

}
