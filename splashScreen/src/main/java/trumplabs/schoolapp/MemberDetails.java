package trumplabs.schoolapp;

public class MemberDetails {
  private String objectId;
  private String type;  // sms_member or app_member
  private String childName;
  public static final String SMS_MEMBER= "SMS";
  public static final String APP_MEMBER = "APP";

  public MemberDetails(String objectId, String type, String childName) {
    super();
    this.objectId = objectId;
    this.type = type;
    this.childName = childName;
  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getChildName() {
    return childName;
  }

  public void setChildName(String childName) {
    this.childName = childName;
  }

}
