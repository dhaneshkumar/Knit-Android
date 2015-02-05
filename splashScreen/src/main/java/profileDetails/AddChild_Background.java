package profileDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import joinclasses.JoinedClasses;
import library.UtilString;
import trumplabs.schoolapp.Constants;
import utility.Utility;

import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

/*
 * Adding child to a new joined group
 */

public class AddChild_Background {
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

    public void execute() {

        ParseUser user = ParseUser.getCurrentUser();
        if (userId != null && childName != null && ParseUser.getCurrentUser() != null) {

      /*
       * Retrieving user details
       */

            childName = childName.trim();
            childName = UtilString.parseString(childName);

            childName = UtilString.changeFirstToCaps(childName);

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("childName", childName);

            boolean isNameUpdate = false;
            try {

                isNameUpdate = ParseCloud.callFunction("updateMemberName", params);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (isNameUpdate) {
                try {
                    user.fetch();
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                Utility.toast("Udpated Associated Name");

                JoinedClasses.joinedGroups = ParseUser.getCurrentUser().getList("joined_groups");

                if (JoinedClasses.joinedadapter != null)
                    JoinedClasses.joinedadapter.notifyDataSetChanged();
            } else {
                Utility.toast("Sorry, Update not possible");
            }

        }
    }
}