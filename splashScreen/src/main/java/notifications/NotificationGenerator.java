package notifications;

import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import utility.PushOpen;

/**
 * Created by ashish on 19/1/15.
 */
public class NotificationGenerator {
    public static String[] events = new String[10];
    public static String[] groupNames = new String[10];

    public static List<NotificationEntity> normalNotificationList = new ArrayList<NotificationEntity>();
    public static NotificationEntity transitionNotification = null;
    public static NotificationEntity updateNotifcation = null;
    public static NotificationEntity linkNotification = null;

    private static int NOTIFICATION_ID = 0; //

    //ids for the 4 notification types
    private static int NORMAL_NOTIFICATION_ID = 0;
    private static int TRANSITION_NOTIFICATION_ID = 1;
    private static int UPDATE_NOTIFICATION_ID = 2;
    private static int LINK_NOTIFICATION_ID = 3;

    public static int count = 0;
    private NotificationManager notificationManager;

    //without extras
    public static void generateNotification(Context context, String contentText,String groupName,
                                            String type, String action){
        generateNotification(context, contentText, groupName, type, action, null);
    }

    //with extra
    public static void generateNotification(Context context, String contentText,String groupName,
                                            String type, String action, Bundle extras) {

        NotificationEntity notEntity = new NotificationEntity(contentText, groupName, type, action);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        context.getApplicationContext().registerReceiver(receiver, new IntentFilter(NOTIFICATION_DELETED_ACTION));


        NotificationCompat.Builder  mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setAutoCancel(true);

        mBuilder.setSmallIcon(R.drawable.notification);
        mBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);

        //set content intent
        Intent clickIntent = new Intent(context, PushOpen.class);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        clickIntent.putExtra("type", notEntity.type);
        clickIntent.putExtra("action", notEntity.action);
        clickIntent.putExtra("notificationId", notEntity.notificationId);

