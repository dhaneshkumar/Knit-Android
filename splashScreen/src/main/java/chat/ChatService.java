package chat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.parse.ParseUser;
import com.quickblox.auth.QBAuth;
import com.quickblox.auth.model.QBSession;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBPrivateChat;
import com.quickblox.chat.QBPrivateChatManager;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBMessageListener;
import com.quickblox.chat.listeners.QBPrivateChatManagerListener;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialog;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.QBEntityCallbackImpl;
import com.quickblox.core.request.QBPagedRequestBuilder;
import com.quickblox.core.request.QBRequestBuilder;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import trumplabs.schoolapp.Application;

/**
 * Created by igorkhomenko on 4/28/15.
 */
public class ChatService {
    public static int chatLoginStage = 0;

    private static final String TAG = ChatService.class.getSimpleName();

    static final int AUTO_PRESENCE_INTERVAL_IN_SECONDS = 30;

    private static ChatService instance;

    public static synchronized ChatService getInstance() {
        if(instance == null) {
            instance = new ChatService();
        }
        return instance;
    }

    public static boolean initIfNeed(Context ctx) {
        if (!QBChatService.isInitialized()) {
            QBChatService.setDebugEnabled(true);
            QBChatService.init(ctx);
            Log.d(TAG, "Initialise QBChatService");

            return true;
        }

        return false;
    }

    private QBChatService chatService;

    private ChatService() {
        chatService = QBChatService.getInstance();
        chatService.addConnectionListener(chatConnectionListener);
    }

    public void addConnectionListener(ConnectionListener listener){
        chatService.addConnectionListener(listener);
    }

    public void removeConnectionListener(ConnectionListener listener){
        chatService.removeConnectionListener(listener);
    }

    public void login(final QBUser user, final QBEntityCallback callback){

        chatLoginStage = 0;
        // Create REST API session
        //
        QBAuth.createSession(user, new QBEntityCallbackImpl<QBSession>() {
            @Override
            public void onSuccess(QBSession session, Bundle args) {
                chatLoginStage = 1;
                user.setId(session.getUserId());

                // login to Chat
                //
                loginToChat(user, new QBEntityCallbackImpl() {

                    @Override
                    public void onSuccess() {
                        chatLoginStage = 2;
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(List errors) {
                        callback.onError(errors);
                    }
                });
            }

            @Override
            public void onError(List<String> errors) {
                if (errors.contains("Unauthorized")) {
                    QBAuth.createSession(new QBEntityCallbackImpl<QBSession>() {

                        @Override
                        public void onSuccess(QBSession session, Bundle params) {
                            chatLoginStage = 3;
                            // You have successfully created the Application session
                            //
                            // Now you can use QuickBlox API!

                            //signup the user
                            String username = ParseUser.getCurrentUser().getUsername();
                            final QBUser newQBUser = new QBUser(username, username);

                            QBUsers.signUp(newQBUser, new QBEntityCallbackImpl<QBUser>() {
                                @Override
                                public void onSuccess(QBUser retQBUser, Bundle args) {
                                    chatLoginStage = 4;
                                    loginToChat(newQBUser, new QBEntityCallbackImpl() {

                                        @Override
                                        public void onSuccess() {
                                            chatLoginStage = 5;
                                            callback.onSuccess();
                                        }

                                        @Override
                                        public void onError(List errors) {
                                            callback.onError(errors);
                                        }
                                    });
                                }

                                @Override
                                public void onError(List<String> errors) {
                                    callback.onError(errors);
                                }
                            });
                        }

                        @Override
                        public void onError(List<String> errors) {
                            callback.onError(errors);
                        }
                    });


                } else {
                    callback.onError(errors);
                }
            }
        });
    }

    public void logout(){
        chatService.logout(new QBEntityCallbackImpl() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(List list) {

            }
        });
    }

    private void loginToChat(final QBUser user, final QBEntityCallback callback){

        chatService.login(user, new QBEntityCallbackImpl() {
            @Override
            public void onSuccess() {

                // Start sending presences
                //
                try {
                    chatService.startAutoSendPresence(AUTO_PRESENCE_INTERVAL_IN_SECONDS);
                } catch (SmackException.NotLoggedInException e) {
                    e.printStackTrace();
                }

                callback.onSuccess();
            }

            @Override
            public void onError(List errors) {
                callback.onError(errors);
            }
        });
    }

