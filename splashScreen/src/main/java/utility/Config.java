package utility;

public class Config {

  /*
   * Parse Configurations
   */
  
   //public static final String APP_ID="jrumkUT2jzvbFn7czsC5fQmFG5JIYSE4P7GJrlOG"; //raven
  public static final String APP_ID = "7kuBxdNpJ5ZW5rcyYrpw3vlxsTLuHbuqtTd65ErZ"; // test_raven
  // public static final String CLIENT_KEY= "nfSgzcWi39af825uQ0Fhj2L7L2YJca9ibBgR9wtQ"; //raven
  public static final String CLIENT_KEY = "bmfdNqrZY0olJgsezG5ZRiBN1OPO4TqO1pH46PU9"; // test_raven

  public static final String defaultParentGroupCode = "TS29734"; // raven
  // "TS26620"; //test_raven
  public static final String defaultTeacherGroupCode = "TS49518"; // raven
  // "TS92462"; //test_raven
  
  
  public static String userId;
  public static String role;
  public static final int inboxMsgCount = 15;
  public static final int createMsgCount = 15;
    public static  final int outboxMsgCount =20;
  public static final int createMsgMax = 100;
  public static final int membersCount = 100;
  public static final String creator = "TextSlate";
  public static final String welcomeMsg =
      "Congratulations! You have successfully joined my classroom. I will use this app to send important reminders and announcements related to my subject.";

    public static final String RemoveMsg = "Sorry! You have been removed from this classroom. You won't receive any notification from this class from now onwards.";
    public static final int faqRefreshingcount = 20;

  public static final int senderPicUpdationCount = 5; // #appOpening count



}
