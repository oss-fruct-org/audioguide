package org.fruct.oss.audioguide.track;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.parsers.AuthRedirectResponse;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.Kml;
import org.fruct.oss.audioguide.parsers.TracksContent;
import org.fruct.oss.audioguide.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class GetsStorage implements IStorage, IRemoteStorage {
	private final static Logger log = LoggerFactory.getLogger(GetsStorage.class);

	public static final String PREF_AUTH_TOKEN = "pref-auth-token";
	public static final String PREF_AUTH_ANON = "pref-auth-anon";

	public static final String GETS_SERVER = "http://getsi.no-ip.info/getslocal";
	//public static final String GETS_SERVER = "http://oss.fruct.org/projects/gets/service";
	public static final String TOKEN = "66cf0b48817ad7eaa1a4cec102984add";

	public static final String LOAD_TRACKS_REQUEST = "<request><params>" +
			"%s" + // Token may be empty
			"<!--<category_name>audio_tracks</category_name>-->" +
			"</params></request>";

	public static final String LOAD_TRACK_REQUEST = "<request><params>" +
			"%s" +
			"<name>%s</name>" +
			"</params></request>";

	public static final String SEND_POINT = "<request><params>" +
			"%s" +
			"<channel>%s</channel>" +
			"<title>%s</title>" +
			"<description>%s</description>" +
			"<link>%s</link>" +
			"<latitude>%f</latitude>" +
			"<longitude>%f</longitude>" +
			"<altitude>%f</altitude>" +
			"<time>%s</time>" +
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
					createLoadTracksRequest());
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
					createLoadTrackRequest(track.getName()));
			GetsResponse response = GetsResponse.parse(responseString, Kml.class);

			if (response.getCode() != 0) {
				throw new RuntimeException("NOT IMPLEMENTED YET");
			}

			Kml kml = ((Kml) response.getContent());
			return new ArrayList<Point>(kml.getPoints());
		} catch (Exception e) {
			log.error("Error: ", e);
			return Collections.emptyList();
		}
	}

	@Override
	public void sendPoint(Track track, Point point) {
		Date currentDate = new Date();
		String timeStr = new SimpleDateFormat("dd MM yyyy HH:mm:ss.SSS", Locale.ROOT).format(currentDate);
		String request = String.format(Locale.ROOT, SEND_POINT, createTokenTag(),
				track.getName(),
				point.getName(),
				point.getDescription(),
				"http://example.com",
				point.getLatE6() / 1e6,
				point.getLonE6() / 1e6,
				0.0,
				timeStr);
		log.debug("sendPoint request ", request);

		try {
			String responseString = Utils.downloadUrl(GETS_SERVER + "/addPoint.php", request);
			// TODO: parse response
		} catch (IOException e) {
			log.error("Error: ", e);
		}
	}

	private String createLoadTracksRequest() {
		return String.format(LOAD_TRACKS_REQUEST, createTokenTag());
	}

	private String createLoadTrackRequest(String trackName) {
		return String.format(LOAD_TRACK_REQUEST, createTokenTag(), trackName);
	}

	private String createTokenTag() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());

		String token = pref.getString(PREF_AUTH_TOKEN, null);
		boolean anonAccount = pref.getBoolean(PREF_AUTH_ANON, false);
		if (token == null || anonAccount)
			return "";
		else
			return "<auth_token>" + token + "</auth_token>";
	}
}
