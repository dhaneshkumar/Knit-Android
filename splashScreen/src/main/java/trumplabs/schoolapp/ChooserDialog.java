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
import java.util.Random;

import trumplab.textslate.R;
import utility.Config;
import utility.ScalingUtilities;
import utility.Utility;

/**
 * Show dialog to select photo from gallery or camera
 */
public class ChooserDialog extends DialogFragment implements OnClickListener {
  File imageFile;
  String imageFileName;
  CommunicatorInterface activity;
  Activity currentActivity;
  boolean flag = false;
    boolean profileCall;  //It tells about caller activity : SendMessage(false) or ProfilePage(true)

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

    if(Config.SHOWLOG) Log.d("camera", "started camera.............");

    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    imageFileName = Utility.getUniqueImageName();

    if(Config.SHOWLOG) Log.d("camera", imageFileName);

    imageFile = new File(Utility.getWorkingAppDir() + "/media/", imageFileName);

    Uri tempUri = Uri.fromFile(imageFile);

    if(Config.SHOWLOG) Log.d("camera", tempUri.toString());
    intent.putExtra(MediaStore.EXTRA_OUTPUT, tempUri);
    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);   //high(1) or low(0) quality images
    startActivityForResult(intent, 100);
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

              String filePath = Utility.getWorkingAppDir() + "/media/" + imageFileName;

              ScalingUtilities.scaleAndSave(filePath, filePath);

              Utility.createThumbnail(getActivity(), imageFileName);

              activity.sendImagePic(imageFileName);
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
              if (ComposeMessage.picProgressBarLayout != null)
                  ComposeMessage.picProgressBarLayout.setVisibility(View.VISIBLE);

              if (ComposeMessage.sendimgpreview != null)
                  ComposeMessage.sendimgpreview.setVisibility(View.GONE);
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

      boolean retrieveSuccess = false; //succesfully retrieved and saved from uri(local or cloud)

      imageName = Utility.getUniqueImageName();

      String imagePath = Utility.getWorkingAppDir() + "/media/" + imageName;

      //Storing file locally in /media folder
      ParcelFileDescriptor parcelFileDescriptor = null;
      try {
        parcelFileDescriptor = currentActivity.getContentResolver().openFileDescriptor(uri, "r");
      } catch (FileNotFoundException e1) {
      }

      if (parcelFileDescriptor != null) {
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

        if (fileDescriptor != null) {
          InputStream inputStream = new FileInputStream(fileDescriptor);
          BufferedInputStream reader = new BufferedInputStream(inputStream);

          // Create an output stream to a file that you want to save to
          BufferedOutputStream outStream;
          try {
            outStream = new BufferedOutputStream(new FileOutputStream(imagePath));
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
        }
      } else
        flag = true;

      if (retrieveSuccess) {
        ScalingUtilities.scaleAndSave(imagePath, imagePath); //overwrite
        // creating thumbnail of that image
        Utility.createThumbnail(currentActivity, imageName);
      }
      else{
        Utility.toast("Failed to load image!");
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void res) {
      activity.sendImagePic(imageName);

      // changing visibility option
        if(!profileCall) {
            if (ComposeMessage.picProgressBarLayout != null)
                ComposeMessage.picProgressBarLayout.setVisibility(View.GONE);

            if (ComposeMessage.sendimgpreview != null) {
                ComposeMessage.sendimgpreview.setVisibility(View.VISIBLE);
            }
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
