package org.fruct.oss.audioguide.track.gets;

import org.fruct.oss.audioguide.BuildConfig;
import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.gets.parsers.CategoriesContent;
import org.fruct.oss.audioguide.track.gets.parsers.CategoriesParser;
import org.fruct.oss.audioguide.track.gets.parsers.Kml;
import org.fruct.oss.audioguide.track.gets.parsers.KmlParser;
import org.fruct.oss.audioguide.util.Utils;
import org.osmdroid.util.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class Gets {
	public static String GETS_SERVER = "http://gets.cs.petrsu.ru/gets/service";

	static {
		if (!BuildConfig.DEBUG) {
			GETS_SERVER = "http://gets.cs.petrsu.ru/gets/service";
		}
	}

	public static final String GET_CATEGORIES_REQUEST =
			"<request><params/></request>";

	private String getsServerUrl;
	private static Logger log =  LoggerFactory.getLogger(Gets.class);

	public Gets(String getsServerUrl) {
		if (getsServerUrl.endsWith("/")) {
			this.getsServerUrl = getsServerUrl;
		} else {
			this.getsServerUrl = getsServerUrl + "/";
		}
	}

	public <T extends IContent> T query(String method, String request, ContentParser<T> parser)
			throws IOException, GetsException {
		String responseStr = downloadUrl(getsServerUrl + method, request);
		GetsResponse<T> response = GetsResponse.parse(responseStr, parser);

		if (response.getCode() != 0) {
			throw new GetsException("Server return error for request " + method);
		}

		return response.getContent();
	}

	/**
	 * Receive list of categories
	 * @return list of categories
	 * @throws java.io.IOException
	 */
	public List<Category> getCategories() throws IOException, GetsException {
		String responseStr = downloadUrl(getsServerUrl + "getCategories.php",
				String.format(GET_CATEGORIES_REQUEST));
		GetsResponse<CategoriesContent> resp = GetsResponse.parse(responseStr, new CategoriesParser());

		if (resp.getCode() != 0) {
			log.warn("getCategories returned with code {} message '{}'", resp.getCode(), resp.getMessage());
			throw new GetsException("Server return error");
		}

		CategoriesContent categories = resp.getContent();
		return categories.getCategories();

	}

	public List<Point> getPoints(Category category, GeoPoint position, int radius) throws IOException, GetsException {

		StringBuilder requestBuilder = new StringBuilder();
		requestBuilder.append("<request><params>");

		requestBuilder.append("<latitude>").append(position.getLatitude()).append("</latitude>");
		requestBuilder.append("<longitude>").append(position.getLongitude()).append("</longitude>");
		requestBuilder.append("<radius>").append(radius / 1000.0).append("</radius>");

		requestBuilder.append("</params></request>");

		String responseStr = downloadUrl(getsServerUrl + "loadPoints.php",requestBuilder.toString());
		log.trace("Req {}", requestBuilder.toString());
		GetsResponse<Kml> kmlGetsResponse = GetsResponse.parse(responseStr, new KmlParser());
		if (kmlGetsResponse.getCode() != 0) {
			log.warn("getCategories returned with code {} message '{}'", kmlGetsResponse.getCode(),
					kmlGetsResponse.getMessage());
			throw new GetsException("Server return error");
		}

		Kml kml = kmlGetsResponse.getContent();

		for (Point point : kml.getPoints()) {
			point.setCategoryId(category.getId());
		}

		return kml.getPoints();

	}

	// TODO: Move this method to Utils
	public static String downloadUrl(String urlString, String postQuery) throws IOException {
		HttpURLConnection conn = null;
		InputStream responseStream = null;

		try {
			URL url = new URL(urlString);
			conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(10000);
			conn.setRequestMethod(postQuery == null ? "GET" : "POST");
			conn.setDoInput(true);
			conn.setDoOutput(postQuery != null);
			conn.setRequestProperty("User-Agent", "RoadSigns/0.4 (http://oss.fruct.org/projects/roadsigns/)");

			if (postQuery != null) {
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
				writer.write(postQuery);
				writer.flush();
				writer.close();
			}

			conn.connect();

			int responseCode = conn.getResponseCode();
			responseStream = conn.getInputStream();
			String response = Utils.inputStreamToString(responseStream);

			log.trace("Request url {} data {}", urlString, postQuery);
			log.trace("Response code {}, response {}", responseCode, response);

			return response;
		} finally {
			if (conn != null)
				conn.disconnect();

			if (responseStream != null)
				responseStream.close();
		}
	}
}
