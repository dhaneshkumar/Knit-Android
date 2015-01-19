package notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONObject;

import library.UtilString;
import trumplab.textslate.R;
import utility.PushOpen;

/**
 * Created by ashish on 19/1/15.
 */
public class NotificationGenerator {
    public static String[] events = new String[10];
    public static String[] groupNames = new String[10];
    private static int NOTIFICATION_ID = 1;
    public static int count = 0;
    private NotificationManager notificationManager;



    public static void generateNotification(Context context, String contentText,String groupName) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent1 = new Intent(NOTIFICATION_DELETED_ACTION);
        PendingIntent deleteIntent = PendingIntent.getBroadcast(context, 0, intent1, 0);
        context.getApplicationContext().registerReceiver(receiver, new IntentFilter(NOTIFICATION_DELETED_ACTION));

        Intent myIntent = new Intent(context, PushOpen.class);
        PendingIntent pendingIntent = PendingIntent.getActivity( context, 0, myIntent, 0);
        NotificationCompat.Builder  mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setAutoCancel(true);
        mBuilder.setContentTitle("Knit");
        mBuilder.setContentText(count+1 + " new messages");
        //      mBuilder.setDeleteIntent(contentIntent);
        //   mBuilder.setTicker("New Message Alert!");
        mBuilder.setSmallIcon(R.drawable.notification);
        mBuilder.setDeleteIntent(deleteIntent);

			      /* Increase notification number every time a new notification arrives */
        //mBuilder.setNumber(count+1);

        if(count<=9){
            events[count]=contentText;
            groupNames[count]=groupName;
        }


        if(count==0)
        {
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle(groupNames[0]);
            bigTextStyle.bigText(events[0]);

            mBuilder.setContentTitle(groupNames[0]);
            mBuilder.setContentText(events[0]);
            mBuilder.setStyle(bigTextStyle);

            ++count;
            notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
        else{
            NotificationCompat.InboxStyle inboxStyle =
                    new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle("Knit");
            mBuilder.setStyle(inboxStyle);

            for (int i=0; i<=9; i++) {
                if( !UtilString.isBlank(events[i]))
                {
                    inboxStyle.addLine(groupNames[i]+": "+events[i]);
                }
                else
                    break;
            }

            notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
            ++count;
        }
    }

    private static final String NOTIFICATION_DELETED_ACTION = "NOTIFICATION_DELETED";

    private static final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("DEBUG_NOTIFICATION_GENERATOR", "got " + NOTIFICATION_DELETED_ACTION);
            for(int i=0;i<10;i++)
                events[i]="";
            count=0;// Do what you want here
            context.getApplicationContext().unregisterReceiver(this);
        }
    };

}
