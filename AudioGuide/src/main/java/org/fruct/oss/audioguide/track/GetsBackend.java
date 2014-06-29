package org.fruct.oss.audioguide.track;

import android.location.Location;

import org.fruct.oss.audioguide.gets.AddPointRequest;
import org.fruct.oss.audioguide.gets.CategoriesRequest;
import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.gets.CreateTrackRequest;
import org.fruct.oss.audioguide.gets.DeleteTrackRequest;
import org.fruct.oss.audioguide.gets.Gets;
import org.fruct.oss.audioguide.gets.LoadPointsRequest;
import org.fruct.oss.audioguide.gets.LoadTrackRequest;
import org.fruct.oss.audioguide.gets.LoadTracksRequest;
import org.fruct.oss.audioguide.parsers.CategoriesContent;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.parsers.Kml;
import org.fruct.oss.audioguide.parsers.TracksContent;
import org.fruct.oss.audioguide.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class GetsBackend implements StorageBackend, CategoriesBackend {
	public static final String PREF_AUTH_TOKEN = "pref-auth-token";
	public static final String LIST_FILES = "<request><params>" +
			"<auth_token>%s</auth_token>" +
			"</params></request>";
	public static final String UPLOAD_FILE = "<request><params>" +
			"<auth_token>%s</auth_token>" +
			"<title>%s</title>" +
			"</params></request>";
	private final Gets gets;

	private List<Category> categories;
	private CountDownLatch updateTrackLatch;

	GetsBackend() {
		this.gets = Gets.getInstance();
	}

	@Override
	public void loadCategories(final Utils.Callback<List<Category>> callback) {
		gets.addRequest(new CategoriesRequest(gets) {
			@Override
			protected void onPostProcess(GetsResponse response) {
				super.onPostProcess(response);

				// TODO: need some code to re-download categories after network up
				if (response.getCode() != 0) {
					return;
				}

				categories = CategoriesContent.filterByPrefix(((CategoriesContent)
							response.getContent()).getCategories());
				callback.call(categories);
			}
		});
	}


	@Override
	public void updateTrack(final Track track, final List<Point> points) {
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

	private void sendPoint(Track track, Point point) {
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
						updateTrack(track, points);
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
	public void loadTracksInRadius(float lat, float lon, float radius, List<Category> categories, final Utils.Callback<List<Track>> callback) {
		Location location = new Location("no-provider");
		location.setLatitude(lat);
		location.setLongitude(lon);

		gets.addRequest(new LoadTracksRequest(gets, location, radius) {
			@Override
			protected void onPostProcess(GetsResponse response) {
				super.onPostProcess(response);

				if (response.getCode() != 0) {
					return;
				}

				TracksContent tracksContent = ((TracksContent) response.getContent());
				List<Track> loadedTracks = new ArrayList<Track>(tracksContent.getTracks());
				callback.call(loadedTracks);
			}
		});
	}

	@Override
	public void loadPointsInRadius(float lat, float lon, float radius, List<Category> activeCategories, final Utils.Callback<List<Point>> callback) {
		Location location = new Location("no-provider");
		location.setLatitude(lat);
		location.setLongitude(lon);

		gets.addRequest(new LoadPointsRequest(gets, location, radius) {
			@Override
			protected void onPostProcess(GetsResponse response) {
				super.onPostProcess(response);

				if (response.getCode() != 0) {
					return;
				}

				Kml tracksContent = ((Kml) response.getContent());
				List<Point> loadedPoints = new ArrayList<Point>(tracksContent.getPoints());
				callback.call(loadedPoints);
			}
		});
	}

	@Override
	public void loadPointsInTrack(Track track, final Utils.Callback<List<Point>> callback) {
		gets.addRequest(new LoadTrackRequest(gets, track.getName()) {
			@Override
			protected void onPostProcess(GetsResponse response) {
				super.onPostProcess(response);

				if (response.getCode() != 0) {
					return;
				}

				Kml kml = ((Kml) response.getContent());
				ArrayList<Point> ret = new ArrayList<Point>(kml.getPoints());
				callback.call(ret);
			}
		});
	}


}
