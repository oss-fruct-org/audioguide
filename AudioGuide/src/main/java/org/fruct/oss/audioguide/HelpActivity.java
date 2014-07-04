package org.fruct.oss.audioguide;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
			HelpEntry[] helpEntries = {
				new HelpEntry("qweasdzxc", "asdzxcasd", R.drawable.ic_action_pause),
				new HelpEntry("asdasdasd", "zxczxczxc", R.drawable.ic_action_refresh)
			};

			switch (position) {
			case 0:
			case 1:
			case 2:
				return PlaceholderFragment.newLayoutInstance(helpEntries);
			default:
				return PlaceholderFragment.newLayoutInstance(helpEntries);
			}
		}

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
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
				View entryView = inflater.inflate(R.layout.help_template, linearLayout);

				TextView titleView = (TextView) entryView.findViewById(R.id.help_template_title);
				TextView textView = (TextView) entryView.findViewById(R.id.help_template_text);
				ImageView iconView = (ImageView) entryView.findViewById(R.id.help_template_icon);

				titleView.setText(entry.title);
				textView.setText(entry.text);

				if (entry.iconRes != 0) {
					iconView.setImageResource(entry.iconRes);
				}
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
		HelpEntry(String title, String text, int iconRes) {
			this.title = title;
			this.text = text;
			this.iconRes = iconRes;
		}

		String title;
		String text;
		int iconRes;
	}
}
