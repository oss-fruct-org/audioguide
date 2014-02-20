package org.fruct.oss.audioguide;

import android.app.Activity;
import android.net.Uri;
import android.support.v4.app.FragmentTransaction;
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
	private int[] panelIds = {R.id.panel1, R.id.panel2, R.id.panel3};

	private static final String STATE_STACK_SIZE = "stack-size";
	private static final String STATE_DRAWER_UP_ENABLED = "drawer-up-enabled";
	private static final String STATE_STACK_PREFIX = "stack-prefix-id-";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

	private int panelsCount = 0;
	private List<Fragment> fragmentStack = new ArrayList<Fragment>();
	private boolean suppressDrawerItemChange = false;

	private FragmentManager fragmentManager;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		fragmentManager = getSupportFragmentManager();

		// Restore old stack state
		if (savedInstanceState != null) {
			int stackSize = savedInstanceState.getInt(STATE_STACK_SIZE);
			for (int i = 0; i < stackSize; i++) {
				Fragment fragment = fragmentManager.getFragment(savedInstanceState, STATE_STACK_PREFIX + i);
				fragmentStack.add(fragment);
			}
			suppressDrawerItemChange = true;
		}

		setContentView(R.layout.activity_main);

		for (int i = 0; i < panelIds.length; i++) {
			View view = findViewById(panelIds[i]);
			if (view == null) {
				panelsCount = i;
				break;
			}
		}
		panelsCount = 3;

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                fragmentManager.findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

		if (savedInstanceState != null) {
			mNavigationDrawerFragment.setUpEnabled(savedInstanceState.getBoolean(STATE_DRAWER_UP_ENABLED));
		}
	}

    @Override
    public void onNavigationDrawerItemSelected(int position) {
		System.out.println("onNavigationDrawerItemSelected");

		if (suppressDrawerItemChange) {
			suppressDrawerItemChange = false;
			return;
		}

		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction trans = fragmentManager.beginTransaction();
		for (int i = 1; i < fragmentStack.size(); i++)
			trans.remove(fragmentStack.get(i));
		trans.commit();

		Fragment fragment = (position == 0
				? TestFragment.newInstance("qwe", "asd")
				: PlaceholderFragment.newInstance(position + 1));
		fragmentStack.clear();
		fragmentStack.add(fragment);

		fragmentManager.beginTransaction()
                .replace(R.id.panel1, fragment)
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
		if (fragmentStack.size() > panelsCount)
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

	@Override
	public void pushFragment(Fragment fragment) {
		fragmentStack.add(fragment);

		if (fragmentStack.size() <= panelsCount) {
			fragmentManager.beginTransaction()
					.replace(panelIds[fragmentStack.size() - 1], fragment)
					.commit();
		} else {
			int firstFragmentIdx = fragmentStack.size() - panelsCount - 1;

			FragmentTransaction trans = fragmentManager.beginTransaction();

			// Detach fragment in first panel
			trans.detach(fragmentStack.get(firstFragmentIdx));

			// Fragments since second panel shifting left
			// First detach remaining fragments
			for (int panelIdx = 1; panelIdx < panelsCount; panelIdx++) {
				int fragmentIdx = firstFragmentIdx + panelIdx;
				Fragment shiftingFragment = fragmentStack.get(fragmentIdx);
				trans.remove(shiftingFragment);
			}
			trans.commit();
			fragmentManager.executePendingTransactions();

			// Then reattach them
			trans = fragmentManager.beginTransaction();
			for (int panelIdx = 0; panelIdx < panelsCount; panelIdx++) {
				int fragmentIdx = firstFragmentIdx + panelIdx + 1;
				Fragment shiftingFragment = fragmentStack.get(fragmentIdx);

				trans.add(panelIds[panelIdx], shiftingFragment);
			}

			// Add new fragment to last position
			//trans.add(panelIds[panelsCount - 1], fragment);
			trans.commit();

			mNavigationDrawerFragment.setUpEnabled(false);
		}
	}

	@Override
	public void popFragment() {
		if (fragmentStack.size() <= panelsCount) {
			fragmentManager.beginTransaction().remove(fragmentStack.get(fragmentStack.size() - 1)).commit();
		} else {
			// Remove last fragment
			FragmentTransaction trans = fragmentManager.beginTransaction();
			int firstFragmentIdx = fragmentStack.size() - panelsCount;

			trans.remove(fragmentStack.get(fragmentStack.size() - 1));

			// Shift other fragments right
			for (int panelIdx = 0; panelIdx < panelsCount - 1; panelIdx++) {
				int fragmentIdx = firstFragmentIdx + panelIdx;
				Fragment shiftingFragment = fragmentStack.get(fragmentIdx);
				trans.remove(shiftingFragment);
			}
			trans.commit();
			fragmentManager.executePendingTransactions();

			// Reattach them
			trans = fragmentManager.beginTransaction();
			for (int panelIdx = 1; panelIdx < panelsCount; panelIdx++) {
				int fragmentIdx = firstFragmentIdx + panelIdx - 1;
				Fragment shiftingFragment = fragmentStack.get(fragmentIdx);

				trans.add(panelIds[panelIdx], shiftingFragment);
			}
			trans.attach(fragmentStack.get(firstFragmentIdx - 1));
			trans.commit();

		}

		fragmentStack.remove(fragmentStack.size() - 1);

		if (fragmentStack.size() <= panelsCount) {
			mNavigationDrawerFragment.setUpEnabled(true);
			restoreActionBar();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt(STATE_STACK_SIZE, fragmentStack.size());
		outState.putBoolean(STATE_DRAWER_UP_ENABLED, fragmentStack.size() <= panelsCount);
		int idx = 0;
		for (Fragment fragment : fragmentStack) {
			fragmentManager.putFragment(outState, STATE_STACK_PREFIX + idx++, fragment);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
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
