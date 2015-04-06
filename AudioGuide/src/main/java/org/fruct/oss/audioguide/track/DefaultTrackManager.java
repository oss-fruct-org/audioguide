package org.fruct.oss.audioguide.track;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.events.DataUpdatedEvent;
import org.fruct.oss.audioguide.track.gets.Category;
import org.fruct.oss.audioguide.util.EventReceiver;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.greenrobot.event.EventBus;

public class DefaultTrackManager implements TrackManager, Closeable {
	private final Database database;
	private final SharedPreferences pref;

	private List<Category> categories;

	private final List<CursorHolder> cursorHolders = new ArrayList<CursorHolder>();

	private boolean isClosed;

	public DefaultTrackManager(Context context) {
		pref = PreferenceManager.getDefaultSharedPreferences(context);

		database = App.getInstance().getDatabase();

		EventBus.getDefault().register(this);
	}

	@Override
	public synchronized void close() {
		EventBus.getDefault().unregister(this);
		isClosed = true;
	}

	@Override
	public void activateTrackMode(Track track) {
		if (track == null)
			pref.edit().remove(PREF_TRACK_MODE).apply();
		else
			pref.edit().putString(PREF_TRACK_MODE, track.getName()).apply();
	}

	@Override
	public CursorHolder loadTracks() {
		CursorHolder cursorHolder = new CursorHolder() {
			@Override
			protected Cursor doQuery() {
				return database.loadAllTracksCursor();
			}
		};

		addCursorHolder(cursorHolder);
		cursorHolder.queryAsync();
		return cursorHolder;
	}

	@Override
	public CursorHolder loadLocalTracks() {
		CursorHolder cursorHolder = new CursorHolder() {
			@Override
			protected Cursor doQuery() {
				return database.loadLocalTracks();
			}
		};
		addCursorHolder(cursorHolder);
		cursorHolder.queryAsync();
		return cursorHolder;
	}

	@Override
	public CursorHolder loadPrivateTracks() {
		CursorHolder cursorHolder = new CursorHolder() {
			@Override
			protected Cursor doQuery() {
				return database.loadPrivateTracks();
			}
		};
		addCursorHolder(cursorHolder);
		cursorHolder.queryAsync();
		return cursorHolder;
	}

	@Override
	public CursorHolder loadPublicTracks() {
		CursorHolder cursorHolder = new CursorHolder() {
			@Override
			protected Cursor doQuery() {
				return database.loadPublicTracks();
			}
		};
		addCursorHolder(cursorHolder);
		cursorHolder.queryAsync();
		return cursorHolder;

	}

	@Override
	public CursorHolder loadLocalPoints() {
		CursorHolder cursorHolder = new CursorHolder() {
			@Override
			protected Cursor doQuery() {
				return database.loadPointsCursor();
			}
		};

		addCursorHolder(cursorHolder);
		cursorHolder.queryAsync();
		return cursorHolder;

	}

	@Override
	public CursorHolder loadPoints(final Track track) {
		CursorHolder cursorHolder = new CursorHolder() {
			@Override
			protected Cursor doQuery() {
				return database.loadPointsCursor(track);
			}
		};

		addCursorHolder(cursorHolder);
		cursorHolder.queryAsync();
		return cursorHolder;
	}

	@Override
	public Track getTrackByName(String name) {
		if (name == null)
			return null;
		return database.getTrackByName(name);
	}

	@Override
	public List<Category> getCategories() {
		if (categories == null) {
			categories = database.getCategories();
		}

		return categories;
	}

	@Override
	public List<String> getPointPhotos(Point point) {
		Cursor photoCursor = database.loadPointPhotos(point);
		ArrayList<String> ret = new ArrayList<String>(photoCursor.getCount());
		while (photoCursor.moveToNext()) {
			ret.add(photoCursor.getString(0));
		}
		photoCursor.close();
		return ret;
	}

	@Override
	public void setCategoryState(Category category, boolean isActive) {
		category.setActive(isActive);
		database.setCategoryState(category);

		for (Category cat : categories) {
			if (category.getId() == cat.getId()) {
				cat.setActive(isActive);
			}
		}

		//Gets.getInstance().setEnv("categories", activeCategories);
		notifyDataChanged();
	}

	private void notifyDataChanged() {
		EventBus.getDefault().post(new DataUpdatedEvent());
	}

	private CursorHolder addCursorHolder(CursorHolder cursorHolder) {
		cursorHolders.add(cursorHolder);
		return cursorHolder;
	}

	@EventReceiver
	public void onEventMainThread(DataUpdatedEvent event) {
		for (Iterator<CursorHolder> iterator = cursorHolders.iterator(); iterator.hasNext(); ) {
			CursorHolder holder = iterator.next();

			if (holder.isClosed()) {
				iterator.remove();
				continue;
			}

			holder.queryAsync();
		}
	}

	private static DefaultTrackManager instance;
	public synchronized static TrackManager getInstance() {
		if (instance == null || instance.isClosed) {
			instance = new DefaultTrackManager(App.getContext());
			instance.getCategories();
		}

		return instance;
	}
}