    public QBUser getCurrentUser(){
        return QBChatService.getInstance().getUser();
    }

    public Integer getOpponentIDForPrivateDialog(QBDialog dialog){
        Integer opponentID = -1;
        for(Integer userID : dialog.getOccupants()){
            if(!userID.equals(getCurrentUser().getId())){
                opponentID = userID;
                break;
            }
        }
        return opponentID;
    }

    ConnectionListener chatConnectionListener = new ConnectionListener() {
        @Override
        public void connected(XMPPConnection connection) {
            Log.i(TAG, "connected");
        }

        @Override
        public void authenticated(XMPPConnection connection) {
            Log.i(TAG, "authenticated");
        }

        @Override
        public void connectionClosed() {
            Log.i(TAG, "connectionClosed");
        }

        @Override
        public void connectionClosedOnError(final Exception e) {
            Log.i(TAG, "connectionClosedOnError: " + e.getLocalizedMessage());
        }

        @Override
        public void reconnectingIn(final int seconds) {
            if(seconds % 5 == 0) {
                Log.i(TAG, "reconnectingIn: " + seconds);
            }
        }

        @Override
        public void reconnectionSuccessful() {
            Log.i(TAG, "reconnectionSuccessful");
        }

        @Override
        public void reconnectionFailed(final Exception error) {
            Log.i(TAG, "reconnectionFailed: " + error.getLocalizedMessage());
        }
    };

    //Actual chat manager and listener

    void initChatManagerIfNeeded(){
        if(privateChatManager == null) {
            privateChatManager = QBChatService.getInstance().getPrivateChatManager();
            privateChatManager.addPrivateChatManagerListener(privateChatManagerListener);
        }
    }

    QBPrivateChatManager privateChatManager; //overall manager

    QBPrivateChatManagerListener privateChatManagerListener = new QBPrivateChatManagerListener() {
        @Override
        public void chatCreated(final QBPrivateChat privateChat, final boolean createdLocally) {
            Log.d("__CHAT pCMngLis", "chatCreated");
            if(!createdLocally){
                Log.d("__CHAT pCMngLis", "chatCreated not locally");
                privateChat.addMessageListener(privateChatMessageListener);
            }
        }
    };

    QBMessageListener<QBPrivateChat> privateChatMessageListener = new QBMessageListener<QBPrivateChat>() {
        @Override
        public void processMessage(QBPrivateChat privateChat, final QBChatMessage chatMessage) {
            Log.d("__CHAT pCMsgLis", "global processMessage " + privateChat.getParticipant());
        }

        @Override
        public void processError(QBPrivateChat privateChat, QBChatException error, QBChatMessage originMessage){
            Log.d("__CHAT pCMsgLis", "global processError " + privateChat.getParticipant());
        }

        @Override
        public void processMessageDelivered(QBPrivateChat privateChat, String messageID){
            Log.d("__CHAT pCMsgLis", "global processMessageDelivered " + privateChat.getParticipant());
        }

        @Override
        public void processMessageRead(QBPrivateChat privateChat, String messageID){
            Log.d("__CHAT pCMsgLis", "global processMessageRead " + privateChat.getParticipant());
        }
    };

    /**
     * GCM Functionality.
     * In order to use GCM Push notifications you need an API key and a Sender ID.
     * Get your key and ID at - https://developers.google.com/cloud-messaging/
     */
    public void gcmRegister() {
        if (checkPlayServices()) {
            Log.d("__CHAT", "call Reg Intent Service");
            Intent intent = new Intent(Application.getAppContext(), RegistrationIntentService.class);
            Application.getAppContext().startService(intent);
            //new RegisterTask().execute();
        } else {
            Log.e("GCM-register", "No valid Google Play Services APK found.");
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(Application.getAppContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                if(Application.getCurrentActivity() != null) {
                    //show in current activity
                    GooglePlayServicesUtil.getErrorDialog(resultCode, Application.getCurrentActivity(), ChatConfig.PLAY_SERVICES_RESOLUTION_REQUEST).show();
                }
            } else {
                Log.e("GCM-check", "This device is not supported.");
            }
            return false;
        }
        return true;
    }

}
