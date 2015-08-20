package chat;

import android.util.Log;

import com.pubnub.api.Callback;
import com.pubnub.api.PubnubError;

/**
 * Created by GleasonK on 7/15/15.
 */
public class BasicCallback extends Callback {
    String caller;
    public BasicCallback(String caller){
        this.caller = caller;
    }

    @Override
    public void successCallback(String channel, Object response) {
        Log.d("__CHAT " + caller, "Success: " + response.toString());
    }

    @Override
    public void connectCallback(String channel, Object message) {
        Log.d("__CHAT " + caller, "Connect: " + message.toString());
    }

    @Override
    public void errorCallback(String channel, PubnubError error) {
        Log.d("__CHAT " + caller, "Error: " + error.toString());
    }
}
