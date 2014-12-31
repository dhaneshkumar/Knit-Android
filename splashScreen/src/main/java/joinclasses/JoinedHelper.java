package joinclasses;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import library.UtilString;
import trumplabs.schoolapp.Constants;
import utility.Utility;

/**
 * Contains helper functions for joined class
 * <p/>
 * Created by Dhanesh on 12/23/2014.
 */
public class JoinedHelper {

    /**
     * @param userId
     * @return classrooms suggestion list
     * @how All codegroup classrooms - joined classrooms - removed list classrooms - created classrooms = suggestion list
     */
    public static List<List<String>> getSuggestionList(String userId) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.CODE_GROUP);
        query.fromLocalDatastore();
        query.whereEqualTo("userId", userId);

        List<ParseObject> codeGroupList = null;
        try {
            codeGroupList = query.find();

            if (codeGroupList != null && codeGroupList.size() > 0) {
                List<List<String>> joinedList = ParseUser.getCurrentUser().getList(Constants.JOINED_GROUPS);
                List<List<String>> removedList = ParseUser.getCurrentUser().getList(Constants.REMOVED_GROUPS);
                List<List<String>> createdList = ParseUser.getCurrentUser().getList(Constants.CREATED_GROUPS);

                //removing joined list
                if (joinedList != null) {
                    for (int i = 0; i < joinedList.size(); i++) {
                        for (int j = 0; j < codeGroupList.size(); j++) {
                            String code = codeGroupList.get(j).getString("code");

                            if (!UtilString.isBlank(code)) {
                                if (code.trim().equals(joinedList.get(i).get(0).trim()))
                                    codeGroupList.remove(j);

                            }
                        }

                    }
                }

                //removing removedlist classrooms
                if (removedList != null) {
                    for (int i = 0; i < removedList.size(); i++) {
                        for (int j = 0; j < codeGroupList.size(); j++) {
                            String code = codeGroupList.get(j).getString("code");
                            if (!UtilString.isBlank(code)) {
                                if (code.trim().equals(removedList.get(i).get(0).trim()))
                                    codeGroupList.remove(j);
                            }
                        }

                    }
                }

                //removing createdlist classrooms
                if (createdList != null) {
                    for (int i = 0; i < createdList.size(); i++) {
                        for (int j = 0; j < codeGroupList.size(); j++) {
                            String code = codeGroupList.get(j).getString("code");
                            if (!UtilString.isBlank(code)) {
                                if (code.trim().equals(createdList.get(i).get(0).trim()))
                                    codeGroupList.remove(j);
                            }
                        }

                    }
                }


                //creating new list of suggestions
                List<List<String>> suggestionList = new ArrayList<List<String>>();

                for (int i = 0; i < codeGroupList.size(); i++) {
                    List<String> suggestion = new ArrayList<String>();

                    String code = codeGroupList.get(i).getString("code");
                    String groupName = codeGroupList.get(i).getString("name");

                    if ((!UtilString.isBlank(code)) && (!UtilString.isBlank(groupName))) {
                        suggestion.add(code);
                        suggestion.add(groupName);
                        suggestionList.add(suggestion);
                    }
                }


                return suggestionList;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }


}
