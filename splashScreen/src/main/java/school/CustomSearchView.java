package school;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;

import java.lang.ref.WeakReference;

/**
 * Created by ashish on 10/9/15.
 */
public class CustomSearchView extends SearchView{
    static final String LOGTAG = "__sv_custom";
    ListView myLVRef;
    SearchViewAdapterInterface mySVAdapterRef;

    long delayMillis = 750L; //delay before search query starts
    int searchThreshold = 2; //min #chars needed to start search

    MyHandler svHandler;
    AdapterView.OnItemClickListener listItemOnClickListener;

    public CustomSearchView(Context context) {
        super(context);
    }

    public CustomSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
                svHandler.removeMessages(0);
                Log.d(LOGTAG, "onQueryTextChange with=" + s);
                if (s.length() < searchThreshold) {
                    myLVRef.setVisibility(View.GONE);
                    Log.d(LOGTAG, "onQueryTextChange ignore short");
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
                String sel = mySVAdapterRef.getStringDescription(position);
                setQuery(sel, false);
                myLVRef.setVisibility(View.GONE);
                svHandler.removeMessages(0);
                Log.d(LOGTAG, "on item click");

                if (listItemOnClickListener != null) {
                    listItemOnClickListener.onItemClick(adapterView, view, position, l);
                }
            }
        });
    }

    void setListItemOnClickListener(AdapterView.OnItemClickListener listItemOnClickListener){
        this.listItemOnClickListener = listItemOnClickListener;
    }

    void setHandler(WeakReference<SearchViewAdapterInterface> adapterRef) {
        svHandler = new MyHandler(adapterRef);
    }

    class MyHandler extends Handler {
        WeakReference<SearchViewAdapterInterface> adapterRef;

        MyHandler(WeakReference<SearchViewAdapterInterface> adapterRef){
            this.adapterRef = adapterRef;
        }

        @Override
        public void handleMessage(Message msg) {
            if(adapterRef.get() != null){
                adapterRef.get().getFilter().filter((String) msg.obj);
            }
        }
    }
}