package utility;

import android.os.Build;

public class HelperFunctions {
  
  public static String getAndroidVersion() {
    String release = Build.VERSION.RELEASE;
    int sdkVersion = Build.VERSION.SDK_INT;
    return sdkVersion + " - " + release;
}

}
