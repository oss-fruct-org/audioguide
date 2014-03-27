package org.fruct.oss.audioguide.track;

import android.content.Intent;
import android.net.Uri;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.parsers.AuthRedirectResponse;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.Kml;
import org.fruct.oss.audioguide.parsers.TracksContent;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class GetsStorage implements IStorage {
	private final static Logger log = LoggerFactory.getLogger(GetsStorage.class);

	public static final String PREF_AUTH_TOKEN = "pref-auth-token";

	//public static final String GETS_SERVER = "http://172.20.2.217:8000/getslocal";
	public static final String GETS_SERVER = "http://oss.fruct.org/projects/gets/service";
	public static final String TOKEN = "66cf0b48817ad7eaa1a4cec102984add";
	public static final String LOAD_TRACKS_REQUEST = "<request><params>" +
			"<auth_token>%s</auth_token>" +
			"<category_name>audio_tracks</category_name>" +
			"</params></request>";

	public static final String LOAD_TRACK_REQUEST = "<request><params>" +
			"<auth_token>%s</auth_token>" +
			"<name>%s</name>" +
			"</params></request>";

	public static final String LOGIN_STAGE_1 = "<request><params></params></request>";
	public static final String LOGIN_STAGE_2 = "<request><params><id>%s</id></params></request>";

	private List<Track> loadedTracks;

	@Override
	public void initialize() {
		//authenticate();
	}

	@Override
	public void close() {

	}

	@Override
	public void load() {
		try {
			String responseString = Utils.downloadUrl(GETS_SERVER + "/loadTracks.php",
					String.format(LOAD_TRACKS_REQUEST, TOKEN));
			GetsResponse response = GetsResponse.parse(responseString, TracksContent.class);

			if (response.getCode() != 0) {
				throw new RuntimeException("NOT IMPLEMENTED YET");
			}

			TracksContent tracksContent = ((TracksContent) response.getContent());
			loadedTracks = new ArrayList<Track>(tracksContent.getTracks());
		} catch (Exception e) {
			log.warn("Error: ", e);
			loadedTracks = Collections.emptyList();
		}
	}

	@Override
	public List<Track> getTracks() {
		return loadedTracks;
	}

	@Override
	public List<Point> getPoints(Track track) {
		try {
			String responseString = Utils.downloadUrl(GETS_SERVER + "/loadTrack.php",
					String.format(LOAD_TRACK_REQUEST, TOKEN, track.getName()));
			GetsResponse response = GetsResponse.parse(responseString, Kml.class);

			if (response.getCode() != 0) {
				throw new RuntimeException("NOT IMPLEMENTED YET");
			}

			Kml kml = ((Kml) response.getContent());
			return new ArrayList<Point>(kml.getPoints());
		} catch (Exception e) {
			log.warn("Error: ", e);
			return Collections.emptyList();
		}
	}

	private void authenticate() {

	}
}
