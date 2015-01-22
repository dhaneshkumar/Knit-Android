package joinclasses;

import android.os.AsyncTask;
import android.view.View;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import library.UtilString;
import profileDetails.ProfilePage;
import trumplabs.schoolapp.Constants;
import utility.Utility;

/**
 * Contain Functions required for school name updation.
 * <p/>
 * Created by Dhanesh on 12/24/2014.
 */
public class School {

    /**
     * @return list of all schools present in local database
     * @how query into local storage and return all school list.
     */
    public static List<String> getSchoolList() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("SCHOOLS");
        query.fromLocalDatastore();
        query.orderByAscending("school_name");

        List<String> schoolNames = null;
        try {
            List<ParseObject> schoolList = query.find();

            if (schoolList != null) {
                schoolNames = new ArrayList<String>();

                for (int i = 0; i < schoolList.size(); i++) {
                    ParseObject obj = schoolList.get(i);

                    if (obj != null && obj.getString("school_name") != null)
                        schoolNames.add(obj.getString("school_name").trim());
                }
            }
        } catch (ParseException e) {
        }
        return schoolNames;
    }

    /**
     * @param schoolId
     * @return school's name corresponding to that schoolId
     * @how query from local storage and returns the school name
     */
    public String getSchoolName(String schoolId) {
        if (UtilString.isBlank(schoolId))
            return null;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("SCHOOLS");
        query.fromLocalDatastore();
        query.whereEqualTo("objectId", schoolId);

        ParseObject obj = null;
        try {
            obj = query.getFirst();
            if (obj != null)
                return obj.getString("school_name");
            else
                return schoolId;
        } catch (ParseException e) {
            e.printStackTrace();

        }
        return schoolId;

    }

    /**
     * @param schoolName
     * @return schoolId if school exist in local database  else return null
     * @how query from local database table <SCHOOLS>, if result is not null then it returns objectId of school else returns null
     */
    public static String schoolIdExist(String schoolName) {
        if (schoolName == null)
            return null;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("SCHOOLS");
        query.fromLocalDatastore();
        query.whereEqualTo("school_name", schoolName.trim());

        ParseObject obj = null;
        try {
            obj = query.getFirst();
        } catch (ParseException e) {
        }
        if (obj != null)
            return obj.getObjectId();


        return null;
    }


    /**
     * @param schoolName
     * @return schoolId of given school name
     * @throws ParseException
     * @how query into local database table "SCHOOLS", if result is null then it query on the server in the same table
     * and then store it locally and return the schoolId
     */
    public static String getSchoolObjectId(String schoolName) throws ParseException {
        if (schoolName == null)
            return null;

        //querying locally
        ParseQuery<ParseObject> query = ParseQuery.getQuery("SCHOOLS");
        query.fromLocalDatastore();
        query.whereEqualTo("school_name", schoolName.trim());

        ParseObject obj = null;
        try {
            obj = query.getFirst();
        } catch (ParseException e) {
        }
        if (obj != null)
            return obj.getObjectId();
        else {

            ParseObject school = new ParseObject("SCHOOLS");
            school.put("school_name", schoolName);
            school.save();          //storing on server
            school.pin();           //storing locally

            if (school != null && school.getObjectId() != null)
                return school.getObjectId();
        }

        return null;
    }


    /**
     * @param schoolId
     * @param standard
     * @param division
     * @action given these inputs, it find all classes with same parameters in "codegroup" table and store locally
     */
    public static void storeSuggestions(String schoolId, String standard, String division, String userId) {
        if (UtilString.isBlank(schoolId) || UtilString.isBlank(standard) || UtilString.isBlank(division))
            return;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Codegroup");
        query.whereEqualTo(Constants.DIVISION, division);
        query.whereEqualTo("standard", standard);
        query.whereEqualTo("school", schoolId);
        query.whereEqualTo("classExist", true);

        List<ParseObject> suggestions = null;
        try {

            suggestions = query.find();

            if (suggestions != null) {
                for (int i = 0; i < suggestions.size(); i++) {
                    ParseObject obj = suggestions.get(i);
                    obj.put("userId", userId);
                    obj.pin();


                     // Utility.ls("suggestions : " + obj.getString("code") + " : " + obj.getString("name"));
                }
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    /**
     * Stores new schools's name on server in background
     */
    public static class UpdateSchoolOnServer extends AsyncTask<String, Void, String> {
        private String schoolName;

        public UpdateSchoolOnServer(String schoolName) {
            this.schoolName = schoolName;
        }

        @Override
        protected String doInBackground(String... params) {

            if (params.length == 0)
                return null;

            String schoolId = null;
            try {
                schoolId = School.getSchoolObjectId(params[0]);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            return schoolId;
        }


        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                ParseUser user = ParseUser.getCurrentUser();
                if (user != null) {

                    if (!UtilString.isBlank(schoolName))
                        ProfilePage.school_textView.setText(schoolName);


                    user.put(Constants.SCHOOL, result); //updating user's school
                    user.saveEventually();
                    ProfilePage.school = result;  // updating text of autocomplete textview
                    ProfilePage.progressBarLayout.setVisibility(View.GONE);  //setting progressbar gone
                    ProfilePage.profileLayout.setVisibility(View.VISIBLE);

                } else
                    {Utility.logout(); return;}
            }
        }
    }

}
