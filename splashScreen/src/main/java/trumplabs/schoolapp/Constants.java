package trumplabs.schoolapp;

import java.util.Date;
import java.util.Map;

public class Constants {
  public static String TEACHER = "teacher";
  public static String PARENT = "parent";

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
  public static final String CONFUSED_COUNT = "counfused_count";
  public static final String LIKE = "like"; //local
  public static final String CONFUSING = "confusing"; //local
  public static final String GROUP_DETAILS = "GroupDetails";
  public static final String DIRTY_BIT = "dirty_bit"; //local
  
  

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
}
