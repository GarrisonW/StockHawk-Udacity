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

/**
 * Created by Garrison on 4/14/2016.
 */
public class StockDetailActivity extends AppCompatActivity {

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

/*

        LineSet dataset = new LineSet(String[] labels, Float[] values);
        dataset.addPoint(new Point(string, float))
        dataset.addPoint(string, float)
*/

    }

}
