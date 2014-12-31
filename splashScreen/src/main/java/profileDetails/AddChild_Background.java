package profileDetails;

import java.util.ArrayList;
import java.util.List;

import joinclasses.JoinedClasses;
import library.UtilString;
import trumplabs.schoolapp.Constants;
import utility.Utility;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

/*
 * Adding child to a new joined group
 */

public class AddChild_Background  {
  private String childName;
  private List<String> joinedClasses;
  private boolean testFlag;
  private String userId;
  private int flag;

  /*
   * to identify from which class, requeset is coming 0- profile edit class 1- joinClass_addchild
   */
  public AddChild_Background(String userId, String childName, List<String> joinedClasses,
      int flag) {
    this.childName = childName;
    this.joinedClasses = joinedClasses;
    this.testFlag = true;
    this.userId = userId;
    this.flag = flag;
  }

  public void  execute() {
    
    if (userId != null && childName != null) {

      /*
       * Retrieving user details
       */

      childName = childName.trim();
      childName = UtilString.parseString(childName);
      
      childName = UtilString.changeFirstToCaps(childName);

      ParseUser user = ParseUser.getCurrentUser();
      if (user != null) {

        /*
         * * updating join group info, by adding child name to joined classes
         */
        final List<List<String>> userList = user.getList("joined_groups");
        final List<List<String>> newList = addChildToClass(childName, joinedClasses);

        if (newList != null)
          user.put("joined_groups", newList);

        /*
         * saving the updates
         */
        user.saveEventually();
        
          /*
           * updating memberlist on server
           */

            addChildToMemberList(userId, joinedClasses, childName);
          
          
          if (JoinedClasses.joinedGroups != null)
          {
            if( JoinedClasses.joinedGroups.get(flag).size() >2)
            {
              JoinedClasses.joinedGroups.get(flag).remove(2);
              JoinedClasses.joinedGroups.get(flag).add(2, childName);
            }
          }
          if(JoinedClasses.joinedadapter != null)
            JoinedClasses.joinedadapter.notifyDataSetChanged();
        }
      Utility.toast("Udpated Associated Name");
      } else {
        Utility.toast("Sorry, Update not possible");
      }
    
    if(JoinedClasses.joinedadapter != null)
      JoinedClasses.joinedadapter.notifyDataSetChanged();
  }


  /*
   * update joined group list by adding username
   */
  public static List<List<String>> addChildToClass(String child_name,
     List<String> userClass) {
    
    ParseUser user  = ParseUser.getCurrentUser();
    List<List<String>> joinClasses = user.getList(Constants.JOINED_GROUPS);
    
    List<List<String>> joinedClasses = new ArrayList<List<String>>();
    
    if(joinClasses == null)
      return null;
    
    for(int i=0; i<joinClasses.size(); i++)
    {
      List<String> item = new ArrayList<String>();
      
      for(int j=0; j<joinClasses.get(i).size();j++)
      {
        item.add(joinClasses.get(i).get(j));
      }
      joinedClasses.add(item);
    }
    
    if (userClass != null && joinedClasses != null) {
      
      for (int i = 0; i < joinedClasses.size(); i++) {
        
          if (userClass.get(0).equals(joinedClasses.get(i).get(0))) {
            if(joinedClasses.get(i).size() >2 && joinedClasses.get(i).get(2)!= null)
              joinedClasses.get(i).remove(2);
            joinedClasses.get(i).add(2, child_name);
          }
      }
    }
      return joinedClasses;
  }

  /*
   * Add child to memberList on server
   */
  public static void addChildToMemberList(String userId, List<String> joinedClass, String child) {
 
    ParseQuery<ParseObject> query = ParseQuery.getQuery("GroupMembers");

    if (joinedClass == null || UtilString.isBlank(child) || UtilString.isBlank(userId))
      return;

    Utility.ls("000");
      query.whereEqualTo("code", joinedClass.get(0));
      query.whereEqualTo("emailId", userId);
      try {
        ParseObject obj = query.getFirst();

        
        Utility.ls("111");
        if (obj != null) {
          List<String> boys = obj.getList("children_names");

          if (boys == null)
            boys = new ArrayList<String>();
            
          Utility.ls("3333");
          boys.clear();
          boys.add(child.trim());

          
          Utility.ls("2222");
          
          obj.put("children_names", boys);
          obj.saveEventually();
        }
      } catch (ParseException e) {
      }

    }

}
