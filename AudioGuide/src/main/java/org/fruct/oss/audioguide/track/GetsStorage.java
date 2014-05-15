package org.fruct.oss.audioguide.track;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Xml;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.gets.Gets;
import org.fruct.oss.audioguide.gets.LoadTrackRequest;
import org.fruct.oss.audioguide.gets.LoadTracksRequest;
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

	public static final String LIST_FILES = "<request><params>" +
			"<auth_token>%s</auth_token>" +
			"</params></request>";

	public static final String UPLOAD_FILE = "<request><params>" +
			"<auth_token>%s</auth_token>" +
			"<title>%s</title>" +
			"</params></request>";

	private ArrayList<Track> loadedTracks;

	@Override
	public void initialize() {
		//authenticate();
	}

	@Override
	public void close() {

	}

	@Override
	public void loadAsync(final Handler handler) {
		Gets gets = Gets.getInstance();
		gets.addRequest(new LoadTracksRequest(gets) {
			@Override
			protected void onPostProcess(GetsResponse response) {
				TracksContent tracksContent = ((TracksContent) response.getContent());
				loadedTracks = new ArrayList<Track>(tracksContent.getTracks());

				Message message = new Message();
				Bundle data = new Bundle();
				data.putParcelableArrayList("tracks", loadedTracks);
				message.setData(data);

				handler.sendMessage(message);
			}
		});
	}

	@Override
	public void loadPoints(Track track, final Handler handler) {
		Gets gets = Gets.getInstance();
		gets.addRequest(new LoadTrackRequest(gets, track.getName()) {
			@Override
			protected void onPostProcess(GetsResponse response) {
				Kml kml = ((Kml) response.getContent());
				ArrayList<Point> ret = new ArrayList<Point>(kml.getPoints());

				Message message = new Message();
				Bundle data = new Bundle();
				data.putParcelableArrayList("points", ret);
				message.setData(data);

				handler.sendMessage(message);
			}
		});
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
			String responseString = Utils.downloadUrl(Gets.GETS_SERVER + "/addPoint.php", request);
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
			String responseString = Utils.downloadUrl(Gets.GETS_SERVER + "/createTrack.php", request);
			GetsResponse response = GetsResponse.parse(responseString, IContent.class);

			// Track already exists
			if (response.getCode() == 2) {
				if (deleteTrack(track)) {
					responseString = Utils.downloadUrl(Gets.GETS_SERVER + "/createTrack.php", request);
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
			String responseString = Utils.downloadUrl(Gets.GETS_SERVER + "/deleteTrack.php", request);
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
		String token = getAuthToken();
		if (token == null)
			return "";
		else
			return "<auth_token>" + token + "</auth_token>";
	}

	private void writeTokenTag(XmlSerializer serializer) throws IOException {
		String token = getAuthToken();
		if (token != null) {
			serializer.startTag(null, "auth_token").text(token).endTag(null, "auth_token");
		}
	}

	private String getAuthToken() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
		return pref.getString(PREF_AUTH_TOKEN, null);
	}
}
