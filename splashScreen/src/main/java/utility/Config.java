package utility;

import trumplabs.schoolapp.Constants;

public class Config {

  /*
   * Parse Configurations
   * 5 Parameters to change before updating app on playstore
   */

    /*
    1. Select APP ID
     */
    //public static final String APP_ID="jrumkUT2jzvbFn7czsC5fQmFG5JIYSE4P7GJrlOG"; //Knit
    public static final String APP_ID = "tTqAhR73SE4NWFhulYX4IjQSDH2TkuTblujAbvOK"; // development_knit

  /*
    2. Select CLIENT Key
     */
   // public static final String CLIENT_KEY= "nfSgzcWi39af825uQ0Fhj2L7L2YJca9ibBgR9wtQ"; // Knit
    public static final String CLIENT_KEY = "4LnlMXS6hFUunIZ6cS3F7IcLrWGuzOIkyLldkxQJ"; // development_knit

  /*
  3.  Uncomment out "minifyEnabled true" & "shrinkResources true" in build.gradle before updating app on playstore
  */

  /*
  4. Set flag to allow to join his own class.  # false : Knit & #true for testing
   */
  //flag to show teacher can join his own class or not
  public static final boolean CAN_JOIN_OWN_CLASS = true;

  /*
  5. Set flag to detect invalid number.  #true : knit & #false for testing
   */
  public static final boolean DETECT_INVALID_NUMBER = false;




  /*************************************************************************************************************/

    public static final String FB_APP_ID = "689390944539813";
    public static int SUBSCRIBER_MIN_LIMIT = 1;


    //Following are used now to NOT SHOW Kio for old users
    public static final String defaultParentGroupCode = "TS29734"; // raven
    public static final String defaultTeacherGroupCode = "TS49518"; // raven

    public static final int outboxMsgRefreshPerClass = 2; //how many sent messages per class will

    //be updated for like/confused/seen count
    public static final int outboxMsgRefreshTotal = 10; //how many latest sent messages will be updated for like/confused
    public static final int outboxMsgMaxFetchCount = 100; //max how many outbox messages to fetch on first time app is opened

    public static final int inboxMsgRefreshTotal= 10; //how many of the latest inbox messages(total) will

    //be updated(periodically in Refresher) for like/confused
    public static final int inboxMsgCount = 20;
    public static final int createMsgCount = 20;
    public static  final int outboxMsgCount =20;
    public static final int createMsgMax = 100;
    public static final int membersCount = 100;
    //public static final String creator = "Knit";
    public static final String welcomeMsg =
            "Congratulations! You have successfully joined my classroom. I will use this app to send important reminders and announcements related to my subject.";

    public static final String RemovalMsg = "You have been removed from this classroom. You won't receive any notification from this class from now onwards.";
    public static final int faqRefreshingcount = 20;

    public static final int senderPicUpdationCount = 5; // #appOpening count


    public static int messageNormalStartTime = 7; //7 AM (7 hours)
    public static int messageNormalEndTime = 22;  //10 PM(22 hours)
    public static String messageTimeWarning = "This might not be the right time to send a message. Send anyway?";

    public static long inboxOutboxUpdateGap = 15 * Constants.MINUTE_MILLISEC; //time gap between two updates of inbox/outbox to be called in when app is foreground
    public static long joinedClassUpdateGap = 60 *12 * Constants.MINUTE_MILLISEC; //time gap between two updates of joined class details i.e name, profile pic. can be called in background refresher thread
}
