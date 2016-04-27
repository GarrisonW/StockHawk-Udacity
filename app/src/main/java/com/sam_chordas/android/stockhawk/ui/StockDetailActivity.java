package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

import java.util.ArrayList;
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

        setTitle(R.string.title_detail_activity);

        if (isConnected) {
            // Call to service to obtain charting values
            mServiceIntent = new Intent(this, StockIntentService.class);
            mServiceIntent.putExtra("tag", "chart");
            Intent thisIntent = getIntent();
            String symbol = thisIntent.getStringExtra("symb");
            mServiceIntent.putExtra("symbol", symbol);
            startService(mServiceIntent);
        }
        else
            Log.v(LOG_TAG, "NO NEtowrkd for charting");


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences != null) {
            loadData(sharedPreferences);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.v(LOG_TAG, "PREFCHANGE");
        if (key.equals(getString(R.string.prefs_detail_string_set))) {
            loadData(sharedPreferences);
        }
    }

    public void loadData(SharedPreferences sharedPreferences) {

        Set<String> valuesSet = sharedPreferences.getStringSet(mContext.getString(R.string.prefs_detail_string_set), null);
        if (valuesSet !=null) {
            String[] labelArray = valuesSet.toArray(new String[valuesSet.size()]);
            Log.v(LOG_TAG, "SIXE " + labelArray.length);
            int max = 0;
            int min = 0;
            LineChartView lineChartView = (LineChartView) findViewById(R.id.linechart);
            LineSet lineSet = new LineSet();
            for (int i = 0; i < labelArray.length; i++) {
                Log.v(LOG_TAG, "NEW STUFF: " + labelArray[i]);
                float f = Float.parseFloat(labelArray[i]);
                Float tempFloat = new Float(f);

                Log.v(LOG_TAG, "INTERGER: " + tempFloat.intValue());
                if (max < tempFloat.intValue())
                    max = tempFloat.intValue();
                if (min == 0 || min > tempFloat.intValue())
                    min = tempFloat.intValue();
                lineSet.addPoint(new Point(labelArray[i], f));
            }

            lineSet.setColor(Color.parseColor("#758cbb"))
                    .setDotsColor(Color.parseColor("#758cbb"))
                    .setThickness(4)
                    .setDashed(new float[]{10f,10f})
                    .beginAt(0);

            lineChartView.setAxisBorderValues(min, max + 1);
            lineChartView.addData(lineSet);
            lineChartView.show();
        }
        else
            Log.v(LOG_TAG, "NO VALUES IN STET");
    }
}
