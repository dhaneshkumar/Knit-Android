package school;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import trumplab.textslate.R;

/**
 * Created by ashish on 10/9/15.
 */
public class CustomSearchView extends SearchView{
    static final String LOGTAG = "__sv_custom";
    ListView myLVRef;
    SearchViewAdapterInterface mySVAdapterRef;
    boolean itemJustSelected = false;

    long delayMillis = 750L; //delay before search query starts
    int searchThreshold = 2; //min #chars needed to start search

    MyHandler svHandler;

    AdapterView.OnItemClickListener listItemOnClickListener;
    Runnable queryTextChangeRunnable;

    public CustomSearchView(Context context) {
        super(context);
    }

    public CustomSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        int id = getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        TextView textView = (TextView) findViewById(id);
        if(textView != null) {
            textView.setTextColor(context.getResources().getColor(R.color.com_facebook_blue));
            //textView.setHintTextColor(context.getResources().getColor(R.color.googleyellow));
        }
    }

    public void setParameters(Long dMillis, int sThreshold, ListView mLVRef,  SearchViewAdapterInterface mSVAdapterRef){
        this.delayMillis = dMillis;
        this.searchThreshold = sThreshold;
        this.myLVRef = mLVRef;
        this.mySVAdapterRef = mSVAdapterRef;


        setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if(itemJustSelected){
                    itemJustSelected = false; //reset as a new potential query
                    return true; //this is because when click selected item, it again triggers onQueryTextChange with full item description, we don't want that
                }

                itemJustSelected = false; //reset as a new potential query

                if(queryTextChangeRunnable != null){
                    queryTextChangeRunnable.run();
                }

                svHandler.removeMessages(0);
                //Log.d(LOGTAG, "onQueryTextChange with=" + s);
                if (s.length() < searchThreshold) {
                    myLVRef.setVisibility(View.GONE);
                    //Log.d(LOGTAG, "onQueryTextChange ignore short");
                    return true;
                }

                svHandler.sendMessageDelayed(svHandler.obtainMessage(0, 0, 0, s), delayMillis);
                return true;
            }
        });

        /*setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOGTAG, "onClick");
                myLVRef.setVisibility(VISIBLE);
            }
        });*/

        myLVRef.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                itemJustSelected = true; //so that we don't show the list unnecessary if any pending query completes after this
                //Log.d(LOGTAG, "sleeping set to " + itemJustSelected);
                String sel = mySVAdapterRef.getStringDescription(position);
                setQuery(sel, false);
                myLVRef.setVisibility(View.GONE);
                svHandler.removeMessages(0); //remove any pending query requests
                Log.d(LOGTAG, "on item click");

                if (listItemOnClickListener != null) {
                    listItemOnClickListener.onItemClick(adapterView, view, position, l);
                }
            }
        });
    }

    public boolean getItemJustSelected(){
        return itemJustSelected;
    }

    void setListItemOnClickListener(AdapterView.OnItemClickListener listItemOnClickListener){
        this.listItemOnClickListener = listItemOnClickListener;
    }

    void setQueryTextChangeRunnable(Runnable queryTextChangeRunnable){
        this.queryTextChangeRunnable = queryTextChangeRunnable;
    }

    void setHandler(WeakReference<SearchViewAdapterInterface> adapterRef, WeakReference<ProgressBar> progressBarRef) {
        svHandler = new MyHandler(adapterRef, progressBarRef);
    }

    class MyHandler extends Handler {
        WeakReference<SearchViewAdapterInterface> adapterRef;
        WeakReference<ProgressBar> progressBarRef;

        MyHandler(WeakReference<SearchViewAdapterInterface> adapterRef, WeakReference<ProgressBar> progressBarRef){
            this.adapterRef = adapterRef;
            this.progressBarRef = progressBarRef;
        }

        @Override
        public void handleMessage(Message msg) {
            boolean filterRequestStarted = false;
            if(adapterRef != null && adapterRef.get() != null){
                Log.d(LOGTAG, "handler called to filter with " + msg.obj);
                adapterRef.get().getFilter().filter((String) msg.obj);
                filterRequestStarted = true;
            }

            if(filterRequestStarted && progressBarRef != null && progressBarRef.get() != null){
                progressBarRef.get().setVisibility(View.VISIBLE); //show progress bar(general)
            }
        }
    }
}