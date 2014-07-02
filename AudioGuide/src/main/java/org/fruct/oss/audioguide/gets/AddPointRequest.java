package org.fruct.oss.audioguide.gets;

import android.util.Xml;

import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.json.JSONException;
import org.json.JSONStringer;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;

public class AddPointRequest extends GetsRequest {
	private final Track track;
	private final long categoryId;

	private final Point point;

	public AddPointRequest(Gets gets, Track track, Point point) {
		super(gets);
		this.track = track;
		this.point = point;
		this.categoryId = -1;
	}

	public AddPointRequest(Gets gets, long categoryId, Point point) {
		super(gets);
		this.categoryId = categoryId;
		this.track = null;
		this.point = point;
	}

	@Override
	protected String createRequestString() {
		return createAddPointRequest(
				track != null ? track.getName() : null,
				categoryId,
				point.getName(),
				createDescription(point),
				"http://example.com",
				point.getLatE6() / 1e6,
				point.getLonE6() / 1e6,
				0.0,
				point.getTime());
	}

	@Override
	protected String getRequestUrl() {
		return Gets.GETS_SERVER + "/addPoint.php";
	}

	@Override
	protected Class<? extends IContent> getContentClass() {
		return IContent.class;
	}

	@Override
	protected boolean onPreExecute() {
		return gets.getEnv("token") != null;
	}

	@Override
	protected void onPostProcess(GetsResponse response) {
	}

	@Override
	protected void onError() {
	}

	private String createAddPointRequest(String trackName, long categoryId, String pointName, String description,
										 String url, double lat, double lon, double alt, String timeStr) {
		try {
			XmlSerializer serializer = Xml.newSerializer();
			StringWriter writer = new StringWriter();
			serializer.setOutput(writer);

			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "request").startTag(null, "params");
			gets.writeTokenTag(serializer);

			if (trackName != null) {
				serializer.startTag(null, "channel").text(trackName).endTag(null, "channel");
			}

			if (categoryId != -1) {
				serializer.startTag(null, "category_id")
						.text(String.valueOf(categoryId))
						.endTag(null, "category_id");
			}

			serializer
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
			return null;
		}
	}

	private String createDescription(Point point) {
		if (!point.hasPhoto() && !point.hasAudio() && point.getCategoryId() == -1) {
			return point.getDescription();
		}

		try {
			JSONStringer stringer = new JSONStringer();
			stringer.object().key("description").value(point.getDescription());
			if (point.hasAudio())
				stringer.key("audio").value(point.getAudioUrl());

			if (point.hasPhoto())
				stringer.key("photo").value(point.getPhotoUrl());

			if (point.getCategoryId() != -1)
				stringer.key("category_id").value(point.getCategoryId());

			return stringer.endObject().toString();//.replace("\\/", "/");
		} catch (JSONException e) {
			return point.getDescription();
		}
	}
}
