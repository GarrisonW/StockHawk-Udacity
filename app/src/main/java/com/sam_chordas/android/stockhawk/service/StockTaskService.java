package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{

  public static String LOG_TAG = StockTaskService.class.getSimpleName();

    public static final String ACTION_DATA_UPDATED =
            "com.sam_chordas.android.stockhawk.app.ACTION_DATA_UPDATED";

  private OkHttpClient client = new OkHttpClient();
  private Context mContext;
  private StringBuilder mStoredSymbols = new StringBuilder();
  private boolean isUpdate;

  public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
  }

  String fetchData(String url) throws IOException{
      Request request = new Request.Builder()
          .url(url)
          .build();

      if (Utils.checkNetworkState(mContext)) {
          try {
              Response response = client.newCall(request).execute();
              return response.body().string();
          }
          catch (SocketTimeoutException ste) {
              Utils.setNetworkStatus(mContext, Utils.SERVER_UNAVAILABLE);
          }
          catch (UnknownHostException uhe) {
              Utils.setNetworkStatus(mContext, Utils.SERVER_NOT_FOUND);
          }
      }

      return null;
  }

  @Override
  public int onRunTask(TaskParams params) {
    Cursor initQueryCursor;

    if (mContext == null) {
      mContext = this;
    }

    StringBuilder urlStringBuilder = new StringBuilder();
Log.v(LOG_TAG, "ON RUN TASK");
    try {
        if (params.getTag().equals("chart")) {
            // Base URL for the Yahoo stock detal query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol = ", "UTF-8"));
            Log.v(LOG_TAG, "SERVICE SYMB: " + params.getExtras().getString("symbol"));
            urlStringBuilder.append(URLEncoder.encode("\"" + params.getExtras().getString("symbol") + "\"", "UTF-8"));
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            int y = calendar.get(Calendar.YEAR);
            int m = calendar.get(Calendar.MONTH) + 1;
            int d = calendar.get(Calendar.DAY_OF_MONTH);
            String startDate = y + "-" + m + "-" + d;
            calendar.add(Calendar.DAY_OF_YEAR, -7);
            y = calendar.get(Calendar.YEAR);
            m = calendar.get(Calendar.MONTH) + 1;
            d = calendar.get(Calendar.DAY_OF_MONTH);
            String endDate = y + "-" + m + "-" + d;

            urlStringBuilder.append(URLEncoder.encode(" and startDate =\"" + startDate + "\" and endDate = \"" + endDate + "\"", "UTF-8"));
        }
        else {
            // Base URL for the Yahoo stock query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol in (", "UTF-8"));
        }


    }
    catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    if (params.getTag().equals("init") || params.getTag().equals("periodic")) {
      isUpdate = true;
      initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI, new String[] { "Distinct " + QuoteColumns.SYMBOL },
                                                            null, null, null);
      if (initQueryCursor.getCount() == 0 || initQueryCursor == null){
        // Init task. Populates DB with quotes for the symbols seen below
        try {
          urlStringBuilder.append(URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }

      }
      else if (initQueryCursor != null) {
          DatabaseUtils.dumpCursor(initQueryCursor);
          initQueryCursor.moveToFirst();
          for (int i = 0; i < initQueryCursor.getCount(); i++) {
              mStoredSymbols.append("\"" + initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")) + "\",");
              initQueryCursor.moveToNext();
          }

          mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
          try {
              urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
          } catch (UnsupportedEncodingException e) {
              e.printStackTrace();
          }
      }
    }
    else if (params.getTag().equals("add")) {
      isUpdate = false;
      // get symbol from params.getExtra and build query
      String stockInput = params.getExtras().getString("symbol");
      try {
          urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
          setTimeStamp(mContext);
    }
      catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    }

    // finalize the URL for the API query.
    urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=");

    String urlString;
    String getResponse;
    int result = GcmNetworkManager.RESULT_FAILURE;

    if (urlStringBuilder != null){
      urlString = urlStringBuilder.toString();

        Log.v(LOG_TAG, "HERE YOU GO: " + urlStringBuilder.toString());
      try {
        getResponse = fetchData(urlString);
        if (getResponse != null) {
            result = GcmNetworkManager.RESULT_SUCCESS;
            if (params.getTag().equals("chart")) {
                // Obtain array for charting
                Log.v(LOG_TAG, "HERE HERE HERE ERHERER");
                ArrayList<String> chartStockValues = Utils.detailsJsonVals(mContext, getResponse);
                for (int i=0; i < chartStockValues.size(); i++) {
                    Log.v(LOG_TAG, "VALUE AT " + i + ": " + chartStockValues.get(i) );
                }
            }
            else {
                //  Load stock quotes into database
                try {
                    ContentValues contentValues = new ContentValues();
                    // update ISCURRENT to 0 (false) so new data is current
                    if (isUpdate) {
                        contentValues.put(QuoteColumns.ISCURRENT, 0);
                        mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues, null, null);
                    }
                    mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY, Utils.quoteJsonToContentVals(mContext, getResponse));
                    setTimeStamp(mContext);

                } catch (RemoteException | OperationApplicationException e) {
                    Log.e(LOG_TAG, "Error applying batch insert", e);
                }
            }
        }
      } catch (IOException e){
        e.printStackTrace();
      }
    }

    return result;
  }

  private void setTimeStamp(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = sharedPreferences.edit();

    Calendar calendar = Calendar.getInstance();
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    int min = calendar.get(Calendar.MINUTE);

    editor.putInt(context.getString(R.string.pref_time_hour), hour);
    editor.putInt(context.getString(R.string.pref_time_min), min);

    updateWidgets();
    editor.apply();
  }

    private void updateWidgets() {
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED).setPackage(mContext.getPackageName());
        mContext.sendBroadcast(dataUpdatedIntent);
    }

}
