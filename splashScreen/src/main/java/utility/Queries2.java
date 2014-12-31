package utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import library.UtilString;
import trumplabs.schoolapp.ClassMembers;
import trumplabs.schoolapp.Classrooms;
import trumplabs.schoolapp.Constants;

import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class Queries2 {

  public boolean isCodegroupExist(String code, String userId) {
    ParseQuery<ParseObject> query = ParseQuery.getQuery("Codegroup");
    query.fromLocalDatastore();
    query.whereEqualTo("code", code);
    query.whereEqualTo("userId", userId);

      Utility.ls(code + " : " + userId + " doesn't exist" );
    ParseObject obj;
    try {
      obj = query.getFirst();

      if (obj != null) {

          Utility.ls("object extidtrd ================================");
          return true;
      }

    } catch (ParseException e) {

      e.printStackTrace();
      return false;
    }

    return false;
  }

  public void updateProfileImage(String code, String userId) throws ParseException {
    /*
     * Retrieving updated pic name
     */
    ParseQuery<ParseObject> query = ParseQuery.getQuery("Codegroup");
    query.whereEqualTo("code", code);

    ParseObject obj = query.getFirst();

    if (obj != null) {
      String newPic = obj.getString("picName");

      if (newPic != null)
        Utility.ls("new pic :" + newPic);


      String senderId = obj.getString("senderId");
      // senderId = senderId.replaceAll("\\.", "");
      senderId = senderId.replaceAll("@", "");
      ParseFile senderPic = obj.getParseFile("senderPic");

      /*
       * Retrieving local pic name
       */
      ParseQuery<ParseObject> query1 = ParseQuery.getQuery("Codegroup");
      query1.fromLocalDatastore();
      query1.whereEqualTo("code", code);
      query1.whereEqualTo("userId", userId);

      ParseObject localObj = query1.getFirst();

      String oldPic = null;
      if (localObj != null)
        oldPic = localObj.getString("picName");


      if (UtilString.isBlank(oldPic)) {


        if (newPic != null)
          localObj.put("picName", newPic);

        if (senderPic != null)
          localObj.put("senderPic", senderPic);

        localObj.pin();

        downloadProfileImage(senderId, senderPic);
      } else if ((!UtilString.isBlank(oldPic)) && (!UtilString.isBlank(newPic))) {

        if (!oldPic.equals(newPic)) {
          localObj.put("picName", newPic);
          localObj.put("senderPic", senderPic);
          localObj.pin();


          downloadProfileImage(senderId, senderPic);
        } else {
          final File senderThumbnailFile =
              new File(Utility.getWorkingAppDir() + "/thumbnail/" + senderId + "_PC.jpg");
          if (!senderThumbnailFile.exists()) {

            downloadProfileImage(senderId, senderPic);
          }

        }

      } else {
        final File senderThumbnailFile =
            new File(Utility.getWorkingAppDir() + "/thumbnail/" + senderId + "_PC.jpg");
        if (!senderThumbnailFile.exists()) {

          downloadProfileImage(senderId, senderPic);
        }
      }

    }


  }


  public static void downloadProfileImage(final String senderId, ParseFile senderImagefile) {

    // System.out.println("start downloading");
    // System.out.println(senderImagefile);
    // System.out.println(senderId);

    // Utility.toast(senderId);

    if (senderImagefile != null && (!UtilString.isBlank(senderId))) {
      senderImagefile.getDataInBackground(new GetDataCallback() {
        public void done(byte[] data, ParseException e) {
          if (e == null) {
            // ////Image download successful
            FileOutputStream fos;
            try {
              fos =
                  new FileOutputStream(Utility.getWorkingAppDir() + "/thumbnail/" + senderId
                      + "_PC.jpg");
              try {
                fos.write(data);
              } catch (IOException e1) {
                e1.printStackTrace();
              } finally {
                try {
                  fos.close();
                } catch (IOException e1) {
                  e1.printStackTrace();
                }
              }

            } catch (FileNotFoundException e2) {
              e2.printStackTrace();
            }

            /*
             * Bitmap mynewBitmap = BitmapFactory.decodeFile(senderThumbnailFile.getAbsolutePath());
             * classimg.setImageBitmap(mynewBitmap);
             */

            // Might be a problem when net is too slow :/
            System.out.println("Profile Image Downloaded"); // ************************************
          } else {
            // Image not downloaded
            System.out.println("Profile Image not Downloaded"); // **********************************
          }
        }
      });
    }


  }

  public void storeCodegroup(String code, String userId) {
    ParseQuery<ParseObject> query = ParseQuery.getQuery("Codegroup");
    query.whereEqualTo("code", code);

      ParseObject obj = null;
      try {
          obj = query.getFirst();
      } catch (ParseException e) {
          e.printStackTrace();
      }

      if (obj != null) {


          obj.put("userId", userId);
          try {
              obj.pin();
          } catch (ParseException e) {
              e.printStackTrace();
          }


      String senderId = obj.getString("senderId");
      // senderId = senderId.replaceAll("\\.", "");
      senderId = senderId.replaceAll("@", "");

      downloadProfileImage(senderId, obj.getParseFile("senderPic"));
    }
  }


  public boolean isGroupMemberExist(String code, String userId) throws ParseException {
    ParseQuery<ParseObject> query = ParseQuery.getQuery("GroupMembers");
    query.fromLocalDatastore();
    query.whereEqualTo("code", code);
    query.whereEqualTo("userId", userId);
    query.whereEqualTo("emailId", userId);
    ParseObject obj = query.getFirst();

    if (obj != null) {
      return true;
    }

    return false;
  }


  public static void storeGroupMember(String code, String userId, boolean adapterFlag)
      throws ParseException {
    ParseQuery<ParseObject> query = ParseQuery.getQuery("GroupMembers");
    query.whereEqualTo("code", code);
    query.whereEqualTo("emailId", userId);

    ParseObject obj = query.getFirst();

    if (obj != null) {
      if (obj.getCreatedAt() != null) {


        ParseObject joinedObj = new ParseObject("JoinedTiming");
        joinedObj.put("objectId", obj.getObjectId());
        joinedObj.put("code", code);
        joinedObj.put("joiningTime", obj.getCreatedAt());
        joinedObj.put("userId", userId);

        joinedObj.pin();

     //   Utility.ls(obj.getCreatedAt() + "joining time");

        if (adapterFlag) {
          if (ClassMembers.myadapter != null)
            ClassMembers.myadapter.notifyDataSetChanged();

          if (Classrooms.myadapter != null)
            Classrooms.myadapter.notifyDataSetChanged();
        }

      }
    } else {
      Utility.ls("null object");
    }
  }


  public static Date getGroupJoinedTime(String code, String userId) throws ParseException {
    ParseQuery<ParseObject> query = ParseQuery.getQuery("JoinedTiming");
    query.fromLocalDatastore();

    query.whereEqualTo("code", code.trim());
    query.whereEqualTo("userId", userId.trim());

    ParseObject obj = query.getFirst();

    if (obj != null)
      return (Date) obj.get("joiningTime");

    return null;
  }


  public boolean isItemExist(List<ParseObject> groupDetails, ParseObject item) {
    if (groupDetails == null)
      return false;

    String itemTitle = item.getString("title");
    Date itemDate = item.getCreatedAt();

    for (int i = 0; i < groupDetails.size(); i++) {
      String title = groupDetails.get(i).getString("title");
      Date date = groupDetails.get(i).getCreatedAt();

      if (!UtilString.isBlank(itemTitle)) {
        if (title.trim().equals(itemTitle.trim()) && date == itemDate)
          return true;
      }
    }

    return false;
  }


  public static void increaseLikeCount(final ParseObject obj) {

    obj.fetchInBackground(new GetCallback<ParseObject>() {

      @Override
      public void done(ParseObject obj, ParseException e) {

        if (e == null) {
          int likeCount = obj.getInt(Constants.LIKE_COUNT);
          final int kk = ++likeCount;

          obj.put(Constants.LIKE_COUNT, kk);
          obj.saveInBackground(new SaveCallback() {

            @Override
            public void done(ParseException e) {

              /*
               * object got updated on parse
               */
              if (e == null) {

                /*
                 * ParseQuery<ParseObject> query = ParseQuery.getQuery(Constants.GROUP_DETAILS);
                 * query.fromLocalDatastore(); query.getInBackground(objectId, new
                 * GetCallback<ParseObject>() {
                 * 
                 * @Override public void done(ParseObject obj, ParseException e) {
                 * 
                 * if (e == null) { obj.put(Constants.LIKE_COUNT, kk); obj.pinInBackground();
                 * 
                 * 
                 * if(Messages.myadapter != null) Messages.myadapter.notifyDataSetChanged(); } }});
                 */
              }
            }
          });
        }
      }
    });
  }

  public static String getSchoolName(ParseUser user) {
    String schoolId = user.getString("school");

    if (schoolId != null) {

      // retrieving school name from its id
      ParseQuery<ParseObject> query = ParseQuery.getQuery("SCHOOLS");
      query.fromLocalDatastore();
      query.whereEqualTo("objectId", schoolId);

      ParseObject obj = null;
      try {
        obj = query.getFirst();
      } catch (ParseException e) {
      }

      String school = null;

      if (obj != null)
        school = obj.getString("school_name");

      return school;
    }

    return null;
  }
}
