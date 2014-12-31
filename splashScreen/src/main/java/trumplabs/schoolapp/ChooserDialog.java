package trumplabs.schoolapp;

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

import org.apache.commons.lang3.RandomStringUtils;

import trumplab.textslate.R;
import utility.Utility;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
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

public class ChooserDialog extends DialogFragment implements OnClickListener {
  File imageFile;
  String capturedimagename;
  CommunicatorInterface activity;
  Activity currentActivity;
  boolean flag = false;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    View view =
        getActivity().getLayoutInflater().inflate(R.layout.custom_chooserdialog_layout, null);

    LinearLayout gallerybutton = (LinearLayout) view.findViewById(R.id.galleryclick);
    LinearLayout camerabutton = (LinearLayout) view.findViewById(R.id.cameraclick);

    activity = (CommunicatorInterface) getTargetFragment();
    gallerybutton.setOnClickListener(this);
    camerabutton.setOnClickListener(this);
    builder.setView(view);
    Dialog dialog = builder.create();
    dialog.show();
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
    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    String formattedDate = sdf.format(date);
    capturedimagename = "Capturedimage" + formattedDate + ".jpg";
    imageFile =
        new File(Utility.getWorkingAppDir() + "/media/", "Capturedimage" + formattedDate + ".jpg");

    if (imageFile != null) {
      Uri tempuri = Uri.fromFile(imageFile);
      if (tempuri != null) {
        intent.putExtra(MediaStore.EXTRA_OUTPUT, tempuri);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        startActivityForResult(intent, 100);
      }
    }
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
          if (imageFile.exists()) {
            Utility.savePicInAppFolder(Utility.getWorkingAppDir() + "/media/" + capturedimagename);
            Utility.createThumbnail(getActivity(), capturedimagename);
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
          ClassMsg.progressLayout.setVisibility(View.VISIBLE);
          ClassMsg.sendimgpreview.setVisibility(View.GONE);

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
    getDialog().dismiss();
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

     
      if (cursor != null) {
        cursor.moveToFirst();

        // _display_name contains image name
        int columnIndex = cursor.getColumnIndex("_display_name");

        String fileName = cursor.getString(columnIndex);

        /*
         * photos app assign name of each image as image.jpg. So we randomly assigning same name to
         * it
         */
        if (fileName == null || fileName.trim().equals("image.jpg")) {
          fileName = RandomStringUtils.random(10, true, true);
          fileName += ".jpg";
        }

        imageName = fileName;
        String targetPath = Utility.getWorkingAppDir() + "/media/" + fileName;


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
                  outStream = new BufferedOutputStream(new FileOutputStream(targetPath));
                  byte[] buf = new byte[2048];
                  int len;
                  while ((len = reader.read(buf)) > 0) {
                    outStream.write(buf, 0, len);
                  }

                  if (outStream != null)
                    outStream.close();

                  // creating thumbnail of that image
                  Utility.createThumbnail(currentActivity, fileName);


                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                }
              }
            }
          }
        } else
          flag = true;

        cursor.close();
      } else
        flag = true;


      return null;
    }


    @Override
    protected void onPostExecute(Void res) {
      activity.sendImagePic(imageName);


      // changing visibility option
      ClassMsg.progressLayout.setVisibility(View.GONE);
      ClassMsg.sendimgpreview.setVisibility(View.VISIBLE);


      if (flag) {
        Utility.toast("Connect to Internet for downloading the image");
        flag = false;
      }
    }

  }


}
