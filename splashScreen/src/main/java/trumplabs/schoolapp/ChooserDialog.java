package trumplabs.schoolapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import org.apache.commons.lang3.RandomStringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import trumplab.textslate.R;
import utility.ScalingUtilities;
import utility.Utility;

/**
 * Show dialog to select photo from gallery or camera
 */
public class ChooserDialog extends DialogFragment implements OnClickListener {
  File imageFile;
  String capturedimagename;
  CommunicatorInterface activity;
  Activity currentActivity;
  boolean flag = false;
    boolean profileCall;  //It tells about caller activity : SendMessage(false) or ProfilePage(true)
    String filePath;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    View view =
        getActivity().getLayoutInflater().inflate(R.layout.custom_chooserdialog_layout, null);

    LinearLayout gallerybutton = (LinearLayout) view.findViewById(R.id.galleryclick);
    LinearLayout camerabutton = (LinearLayout) view.findViewById(R.id.cameraclick);

    activity = (CommunicatorInterface) getActivity();
    gallerybutton.setOnClickListener(this);
    camerabutton.setOnClickListener(this);
    builder.setView(view);
    Dialog dialog = builder.create();
    dialog.setCanceledOnTouchOutside(true);
    dialog.show();

      if(getArguments() != null) {
          String flag = getArguments().getString("flag");
          if(flag.equals("PROFILE"))
          {
              profileCall = true;
          }
          else
              profileCall = false;
      }
      else
          profileCall = false;

