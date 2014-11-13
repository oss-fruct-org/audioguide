package org.fruct.oss.audioguide.gets;


import android.location.Location;
import android.util.Xml;

import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;
import org.fruct.oss.audioguide.parsers.TracksContent;
import org.fruct.oss.audioguide.track.Track;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class LoadTracksRequest extends GetsRequest {
	private final Location location;
	private final float radius;

	private List<Category> categories;
	private List<Track> loadedTracks = new ArrayList<Track>();

	public LoadTracksRequest(Gets gets, Location location, float radius) {
		super(gets);

		this.location = location;
		this.radius = radius;
	}

	@Override
	protected String createRequestString() {
		try {
			Category cat = categories.remove(categories.size() - 1);

			XmlSerializer serializer = Xml.newSerializer();
			StringWriter writer = new StringWriter();
			serializer.setOutput(writer);

			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "request").startTag(null, "params");

			gets.writeTokenTag(serializer);
			serializer.startTag(null, "category_name").text(cat.getName()).endTag(null, "category_name");
			serializer.startTag(null, "space").text("all").endTag(null, "space");

			if (location != null) {
				serializer.startTag(null, "latitude").text(String.valueOf(location.getLatitude())).endTag(null, "latitude");
				serializer.startTag(null, "longitude").text(String.valueOf(location.getLongitude())).endTag(null, "longitude");
				serializer.startTag(null, "radius").text(String.valueOf(radius / 1000.0)).endTag(null, "radius");
			}

			serializer.endTag(null, "params").endTag(null, "request");
			serializer.flush();

			return writer.toString();
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	protected String getRequestUrl() {
		return Gets.GETS_SERVER + "/loadTracks.php";
	}

	@Override
	protected Class<? extends IContent> getContentClass() {
		return TracksContent.class;
	}

	@Override
	protected boolean onPreExecute() {
		if (categories != null)
			return true;

		categories = ((List<Category>) gets.getEnv("activeCategories"));
		if (categories == null || categories.isEmpty()) {
			categories = ((List<Category>) gets.getEnv("categories"));
		}

		if (categories == null || categories.isEmpty())
			return false;

		categories = new ArrayList<Category>(categories);
		return true;
	}

	@Override
	protected boolean onPostExecute(GetsResponse response) {
		if (response.getCode() != 0)
			return false;
		else {
			loadedTracks.addAll(((TracksContent) response.getContent()).getTracks());
			return !categories.isEmpty();
		}
	}

	@Override
	protected void onPostProcess(GetsResponse response) {
		if (response.getCode() == 0)
			((TracksContent) response.getContent()).setTracks(loadedTracks);
	}

	@Override
	protected void onError() {
	}
}