        Log.d("DEBUG_NOTIFICATION_GENERATOR", "type " + notEntity.type + " " + notEntity.action);
        PendingIntent clickPendingIntent = PendingIntent.getActivity( context, 0, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Note the use of PendingIntent.FLAG_CANCEL_CURRENT. This is important so to overwrite the extras in intent.
        // Otherwise it reuses the old intent because requestCode is same = 0

        mBuilder.setContentIntent(clickPendingIntent);

        //set delete intent
        Intent deleteIntent = new Intent(NOTIFICATION_DELETED_ACTION);
        deleteIntent.putExtra("type", notEntity.type);
        deleteIntent.putExtra("notificationId", notEntity.notificationId);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        mBuilder.setDeleteIntent(deletePendingIntent);

        mBuilder.setContentTitle(notEntity.groupName);
        mBuilder.setContentText(notEntity.contentText);

        if(notEntity.type.equals(Constants.NORMAL_NOTIFICATION)){
            Log.d("DEBUG_NOTIFICATION_GENERATOR", "normal notification");
            normalNotificationList.add(notEntity);

            if(normalNotificationList.size() == 1){
                //set title, content
                NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
                bigTextStyle.setBigContentTitle(notEntity.groupName);
                bigTextStyle.bigText(notEntity.contentText);

                mBuilder.setStyle(bigTextStyle);

                //add actions
                mBuilder.addAction(R.drawable.seen, "DISMISS", deletePendingIntent);
                mBuilder.addAction(R.drawable.fwd, "INBOX", clickPendingIntent);
            }
            else{
                NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                inboxStyle.setBigContentTitle("Knit");
                for(int i=0; i<normalNotificationList.size(); i++){
                    NotificationEntity entity = normalNotificationList.get(i);
                    inboxStyle.addLine(entity.groupName + " " + entity.contentText);
                }
                mBuilder.setStyle(inboxStyle);
            }

            notificationManager.notify(NORMAL_NOTIFICATION_ID, mBuilder.build());
        }
        else if(notEntity.type.equals(Constants.TRANSITION_NOTIFICATION)){
            Log.d("DEBUG_NOTIFICATION_GENERATOR", "trans notification");
            transitionNotification = notEntity;

            //set title, content
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle(notEntity.groupName);
            bigTextStyle.bigText(notEntity.contentText);

            mBuilder.setStyle(bigTextStyle);

            //add actions
            mBuilder.addAction(R.drawable.seen, "DISMISS", deletePendingIntent);
            if(notEntity.action.equals(Constants.INVITE_TEACHER_ACTION)){
                mBuilder.addAction(R.drawable.fwd, "INVITE", clickPendingIntent);
            }
            else if(notEntity.action.equals(Constants.CLASSROOMS_ACTION)){
                mBuilder.addAction(R.drawable.fwd, "OPEN", clickPendingIntent);
            }
            else if(notEntity.action.equals(Constants.OUTBOX_ACTION)){
                mBuilder.addAction(R.drawable.fwd, "OPEN", clickPendingIntent);
            }
            else if(notEntity.action.equals(Constants.INVITE_PARENT_ACTION)){
                Log.d("DEBUG_NOTIFICATION_GEN", "invite parent action");
                if(extras != null){
                    clickIntent.putExtra("grpCode", extras.getString("grpCode"));
                    clickIntent.putExtra("grpName", extras.getString("grpName"));
                    PendingIntent overrideClickPendingIntent = PendingIntent.getActivity( context, 0, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    mBuilder.setContentIntent(overrideClickPendingIntent);
                    mBuilder.addAction(R.drawable.fwd, "INVITE", overrideClickPendingIntent);
                }
            }
            else if(notEntity.action.equals(Constants.CREATE_CLASS_ACTION)){
                mBuilder.addAction(R.drawable.fwd, "CREATE NEW", clickPendingIntent);
            }

            notificationManager.notify(TRANSITION_NOTIFICATION_ID, mBuilder.build());
        }
        else if(notEntity.type.equals(Constants.UPDATE_NOTIFICATION)){
            Log.d("DEBUG_NOTIFICATION_GENERATOR", "update notification");
            updateNotifcation = notEntity;

            //set title, content
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle(notEntity.groupName);
            bigTextStyle.bigText(notEntity.contentText);

            mBuilder.setStyle(bigTextStyle);

            //add actions
            mBuilder.addAction(R.drawable.seen, "DISMISS", deletePendingIntent);
            mBuilder.addAction(R.drawable.update, "UPDATE NOW", clickPendingIntent);

            notificationManager.notify(UPDATE_NOTIFICATION_ID, mBuilder.build());
        }
        else if(notEntity.type.equals(Constants.LINK_NOTIFICATION)){
            Log.d("DEBUG_NOTIFICATION_GENERATOR", "link notification");
            transitionNotification = notEntity;

            //set title, content
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle(notEntity.groupName);
            bigTextStyle.bigText(notEntity.contentText);

            mBuilder.setStyle(bigTextStyle);

            //add actions
            mBuilder.addAction(R.drawable.seen, "DISMISS", deletePendingIntent);
            mBuilder.addAction(R.drawable.update, "VISIT", clickPendingIntent);

            notificationManager.notify(LINK_NOTIFICATION_ID, mBuilder.build());
        }
    }

    private static final String NOTIFICATION_DELETED_ACTION = "NOTIFICATION_DELETED";

    private static final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            int notificationId = intent.getIntExtra("notificationId", 100);
            if(notificationId == 100)
                notificationManager.cancelAll();
            else
                notificationManager.cancel(notificationId);

            String type = intent.getStringExtra("type");
            Log.d("DEBUG_NOTIFICATION_GENERATOR", "got " + NOTIFICATION_DELETED_ACTION + " for type " + type + "notid " + notificationId);

            if(type.equals(Constants.NORMAL_NOTIFICATION)){
                Log.d("DEBUG_NOTIFICATION_GENERATOR", "clearing normal notification list");
                normalNotificationList.clear();
            }
            //only clearing normalNotificationList is important
            //rest types need not be handled since they are singular entities and will be overwritten
            //when new notification of that type arrives
        }
    };


    public static class NotificationEntity{
        String contentText;
        String groupName;
        String type;
        String action;
        int notificationId;

        NotificationEntity(String tempContentText, String tempGroupName, String tempType, String tempAction){
            contentText = tempContentText;
            groupName = tempGroupName;
            action = tempAction;
            type = tempType;


            if(type == null || action == null ){
                type = Constants.NORMAL_NOTIFICATION;
                action = Constants.INBOX_ACTION;
                notificationId = NORMAL_NOTIFICATION_ID;
            }
            else if(type.equals(Constants.TRANSITION_NOTIFICATION)){
                notificationId = TRANSITION_NOTIFICATION_ID;
                //do nothing
            }
            else if(type.equals(Constants.UPDATE_NOTIFICATION)){
                notificationId = UPDATE_NOTIFICATION_ID;
                //do nothing
            }
            else if(type.equals(Constants.LINK_NOTIFICATION)){
                notificationId = LINK_NOTIFICATION_ID;
                //do nothing
            }
            else{
                type = Constants.NORMAL_NOTIFICATION;
                action = Constants.INBOX_ACTION;
                notificationId = NORMAL_NOTIFICATION_ID;
            }
        }
    }
}