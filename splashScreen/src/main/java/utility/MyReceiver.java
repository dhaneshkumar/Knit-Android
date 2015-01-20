package utility;

import library.UtilString;

import org.json.JSONException;
import org.json.JSONObject;

import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.Messages;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.parse.ParsePushBroadcastReceiver;

/**
 * Notification receiver class.
 * It receives notifications,customize and then push it mobile
 */
public class MyReceiver extends ParsePushBroadcastReceiver {
    public static String[] events = new String[10];
    public static String[] events1 = new String[10];
    private static int NOTIFICATION_ID = 1;
    public static int count = 0;
    public static int numMessages = 0;
    private NotificationManager notificationManager;

    @Override
    public void onPushReceive(Context context, Intent intent) {

        notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Bundle extras = intent.getExtras();
        String jsonData = extras.getString("com.parse.Data");

        try {
            if(jsonData != null) {
                JSONObject json = new JSONObject(jsonData);
                String contenttext = json.getString("msg");
                String groupname = json.getString("groupName");

                generateNotification(context, json, contenttext, groupname);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onPushOpen(Context context, Intent intent)
    {
        count=0;
        for(int i=0;i<10;i++)
            events[i]="";
    }

    @Override
    protected void onPushDismiss(Context contex,Intent intent)
    {
        count=0;
        for(int i=0;i<10;i++)
            events[i]="";
    }

    private static final String NOTIFICATION_DELETED_ACTION = "NOTIFICATION_DELETED";

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MyReceiver myreceiver = null;
            for(int i=0;i<10;i++)
                myreceiver.events[i]="";
            myreceiver.count=0;// Do what you want here
            context.getApplicationContext().unregisterReceiver(this);
        }
    };


    /**
     * Create new notifications and push it internally on phone
     * @param context
     * @param json
     * @param contenttext
     * @param groupname
     */
    private void generateNotification(Context context,
                                         JSONObject json, String contenttext,String groupname) {

        Intent intent1 = new Intent(NOTIFICATION_DELETED_ACTION);
        PendingIntent pendintIntent = PendingIntent.getBroadcast(context, 0, intent1, 0);
        context.getApplicationContext().registerReceiver(receiver, new IntentFilter(NOTIFICATION_DELETED_ACTION));

        //Creating notifications.
        Intent myIntent = new Intent(context, PushOpen.class);

        myIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        myIntent.putExtra("TYPE", "ACTION");
        myIntent.putExtra("URL", "https://play.google.com/store/apps/details?id=trumplab.textslate");

        PendingIntent pendingIntent = PendingIntent.getActivity( context, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder  mBuilder =
                new NotificationCompat.Builder(context);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setAutoCancel(true);
        mBuilder.setContentTitle("Knit");
        mBuilder.setContentText(count+1 + " new messages");
        mBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
        mBuilder.setSmallIcon(R.drawable.notification);
        mBuilder.setDeleteIntent(pendintIntent);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        if(count<=9){
            events[count]=contenttext;
            events1[count]=groupname;
        }


        if(count==0)
        {
            /*
            Notification for single Message
            Title : Group name
            Msg : msg
             */
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle(events1[0]);
            bigTextStyle.bigText(events[0]);


            NotificationCompat.Builder  mBuilder1 =
                    new NotificationCompat.Builder(context);
            mBuilder1.setContentIntent(pendingIntent);
            mBuilder1.setAutoCancel(true);
            mBuilder1.setContentTitle(events1[0]);
            mBuilder1.setContentText(events[0]);
            mBuilder1.setSmallIcon(R.drawable.notification);
            mBuilder1.setStyle(bigTextStyle);
            mBuilder1.setDeleteIntent(pendintIntent);
            mBuilder1.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
            //mBuilder1.addAction();


           /* RemoteViews contentView = new RemoteViews(Application.getAppContext().getPackageName(), R.layout.custom_notification);
            contentView.setTextViewText(R.id.notification_title, events1[0]);
            contentView.setTextViewText(R.id.notification_text, events[0]);
            notification.contentView = contentView;
            mBuilder1.setContent(contentView);*/
            mBuilder1.addAction(R.drawable.tick, "DONE", pendingIntent);
            mBuilder1.addAction(R.drawable.social_send_now, "UPDATE", pendingIntent);
            ++count;

            //pushing notification
            notificationManager.notify(NOTIFICATION_ID, mBuilder1.build());


        }
        else{

             /*
            Notification for Multiple Messages
            Title : Knit
            Msg format: (group name : msg)
             */
            inboxStyle.setBigContentTitle("Knit");

            for (int i=0; i<=9; i++) {
                if( !UtilString.isBlank(events[i]))
                    inboxStyle.addLine(events1[i]+": "+events[i]);
                else
                    break;
            }

            mBuilder.setStyle(inboxStyle);  //setting inbox style

            //pushing notification
            notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
            ++count;
        }
    }

}

