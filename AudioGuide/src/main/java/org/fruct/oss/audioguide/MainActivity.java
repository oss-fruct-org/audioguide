package org.fruct.oss.audioguide;

import android.app.Activity;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, MultiPanel, TestFragment.OnFragmentInteractionListener {
	//private static final int[] PANEL_IDS = {R.id.panel1, R.id.panel2, R.id.panel3};

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;


    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

	private int panelsCount = 1;
	private int currentIndex = 0;
	private List<Fragment> fragmentStack = new ArrayList<Fragment>();
	private  FragmentManager fragmentManager;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		fragmentManager = getSupportFragmentManager();
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                fragmentManager.findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		while (fragmentManager.getBackStackEntryCount() > 0)
			fragmentManager.popBackStack();

        fragmentManager.beginTransaction()
                .replace(R.id.panel1, position == 0
						? TestFragment.newInstance("qwe", "asd")
						: PlaceholderFragment.newInstance(position + 1))
				.commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

	@Override
	public boolean onSupportNavigateUp() {
		if (fragmentManager.getBackStackEntryCount() > 0)
			popFragment();

		return super.onSupportNavigateUp();
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

	/*private void updateFragmentsLayout() {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		for (int i = 0; i < panelsCount; i++) {
			int fragmentIdx = currentIndex + i;

			if (fragmentIdx < fragmentStack.size())
				transaction.replace(PANEL_IDS[i], fragmentStack.get(fragmentIdx));

		}

		transaction.commit();
	}*/

	@Override
	public void pushFragment(Fragment fragment) {
		mNavigationDrawerFragment.setUpEnabled(false);
		fragmentManager.beginTransaction()
				.replace(R.id.panel1, fragment)
				.addToBackStack(null)
				.commit();
	}

	@Override
	public void popFragment() {
		int count = fragmentManager.getBackStackEntryCount();

		// Don't allow pop last fragment
		if (count > 0)
			fragmentManager.popBackStack();

		// This is root fragment, re-attach drawer
		if (count == 1) {
			mNavigationDrawerFragment.setUpEnabled(true);
			restoreActionBar();
		}
	}

	@Override
	public void onFragmentInteraction(Uri uri) {

	}

	/**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}
