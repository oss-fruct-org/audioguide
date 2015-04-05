package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.fruct.oss.audioguide.App;
import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.NavigationDrawerFragment;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.TrackCursorAdapter;
import org.fruct.oss.audioguide.config.Config;
import org.fruct.oss.audioguide.track.CursorHolder;
import org.fruct.oss.audioguide.track.DefaultTrackManager;
import org.fruct.oss.audioguide.track.Point;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;
import org.fruct.oss.audioguide.track.tasks.StoreTrackTask;
import org.fruct.oss.audioguide.track.tasks.TracksTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackFragment extends ListFragment implements AdapterView.OnItemLongClickListener {
	private final static Logger log = LoggerFactory.getLogger(TrackFragment.class);

	public static final String STATE_SPINNER_STATE = "spinner-state";
	private static final int HIGHLIGHT_COLOR = 0xff99ff99;

	private MultiPanel multiPanel;
	private TrackManager trackManager;
	private int selectedSpinnerItem = 0;

	private TrackCursorAdapter trackCursorAdapter;
	private CursorHolder cursorHolder;

	private StoreTrackTask storeTrackTask;
	private TracksTask tracksTask;

	private Track selectedTrack;
	private int selectedPosition;
	private ActionMode actionMode;

	public static TrackFragment newInstance() {
		return new TrackFragment();
	}

	public TrackFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		trackManager = DefaultTrackManager.getInstance();
		cursorHolder = trackManager.loadTracks();

		trackCursorAdapter = new TrackCursorAdapter(getActivity());
		cursorHolder.attachToAdapter(trackCursorAdapter);

		setListAdapter(trackCursorAdapter);

		setHasOptionsMenu(true);

		if (savedInstanceState != null) {
			selectedSpinnerItem = savedInstanceState.getInt(STATE_SPINNER_STATE);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_list_view, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();

		setupSpinner();
		getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
	}

	@Override
	public void onPause() {
		super.onPause();

		ActionBarActivity activity = (ActionBarActivity) getActivity();
		ActionBar actionBar = activity.getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}

	private void setupSpinner() {
		ActionBarActivity activity = (ActionBarActivity) getActivity();
		ActionBar actionBar = activity.getSupportActionBar();


		ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(actionBar.getThemedContext(),
				R.array.track_spinner_array,
				android.support.v7.appcompat.R.layout.support_simple_spinner_dropdown_item);

		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		/*actionBar.setListNavigationCallbacks(spinnerAdapter, new ActionBar.OnNavigationListener() {
			@Override
			public boolean onNavigationItemSelected(int i, long l) {
				selectedSpinnerItem = i;
				switch (i) {
				case 0: // All tracks
					trackAdapter.setFilter(new TrackModelAdapter.Filter() {
						@Override
						public boolean check(Track track) {
							return true;
						}
					});
					break;

				case 1: // Private tracks
					trackAdapter.setFilter(new TrackModelAdapter.Filter() {
						@Override
						public boolean check(Track track) {
							return track.isPrivate();
						}
					});
					break;

				case 2: // Public tracks
					trackAdapter.setFilter(new TrackModelAdapter.Filter() {
						@Override
						public boolean check(Track track) {
							return !track.isPrivate();
						}
					});
					break;

				case 3: // Local tracks
					trackAdapter.setFilter(new TrackModelAdapter.Filter() {
						@Override
						public boolean check(Track track) {
							return track.isLocal();
						}
					});
					break;

				case 4: // Active tracks
					trackAdapter.setFilter(new TrackModelAdapter.Filter() {
						@Override
						public boolean check(Track track) {
							return track.isActive();
						}
					});
					break;

				default:
					return false;
				}

				return true;
			}
		});*/
		actionBar.setSelectedNavigationItem(selectedSpinnerItem);
	}

	@Override
	public void onStart() {
		super.onStart();
		getListView().setOnItemLongClickListener(this);
	}

	@Override
	public void onDestroy() {
		if (storeTrackTask != null) {
			storeTrackTask.cancel(true);
		}

		if (tracksTask != null) {
			tracksTask.cancel(true);
		}

		cursorHolder.close();
		trackCursorAdapter.close();
		super.onDestroy();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			multiPanel = (MultiPanel) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement MultiFragment");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		multiPanel = null;
	}

	private void showContextualActionBar(int position) {
		selectedTrack = trackCursorAdapter.getTrack(position);
		selectedPosition = position;
		getListView().setItemChecked(position, true);

		ActionBarActivity activity = ((ActionBarActivity) getActivity());
		actionMode = activity.startSupportActionMode(actionModeCallback);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
		if (actionMode == null) {
			showContextualActionBar(i);
		} else {
			actionMode.finish();
			actionMode = null;
		}

		return true;
	}


	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (actionMode == null) {
			Track track = trackCursorAdapter.getTrack(position);
			multiPanel.replaceFragment(PointFragment.newInstance(track), this);

			if (storeTrackTask != null) {
				storeTrackTask.cancel(true);
			}

			storeTrackTask = new StoreTrackTask(track, false);
			storeTrackTask.execute();
		} else {
			actionMode.finish();
			actionMode = null;
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.refresh, menu);
		if (!Config.isEditLocked()) {
			inflater.inflate(R.menu.edit_track_menu, menu);
		}
		inflater.inflate(R.menu.categories_filter, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_refresh:
			tracksTask = new TracksTask(getActivity());
			tracksTask.execute();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt(STATE_SPINNER_STATE, selectedSpinnerItem);
	}

	private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
		@Override
		public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
			actionMode.getMenuInflater().inflate(R.menu.track_action_menu, menu);

			if (!selectedTrack.isLocal()) {
				menu.findItem(R.id.action_delete).setVisible(false);
				menu.findItem(R.id.action_start_guide).setVisible(false);
			}

			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
			switch (menuItem.getItemId()) {
			case R.id.action_start_guide:
				startGuide();
				actionMode.finish();
				TrackFragment.this.actionMode = null;
				return true;

			case R.id.action_save:
				// FIXME: selectedTrack can sometimes be null
				if (storeTrackTask != null) {
					storeTrackTask.cancel(true);
				}

				storeTrackTask = new StoreTrackTask(selectedTrack);
				storeTrackTask.execute();

				actionMode.finish();
				TrackFragment.this.actionMode = null;
				return true;

			case R.id.action_delete:
				startDeletingTrack(selectedTrack);
				actionMode.finish();
				TrackFragment.this.actionMode = null;
				return true;

			default:
				return false;
			}
		}

		@Override
		public void onDestroyActionMode(ActionMode actionMode) {
			getListView().setItemChecked(selectedPosition, false);
		}
	};

	private void startGuide() {
		final Track selectedTrack = this.selectedTrack;
		final CursorHolder holder = trackManager.loadPoints(selectedTrack);
		holder.setListener(new CursorHolder.Listener() {
			@Override
			public void onReady(Cursor cursor) {
				if (cursor.moveToFirst()) {
					Point firstPoint = new Point(cursor);

					Bundle args = new Bundle();
					args.putParcelable(MapFragment.ARG_POINT, firstPoint);

					trackManager.activateTrackMode(selectedTrack);
					NavigationDrawerFragment frag =
							(NavigationDrawerFragment)
									getActivity().getSupportFragmentManager()
											.findFragmentById(R.id.navigation_drawer);

					frag.selectItem(1, args);
				}
				holder.close();
			}
		});
	}

	private void startDeletingTrack(final Track track) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.delete_track);

		builder.setNeutralButton(R.string.delete_track_local, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				App.getInstance().getDatabase().deleteTrack(track);
				App.getInstance().getPersistenceChecker().updatePersistentUrls();
			}
		});

		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
			}
		});
		builder.create().show();
	}
}
