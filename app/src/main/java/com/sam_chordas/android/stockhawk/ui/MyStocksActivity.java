package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

import java.util.Set;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener{

    private static String LOG_TAG = MyStocksActivity.class.getSimpleName();
    /**
      * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
    */

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;

    FloatingActionButton fab;

    public TextView mNetworkStatusTextView;
    public TextView mStatusTextView;
    boolean isConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        LocalBroadcastManager.getInstance(mContext).registerReceiver(mStockLoadReceiver,
                new IntentFilter(mContext.getString(R.string.broadcast_stock_search)));

        setContentView(R.layout.activity_my_stocks);
          // The intent service is for executing immediate pulls from the Yahoo API
          // GCMTaskService can only schedule tasks, they cannot execute immediately
        mServiceIntent = new Intent(this, StockIntentService.class);
        if (savedInstanceState == null){
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra("tag", "init");
            startService(mServiceIntent);
        }

        isConnected = Utils.checkNetworkState(mContext);

        mNetworkStatusTextView = (TextView) findViewById(R.id.textview_net_status);
        mStatusTextView = (TextView) findViewById(R.id.textview_status);
        setStatusText();


        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        //TODO:
                        // do something on item click
                    }
                }));
        recyclerView.setAdapter(mCursorAdapter);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                        .content(R.string.content_test)
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {

                                // On FAB click, receive user input. Make sure the stock doesn't already exist
                                // in the DB and proceed accordingly
                                Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                        new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                        new String[]{input.toString()}, null);
                                if (c.getCount() != 0) {
                                    Toast toast =
                                            Toast.makeText(MyStocksActivity.this, R.string.error_stock_symbol_already_listed,
                                                    Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                    toast.show();
                                    return;
                                } else {
                                    // Add the stock to DB
                                    mServiceIntent.putExtra("tag", "add");
                                    mServiceIntent.putExtra("symbol", input.toString());
                                    startService(mServiceIntent);
                                }
                            }
                        })
                        .show();
            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();
        if (isConnected) {
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                .setService(StockTaskService.class)
                .setPeriod(period)
                .setFlex(flex)
                .setTag(periodicTag)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setRequiresCharging(false)
                .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
          getMenuInflater().inflate(R.menu.my_stocks, menu);
          restoreActionBar();
          return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
          return true;
        }

        if (id == R.id.action_change_units){
          // this is for changing stock changes from percent value to dollar value
          Utils.showPercent = !Utils.showPercent;
          this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.v(LOG_TAG, "ON RESUME");
        setStatus();
    }

    public void setStatus() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (sharedPreferences != null) {
            switch (sharedPreferences.getInt(getString(R.string.prefs_network_status), Utils.NETWORK_OK)) {
                case Utils.SERVER_UNAVAILABLE:
                    fab.setVisibility(FloatingActionButton.INVISIBLE);
                    mNetworkStatusTextView.setVisibility(TextView.VISIBLE);
                    mNetworkStatusTextView.setText(getString(R.string.server_unavailable));
                    break;
                case Utils.SERVER_NOT_FOUND:
                    fab.setVisibility(FloatingActionButton.INVISIBLE);
                    mNetworkStatusTextView.setVisibility(TextView.VISIBLE);
                    mNetworkStatusTextView.setText(getString(R.string.no_server));
                    break;
                case Utils.NETWORK_UNAVAILABLE:
                    fab.setVisibility(FloatingActionButton.INVISIBLE);
                    mNetworkStatusTextView.setVisibility(TextView.VISIBLE);
                    mNetworkStatusTextView.setText(getString(R.string.no_network));
                    break;
                case Utils.NETWORK_OK:
                    fab.setVisibility(FloatingActionButton.VISIBLE);
                    mNetworkStatusTextView.setVisibility(TextView.INVISIBLE);
                    break;
                default: break;
            }
            Log.v(LOG_TAG, "STTTTSSS: " + sharedPreferences.getInt(getString(R.string.prefs_network_status), Utils.NETWORK_OK));
        }
    }

    @Override
      public Loader<Cursor> onCreateLoader(int id, Bundle args){
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
            new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
            QuoteColumns.ISCURRENT + " = ?",
            new String[]{"1"},
            null);
      }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data){
      mCursorAdapter.swapCursor(data);
      mCursor = data;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mStockLoadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(mContext.getString(R.string.broadcast_stock_search_message));
            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onLoaderReset(Loader<Cursor> loader){
        mCursorAdapter.swapCursor(null);
      }

    private void setStatusText() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String theClock = "";
        int hour = 0;
        int min = 0;
        String minute = "00";

        if (sharedPreferences != null) {
            hour = sharedPreferences.getInt(getString(R.string.pref_time_hour), 0);
            min = sharedPreferences.getInt(getString(R.string.pref_time_min), 0);
            minute = String.valueOf(min);
            if (min < 10)
                minute = "0" + minute;
        }
        theClock = hour + ":" + minute;

        String lastUpdated = getString(R.string.last_update);
        mStatusTextView.setText(lastUpdated + " " + theClock);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.v(LOG_TAG, "SHARED PREFERENCES CHANGED");
        if (key.equals(getString(R.string.prefs_network_status))) {
            setStatus();
        }
    }
}
