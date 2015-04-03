package org.fruct.oss.audioguide.track.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Xml;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.events.DataUpdatedEvent;
import org.fruct.oss.audioguide.gets.Category;
import org.fruct.oss.audioguide.preferences.SettingsActivity;
import org.fruct.oss.audioguide.track.Database;
import org.fruct.oss.audioguide.track.LocationEvent;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.gets.Gets;
import org.fruct.oss.audioguide.track.gets.GetsException;
import org.fruct.oss.audioguide.track.gets.parsers.TracksContent;
import org.fruct.oss.audioguide.track.gets.parsers.TracksParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class TracksTask extends AsyncTask<Void, Void, List<Track>> {
	private final Location location;
	private final float radiusKm;
	private final Gets gets;

	public TracksTask(Context context) {
		this.gets = new Gets(Gets.GETS_SERVER);

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		this.radiusKm = pref.getInt(SettingsActivity.PREF_LOAD_RADIUS, 500);

		LocationEvent locationEvent = EventBus.getDefault().getStickyEvent(LocationEvent.class);
		this.location = locationEvent.getLocation();
	}

	@Override
	protected List<Track> doInBackground(Void... params) {
		if (location == null) {
			return null;
		}

		Database database = App.getInstance().getDatabase();

		List<Category> activeCategories = database.getActiveCategories();
		List<Track> ret = new ArrayList<>();

		try {
			for (Category cat : activeCategories) {
				ret.addAll(loadInCategory(cat));
			}

			storeToDatabase(ret);
			EventBus.getDefault().post(new DataUpdatedEvent());
			return ret;
		} catch (IOException | GetsException e) {
			return null;
		}
	}

	private List<Track> loadInCategory(Category cat) throws IOException, GetsException {
		String requestString = createRequestString(cat);
		TracksContent tracks = gets.query("loadTracks.php", requestString, new TracksParser());
		return tracks.getTracks();
	}

	private String createRequestString(Category cat) {
		try {
			XmlSerializer serializer = Xml.newSerializer();
			StringWriter writer = new StringWriter();
			serializer.setOutput(writer);

			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "request").startTag(null, "params");

			serializer.startTag(null, "category_name").text(cat.getName()).endTag(null, "category_name");
			serializer.startTag(null, "space").text("all").endTag(null, "space");

			if (location != null) {
				serializer.startTag(null, "latitude").text(String.valueOf(location.getLatitude())).endTag(null, "latitude");
				serializer.startTag(null, "longitude").text(String.valueOf(location.getLongitude())).endTag(null, "longitude");
				serializer.startTag(null, "radius").text(String.valueOf(radiusKm)).endTag(null, "radius");
			}

			serializer.endTag(null, "params").endTag(null, "request");
			serializer.flush();

			return writer.toString();
		} catch (IOException e) {
			return null;
		}
	}


	private void storeToDatabase(List<Track> tracks) {
		Database database = App.getInstance().getDatabase();

		for (Track track : tracks) {
			track.setLocal(false);
			database.insertTrack(track);
		}
	}
}
