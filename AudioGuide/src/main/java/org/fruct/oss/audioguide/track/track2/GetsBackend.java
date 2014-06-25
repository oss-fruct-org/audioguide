package org.fruct.oss.audioguide.track.track2;

import org.fruct.oss.audioguide.gets.AddPointRequest;
import org.fruct.oss.audioguide.gets.CategoriesRequest;
import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.gets.CreateTrackRequest;
import org.fruct.oss.audioguide.gets.DeleteTrackRequest;
import org.fruct.oss.audioguide.gets.Gets;
import org.fruct.oss.audioguide.parsers.CategoriesContent;
import org.fruct.oss.audioguide.parsers.GetsResponse;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.util.Utils;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class GetsBackend implements StorageBackend, CategoriesBackend {
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
	public void loadTracksInRadius(float lat, float lon, float radius, List<Category> categories, Utils.Callback<List<Track>> callback) {
	}

	@Override
	public void loadPointsInRadius(float lat, float lon, float radius, List<Category> activeCategories, Utils.Callback<List<Point>> callback) {
	}

	@Override
	public void loadPointsInTrack(Track track, Utils.Callback<List<Point>> callback) {
	}


}
