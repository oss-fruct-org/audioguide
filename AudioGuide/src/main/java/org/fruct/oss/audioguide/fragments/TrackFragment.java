package org.fruct.oss.audioguide.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.TrackModelAdapter;
import org.fruct.oss.audioguide.fragments.edit.EditTrackDialog;
import org.fruct.oss.audioguide.models.CombineModel;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.track2.DefaultTrackManager;
import org.fruct.oss.audioguide.track.track2.TrackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackFragment extends ListFragment implements PopupMenu.OnMenuItemClickListener {
	private final static Logger log = LoggerFactory.getLogger(TrackFragment.class);

	public static final String STATE_SPINNER_STATE = "spinner-state";
	private static final int HIGHLIGHT_COLOR = 0xff99ff99;

	private SharedPreferences pref;

	private MultiPanel multiPanel;
	private TrackManager trackManager;
	private int selectedSpinnerItem = 0;

	private TrackModelAdapter trackAdapter;

	private MenuItem popupShowPoints;
	private MenuItem popupItemEdit;
	private MenuItem popupItemEditPoints;
	private MenuItem popupItemDeactivate;
	private MenuItem popupItemActivate;
	private MenuItem popupItemDownload;
	private MenuItem popupItemSend;

	private Track selectedTrack;

	public static TrackFragment newInstance() {
		return new TrackFragment();
	}

	public TrackFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

		trackManager = DefaultTrackManager.getInstance();

		trackAdapter = new TrackModelAdapter(getActivity(), R.layout.list_track_item,
				trackManager.getTracksModel());
		//trackAdapter.addTrackHighlight(trackManager.getEditingTrack(), HIGHLIGHT_COLOR);

		setListAdapter(trackAdapter);

		setHasOptionsMenu(true);

		if (savedInstanceState != null) {
			selectedSpinnerItem = savedInstanceState.getInt(STATE_SPINNER_STATE);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		setupSpinner();
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
		actionBar.setListNavigationCallbacks(spinnerAdapter, new ActionBar.OnNavigationListener() {
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
		});
		actionBar.setSelectedNavigationItem(selectedSpinnerItem);
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onDestroy() {
		trackAdapter.close();
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

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Track track = trackAdapter.getItem(position);
		multiPanel.replaceFragment(PointFragment.newInstance(track), this);
		trackManager.requestPointsInTrack(track);

		/*
		PopupMenu popupMenu = new PopupMenu(getActivity(), v);
		Menu menu = popupMenu.getMenu();


		if (track.isLocal()) {
			popupShowPoints = menu.add("Show points");

			if (track.isActive()) {
				popupItemDeactivate = menu.add("Deactivate");
			} else {
				popupItemActivate = menu.add("Activate");
			}
		} else {
			popupItemDownload = menu.add("Download");
		}

		if (track.isPrivate() && track.isLocal()) {
			SubMenu editingMenu = menu.addSubMenu("Editing");

			popupItemEdit = editingMenu.add("Edit description");
			popupItemEditPoints = editingMenu.add("Edit points");

			if (pref.getString(GetsStorage.PREF_AUTH_TOKEN, null) != null) {
				popupItemSend = editingMenu.add("Send to server");
			}
		}

		selectedTrack = track;
		popupMenu.setOnMenuItemClickListener(this);
		popupMenu.show();
*/
		//trackClicked(track);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.refresh, menu);
		inflater.inflate(R.menu.edit_track_menu, menu);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_refresh:
			trackManager.requestTracksInRadius(0, 0, 7000);
			break;
		case R.id.action_add:
			EditTrackDialog dialog = EditTrackDialog.newInstance(null);
			dialog.setListener(editDialogListener);
			dialog.show(getFragmentManager(), "edit-track-dialog");
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		ActionBarActivity activity = (ActionBarActivity) getActivity();
		ActionBar actionBar = activity.getSupportActionBar();

		outState.putInt(STATE_SPINNER_STATE, selectedSpinnerItem);
	}

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		if (menuItem == popupItemActivate) {
			//trackManager.activateTrack(selectedTrack);
		} else if (menuItem == popupItemDeactivate) {
			//trackManager.deactivateTrack(selectedTrack);
		} else if (menuItem == popupItemDownload) {
			//trackManager.refreshPoints(selectedTrack);
		} else if (menuItem == popupShowPoints) {
			multiPanel.replaceFragment(PointFragment.newInstance(selectedTrack), this);
		} else if (menuItem == popupItemEdit) {
			EditTrackDialog dialog = EditTrackDialog.newInstance(selectedTrack);
			dialog.setListener(editDialogListener);
			dialog.show(getFragmentManager(), "edit-track-dialog");
		} else if (menuItem == popupItemEditPoints) {
			//trackManager.setEditingTrack(selectedTrack);

			trackAdapter.clearTrackHighlights();
			//trackAdapter.addTrackHighlight(trackManager.getEditingTrack(), HIGHLIGHT_COLOR);
			trackAdapter.notifyDataSetChanged();
		} else if (menuItem == popupItemSend) {
			//trackManager.sendTrack(selectedTrack);
			//trackManager.setEditingTrack(null);
			trackAdapter.clearTrackHighlights();
			trackAdapter.notifyDataSetChanged();
		}

		return false;
	}

	private EditTrackDialog.Listener editDialogListener = new EditTrackDialog.Listener() {
		@Override
		public void trackCreated(Track track) {
			log.debug("Track created callback");
			//trackManager.storeLocal(track);
		}

		@Override
		public void trackUpdated(Track track) {
			log.debug("Track updated callback");
			//trackManager.storeLocal(track);
		}
	};

}
