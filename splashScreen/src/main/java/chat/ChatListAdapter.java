package chat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.client.Query;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;
import java.util.Date;

import library.UtilString;
import trumplab.textslate.R;

/**
 * @author greg
 * @since 6/21/13
 *
 * This class is an example of how to use FirebaseListAdapter. It uses the <code>Chat</code> class to encapsulate the
 * data for each individual chat message
 */
public class ChatListAdapter extends FirebaseListAdapter<Chat> {

    // The mUsername for this client. We use this to indicate which messages originated from this user
    private String mUsername;

    public ChatListAdapter(Query ref, Activity activity, int layout, String mUsername) {
        super(ref, Chat.class, layout, activity);
        this.mUsername = mUsername;
    }

    /**
     * Bind an instance of the <code>Chat</code> class to our view. This method is called by <code>FirebaseListAdapter</code>
     * when there is a data change, and we are given an instance of a View that corresponds to the layout that we passed
     * to the constructor, as well as a single <code>Chat</code> instance that represents the current data to bind.
     *
     * @param view A view instance corresponding to the layout we passed to the constructor.
     * @param chat An instance representing the current state of a chat message
     */
    @Override
    protected void populateView(View view, Chat chat) {
        // Map a Chat object to an entry in our listview
        String author = chat.getAuthor();
        Long time = Long.valueOf(chat.getTime());

        boolean received = true;
        if(author.equals(ParseUser.getCurrentUser().getUsername())){
            received = false;
        }

        LinearLayout chatHolder = (LinearLayout) view.findViewById(R.id.holder);
        LinearLayout contentWithBackground = (LinearLayout) view.findViewById(R.id.contentWithBackground);
        TextView messageTV = (TextView) view.findViewById(R.id.message);
        TextView authorTV = (TextView) view.findViewById(R.id.author);
        ImageView attachedIV = (ImageView) view.findViewById(R.id.attachedImage);

        if(received){
            //change content
            contentWithBackground.setBackgroundResource(R.drawable.incoming_message_bg);

            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) contentWithBackground.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            contentWithBackground.setLayoutParams(layoutParams);

            //change text gravity
            layoutParams = (LinearLayout.LayoutParams) messageTV.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            messageTV.setLayoutParams(layoutParams);
            authorTV.setLayoutParams(layoutParams);
            attachedIV.setLayoutParams(layoutParams);
        }
        else{
            //change content
            contentWithBackground.setBackgroundResource(R.drawable.outgoing_message_bg);

            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) contentWithBackground.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            contentWithBackground.setLayoutParams(layoutParams);

            //change text gravity
            layoutParams = (LinearLayout.LayoutParams) messageTV.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            messageTV.setLayoutParams(layoutParams);
            authorTV.setLayoutParams(layoutParams);
            attachedIV.setLayoutParams(layoutParams);
        }

        if(!UtilString.isBlank(chat.getImageData())) {
            byte[] decodedString = Base64.decode(chat.getImageData(), Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            attachedIV.setImageBitmap(bmp);
            attachedIV.setVisibility(View.VISIBLE);
        }
        else{
            attachedIV.setImageBitmap(null);
            attachedIV.setVisibility(View.GONE);
        }

        String timeString = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a").format(new Date(time));

        authorTV.setText(timeString + ": ");
        // If the message was sent by this user, color it differently
        if (author != null && author.equals(mUsername)) {
            authorTV.setTextColor(Color.RED);
        } else {
            authorTV.setTextColor(Color.BLUE);
        }
        messageTV.setText(chat.getMessage());
    }
}
