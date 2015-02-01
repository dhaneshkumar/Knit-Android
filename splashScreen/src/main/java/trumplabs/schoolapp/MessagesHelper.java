package trumplabs.schoolapp;

import android.os.AsyncTask;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.HashMap;

import utility.Utility;

/**
 * Created by Dhanesh on 12/31/2014.
 */
public class MessagesHelper {


    String printHello(String str)
    {
        return null;
    }

    /**
     * @action increment like counter on server using background process
     * @how fetch current msg obj from server, updates its counter and then save back it globally and locally
     */

    static class IncreaseLikeCount extends AsyncTask<Void, Void, Void> {
        private ParseObject object;

        IncreaseLikeCount(ParseObject object) {
            this.object = object;
        }

        @Override
        protected Void doInBackground(Void... params) {

            if (object == null)
                return null;

            HashMap<String, Object> pars = new HashMap<String, Object>();
            pars.put("objectId", object.getObjectId());

            //calling parse cloud function "likeCountIncrement"
            ParseCloud.callFunctionInBackground("likeCountIncrement", pars, new FunctionCallback<Integer>() {
                public void done(Integer likesCount, ParseException e) {
                    if (e == null) {
                        if(likesCount > 0) {
                            object.put(Constants.LIKE, likesCount);
                            try {
                                object.pin();

                                Utility.ls(object.getInt(Constants.LIKE ) + " : like count");
                            } catch (ParseException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                    else
                    {
                        Utility.ls("error likes count ");
                        e.printStackTrace();
                    }
                }
            });

            return null;
        }
    }


    /**
     * @action decrement like counter on server using background process
     * @how fetch current msg obj from server, updates its counter and then save back it globally and locally
     */

    static class DecreaseLikeCount extends AsyncTask<Void, Void, Void> {
        String objectId;

        DecreaseLikeCount(String objectId) {
            this.objectId = objectId;
        }

        @Override
        protected Void doInBackground(Void... params) {

            if (objectId == null)
                return null;

            ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.GROUP_DETAILS);
            query.whereEqualTo("objectId", objectId);
            ParseObject obj = null;
            try {
                obj = query.getFirst();

                if (obj != null) {
                    int likeCount = obj.getInt(Constants.LIKE_COUNT);

                    if (likeCount <= 0)
                        return null;

                    --likeCount;

                    obj.put(Constants.LIKE_COUNT, likeCount);



                    obj.save();
                    obj.pin();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            return null;
        }
    }


    /**
     * @action increment confused counter on server uisng background process
     * @how fetch current msg obj from server, updates its counter and then save back it globally and locally
     */

    static class IncreaseCounfusedCount extends AsyncTask<Void, Void, Void> {
        String objectId;

        IncreaseCounfusedCount(String objectId) {
            this.objectId = objectId;
        }

        @Override
        protected Void doInBackground(Void... params) {

            if (objectId == null)
                return null;

            ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.GROUP_DETAILS);
            query.whereEqualTo("objectId", objectId);

            ParseObject obj = null;
            try {
                obj = query.getFirst();

                if (obj != null) {
                    int confusedCount = obj.getInt(Constants.CONFUSED_COUNT);


                    Utility.ls("Intial count  : " + confusedCount);

                    ++confusedCount;

                    Utility.ls("final count  : " + confusedCount);

                    if(obj.getString(Constants.LIKE) != null)
                        Utility.ls("++++++++++++++++++++++++++++++++++++++++++++++=");
                        Utility.ls(objectId +  " :  " +  obj.getString(Constants.LIKE));

                    obj.put(Constants.CONFUSED_COUNT, confusedCount);
                    obj.save();
                    Utility.ls(objectId +  " :  " +  obj.getString(Constants.LIKE));
                    obj.pin();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    /**
     * @action decrement confused counter on server using background process
     * @how fetch current msg obj from server, updates its counter and then save back it globally and locally
     */
    static class DecreaseConfusedCount extends AsyncTask<Void, Void, Void> {
        String objectId;

        DecreaseConfusedCount(String objectId) {
            this.objectId = objectId;
        }

        @Override
        protected Void doInBackground(Void... params) {

            if (objectId == null)
                return null;

            ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.GROUP_DETAILS);
            query.whereEqualTo("objectId", objectId);
            ParseObject obj = null;
            try {
                obj = query.getFirst();

                if (obj != null) {
                    int confusedCount = obj.getInt(Constants.CONFUSED_COUNT);

                    if (confusedCount <= 0)
                        return null;

                    --confusedCount;

                    obj.put(Constants.CONFUSED_COUNT, confusedCount);
                    obj.save();
                    obj.pin();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
