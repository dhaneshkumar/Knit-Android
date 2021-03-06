package notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import utility.Config;
import utility.PushOpen;
import utility.SessionManager;

/**
 * Created by ashish on 19/1/15.
 */
public class NotificationGenerator {
    private static final String LOGTAG = "DEBUG_NOTIFICATION_GEN";
    public static List<NotificationEntity> normalNotificationList = new ArrayList<>();

    //ids for the notification types
    private static int NORMAL_NOTIFICATION_ID = 0; //since multiple of these will be merged and shown as one big notification
    //For others, unique notification id generated at runtime using SessionManager.getNextNotificationId()

    //without extras
    public static void generateNotification(Context context, String contentText,String groupName,
                                            String type, String action){
        generateNotification(context, contentText, groupName, type, action, null);
    }

    //with extras
    public static void generateNotification(Context context, String contentText,String groupName,
                                            String type, String action, Bundle extras) {

        if(type == null || action==null || groupName==null || contentText==null){
            if(Config.SHOWLOG) Log.d(LOGTAG, "Ignoring Notification : some parameters null");
            return; //we don't cater to notifications without type or action
        }

        NotificationEntity notEntity = new NotificationEntity(context, contentText, groupName, type, action);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        context.getApplicationContext().registerReceiver(receiver, new IntentFilter(NOTIFICATION_DELETED_ACTION));


        NotificationCompat.Builder  mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setAutoCancel(true);

        if(Build.VERSION.SDK_INT <21)
            mBuilder.setSmallIcon(R.drawable.notification);
        else {
            mBuilder.setSmallIcon(R.drawable.notification_lollipop);
            mBuilder.setColor(context.getResources().getColor(R.color.color_secondary) );
        }

        mBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);

        //set content intent
        Intent clickIntent = new Intent(context, PushOpen.class);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        clickIntent.putExtra("type", notEntity.type);
        clickIntent.putExtra("action", notEntity.action);
        clickIntent.putExtra("notificationId", notEntity.notificationId);

