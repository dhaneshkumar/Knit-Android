package utility;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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
    public static final String LOGTAG = "__IC";
    //global cache
    private static LruCache<String, Bitmap> mMemoryCache;

    public static void initialize(){
        if(mMemoryCache != null){
            //if(Config.SHOWLOG) Log.d(LOGTAG, "already initialized");
            return;
        }

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        // cache thumbnails(size ~20 KB) - i.e 50 thumbnails per MB

        final int cacheSize = maxMemory / 8;
        if(Config.SHOWLOG) Log.d(LOGTAG, "initializing to cache size of " + cacheSize + " KB");

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
            if(Config.SHOWLOG) Log.d(LOGTAG, "(i) adding to cache key=" + key +
                    ", rows=" + bitmap.getRowBytes() +
                    ", h=" + bitmap.getHeight() +
                    ", size=" + bitmap.getRowBytes()*bitmap.getHeight()/1024);

            if (getBitmapFromMemCache(key) == null) {
                mMemoryCache.put(key, bitmap);
            }
        }
    }

    private static Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    //call fron UI-thread
    public static boolean showIfInCache(String imageName, ImageView mImageView){
        initialize();

        Bitmap bitmap = getBitmapFromMemCache(imageName);
        if(bitmap != null){
            mImageView.setImageBitmap(bitmap);
            return true;
        }
        return false;
    }

    /*
        Required :
            * imageName file must be present in 'media' folder
            Or
            * data non-null(so that we can create the file)

        How:
            * Writes data to file(if non-null)
            * Creates thumbnail (if not already present)
            * Creates the bitmap, add to cache.
            * onPostExecute() : run uiWork and also set the bitmap to mImageView
     */
    public static class WriteLoadAndShowTask extends AsyncTask<Void, Void, Void>{
        byte[] data;
        String imageName;
        ImageView mImageView;
        Activity attachedActivity;
        Runnable uiWork;

        Bitmap bitmap;

        public WriteLoadAndShowTask(byte[] data, String imageName, ImageView mImageView, Activity activity, Runnable uiWork){
            this.data = data;
            this.imageName = imageName;
            this.mImageView = mImageView;
            this.attachedActivity = activity;
            this.uiWork = uiWork;

            this.bitmap = null;
        }

        @Override
        protected Void doInBackground(Void... params) {
            initialize();

            bitmap = getBitmapFromMemCache(imageName); //key imageName
            if(bitmap == null){
                String imagePath = Utility.getWorkingAppDir() + "/media/" + imageName;

                //first write to file if data not null
                if(data != null){
                    writeToDisk(data, imagePath);
                }

                //Here imgFile should exist (either already exists or data written to the file above)
                final File imgFile = new File(imagePath);
                if(!imgFile.exists()){//shouldn't happen
                    return null;
                }

                String thumbnailPath = Utility.getWorkingAppDir() + "/thumbnail/" + imageName;
                final File thumbnailFile = new File(thumbnailPath);
                if(!thumbnailFile.exists()) {
                    Utility.createThumbnail(attachedActivity, imageName);
                }

                //Here thumbnailFile should exist (either already exists or created above)
                if(Config.SHOWLOG) Log.d(LOGTAG, "(i) loading from disk : " + imageName);

                //dummy time taking
                //TestingUtililty.sleep(1000);

                bitmap = BitmapFactory.decodeFile(thumbnailPath);
                addBitmapToMemoryCache(imageName, bitmap); //key imageName
            }
            else{
                //if(Config.SHOWLOG) Log.d(LOGTAG, "(i) cached image thumbnail : " + imageName);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if(mImageView != null){
                mImageView.setImageBitmap(bitmap);
            }
            uiWork.run();
        }
    }

    /*
        Writes data to specified file.
        Returns true on success, false otherwise
     */
    static boolean writeToDisk(byte[] data, String filePath){
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(filePath);
            try {
                fos.write(data);
                return true;
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

        return false; //some error
    }
}