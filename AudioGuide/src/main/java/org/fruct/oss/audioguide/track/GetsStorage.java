package org.fruct.oss.audioguide.track;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Xml;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.gets.AddPointRequest;
import org.fruct.oss.audioguide.gets.CreateTrackRequest;
import org.fruct.oss.audioguide.gets.DeleteTrackRequest;
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

	public static final String LIST_FILES = "<request><params>" +
			"<auth_token>%s</auth_token>" +
			"</params></request>";

	public static final String UPLOAD_FILE = "<request><params>" +
			"<auth_token>%s</auth_token>" +
			"<title>%s</title>" +
			"</params></request>";

	private ArrayList<Track> loadedTracks;

	private Location lastLocation;
	private int radius;

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
		gets.addRequest(new LoadTracksRequest(gets, lastLocation, radius) {
			@Override
			protected void onPostProcess(GetsResponse response) {
				super.onPostProcess(response);
				
				if (response.getCode() != 0) {
					return;
				}

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
				super.onPostProcess(response);

				if (response.getCode() != 0) {
					return;
				}

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
		Gets gets = Gets.getInstance();
		gets.addRequest(new AddPointRequest(gets, track, point));
	}

	private void processCreateTrackResponse(GetsResponse response, final Track track, final List<Point> points) {
		// Track already exists
		if (response.getCode() == 2) {
			final Gets gets = Gets.getInstance();
			gets.addRequest(new DeleteTrackRequest(gets, track) {
				@Override
				protected void onPostProcess(GetsResponse response) {
					super.onPostProcess(response);

					if (response.getCode() == 0) {
						// FIXME: Dangerous. Can cause infinite recursion
						// FIXME: if GeTS return 'success' but track not deleted
						sendTrack(track, points);
					}
				}
			});
		} else {
			for (Point point : points) {
				sendPoint(track, point);
			}
		}
	}

	@Override
	public void sendTrack(final Track track, final List<Point> points) {
		final Gets gets = Gets.getInstance();
		gets.addRequest(new CreateTrackRequest(gets, track) {
			@Override
			protected void onPostProcess(GetsResponse response) {
				super.onPostProcess(response);

				if (response.getCode() != 0 && response.getCode() != 2) {
					return;
				}
				
				processCreateTrackResponse(response, track, points);
			}
		});
	}

	@Override
	public boolean deleteTrack(Track track) {
		Gets gets = Gets.getInstance();
		gets.addRequest(new DeleteTrackRequest(gets, track));
		return true;
	}

	public void updateUserLocation(Location location) {
		this.lastLocation = location;
	}

	public void updateLoadRadius(int radius) {
		this.radius = radius;
	}
}
