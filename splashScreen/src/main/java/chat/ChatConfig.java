package chat;

/**
 * Created by ashish on 26/8/15.
 */
public class ChatConfig {

    public static final String NON_TEACHER = "non_teacher";
    public static final String TEACHER = "teacher";

    public static final String MSG_TS_FORMAT = "dd/MM/yyyy hh:mm:ss a";
    public static final String LAST_SEEN_FORMAT = "dd/MM hh:mm a";

    public static final String ONE_SIGNAL_APP_ID = "8b7bb84e-4aa7-11e5-9676-2360b1b515f4";
    public static final String GOOGLE_PROJECT_NUMBER = "110521105118";

    public static final String FIREBASE_URL = "https://devknitchat.firebaseio.com";

    //Need input, channelId, mUsername, opponentOneSignalId
    static class ChatNotificationTable{
        public static final String TABLE = "ChatNotificationTable";

        public static final String CHANNEL = "channel";
        public static final String MSG_CONTENT = "msgContent";
        public static final String MSG_TITLE = "msgTitle";
        public static final String OPP_ONE_SIGNAL_ID = "oppOneSignalId"; //one signal id of opponent to send notification to
        public static final String PENDING = "pending";
        public static final String TIME = "time";
    }

    static class OneSignalErrors{
        public static final String NOT_SUBSCRIBED = "All included players are not subscribed";
    }
}
