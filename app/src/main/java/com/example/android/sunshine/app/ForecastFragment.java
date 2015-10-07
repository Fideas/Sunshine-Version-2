package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

/**
 * Fragment used to retrieve weather data from the API
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;
    private static final int WEATHER_LOADER_ID = 0;
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };
    private static final String LIST_ITEM_POSITION = "listItemPosition";
    private final String LOG_TAG = ForecastFragment.class.getSimpleName();
    ForecastAdapter mForecastAdapter;
    private ListView mListView;
    private TextView mEmptyView;
    private int mPosition = ListView.INVALID_POSITION;
    private boolean mUseTodayLayout;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Let the fragment populate the menu
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //read the position from the bundle
        if (savedInstanceState != null && savedInstanceState.containsKey(LIST_ITEM_POSITION)) {
            mPosition = savedInstanceState.getInt(LIST_ITEM_POSITION);
        }

        //Create the adapter
        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);

        //Get the ListView by id and bind adapter
        mListView = (ListView) rootView.findViewById(R.id.listview_fragment);
        mListView.setAdapter(mForecastAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                mPosition = position;
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(mPosition);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    ((Callback) getActivity())
                            .onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                    locationSetting, cursor.getLong(COL_WEATHER_DATE)
                            ));
                }
            }
        });
        //Add the empty view to the ListView
        mEmptyView = (TextView) rootView.findViewById(R.id.empty_view);
        mListView.setEmptyView(mEmptyView);

        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        //Prepare the loader
        getLoaderManager().initLoader(WEATHER_LOADER_ID, null, this);

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        super.onResume();

    }

    @Override
    public void onPause() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateWeather() {
        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    public void onLocationChanged() {
        updateWeather();
        getLoaderManager().restartLoader(WEATHER_LOADER_ID, null, this);
    }

    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String locationSetting = Utility.getPreferredLocation(getActivity());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());
        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        updateEmptyView();
        mForecastAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            mListView.smoothScrollToPosition(mPosition);
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(LIST_ITEM_POSITION, mPosition);
        }
        ;
        super.onSaveInstanceState(outState);
    }

    private void updateEmptyView() {
        if (mForecastAdapter == null || mForecastAdapter.getCount() == 0) {
            int locationStatus = Utility.getLocationStatus(getActivity());
            mEmptyView.setText(getString(R.string.empty_forecast_list));
            switch (locationStatus) {
                case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN: {
                    mEmptyView.append(getString(R.string.server_down));
                    break;
                }
                case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID: {
                    mEmptyView.append(getString(R.string.server_error));
                    break;
                }
                case SunshineSyncAdapter.LOCATION_STATUS_INVALID: {
                    mEmptyView.append(getString(R.string.invalid_location));
                    break;
                }
                default: {
                    if (!Utility.isOnline(getActivity())) {
                        mEmptyView.append(getString(R.string.no_connection));
                    }
                    break;
                }
            }
        }
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (mForecastAdapter != null) {
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }

    public void openPreferredLocationInMap() {
        //Get location from cursor
        if (mForecastAdapter != null) {
            Cursor cursor = mForecastAdapter.getCursor();

            if (cursor != null) {
                cursor.moveToFirst();
                String lat = cursor.getString(COL_COORD_LAT);
                String lon = cursor.getString(COL_COORD_LONG);

                //Build URI
                String uriString = String.format("geo:%s,%s", lat, lon);
                Uri geoLocation = Uri.parse(uriString);
                //Create Intent and append the data
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.e(LOG_TAG, "Couldn't call " + geoLocation.toString()
                            + ". No receiving apps installed!");
                }
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(getString(R.string.pref_location_status_key))) {
            updateEmptyView();
        }
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        void onItemSelected(Uri dateUri);
    }
}