package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * Created by Garrison on 4/1/2016.
 */
public class QuoteWidgetRemoteViewsService extends RemoteViewsService {

    public final String LOG_TAG = QuoteWidgetRemoteViewsService.class.getSimpleName();

    private static final String[] QUOTE_COLUMNS = {
            QuoteColumns._ID,
            QuoteColumns.SYMBOL,
            QuoteColumns.BIDPRICE,
            QuoteColumns.PERCENT_CHANGE,
            QuoteColumns.CHANGE,
    };

    // these indices must match the projection
    static final int INDEX_ID = 0;
    static final int INDEX_SYMBOL = 1;
    static final int INDEX_BIDPRICE = 2;
    static final int INDEX_PERCENT_CHANGE = 3;
    static final int INDEX_CHANGE = 4;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {

        Log.v(LOG_TAG, "FEMOVTE VIEWS FACTORY");

        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                Log.v(LOG_TAG, "VIEWS FACTORY CREATE");
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }

                Log.v(LOG_TAG, "SATASET CHANGED");
                final long identityToken = Binder.clearCallingIdentity();

                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        QUOTE_COLUMNS,
                        null,
                        null,
                        null);

                if (data != null)
                    Log.v(LOG_TAG, "NO NO NO NULL");
                else
                    Log.v(LOG_TAG, "NULL NULL");

                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION || data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_collection_item);
 Log.v(LOG_TAG, "SETTING VIEW DATA: " + getPackageName() + "  " +  data.getString(INDEX_SYMBOL));
                views.setTextViewText(R.id.widget_stock_symbol, data.getString(INDEX_SYMBOL));
                views.setTextViewText(R.id.widget_bid_price, data.getString(INDEX_BIDPRICE));
                views.setTextViewText(R.id.widget_change, data.getString(INDEX_CHANGE));

                //final Intent fillInIntent = new Intent();
                //fillInIntent.setData(QuoteProvider.Quotes.CONTENT_URI);
                //views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_collection_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(INDEX_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }

}
