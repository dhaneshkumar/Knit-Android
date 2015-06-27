package trumplabs.schoolapp;

import java.util.Map;

public class Constants {
  public static final String TEACHER = "teacher";
  public static final String PARENT = "parent";
  public static final String STUDENT = "student";

  public static boolean updatedTimestamp = false;
  public static final String TIMESTAMP = "createdAt";
  public static final String ROLE = "role";
  public static Map<String, Integer> serverMsgCounter;

  public static final String CREATED_GROUPS = "Created_groups";
  public static final String JOINED_GROUPS = "joined_groups";
  public static final String NAME = "name";
  public static final String PHONE = "phone";

  /*
   * groupDetails table variables
   */
  public static final String GROUP_DETAILS = "GroupDetails";
  public static final String LIKE_COUNT = "like_count";
  public static final String CONFUSED_COUNT = "confused_count";
  public static final String SEEN_COUNT = "seen_count";
  public static final String LIKE = "like"; //local
  public static final String CONFUSING = "confusing"; //local
  public static final String SYNCED_LIKE = "synced_like"; //local like status last synced
  public static final String SYNCED_CONFUSING = "synced_confusing"; //local
  public static final String DIRTY_BIT = "dirty_bit"; //local
  public static final String SEEN_STATUS = "seen_status"; //local  0(seen locally) or 1(synced)
  public static final String USER_ID = "userId"; //local  user id
  public static final String LIKE_STATUS = "like_status";
  public static final String CONFUSED_STATUS = "confused_status";

  /*
   * Invitation table
   */
  public static final String INVITATION = "Invitation";
  public static final String RECEIVER = "receiver"; //<email> or <phone number>
  public static final String RECEIVER_NAME = "name"; //name of the receiver
  public static final String TYPE = "type"; //see below 1, 2, 3, 4
  public static final String PENDING = "pending"; // {true, false}
  public static final String MODE = "mode"; //{phone, email}
  public static final String CLASS_CODE = "class_code"; //nullable


  public static final int INVITATION_P2T = 1;
  public static final int INVITATION_T2P = 2;
  public static final int INVITATION_P2P = 3;
  public static final int INVITATION_SPREAD = 4;

  public static final String SOURCE_APP = "app";
  public static final String SOURCE_NOTIFICATION = "notification";

  public static final String MODE_PHONE = "phone";
  public static final String MODE_EMAIL = "email";
  public static final String MODE_WHATSAPP = "whatsapp";
  public static final String MODE_RECEIVE_INSTRUCTIONS = "receiveInstructions";


  /*
   * Database table names
   */
  public static final String MESSAGE_NEEDERS= "Messageneeders";
  

    // codegroup

    public static  final String CODE_GROUP = "Codegroup";
    public static  final String DIVISION = "divison";
    public static final String IS_SUGGESTION = "isSuggestion"; //flag indicating whether this codegroup entry is a suggestion

    //group members
    public static final String GROUP_MEMBERS = "GroupMembers";

    //time milliseconds
    public static final int MINUTE_MILLISEC = 60 * 1000;
    public static final int HOUR_MILLISEC = 60 * 60 * 1000;
    public static final int DAY_MILLISEC = 24 * 60 * 60 * 1000;

    public static final String DEFAULT_NAME = "Knit";

    //Notification types
    public static class Notifications{
      public static final String NORMAL_NOTIFICATION = "NORMAL";
      public static final String TRANSITION_NOTIFICATION = "TRANSITION";
      public static final String UPDATE_NOTIFICATION = "UPDATE";
      public static final String LINK_NOTIFICATION = "LINK";
      public static final String USER_REMOVED_NOTIFICATION = "REMOVE";
    }

    //Notification actions
    public static class Actions{
      public static final String INBOX_ACTION = "INBOX"; //for normal notification

      public static final String INVITE_TEACHER_ACTION = "INVITE_TEACHER";
      public static final String CLASSROOMS_ACTION = "CLASSROOMS";
      public static final String OUTBOX_ACTION = "OUTBOX";
      public static final String INVITE_PARENT_ACTION = "INVITE_PARENT"; //open invite parent activity for that class
      public static final String CREATE_CLASS_ACTION = "CREATE_CLASS"; //open create class dialog
      public static final String SEND_MESSAGE_ACTION = "SEND_MESSAGE"; //go to specified created class to send a message

      //new actions
      public static final String LIKE_ACTION = "LIKE";
      public static final String CONFUSE_ACTION = "CONFUSE"; //when significant confused parents(go to outbox, scroll to that message and highlight the edit option)
      public static final String CLASS_PAGE_ACTION = "CLASS_PAGE"; //go to specified created class page(where option to view subscribers and invite)
    }

    public static final String PROFILE_PAGE_ACTION = "PROFILE"; //for update notification type
    //Note : action corresponding to LINK type will be a url

    public static int actionBarHeight = 0;
    public static final String SENT_MESSAGES_TABLE = "SentMessages";

    public static boolean signup_classrooms =false;
    public static boolean signup_inbox = false;
    public static boolean signup_outbox = false;
    public static boolean IS_SIGNUP = false;

    public static class TutorialKeys{
      //<username> + <key> becomes the key for shared preferences
      public static final String PARENT_RESPONSE = "_parent_response_tutorial";
      public static final String TEACHER_RESPONSE = "_teacher_response_tutorial";
    }

    public static class ComposeSource{
      public static final String KEY = "SOURCE";
      public static final String INSIDE = "INSIDE";
      public static final String OUTSIDE = "OUTSIDE";
    }
}