    return dialog;
  }


  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.galleryclick:
        sendGalleryPic();
        break;
      case R.id.cameraclick:
        takePicture();
        break;
      default:
        break;
    }
  };

  /**
   * pick images from gallery
   */
  private void sendGalleryPic() {
    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
    photoPickerIntent.setType("image/*");
    startActivityForResult(photoPickerIntent, 101);
  }

  /**
   * pick images from camera
   */
  private void takePicture() {

    Log.d("camera", "started camera.............");

    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    String formattedDate = sdf.format(date);
    capturedimagename = "Capturedimage" + formattedDate + ".jpg";

    Log.d("camera", capturedimagename);

    imageFile =
        new File(Utility.getWorkingAppDir() + "/media/", "Capturedimage" + formattedDate + ".jpg");

    if (imageFile != null) {
      Uri tempuri = Uri.fromFile(imageFile);
      if (tempuri != null) {

          Log.d("camera", tempuri.toString());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, tempuri);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);   //high(1) or low(0) quality images
        startActivityForResult(intent, 100);
      }
        else
          Log.d("camera", " tempuri null");
    }
      else
        Log.d("camera", " null imagefile ");
  }

  /**
   * return activity result
   */
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);

    // pics taken using camera
    if (requestCode == 100) {
      switch (resultCode) {
        case Activity.RESULT_OK:
          if (imageFile != null && imageFile.exists()) {

              filePath = Utility.getWorkingAppDir() + "/media/" + capturedimagename;

              Utility.savePicInAppFolder(filePath);
              Utility.createThumbnail(getActivity(), capturedimagename);

            /*  if(profileCall) {
                  Uri selectedImg = intent.getData();

                 // doCrop(selectedImg);
              }*/



              activity.sendImagePic(capturedimagename);
          }


          break;
        case Activity.RESULT_CANCELED:
          break;
      }
    } else if (requestCode == 101) { // pics taken from gallery
      switch (resultCode) {
        case Activity.RESULT_OK:

          Uri selectedImage = intent.getData();
          currentActivity = getActivity();


          // changing visibility option of progressbar and imageview

          if(!profileCall) {
              if (SendMessage.picProgressBarLayout != null)
                  SendMessage.picProgressBarLayout.setVisibility(View.VISIBLE);

              if (SendMessage.sendimgpreview != null)
                  SendMessage.sendimgpreview.setVisibility(View.GONE);
          }
          /*
           * Executing background class to load image in imageview from gallery and photos app
           */
          loadImage loadimage = new loadImage(selectedImage);
          loadimage.execute();


          break;
        case Activity.RESULT_CANCELED:
          break;
      }
    }
    else if (requestCode == 110) {
          switch (resultCode) {
              case Activity.RESULT_OK:
                  if (imageFile != null && imageFile.exists()) {

                      activity.sendImagePic(capturedimagename);
                  }


                  break;
              case Activity.RESULT_CANCELED:
                  break;
          }



      }
      getDialog().dismiss();
  }


    public void doCrop(Uri uriOfImageToCrop) {
        Log.d("DEBUG_PROFILE_PAGE", "into doCrop");
        final Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setData(uriOfImageToCrop);
        intent.putExtra("outputX", 200);
        intent.putExtra("outputY", 200);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("scale", true);
        intent.putExtra("noFaceDetection", true);

        File file = new File(filePath);
        if (file.exists())
            file.delete();
        intent.putExtra("output", Uri.fromFile(file));
        startActivityForResult(intent, 110);
    }



  public interface CommunicatorInterface {
    void sendImagePic(String imgname);
  }


  /**
   * loading image in background fetching image from photos app then loading in imageviewer
   */
  class loadImage extends AsyncTask<Void, Void, Void> {
    private Uri uri;
    private String imageName;

    loadImage(Uri uri) {
      this.uri = uri;
    }

    @Override
    protected Void doInBackground(Void... params) {

      Cursor cursor = currentActivity.getContentResolver().query(uri, null, null, null, null);

      boolean retrieveSuccess = false; //succesfully retrieved and saved from uri(local or cloud)
        String tempTargetPath = null;
        String targetPath = null;
        String fileName = null;

      if (cursor != null) {
        cursor.moveToFirst();

        // _display_name contains image name
        int columnIndex = cursor.getColumnIndex("_display_name");

        fileName = cursor.getString(columnIndex);

        /*
         * photos app assign name of each image as image.jpg. So we randomly assigning same name to
         * it
         */
        if (fileName == null || fileName.trim().equals("image.jpg")) {
          fileName = RandomStringUtils.random(10, true, true);
          fileName += ".jpg";
        }

        imageName = fileName;
        tempTargetPath = Utility.getWorkingAppDir() + "/media/" + fileName + "_temp.jpg";
        targetPath = Utility.getWorkingAppDir() + "/media/" + fileName;

        /*
         * Storing file locally in /media folder
         */

        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
          parcelFileDescriptor = currentActivity.getContentResolver().openFileDescriptor(uri, "r");
        } catch (FileNotFoundException e1) {
        }

        if (parcelFileDescriptor != null) {
          FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

          if (fileDescriptor != null) {
            InputStream inputStream = new FileInputStream(fileDescriptor);

            if (inputStream != null) {
              BufferedInputStream reader = new BufferedInputStream(inputStream);

              // Create an output stream to a file that you want to save to

              if (reader != null) {
                BufferedOutputStream outStream;
                try {
                  outStream = new BufferedOutputStream(new FileOutputStream(tempTargetPath));
                  byte[] buf = new byte[2048];
                  int len;
                  while ((len = reader.read(buf)) > 0) {
                    outStream.write(buf, 0, len);
                  }

                  if (outStream != null)
                    outStream.close();


                    retrieveSuccess = true;



                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                }
                finally {
                    cursor.close();
                }
              }
            }
          }
        } else
          flag = true;

      } else
        flag = true;

      if(retrieveSuccess){
          ScalingUtilities.scaleAndSave(tempTargetPath, targetPath); //overwrite
          // creating thumbnail of that image
          Utility.createThumbnail(currentActivity, fileName);
          //delete the temporary image file from knit/media folder
          File tempImageFile = new File(tempTargetPath);
          if(tempImageFile.exists()){
              tempImageFile.delete();
          }
      }

      return null;
    }


    @Override
    protected void onPostExecute(Void res) {
      activity.sendImagePic(imageName);


      // changing visibility option
        if(!profileCall) {
            if (SendMessage.picProgressBarLayout != null)
                SendMessage.picProgressBarLayout.setVisibility(View.GONE);

            if (SendMessage.sendimgpreview != null)
                SendMessage.sendimgpreview.setVisibility(View.VISIBLE);
        }

      if (flag) {
        Utility.toast("Connect to Internet for downloading the image");
        flag = false;
      }
    }

  }


    /** getResizedBitmap method is used to Resized the Image according to custom width and height
     * @param image
     * @param newHeight (new desired height)
     * @param newWidth (new desired Width)
     * @return image (new resized image)
     * */
    public static Bitmap getResizedBitmap(Bitmap image, int newHeight, int newWidth) {
        int width = image.getWidth();
        int height = image.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);
        // recreate the new Bitmap
        Bitmap resizedBitmap = Bitmap.createBitmap(image, 0, 0, width, height,
                matrix, false);
        return resizedBitmap;
    }


}
