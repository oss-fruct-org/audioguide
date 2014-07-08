package org.fruct.oss.audioguide;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.graphics.PorterDuff;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.fruct.oss.audioguide.R;

public class HelpActivity extends ActionBarActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
			HelpEntry[] helpEntries1 = {
				new HelpEntry("Sound", "Use this button to start sound automatically", R.drawable.ic_action_volume_on, true),
				new HelpEntry("Refresh", "Press to refresh list of tracks or points", R.drawable.ic_action_refresh, true),
				new HelpEntry("Filter", "Use \"Filter\" to show tracks or points from selected categories", 0, true)
			};

			HelpEntry[] helpEntries2 = {
					new HelpEntry("Tracks", "Press \"Add\" button to create track", R.drawable.ic_action_new, true),
					new HelpEntry("Track menu", "Long press to show track's menu", R.drawable.ic_action_refresh, true),
					new HelpEntry("Activate", "Show track on map hiding unrelated points", R.drawable.ic_action_location_found, true),
					new HelpEntry("Save", "Save content of track to your device", R.drawable.ic_action_save, false),
					new HelpEntry("Delete", "Delete track. AudioGuide will ask you delete track locally or on remote server", R.drawable.ic_action_discard, true)
			};

			HelpEntry[] helpEntries3 = {
					new HelpEntry("Modes", "Map has two modes: track and points", 0, true),
					new HelpEntry("Track mode", "In track mode map shows only active track. Track mode can be switched from menu or from track window by selecting \"Activate\" option from track's menu. " +
							"Track mode can be deactivated by choosing menu button", android.R.drawable.ic_menu_mapmode, true),
					new HelpEntry("Point mode", "Show all loaded points", android.R.drawable.ic_menu_mapmode, true),
					new HelpEntry("Search", "Search near points. Searching radius can be adjusted in settings window", R.drawable.ic_action_search, true),
					new HelpEntry("Place here", "Set position to current map center", 0, true),
					new HelpEntry("Add point", "Create new point", R.drawable.ic_action_new, true)
			};

			HelpEntry[] helpEntries4 = {
					new HelpEntry("Point menu", "Long press point to access point menu", 0, true),
					new HelpEntry("Add to track", "Add point to track", R.drawable.ic_action_share, true),
					new HelpEntry("Edit", "Edit point description, image and audio", R.drawable.ic_action_edit, true),
			};

			switch (position) {
			case 0:
				return PlaceholderFragment.newLayoutInstance(helpEntries1);
			case 1:
				return PlaceholderFragment.newLayoutInstance(helpEntries2);
			case 2:
				return PlaceholderFragment.newLayoutInstance(helpEntries3);
			case 3:
				return PlaceholderFragment.newLayoutInstance(helpEntries4);
			default:
				return PlaceholderFragment.newLayoutInstance(helpEntries1);
			}
		}

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 1:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 3:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		private static final String ARG_ENTRIES = "layout_number";

		public static Fragment newLayoutInstance(HelpEntry[] entries) {
			ArrayList<HelpEntry> entriesArrList = new ArrayList<HelpEntry>();
			Collections.addAll(entriesArrList, entries);

			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();

			args.putSerializable(ARG_ENTRIES, entriesArrList);

			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
								 Bundle savedInstanceState) {
			List<HelpEntry> entires = (List) getArguments().getSerializable(ARG_ENTRIES);

			ScrollView scrollView = new ScrollView(getActivity());
			scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			scrollView.setHorizontalScrollBarEnabled(false);
			scrollView.setVerticalScrollBarEnabled(false);

			LinearLayout linearLayout = new LinearLayout(getActivity());
			linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			linearLayout.setOrientation(LinearLayout.VERTICAL);

			scrollView.addView(linearLayout);

			for (HelpEntry entry : entires) {
				View entryView = inflater.inflate(R.layout.help_template, linearLayout, false);

				TextView titleView = (TextView) entryView.findViewById(R.id.help_template_title);
				TextView textView = (TextView) entryView.findViewById(R.id.help_template_text);
				ImageView iconView = (ImageView) entryView.findViewById(R.id.help_template_icon);

				titleView.setText(entry.title);
				textView.setText(entry.text);

				if (entry.iconRes != 0) {
					iconView.setImageResource(entry.iconRes);
					iconView.setVisibility(View.VISIBLE);

					if (entry.reversed) {
						iconView.setColorFilter(0xff515151, PorterDuff.Mode.SRC_ATOP);
					}
				}


				linearLayout.addView(entryView);
			}

			return scrollView;
		}
	}

	@Override
	public void onBackPressed() {
		finish();
	}

	@Override
	public boolean onNavigateUp() {
		finish();
		return true;
	}


	private static class HelpEntry implements Serializable {
		HelpEntry(String title, String text, int iconRes, boolean reversed) {
			this.title = title;
			this.text = text;
			this.iconRes = iconRes;
			this.reversed = reversed;
		}

		String title;
		String text;
		int iconRes;
		boolean reversed;
	}
}
