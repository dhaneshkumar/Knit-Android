package chat;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import trumplab.textslate.R;

/**
 * Created by GleasonK on 7/14/15.
 */
public class MyGcmListenerService extends GcmListenerService {

    private final static String TAG = "__CHAT GCMListen";
    @Override
    public void onMessageReceived(String from, Bundle data) {
        if(from != null && from.equals(ChatConfig.GCM_SENDER_ID)) {
            Log.d(TAG, "From: " + from);
            Log.d(TAG, "Message: " + data);
            showNotification(data);
        }
        else{
            Log.d(TAG, "From: " + from + "(possibly Parse)");
        }
    }

    private void showNotification(Bundle extras) {
        if (extras==null) return;
        Log.d("GCM-notif",extras.toString());
        String chatRoom = extras.getString(ChatConfig.GCM_CHAT_ROOM);
        String notifBigTex  = "Poke from " + extras.getString(ChatConfig.GCM_POKE_FROM);
        String notifContent = "In chat room " + chatRoom;

        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        /*Intent intent = new Intent(this, ChatActivityRecyclerView.class);
        intent.putExtra(ChatConfig.CHAT_ROOM, chatRoom);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);*/

        Bitmap icon = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.notification);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setLargeIcon(icon)
                        .setSmallIcon(R.drawable.notification)
                        .setContentTitle(notifBigTex)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(notifBigTex))
                        .setContentText(notifContent)
                        .setAutoCancel(true);

        //mBuilder.setContentIntent(contentIntent);
        Notification pnNotif = mBuilder.build();
        mNotificationManager.notify(0, pnNotif);  // Set notification ID
    }
}
