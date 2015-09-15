package com.example.android.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;

/**
 * A simple fragment for the DetailActivity
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_WEATHER_HUMIDITY = 5;
    static final int COL_WEATHER_PRESSURE = 6;
    static final int COL_WEATHER_WINDSPEED = 7;
    static final int COL_WEATHER_DEGREES = 8;
    static final int COL_WEATHER_CONDITION_ID = 9;

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();
    private static final String SHARE_TAG = "#SunshineApp";
    private static final int WEATHER_LOADER_ID = 0;
    private static final String[] DETAILED_COLUMNS = {
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
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };
    private String mForecastStr;
    private ShareActionProvider mShareActionProvider;

    private ImageView mIconView;
    private TextView mFriendlyDateView;
    private TextView mDescriptionView;
    private TextView mHighTempView;
    private TextView mLowTempView;
    private TextView mHumidityView;
    private TextView mWindView;
    private TextView mPressureView;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        //Prepare the loader
        getLoaderManager().initLoader(WEATHER_LOADER_ID, null, this);

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);
        mIconView = (ImageView) view.findViewById(R.id.list_item_icon);
        mFriendlyDateView = (TextView) view.findViewById(R.id.list_item_date_textview);
        mDescriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
        mHighTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
        mLowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
        mHumidityView = (TextView) view.findViewById(R.id.list_item_humidity_textview);
        mWindView = (TextView) view.findViewById(R.id.list_item_wind_textview);
        mPressureView = (TextView) view.findViewById(R.id.list_item_pressure_textview);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate menu resource file.
        inflater.inflate(R.menu.detailfragment, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.action_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        if (mForecastStr != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
        } else {
            Log.d(LOG_TAG, "Shared action provider is null?");
        }
    }

    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Intent intent = getActivity().getIntent();
        if (intent == null) {
            return null;
        } else {
            return new CursorLoader(getActivity(),
                    intent.getData(),
                    DETAILED_COLUMNS,
                    null,
                    null,
                    null);
        }
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }
        //Set weather icon
        int weatherConditionId = data.getInt(COL_WEATHER_CONDITION_ID);
        int weatherArt = Utility.getArtResourceForWeatherCondition(weatherConditionId);
        mIconView.setImageResource(weatherArt);

        //Set Date
        long date = data.getLong(COL_WEATHER_DATE);
        mFriendlyDateView.setText(Utility.getFriendlyDayString(getActivity(), date));

        //Set Forecast
        String forecast = data.getString(COL_WEATHER_DESC);
        mDescriptionView.setText(forecast);

        boolean isMetric = Utility.isMetric(getActivity());

        //Set Temperature
        float high = data.getFloat(COL_WEATHER_MAX_TEMP);
        mHighTempView.setText(Utility.formatTemperature(getActivity(), high, isMetric));
        float low = data.getFloat(COL_WEATHER_MIN_TEMP);
        mLowTempView.setText(Utility.formatTemperature(getActivity(), low, isMetric));

        //Set Pressure
        float pressure = data.getFloat(COL_WEATHER_PRESSURE);
        mPressureView.setText(getString(R.string.format_pressure, pressure));

        //Set humidity
        float humidity = data.getFloat(COL_WEATHER_HUMIDITY);
        mHumidityView.setText(getString(R.string.format_humidity, humidity));

        //Set wind information
        float windSpeed = data.getFloat(COL_WEATHER_WINDSPEED);
        float windDirection = data.getFloat(COL_WEATHER_DEGREES);
        mWindView.setText(Utility.getFormattedWind(getActivity(), windSpeed, windDirection));

        // If onCreateOptionsMenu has already happened, we need to update the share intent now.
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecastStr + SHARE_TAG);
        return shareIntent;
    }
}
