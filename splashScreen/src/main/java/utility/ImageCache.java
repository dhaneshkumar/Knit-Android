package utility;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by ashish on 28/7/15.
 */
public class ImageCache {
    static final String LOGTAG = "__IC";
    //global cache
    private static LruCache<String, Bitmap> mMemoryCache;

    public static void initialize(){
        if(Config.SHOWLOG) Log.d(LOGTAG, "initializing");

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
            /*
             * The cache size will be measured in kilobytes rather than number of items.
             */
                return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
            }
        };
    }

    private static void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (key != null && bitmap != null) {
            if (getBitmapFromMemCache(key) == null) {
                mMemoryCache.put(key, bitmap);
            }
        }
    }

    private static Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    /*
        Call from UI-thread only as it directly modifies GUI
        how ?
            - check if already in cache
            - if file exists but not thumbnail, create thumbnail, add to cache
            - if file and thumbnail both exists, add to cache
            - if file not present, download file, create thumbnail, add to cache
     */
    public static void loadBitmap(final String imageName, final ImageView mImageView, final Activity currentActivity,
                                  final ProgressBar downloadProgressBar, ParseFile imageParseFile) {
        if(mMemoryCache == null){
            initialize();
        }

        if(mMemoryCache == null){//this won't happen
            return;
        }

        //setup
        final File imgFile = new File(Utility.getWorkingAppDir() + "/media/" + imageName);
        final File thumbnailFile = new File(Utility.getWorkingAppDir() + "/thumbnail/" + imageName);
        mImageView.setTag(imgFile.getAbsolutePath()); //tag is original file path
        mImageView.setImageBitmap(null); //initialize it with this

        final Bitmap bitmap = getBitmapFromMemCache(imageName); //key is just file name(not path)
        if (bitmap != null) {
            if(Config.SHOWLOG) Log.d(LOGTAG, "cached image thumbnail : " + imageName);
            mImageView.setImageBitmap(bitmap);
            return;
        }

        //if imgFile exists but not thumbnail
        if (imgFile.exists()) {
            if(!thumbnailFile.exists()) {
                if(Config.SHOWLOG) Log.d(LOGTAG, "creating thumbnail : " + imageName);
                Utility.createThumbnail(currentActivity, imageName);
            }

            if(Config.SHOWLOG) Log.d(LOGTAG, "loading from disk : " + imageName);
            //now thumbnail present
            Bitmap myBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
            mImageView.setImageBitmap(myBitmap);

            addBitmapToMemoryCache(imageName, myBitmap); //key is just file name(not path)
        }
        else if(imageParseFile != null){
            if(Config.SHOWLOG) Log.d(LOGTAG, "downloading data : " + imageName);
            downloadProgressBar.setVisibility(View.VISIBLE);
            imageParseFile.getDataInBackground(new GetDataCallback() {
                @Override
                public void done(byte[] data, ParseException e) {
                    if (e == null) {
                        //Image download successful
                        FileOutputStream fos;
                        try {
                            //store image
                            fos = new FileOutputStream(imgFile.getAbsoluteFile());
                            try {
                                fos.write(data);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            } finally {
                                try {
                                    fos.close();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        } catch (FileNotFoundException e2) {
                            e2.printStackTrace();
                        }

                        if(Config.SHOWLOG) Log.d(LOGTAG, "download over : " + imageName);

                        Utility.createThumbnail(currentActivity, imageName);
                        //now thumbnail present
                        Bitmap myBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());

                        if(mImageView != null) {
                            mImageView.setImageBitmap(myBitmap);
                        }

                        addBitmapToMemoryCache(imageName, myBitmap); //key is just file name(not path)

                        if(downloadProgressBar != null){
                            downloadProgressBar.setVisibility(View.GONE);
                        }
                    } else {
                        // Image not downloaded
                        Utility.LogoutUtility.checkAndHandleInvalidSession(e);
                        if(downloadProgressBar != null){
                            downloadProgressBar.setVisibility(View.GONE);
                        }
                    }
                }
            });
        }
    }
}