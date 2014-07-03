package org.fruct.oss.audioguide.gets;

import android.util.Xml;

import org.fruct.oss.audioguide.parsers.IContent;
import org.fruct.oss.audioguide.track.Point;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;

public class UpdatePointRequest extends GetsRequest {
	private final Point point;

	public UpdatePointRequest(Gets gets,  Point point) {
		super(gets);

		this.point = point;
	}

	@Override
	protected String createRequestString() {
		return createUpdatePointRequest(point.getUuid(), point.getName(), AddPointRequest.createDescription(point),
				"http://example.com", point.getLatE6() / 1e6, point.getLonE6() / 1e6,
				0f, point.getTime());
	}

	@Override
	protected String getRequestUrl() {
		return Gets.GETS_SERVER + "/updatePoint.php";
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
	protected void onError() {
	}

	private String createUpdatePointRequest(String uuid, String pointName, String description,
											String url, double lat, double lon, double alt, String timeStr) {
		// TODO: apply timeStr field
		try {
			XmlSerializer serializer = Xml.newSerializer();
			StringWriter writer = new StringWriter();
			serializer.setOutput(writer);

			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "request").startTag(null, "params");
			gets.writeTokenTag(serializer);

			serializer.startTag(null, "uuid").text(uuid).endTag(null, "uuid");

			serializer
					.startTag(null, "title").text(pointName).endTag(null, "title")
					.startTag(null, "description").text(description).endTag(null, "description")
					.startTag(null, "link").text(url).endTag(null, "link")
					.startTag(null, "latitude").text(String.valueOf(lat)).endTag(null, "latitude")
					.startTag(null, "longitude").text(String.valueOf(lon)).endTag(null, "longitude")
					.startTag(null, "altitude").text(String.valueOf(alt)).endTag(null, "altitude")
					//.startTag(null, "time").text(timeStr).endTag(null, "time")
					.endTag(null, "params").endTag(null, "request").endDocument();
			serializer.flush();

			return writer.toString();
		} catch (IOException e) {
			return null;
		}
	}

}
