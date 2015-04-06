package org.fruct.oss.audioguide.track.tasks;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Xml;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.events.DataUpdatedEvent;
import org.fruct.oss.audioguide.files.AudioDownloadService;
import org.fruct.oss.audioguide.track.Database;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.gets.Gets;
import org.fruct.oss.audioguide.track.gets.GetsException;
import org.fruct.oss.audioguide.track.gets.parsers.Kml;
import org.fruct.oss.audioguide.track.gets.parsers.KmlParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class StoreTrackTask extends AsyncTask<Void, Void, Boolean> {
	private final Track track;
	private final boolean local;

	public StoreTrackTask(Track track) {
		this.track = track;
		this.local = true;
	}

	public StoreTrackTask(Track track, boolean local) {
		this.track = track;
		this.local = local;
	}

	@Override
	protected Boolean doInBackground(Void... voids) {
		String request = createRequestString(track.getName());
		Gets gets = new Gets(Gets.GETS_SERVER);

		try {
			Kml kml = gets.query("loadTrack.php", request, new KmlParser());
			storeToDatabase(track, kml.getPoints());

			if (local) {
				App.getInstance().getPersistenceChecker().updatePersistentUrls();
				queueAudioDownload(kml.getPoints());
			}

			EventBus.getDefault().post(new DataUpdatedEvent());
			return true;
		} catch (IOException | GetsException e) {
			return false;
		}
	}

	private void queueAudioDownload(List<Point> points) {
		Intent intent = new Intent(AudioDownloadService.ACTION_DOWNLOAD, null, App.getContext(), AudioDownloadService.class);
		intent.putExtra(AudioDownloadService.ARG_POINTS, new ArrayList<>(points));

		App.getContext().startService(intent);
	}

	private void storeToDatabase(Track track, List<Point> points) {
		Database database = App.getInstance().getDatabase();

		track.setLocal(local);
		database.insertTrack(track);

		for (Point point : points) {
			point.setPrivate(track.isPrivate());

			if (point.getCategoryId() == -1) {
				point.setCategoryId(track.getCategoryId());
			}
		}

		database.insertPointsToTrack(track, points);
	}

	private String createRequestString(String trackName) {
		try {
			XmlSerializer serializer = Xml.newSerializer();
			StringWriter writer = new StringWriter();
			serializer.setOutput(writer);

			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "request").startTag(null, "params");

			serializer.startTag(null, "name").text(trackName).endTag(null, "name");
			serializer.endTag(null, "params").endTag(null, "request");
			serializer.flush();

			return writer.toString();
		} catch (IOException e) {
			return null;
		}
	}

}
