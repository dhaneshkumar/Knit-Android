package trumplabs.schoolapp;

import java.util.Date;
import java.util.Map;

public class Constants {
  public static String TEACHER = "teacher";
  public static String PARENT = "parent";
    public static String STUDENT = "student";

  public static Map<String, String> groupSenderMap;
  public static Date currentTimeStamp;
  public static boolean updatedTimestamp = false;
  public static String TIMESTAMP = "createdAt";
  public static final String ROLE = "role";
  public static Map<String, Integer> serverMsgCounter;

  public static final String CREATED_GROUPS = "Created_groups";
  private final String EMAIL = "email";
  public static final String JOINED_GROUPS = "joined_groups";
  public static final String NAME = "name";
  public static final String PHONE = "phone";
  public static final String PID = "pid";
  public static final String SCHOOL = "school";
  public static final String GROUP_TYPE = "group_type";
  public static final String REMOVED_GROUPS = "removed_groups";
  
  /*
   * groupDetails table variables
   */
  public static final String LIKE_COUNT = "like_count";
  public static final String CONFUSED_COUNT = "confused_count";
  public static final String SEEN_COUNT = "seen_count";
  public static final String LIKE = "like"; //local
  public static final String CONFUSING = "confusing"; //local
  public static final String GROUP_DETAILS = "GroupDetails";
  public static final String DIRTY_BIT = "dirty_bit"; //local
  public static final String SEEN_STATUS = "seen_status"; //local  0(seen locally) or 1(synced)
  public static final String USER_ID = "userId"; //local  user id


  

  /*
   * MessageState table variables [only on cloud]
   * MessageState is written as (LIKE_STATUS, CONSFUSED_STATUS)
   * 3 states possible 00, 10(liked), 01(confused)
   */
   public static final String MESSAGE_STATE = "MessageState";
   public static final String USERNAME = "username";
   public static final String MESSAGE_ID = "message_id";
   public static final String LIKE_STATUS = "like_status";
   public static final String CONFUSED_STATUS = "confused_status";


  /*
   * Database table names
   */
  public static final String MESSAGE_NEEDERS= "Messageneeders";
  

    // codegroup

    public static  final String CODE_GROUP = "Codegroup";
    public static  final String DIVISION = "divison";


    //group members
    public static final String GROUP_MEMBERS = "GroupMembers";

    //time milliseconds
    public static int MINUTE_MILLISEC = 60 * 1000;
    public static int HOUR_MILLISEC = 60 * 60 * 1000;
    public static int DAY_MILLISEC = 24 * 60 * 60 * 1000;

    //Default messages details
    public static String DEFAULT_CREATOR = "Mr. Kio";
    public static String DEFAULT_SENDER_ID_TEACHER = "ttextslate@trumplab.com";
    public static String DEFAULT_SENDER_ID_PARENT = "ptextslate@trumplab.com";

    public static String DEFAULT_NAME = "Mr. Kio";

    public static String WELCOME_MESSAGE_TEACHER = "Hey there! Welcome to my classroom.You can also create your own classroom and start broadcasting messages to all the parents. Don’t forget to share code of your classroom with parents. Without code how will they subscribe to your classroom?";
    public static String WELCOME_MESSAGE_PARENT = " Hey there! Welcome to my classroom. You can also join any classroom created by your child’s teacher but you will need a class-code. If your child’s teacher haven’t yet started using Knit then invite them";
    public static String CLASS_CREATION_MESSAGE_TEACHER = "Ola! Your classroom created successfully. Do you know that parents who don’t use android phone can also subscribe to your updates? They just have to send your classroom-code to our number 9243000080. FYI I have already sent you an e-mail to help you with inviting parents";

    //Notification types
    public static String NORMAL_NOTIFICATION = "NORMAL";
    public static String TRANSITION_NOTIFICATION = "TRANSITION";
    public static String UPDATE_NOTIFICATION = "UPDATE";
    public static String LINK_NOTIFICATION = "LINK";

    //Notification actions
    public static String INBOX_ACTION = "INBOX"; //for normal notification

    public static String INVITE_TEACHER_ACTION = "INVITE_TEACHER"; //e.g for transition notification
    public static String CLASSROOMS_ACTION = "CLASSROOMS"; //e.g for transition notification
    public static String INVITE_PARENT_ACTION = "INVITE_PARENT"; //e.g for transition notification
    public static String CREATE_CLASS_ACTION = "CREATE_CLASS"; //e.g for transition notification

    public static String PROFILE_PAGE_ACTION = "PROFILE"; //for update notification type
    //Note : action corresponding to LINK type will be a url

}
