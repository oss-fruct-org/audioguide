package org.fruct.oss.audioguide.gets;


import android.location.Location;
import android.util.Xml;

import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;
import org.fruct.oss.audioguide.parsers.TracksContent;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;

public class LoadTracksRequest extends GetsRequest {
	private final Location location;
	private final int radius;

	public LoadTracksRequest(Gets gets, Location location, int radius) {
		super(gets);

		this.location = location;
		this.radius = radius;
	}

	@Override
	protected String createRequestString() {
		try {
			XmlSerializer serializer = Xml.newSerializer();
			StringWriter writer = new StringWriter();
			serializer.setOutput(writer);

			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "request").startTag(null, "params");

			gets.writeTokenTag(serializer);
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
	protected int getPriority() {
		return 3;
	}

	@Override
	protected Class<? extends IContent> getContentClass() {
		return TracksContent.class;
	}

	@Override
	protected void onPostProcess(GetsResponse response) {

	}

	@Override
	protected void onError() {

	}
}
