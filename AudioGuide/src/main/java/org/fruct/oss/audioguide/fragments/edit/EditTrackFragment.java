package org.fruct.oss.audioguide.fragments.edit;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import org.fruct.oss.audioguide.MultiPanel;
import org.fruct.oss.audioguide.R;
import org.fruct.oss.audioguide.adapters.TrackAdapter;
import org.fruct.oss.audioguide.track.Track;
import org.fruct.oss.audioguide.track.TrackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EditTrackFragment extends ListFragment implements TrackManager.Listener {
	private final static Logger log = LoggerFactory.getLogger(EditTrackFragment.class);

    private MultiPanel multiPanel;
	private TrackManager trackManager;
	private TrackAdapter trackAdapter;

	public static EditTrackFragment newInstance() {
		return new EditTrackFragment();
    }

    public EditTrackFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		trackManager = TrackManager.getInstance();
		trackManager.addListener(this);

		trackAdapter = new TrackAdapter(getActivity(), R.layout.list_track_item, trackManager.getEditingTracks());
		setListAdapter(trackAdapter);
		setHasOptionsMenu(true);
    }

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.edit_track_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			log.debug("Add track action");
			showEditDialog(null);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            multiPanel = (MultiPanel) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement MultiPanel");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        multiPanel = null;
    }

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final Track track = trackAdapter.getItem(position);

		PopupMenu menu = new PopupMenu(getActivity(), v);

		final MenuItem editMenuItem= menu.getMenu().add("Edit description");
		final MenuItem pointsMenuItem = menu.getMenu().add("Edit points");

		menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				if (menuItem == editMenuItem) {
					showEditDialog(track);
				} else if (menuItem == pointsMenuItem) {

				}

				return true;
			}
		});
		menu.show();
	}

	@Override
	public void tracksUpdated() {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				List<Track> editingTracks = trackManager.getEditingTracks();

				trackAdapter.clear();
				for (Track track : editingTracks) {
					trackAdapter.add(track);
				}

				trackAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void trackUpdated(Track track) {
		tracksUpdated();
	}

	@Override
	public void pointsUpdated(Track track) {

	}

	private void showEditDialog(Track track) {
		EditTrackDialog dialog = new EditTrackDialog(track);
		dialog.setListener(editDialogListener);
		dialog.show(getFragmentManager(), "edit-track-dialog");
	}

	private EditTrackDialog.Listener editDialogListener = new EditTrackDialog.Listener() {
		@Override
		public void trackCreated(Track track) {
			log.debug("Track created callback");
			trackManager.storeLocal(track);
		}

		@Override
		public void trackUpdated(Track track) {
			log.debug("Track updated callback");
			trackManager.storeLocal(track);
		}
	};
}