        if(Config.SHOWLOG) Log.d(LOGTAG, "type=" + notEntity.type + ", action=" + notEntity.action + ", id=" + notEntity.notificationId);
        PendingIntent clickPendingIntent = PendingIntent.getActivity( context, notEntity.notificationId, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Note the use of PendingIntent.FLAG_CANCEL_CURRENT. This is important so to overwrite the extras in intent.
        // Otherwise it reuses the old intent because requestCode was same = 0

        mBuilder.setContentIntent(clickPendingIntent);

        //set delete intent
        Intent deleteIntent = new Intent(NOTIFICATION_DELETED_ACTION);
        deleteIntent.putExtra("type", notEntity.type);
        deleteIntent.putExtra("notificationId", notEntity.notificationId);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, notEntity.notificationId, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        mBuilder.setDeleteIntent(deletePendingIntent);

        //set title and content in unexpanded form
        mBuilder.setContentTitle(notEntity.groupName);
        mBuilder.setContentText(notEntity.contentText);

        if(notEntity.type.equals(Constants.Notifications.NORMAL_NOTIFICATION)){
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
                String text = "";
                for(int i=0; i<normalNotificationList.size(); i++){
                    NotificationEntity entity = normalNotificationList.get(i);
                    inboxStyle.addLine(entity.groupName + " : " + entity.contentText);
                    text += entity.groupName + " : " + entity.contentText + "\n";
                }

                //override title and content in unexpanded form
                mBuilder.setContentTitle("Knit");
                mBuilder.setContentText(text);

                mBuilder.setStyle(inboxStyle);
            }

            notificationManager.notify(notEntity.notificationId, mBuilder.build());
        }
        else if(notEntity.type.equals(Constants.Notifications.TRANSITION_NOTIFICATION)){

            boolean show = true; //if notification action is one of the known types - only then show
            //set title, content
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle(notEntity.groupName);
            bigTextStyle.bigText(notEntity.contentText);

            mBuilder.setStyle(bigTextStyle);

            //add actions
            mBuilder.addAction(R.drawable.seen, "DISMISS", deletePendingIntent);
            if(notEntity.action.equals(Constants.Actions.INVITE_TEACHER_ACTION)){
                mBuilder.addAction(R.drawable.fwd, "INVITE", clickPendingIntent);
            }
            else if(notEntity.action.equals(Constants.Actions.CLASSROOMS_ACTION)){
                mBuilder.addAction(R.drawable.fwd, "OPEN", clickPendingIntent);
            }
            else if(notEntity.action.equals(Constants.Actions.OUTBOX_ACTION)){
                mBuilder.addAction(R.drawable.fwd, "OUTBOX", clickPendingIntent);
            }
            else if(notEntity.action.equals(Constants.Actions.INVITE_PARENT_ACTION)){
                if(Config.SHOWLOG) Log.d(LOGTAG, "special invite parent action(locally gen)");
                if(extras != null){
                    clickIntent.putExtra("classCode", extras.getString("grpCode"));
                    clickIntent.putExtra("className", extras.getString("grpName"));
                    PendingIntent overrideClickPendingIntent = PendingIntent.getActivity( context, notEntity.notificationId, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    mBuilder.setContentIntent(overrideClickPendingIntent);
                    mBuilder.addAction(R.drawable.fwd, "INVITE", overrideClickPendingIntent);
                }
            }
            else if(notEntity.action.equals(Constants.Actions.SEND_MESSAGE_ACTION)){
                if(Config.SHOWLOG) Log.d(LOGTAG, "special send message action(locally gen)");
                if(extras != null){
                    clickIntent.putExtra("classCode", extras.getString("grpCode"));
                    clickIntent.putExtra("className", extras.getString("grpName"));
                    PendingIntent overrideClickPendingIntent = PendingIntent.getActivity( context, notEntity.notificationId, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    mBuilder.setContentIntent(overrideClickPendingIntent);
                    mBuilder.addAction(R.drawable.fwd, "SEND", overrideClickPendingIntent);
                }
            }
            else if(notEntity.action.equals(Constants.Actions.CREATE_CLASS_ACTION)){
                mBuilder.addAction(R.drawable.fwd, "CREATE", clickPendingIntent);
            }
            else if(notEntity.action.equals(Constants.Actions.LIKE_ACTION) || notEntity.action.equals(Constants.Actions.CONFUSE_ACTION)){
                if(Config.SHOWLOG) Log.d(LOGTAG, "special action=" + notEntity.action);
                if(extras != null){
                    clickIntent.putExtra("id", extras.getString("id"));
                    PendingIntent overrideClickPendingIntent = PendingIntent.getActivity(context, notEntity.notificationId, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    mBuilder.setContentIntent(overrideClickPendingIntent);
                    mBuilder.addAction(R.drawable.fwd, "VIEW", overrideClickPendingIntent);
                }
            }
            else if(notEntity.action.equals(Constants.Actions.MEMBER_ACTION)){
                if(Config.SHOWLOG) Log.d(LOGTAG, "special action=" + notEntity.action);
                if(extras != null){
                    clickIntent.putExtra("classCode", extras.getString("classCode"));
                    clickIntent.putExtra("className", notEntity.groupName);
                    PendingIntent overrideClickPendingIntent = PendingIntent.getActivity(context, notEntity.notificationId, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    mBuilder.setContentIntent(overrideClickPendingIntent);
                    mBuilder.addAction(R.drawable.fwd, "VIEW", overrideClickPendingIntent);
                }
            }
            else{
                show = false;//ignore this as this might be a new type of notification - recognized in updated app
                if(Config.SHOWLOG) Log.d(LOGTAG, "Ignoring type=" + type + ", action=" + action);
            }

            if(show){
                notificationManager.notify(notEntity.notificationId, mBuilder.build());
            }
        }
        else if(notEntity.type.equals(Constants.Notifications.UPDATE_NOTIFICATION)){

            //set title, content
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle(notEntity.groupName);
            bigTextStyle.bigText(notEntity.contentText);

            mBuilder.setStyle(bigTextStyle);

            //add actions
            mBuilder.addAction(R.drawable.seen, "DISMISS", deletePendingIntent);
            mBuilder.addAction(R.drawable.update, "UPDATE NOW", clickPendingIntent);

            notificationManager.notify(notEntity.notificationId, mBuilder.build());
        }
        else if(notEntity.type.equals(Constants.Notifications.LINK_NOTIFICATION)){

            //set title, content
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle(notEntity.groupName);
            bigTextStyle.bigText(notEntity.contentText);

            mBuilder.setStyle(bigTextStyle);

            //add actions
            mBuilder.addAction(R.drawable.seen, "DISMISS", deletePendingIntent);
            mBuilder.addAction(R.drawable.update, "VISIT", clickPendingIntent);

            notificationManager.notify(notEntity.notificationId, mBuilder.build());
        }
        else if(notEntity.type.equals(Constants.Notifications.USER_REMOVED_NOTIFICATION)){

            //set title, content
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle(notEntity.groupName);
            bigTextStyle.bigText(notEntity.contentText);

            mBuilder.setStyle(bigTextStyle);

            //add actions
            mBuilder.addAction(R.drawable.seen, "DISMISS", deletePendingIntent);
            mBuilder.addAction(R.drawable.fwd, "INBOX", clickPendingIntent);

            if(extras != null){
                //run PushOpen.UserRemovedTask to generate local msg in background
                String classCode = extras.getString("classCode");
                if(classCode != null) {
                    notificationManager.notify(notEntity.notificationId, mBuilder.build());
                    //This type of notification will be generated in onReceive of notification receiver(i.e in GUI)
                    //and not within a thread or such. Hence can run following AsyncTask safely
                    PushOpen.UserRemovedTask userRemovedTask = new PushOpen.UserRemovedTask(groupName, classCode);
                    userRemovedTask.execute();
                }
            }
        }
        else{
            //just ignore it ! - as unknown type
            if(Config.SHOWLOG) Log.d(LOGTAG, "Ignoring type=" + type + ", action=" + action);
        }
    }

    private static final String NOTIFICATION_DELETED_ACTION = "NOTIFICATION_DELETED";

    private static final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            int notificationId = intent.getIntExtra("notificationId", -1);
            if(notificationId == -1)
                notificationManager.cancelAll();
            else
                notificationManager.cancel(notificationId);

            String type = intent.getStringExtra("type");
            if(Config.SHOWLOG) Log.d(LOGTAG, "got " + NOTIFICATION_DELETED_ACTION + " for type " + type + "notid " + notificationId);

            if(type.equals(Constants.Notifications.NORMAL_NOTIFICATION)){
                if(Config.SHOWLOG) Log.d(LOGTAG, "clearing normal notification list");
                normalNotificationList.clear();
            }
        }
    };


    public static class NotificationEntity{
        String contentText;
        String groupName;
        String type;
        String action;
        int notificationId;

        NotificationEntity(Context context, String tempContentText, String tempGroupName, String tempType, String tempAction){
            if(tempType != null && tempType.equals(Constants.Notifications.NORMAL_NOTIFICATION)){
                notificationId = NORMAL_NOTIFICATION_ID; //club them all up
            }
            else {
                notificationId = SessionManager.getInstance().getNextNotificationId(); //get a new id
            }

            contentText = tempContentText;
            groupName = tempGroupName;
            action = tempAction;
            type = tempType;
        }
    }
}