package chat;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.parse.ParseUser;
import com.quickblox.core.QBEntityCallbackImpl;
import com.quickblox.messages.QBMessages;
import com.quickblox.messages.model.QBEnvironment;
import com.quickblox.messages.model.QBSubscription;

import java.util.ArrayList;
import java.util.List;

import trumplabs.schoolapp.Application;
import utility.Utility;

/**
 * Created by ashish on 20/8/15.
 */


public class RegistrationIntentService extends IntentService {

    private static final String TAG = "__CHAT RegIntentService";
    private static final String[] TOPICS = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            // In the (unlikely) event that multiple refresh operations occur simultaneously,
            // ensure that they are processed sequentially.
            synchronized (TAG) {
                String syncedRegId = getRegistrationId();
                InstanceID instanceID = InstanceID.getInstance(this);
                final String token = instanceID.getToken(ChatConfig.GCM_SENDER_ID,
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

                //old and new reg ids are not same
                if(token != null && syncedRegId != null && !token.equals(syncedRegId)){
                    Log.i(TAG, "New GCM Registration Token: " + token);
                    Application.applicationHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            subscribeToPushNotifications(token);
                        }
                    });
                }
                else{
                    Log.i(TAG, "Same/Null GCM Registration Token: " + token);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
        }
    }

    /**
     * Subscribe to Push Notifications
     *
     * @param regId registration ID
     */
    private void subscribeToPushNotifications(final String regId) {
        //Create push token with  Registration Id for Android

        Log.d(TAG, "subscribing...");

        String deviceId;

        final TelephonyManager mTelephony = (TelephonyManager) Application.getAppContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        if (mTelephony.getDeviceId() != null) {
            deviceId = mTelephony.getDeviceId(); //*** use for mobiles
        } else {
            deviceId = Settings.Secure.getString( Application.getAppContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID); //*** use for tablets
        }

        Log.d(TAG, "calling subscribeToPushNotificationsTask regId=" + regId + "deviceId=" + deviceId);

        QBMessages.subscribeToPushNotificationsTask(regId, deviceId, QBEnvironment.DEVELOPMENT, new QBEntityCallbackImpl<ArrayList<QBSubscription>>() {
            @Override
            public void onSuccess(ArrayList<QBSubscription> qbSubscriptions, Bundle bundle) {
                Log.d(TAG, "gcm subscribed to quickblox success");
                storeRegistrationId(regId);

                Application.applicationHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Utility.toast("GCM Reg success");
                    }
                });
            }

            @Override
            public void onError(List<String> errors) {
                Log.d(TAG, "gcm subscribed error = " + errors);
            }
        });
    }

    /*
        key = <ParseUsername>_gcm_id
     */
    private String getRegistrationId() {
        if(ParseUser.getCurrentUser() == null){
            return "";
        }

        final SharedPreferences prefs = getSharedPreferences(ChatConfig.CHAT_PREFS, Context.MODE_PRIVATE);

        String registrationId = prefs.getString(ParseUser.getCurrentUser().getUsername() + "_gcmRegId", "");
        Log.d(TAG, "Registration id=" + registrationId);
        return registrationId;
    }

    /*
        Called when successfully sent to Quickblox servers
        store with key = <ParseUsername>_gcm_id
     */
    private void storeRegistrationId(String regId) {
        if(ParseUser.getCurrentUser() == null){
            return;
        }

        final SharedPreferences prefs = getSharedPreferences(ChatConfig.CHAT_PREFS, Context.MODE_PRIVATE);
        Log.i(TAG, "Saving regId=" + regId);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(ParseUser.getCurrentUser().getUsername() + "_gcmRegId", regId);
        editor.apply();
    }
}