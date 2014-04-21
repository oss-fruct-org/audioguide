package org.fruct.oss.audioguide.track;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Xml;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.parsers.GetsException;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;
import org.fruct.oss.audioguide.parsers.Kml;
import org.fruct.oss.audioguide.parsers.TracksContent;
import org.fruct.oss.audioguide.util.Utils;
import org.json.JSONException;
import org.json.JSONStringer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class GetsStorage implements IStorage, IRemoteStorage {
	private final static Logger log = LoggerFactory.getLogger(GetsStorage.class);

	public static final String PREF_AUTH_TOKEN = "pref-auth-token";

	//public static final String GETS_SERVER = "http://getsi.no-ip.info/getslocal";
	public static final String GETS_SERVER = "http://oss.fruct.org/projects/gets/service";

	public static final String LOAD_TRACKS_REQUEST = "<request><params>" +
			"%s" + // Token may be empty
			"<!--<category_name>audio_tracks</category_name>-->" +
			"</params></request>";

	public static final String LOAD_TRACK_REQUEST = "<request><params>" +
			"%s" +
			"<name>%s</name>" +
			"</params></request>";

	public static final String CREATE_TRACK = "<request><params>" +
			"%s" +
			"<name>%s</name>" +
			"%s" +
			"<description>%s</description>" +
			"<url>%s</url>" +
			"</params></request>";

	public static final String DELETE_TRACK = "<request><params>" +
			"%s" +
			"<name>%s</name>" +
			"</params></request>";

	public static final String LOGIN_STAGE_1 = "<request><params></params></request>";
	public static final String LOGIN_STAGE_2 = "<request><params><id>%s</id></params></request>";

	public static final String LIST_FILES = "<request><params>" +
			"<auth_token>%s</auth_token>" +
			"</params></request>";

	public static final String UPLOAD_FILE = "<request><params>" +
			"<auth_token>%s</auth_token>" +
			"<title>%s</title>" +
			"</params></request>";

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
			List<Track> publicTracks = loadPublicTracks();
			List<Track> privateTracks = loadPrivateTracks();

			loadedTracks = new ArrayList<Track>();
			loadedTracks.addAll(publicTracks);
			loadedTracks.addAll(privateTracks);
		} catch (Exception e) {
			log.warn("Error: ", e);
			loadedTracks = Collections.emptyList();
		}
	}

	private List<Track> loadPublicTracks() throws Exception {
		String responseString = Utils.downloadUrl(GETS_SERVER + "/loadTracks.php",
				String.format(Locale.ROOT, LOAD_TRACKS_REQUEST, "<!-- public request -->"));
		GetsResponse response = GetsResponse.parse(responseString, TracksContent.class);

		if (response.getCode() != 0) {
			throw new RuntimeException("Error code from GetsServer " + response.getCode());
		}

		TracksContent tracksContent = ((TracksContent) response.getContent());
		return tracksContent.getTracks();
	}

	private List<Track> loadPrivateTracks() throws Exception {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		String token = pref.getString(PREF_AUTH_TOKEN, null);
		if (token == null)
			return Collections.emptyList();

		String responseString = Utils.downloadUrl(GETS_SERVER + "/loadTracks.php",
				createLoadTracksRequest());
		GetsResponse response = GetsResponse.parse(responseString, TracksContent.class);

		if (response.getCode() != 0) {
			return Collections.emptyList();
		}

		TracksContent tracksContent = ((TracksContent) response.getContent());
		for (Track track : tracksContent.getTracks()) {
			track.setPrivate(true);
		}

		return tracksContent.getTracks();
	}

	@Override
	public List<Track> getTracks() {
		return loadedTracks;
	}

	@Override
	public List<Point> getPoints(Track track) throws IOException {
		try {
			String responseString = Utils.downloadUrl(GETS_SERVER + "/loadTrack.php",
					createLoadTrackRequest(track.getName()));
			GetsResponse response = GetsResponse.parse(responseString, Kml.class);

			if (response.getCode() != 0) {
				throw new RuntimeException("Wrong code from GeTS");
			}

			Kml kml = ((Kml) response.getContent());
			return new ArrayList<Point>(kml.getPoints());
		} catch (GetsException e) {
			throw new IOException("Wrong response from GeTS server", e);
		}
	}

	@Override
	public void sendPoint(Track track, Point point) {
		Date currentDate = new Date();
		String timeStr = new SimpleDateFormat("dd MM yyyy HH:mm:ss.SSS", Locale.ROOT).format(currentDate);
		String desc;
		String request = createAddPointRequest(
				track.getName(),
				point.getName(),
				desc = createDescription(point),
				"http://example.com",
				point.getLatE6() / 1e6,
				point.getLonE6() / 1e6,
				0.0,
				timeStr);

		log.debug("Description {}", desc);
		log.debug("sendPoint request {}", request);

		try {
			String responseString = Utils.downloadUrl(GETS_SERVER + "/addPoint.php", request);
			// TODO: parse response
		} catch (IOException e) {
			log.error("Error: ", e);
		}
	}

	private String createDescription(Point point) {
		if (!point.hasPhoto() && !point.hasAudio()) {
			return point.getDescription();
		}

		try {
			JSONStringer stringer = new JSONStringer();
			stringer.object().key("description").value(point.getDescription());
			if (point.hasAudio())
				stringer.key("audio").value(point.getAudioUrl());

			if (point.hasPhoto())
				stringer.key("photo").value(point.getPhotoUrl());

			// Remove slash escapes
			return stringer.endObject().toString().replace("\\/", "/");
		} catch (JSONException e) {
			log.warn("JSON encoding error", e);
			return point.getDescription();
		}
	}

	@Override
	public void sendTrack(Track track, List<Point> points) {
		String request = String.format(Locale.ROOT, CREATE_TRACK, createTokenTag(),
				track.getName(), createHnameTag(track),  track.getDescription(), track.getUrl());

		try {
			String responseString = Utils.downloadUrl(GETS_SERVER + "/createTrack.php", request);
			GetsResponse response = GetsResponse.parse(responseString, IContent.class);

			// Track already exists
			if (response.getCode() == 2) {
				if (deleteTrack(track)) {
					responseString = Utils.downloadUrl(GETS_SERVER + "/createTrack.php", request);
					response = GetsResponse.parse(responseString, IContent.class);
				} else {
					log.error("Cannot update track {}", track.getName());
				}
			}

			if (response.getCode() != 0) {
				log.error("Cannot create track {}", track.getName());
				return;
			}

			// TODO: parse response
		} catch (IOException e) {
			log.error("Error: ", e);
			return;
		} catch (GetsException e) {
			e.printStackTrace();
			return;
		}

		for (Point point : points) {
			sendPoint(track, point);
		}
	}

	@Override
	public boolean deleteTrack(Track track) {
		String request = String.format(Locale.ROOT, DELETE_TRACK, createTokenTag(),
				track.getName());
		try {
			String responseString = Utils.downloadUrl(GETS_SERVER + "/deleteTrack.php", request);
			GetsResponse response = GetsResponse.parse(responseString, IContent.class);

			if (response.getCode() != 0) {
				log.error("Cannot delete track from GeTS server");
				return false;
			}
		} catch (IOException e) {
			log.error("Error: ", e);
			return false;
		} catch (GetsException e) {
			log.warn("Incorrect answer from server: ", e);
			return false;
		}

		return true;
	}

	private String createHnameTag(Track track) {
		if (!Utils.isNullOrEmpty(track.getHname()))
			return "<hname>" + track.getHname() + "</hname>";
		else
			return "";
	}

	private String createLoadTracksRequest() {
		return String.format(LOAD_TRACKS_REQUEST, createTokenTag());
	}

	private String createLoadTrackRequest(String trackName) {
		return String.format(LOAD_TRACK_REQUEST, createTokenTag(), trackName);
	}

	/*
			"<request><params>" +
			"%s" +
			"<channel>%s</channel>" +
			"<title>%s</title>" +
			"<description>%s</description>" +
			"<link>%s</link>" +
			"<latitude>%f</latitude>" +
			"<longitude>%f</longitude>" +
			"<altitude>%f</altitude>" +
			"<time>%s</time>" +
			"</params></request>"
	 */

	private String createAddPointRequest(String trackName, String pointName, String description,
										 String url, double lat, double lon, double alt, String timeStr) {
		try {
			XmlSerializer serializer = Xml.newSerializer();
			StringWriter writer = new StringWriter();
			serializer.setOutput(writer);

			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "request").startTag(null, "params");
			writeTokenTag(serializer);

			serializer.startTag(null, "channel").text(trackName).endTag(null, "channel")
					.startTag(null, "title").text(pointName).endTag(null, "title")
					.startTag(null, "description").text(description).endTag(null, "description")
					.startTag(null, "link").text(url).endTag(null, "link")
					.startTag(null, "latitude").text(String.valueOf(lat)).endTag(null, "latitude")
					.startTag(null, "longitude").text(String.valueOf(lon)).endTag(null, "longitude")
					.startTag(null, "altitude").text(String.valueOf(alt)).endTag(null, "altitude")
					.startTag(null, "time").text(timeStr).endTag(null, "time")
					.endTag(null, "params").endTag(null, "request").endDocument();
			serializer.flush();

			return writer.toString();
		} catch (IOException e) {
			log.error("Can't create xml document: ", e);
			return null;
		}
	}

	private String createTokenTag() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		String token = pref.getString(PREF_AUTH_TOKEN, null);
		if (token == null)
			return "";
		else
			return "<auth_token>" + token + "</auth_token>";
	}

	private void writeTokenTag(XmlSerializer serializer) throws IOException {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		String token = pref.getString(PREF_AUTH_TOKEN, null);
		if (token != null) {
			serializer.startTag(null, "auth_token").text(token).endTag(null, "auth_token");
		}
	}
}
