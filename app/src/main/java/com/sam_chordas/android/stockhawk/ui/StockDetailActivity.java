package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

import java.util.Set;

/**
 * Created by Garrison on 4/14/2016.
 */
public class StockDetailActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String LOG_TAG = StockDetailActivity.class.getSimpleName();

    public Context mContext;
    public Intent mServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        setContentView(R.layout.activity_line_graph);

        boolean isConnected = Utils.checkNetworkState(mContext);

        if (isConnected) {
            // Call to service to obtain charting values
            mServiceIntent = new Intent(this, StockIntentService.class);
            mServiceIntent.putExtra("tag", "chart");
            Intent thisIntent = getIntent();
            String symbol = thisIntent.getStringExtra("symb");
            mServiceIntent.putExtra("symbol", symbol);
            startService(mServiceIntent);
        }


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
/*

        LineSet dataset = new LineSet(String[] labels, Float[] values);
        dataset.addPoint(new Point(string, float))
        dataset.addPoint(string, float)
*/

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
Log.v(LOG_TAG, "LISTENER REACHED");
        if (key.equals(getString(R.string.prefs_detail_string_set))) {
            Set<String> valuesSet = sharedPreferences.getStringSet(mContext.getString(R.string.prefs_detail_string_set), null);
            if (valuesSet !=null) {
                String[] labelArray = valuesSet.toArray(new String[valuesSet.size()]);

                for (int i = 0; i < labelArray.length; i++) {
                    Log.v(LOG_TAG, "NEW STUFF: " + labelArray[i]);
                }
        /*
        LineSet dataset = new LineSet(String[] labels, Float[] values);
        dataset.addPoint(new Point(string, float))
        dataset.addPoint(string, float)
        */
            }
            else
                Log.v(LOG_TAG, "NO VALUES IN STET");
        }
    }
}
