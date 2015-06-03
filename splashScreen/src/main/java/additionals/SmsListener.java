package additionals;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import loginpages.PhoneSignUpVerfication;
import trumplabs.schoolapp.Application;

/**
 * Created by ashish on 27/2/15.
 * Used to listen to messages to autodetect and send verification code to authenticate the user
 */

public class SmsListener extends BroadcastReceiver {
    static boolean isListeningOn = false;

    static SmsListener listener = new SmsListener();

    String startMsgContent = "Your requested verification code is";
    String senderCore1 = "TXTSLT";
    String senderCore2 = "myKNIT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED") && isListeningOn){
            Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
            SmsMessage[] msgs = null;
            String msgFrom;
            if (bundle != null){
                //---retrieve the SMS message received---
                try{
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                    for(int i=0; i<msgs.length; i++){
                        msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                        msgFrom = msgs[i].getOriginatingAddress();
                        String msgBody = msgs[i].getMessageBody();
                        if((msgFrom.contains(senderCore1) || msgFrom.contains(senderCore2)) && msgBody.startsWith(startMsgContent)){
                            Log.d("DEBUG_SMS_LISTENER", "FOUND : msgBody " + msgBody + " msgFrom " + msgFrom);
                            isListeningOn = false;
                            final String code = extractCode(msgBody);
                            if(code != null) {
                                if (PhoneSignUpVerfication.verificationCodeET != null) {
                                    Log.d("DEBUG_SMS_LISTENER", "posting to PhoneSignUpVerfication");
                                    PhoneSignUpVerfication.verificationCodeET.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            PhoneSignUpVerfication.smsListenerVerifyTask(code);
                                        }
                                    });
                                    break; //ignore other messages
                                }
                            }
                        }
                        else {
                            Log.d("DEBUG_SMS_LISTENER", "STRAY : msgBody " + msgBody + " msgFrom " + msgFrom);
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    static String extractCode(String msgBody){
        //msg is of following type "Your requested verification code is 3822"
        String tokens[] = msgBody.split(" ");
        if(tokens.length != 6){
            return null;
        }
        return tokens[5]; //the code
    }

    public static void register(){
        //unregister old receiver first
        unRegister();
        isListeningOn = true;

        Log.d("DEBUG_SMS_LISTENER", "register() - called");
        if(listener != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.provider.Telephony.SMS_RECEIVED");
            Application.getAppContext().registerReceiver(listener, filter);
        }
        else{
            Log.d("DEBUG_SMS_LISTENER", "register() - LISTENER NULL");
        }
    }

    public static void unRegister(){
        isListeningOn = false;
        Log.d("DEBUG_SMS_LISTENER", "unRegister() - called");
        if(listener != null) {
            try{
                Application.getAppContext().unregisterReceiver(listener);
            }
            catch (IllegalArgumentException e){
                Log.d("DEBUG_SMS_LISTENER", "unRegister() - ALREADY NOT REGISTERED");
            }
        }
        else{
            Log.d("DEBUG_SMS_LISTENER", "unRegister() - LISTENER NULL");
        }
    }
}