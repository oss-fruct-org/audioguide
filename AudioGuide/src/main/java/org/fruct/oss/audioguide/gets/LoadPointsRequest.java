package org.fruct.oss.audioguide.gets;


import android.location.Location;
import android.util.Xml;

import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.IContent;
import org.fruct.oss.audioguide.parsers.Kml;
import org.fruct.oss.audioguide.parsers.TracksContent;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.util.Utils;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class LoadPointsRequest extends GetsRequest {
	private final Location location;
	private final float radius;
	private int leastCode = Integer.MAX_VALUE;

	private Category currentCategory;
	private List<Category> categories;
	private List<Point> loadedPoints = new ArrayList<Point>();

	public LoadPointsRequest(Gets gets, Location location, float radius) {
		super(gets);

		this.location = location;
		this.radius = radius;
	}

	@Override
	protected String createRequestString() {
		try {
			Category cat = categories.remove(categories.size() - 1);
			currentCategory = cat;
			XmlSerializer serializer = Xml.newSerializer();
			StringWriter writer = new StringWriter();
			serializer.setOutput(writer);

			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "request").startTag(null, "params");

			gets.writeTokenTag(serializer);

			if (location != null) {
				serializer.startTag(null, "latitude").text(String.valueOf(location.getLatitude())).endTag(null, "latitude");
				serializer.startTag(null, "longitude").text(String.valueOf(location.getLongitude())).endTag(null, "longitude");
				serializer.startTag(null, "radius").text(String.valueOf(radius / 1000.0)).endTag(null, "radius");
			}

			serializer.startTag(null, "category_id").text(String.valueOf(cat.getId())).endTag(null, "category_id");

			if (!Utils.isNullOrEmpty(gets.getToken())) {
				serializer.startTag(null, "space").text("all").endTag(null, "space");
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
		return Gets.GETS_SERVER + "/loadPoints.php";
	}

	@Override
	protected Class<? extends IContent> getContentClass() {
		return Kml.class;
	}

	@Override
	protected boolean onPreExecute() {
		if (categories != null)
			return true;

		categories = ((List<Category>) gets.getEnv("categories"));

		if (categories == null)
			return false;

		categories = new ArrayList<Category>(categories);
		return true;
	}

	@Override
	protected boolean onPostExecute(GetsResponse response) {
		if (response.getCode() == 0) {
			List<Point> points = ((Kml) response.getContent()).getPoints();

			for (Point point : points) {
				point.setCategoryId(currentCategory.getId());
			}

			loadedPoints.addAll(points);
		}

		leastCode = Math.min(leastCode, response.getCode());

		// TODO: ignore only "no category with given id" error
		return !categories.isEmpty();
	}

	@Override
	protected void onPostProcess(GetsResponse response) {
		if (!loadedPoints.isEmpty()) {
			response.setCode(leastCode);
			response.setContent(new Kml(loadedPoints));
		}
	}
	@Override
	protected void onError() {
	}
}
