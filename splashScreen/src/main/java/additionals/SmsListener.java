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
import utility.Config;

/**
 * Created by ashish on 27/2/15.
 * Used to listen to messages to autodetect and send verification code to authenticate the user
 */

public class SmsListener extends BroadcastReceiver {
    static boolean isListeningOn = false;

    static SmsListener listener = new SmsListener();

    String endMsgContent = "is your Knit verification Code"; //genCode2
    String senderCore = "myKnit"; //genCode2

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
                        if(msgFrom == null || msgBody == null){
                            continue; //go to next message
                        }

                        if(msgFrom.toLowerCase().contains(senderCore.toLowerCase()) && msgBody.toLowerCase().contains(endMsgContent.toLowerCase())){
                            if(Config.SHOWLOG) Log.d("DEBUG_SMS_LISTENER", "FOUND : msgBody " + msgBody + " msgFrom " + msgFrom);
                            isListeningOn = false;
                            final String code = extractCode(msgBody);
                            if(code != null) {
                                if (PhoneSignUpVerfication.verificationCodeET != null) {
                                    if(Config.SHOWLOG) Log.d("DEBUG_SMS_LISTENER", "posting to PhoneSignUpVerfication");
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
                            if(Config.SHOWLOG) Log.d("DEBUG_SMS_LISTENER", "STRAY : msgBody " + msgBody + " msgFrom " + msgFrom);
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
        return tokens[0]; //the code
    }

    public static void register(){
        //unregister old receiver first
        unRegister();
        isListeningOn = true;

        if(Config.SHOWLOG) Log.d("DEBUG_SMS_LISTENER", "register() - called");
        if(listener != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.provider.Telephony.SMS_RECEIVED");
            Application.getAppContext().registerReceiver(listener, filter);
        }
        else{
            if(Config.SHOWLOG) Log.d("DEBUG_SMS_LISTENER", "register() - LISTENER NULL");
        }
    }

    public static void unRegister(){
        isListeningOn = false;
        if(Config.SHOWLOG) Log.d("DEBUG_SMS_LISTENER", "unRegister() - called");
        if(listener != null) {
            try{
                Application.getAppContext().unregisterReceiver(listener);
            }
            catch (IllegalArgumentException e){
                if(Config.SHOWLOG) Log.d("DEBUG_SMS_LISTENER", "unRegister() - ALREADY NOT REGISTERED");
            }
        }
        else{
            if(Config.SHOWLOG) Log.d("DEBUG_SMS_LISTENER", "unRegister() - LISTENER NULL");
        }
    }
}