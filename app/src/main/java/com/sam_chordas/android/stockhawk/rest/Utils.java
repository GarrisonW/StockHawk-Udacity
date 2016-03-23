package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();

    private static Context mContext;
    public static boolean showPercent = true;

    public static final int NETWORK_STATUS_OK = 0;
    public static final int NETWORK_STATUS_UNAVAILABLE = 1;
    public static final int NETWORK_STATUS_UNKNOWN = 2;

    public static ArrayList quoteJsonToContentVals(Context context, String JSON) {

        mContext = context;

        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;

        Log.i(LOG_TAG, "GET FB: " +JSON);

        try {
          jsonObject = new JSONObject(JSON);

          if (jsonObject != null && jsonObject.length() != 0) {
            jsonObject = jsonObject.getJSONObject("query");
            int count = Integer.parseInt(jsonObject.getString("count"));
            if (count == 1) {
              jsonObject = jsonObject.getJSONObject("results").getJSONObject("quote");
              if (jsonObject.getString("Ask") == "null") {
                Log.v(LOG_TAG, "SYMBOL NOT FOUND");
                sendBroadcastMessage(mContext.getString(R.string.error_stock_symbol_not_found));
              }
              else
                batchOperations.add(buildBatchOperation(jsonObject));
            }
            else {
              resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");
              if (resultsArray != null && resultsArray.length() != 0) {
                for (int i = 0; i < resultsArray.length(); i++) {
                  jsonObject = resultsArray.getJSONObject(i);
                  batchOperations.add(buildBatchOperation(jsonObject));
                }
              }
            }
          }
        } catch (JSONException e){
          Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0,1);
        String ampersand = "";
        if (isPercentChange){
          ampersand = change.substring(change.length() - 1, change.length());
          change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
          String change = jsonObject.getString("Change");
          builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
          builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
          builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(jsonObject.getString("ChangeinPercent"), true));
          builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
          builder.withValue(QuoteColumns.ISCURRENT, 1);
          if (change.charAt(0) == '-') {
            builder.withValue(QuoteColumns.ISUP, 0);
          } else {
            builder.withValue(QuoteColumns.ISUP, 1);
          }
        }
        catch (JSONException e){
          e.printStackTrace();
        }
        return builder.build();
    }

    private static void sendBroadcastMessage(String message) {
            Intent intent = new Intent(mContext.getString(R.string.broadcast_stock_search));
            // You can also include some extra data.
            intent.putExtra(mContext.getString(R.string.broadcast_stock_search_message), message);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    public static boolean checkNetworkState(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            setNetworkStatus(context, NETWORK_STATUS_OK);
            return true;
        }
        else {
            setNetworkStatus(context, NETWORK_STATUS_UNAVAILABLE);
            return false;
        }
    }
    private static void setNetworkStatus(Context context, int status) {

        Log.v(LOG_TAG, "SETTING STATUS");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(context.getString(R.string.prefs_network_status), status);
        editor.apply();
    }
}
