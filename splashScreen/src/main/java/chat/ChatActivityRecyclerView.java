package chat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.parse.ParseUser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.ChooserDialog;
import utility.Utility;

public class ChatActivityRecyclerView extends MyActionBarActivity implements ChooserDialog.CommunicatorInterface {


    private String mUsername;
    private String channel;
    public static String notificationChannel;

    private Long startTimeToken = 0L;

    private GoogleCloudMessaging gcm;
    private String gcmRegId;


    private ReclycleAdapter mChatListAdapter;
    private LinearLayoutManager mLayoutManager;

    private String childName;
    private String childId;
    private String classCode;

    LinearLayout imagePreview;
    ImageView attachedImage;
    ImageView removeButton;

    int lastTotalCount = -1;
    RecyclerView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity_recycler_view);

        // Make sure we have a mUsername
        setupUsername();

        lastTotalCount = -1;

        if(getIntent()!= null && getIntent().getExtras() != null)
        {
            classCode = getIntent().getExtras().getString("classCode");
            childName = getIntent().getExtras().getString("childName");
            childId = getIntent().getExtras().getString("childId");
        }

        setTitle("With " + childName + " as " + classCode);

        channel = classCode + "_" + childId;
        notificationChannel = "gcm_" + classCode + "_" + childId;

        //Firebase.goOffline();
        //mFirebaseRef = new Firebase(FIREBASE_URL).child("chat");

        // Setup our input methods. Enter key on the keyboard or pushing the send button
        EditText inputText = (EditText) findViewById(R.id.messageInput);
        inputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_NULL && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    sendMessage();
                }
                return true;
            }
        });

        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        findViewById(R.id.attachButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getSupportFragmentManager();
                ChooserDialog openchooser = new ChooserDialog();
                openchooser.show(fm, "Add Image");
            }
        });

        imagePreview = (LinearLayout) findViewById(R.id.imagePreview);
        attachedImage = (ImageView) findViewById(R.id.attachedImage);
        removeButton = (ImageView) findViewById(R.id.removeButton);

        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagePreview.setTag("");
                imagePreview.setVisibility(View.GONE);
                attachedImage.setImageBitmap(null);

               /* if (typedmsg.getText() != null) {
                    if (typedmsg.getText().length() < 1)
                        sendmsgbutton.setImageResource(R.drawable.send_grey);
                }*/
            }
        });

    }

    @Override
    public void sendImagePic(String imgname) {

        // The image was brought into the App folder hence only name was passed
        imagePreview.setVisibility(View.VISIBLE);
        imagePreview.setTag(Utility.getWorkingAppDir() + "/media/" + imgname);
        File thumbnailFile = new File(Utility.getWorkingAppDir() + "/thumbnail/" + imgname);

        // The thumbnail is already created
        Bitmap myBitmap = BitmapFactory.decodeFile(thumbnailFile.getAbsolutePath());
        attachedImage.setImageBitmap(myBitmap);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout chatHolder;
        LinearLayout contentWithBackground;
        TextView messageTV;
        TextView authorTV;
        ImageView attachedIV;

        public ViewHolder(View view) {
            super(view);

            chatHolder = (LinearLayout) view.findViewById(R.id.holder);
            contentWithBackground = (LinearLayout) view.findViewById(R.id.contentWithBackground);
            messageTV = (TextView) view.findViewById(R.id.message);
            authorTV = (TextView) view.findViewById(R.id.author);
            attachedIV = (ImageView) view.findViewById(R.id.attachedImage);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Setup our view and list adapter. Ensure it scrolls to the bottom as data changes
        listView = (RecyclerView) findViewById(R.id.chatList);

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setStackFromEnd(true);

        mChatListAdapter = new ReclycleAdapter();
        listView.setAdapter(mChatListAdapter);

        listView.setLayoutManager(mLayoutManager);
        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {
                int visibleItemCount = mLayoutManager.getChildCount();
                int totalItemCount = mLayoutManager.getItemCount();
                int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();

                if (totalItemCount > 0 && firstVisibleItem == 0) {
                    if (totalItemCount != lastTotalCount) {
                        //remove duplicate listeners by making sure that another listener is added only if total count changes
                        Log.d("__CHAT_KKP", "Top onScrollNew " + firstVisibleItem + ", lTC=" + lastTotalCount + ", fVI=" + firstVisibleItem + ", vIC=" + visibleItemCount + ", tIC=" + totalItemCount);
                        lastTotalCount = totalItemCount;

                        //moreHistory();

                        /*if (oldQuery != null) {
                            //Log.d("__CHAT_KKP", "removing Listener visibleItemCount=" + visibleItemCount + ", totalItemCount=" + totalItemCount);
                            oldQuery.removeEventListener(mOldListener);
                        }

                        oldQuery = mFirebaseRef.limit(4).orderByKey().endAt(mChatListAdapter.mKeys.get(0)); //key for first item
                        oldQuery.addChildEventListener(mOldListener);*/
                    } else {
                        Log.d("__CHAT_KKQ", "Top onScrollDuplicate " + firstVisibleItem + ", lTC=" + lastTotalCount + ", fVI=" + firstVisibleItem + ", vIC=" + visibleItemCount + ", tIC=" + totalItemCount);
                    }
                }
            }
        });
    }

    public class ReclycleAdapter extends RecyclerView.Adapter<ViewHolder>{
        public List<ChatMessage> mModels;
        public List<String> mKeys;

        public ReclycleAdapter(){
            mModels = new ArrayList<>();
            mKeys = new ArrayList<>();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {

            View row = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.chat_message, viewGroup, false);
            ViewHolder holder = new ViewHolder(row);
            return holder;
        }

        @Override
        public int getItemCount() {
            return mModels.size();
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            ChatMessage chat = mModels.get(position);

            // Map a Chat object to an entry in our listview
            String author = chat.getAuthor();
            Long time = Long.valueOf(chat.getTimeStamp());

            boolean received = true;
            if(author.equals(ParseUser.getCurrentUser().getUsername())){
                received = false;
            }

            LinearLayout chatHolder = holder.chatHolder;
            LinearLayout contentWithBackground = holder.contentWithBackground;
            TextView messageTV = holder.messageTV;
            TextView authorTV = holder.authorTV;
            ImageView attachedIV = holder.attachedIV;

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

    /**
     * Might want to unsubscribe from PubNub here and create background service to listen while
     *   app is not in foreground.
     * PubNub will stop subscribing when screen is turned off for this demo, messages will be loaded
     *   when app is opened through a call to history.
     * The best practice would be creating a background service in onStop to handle messages.
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * Instantiate PubNub object if it is null. Subscribe to channel and pull old messages via
     *   history.
     */
    @Override
    protected void onRestart() {
        super.onRestart();
    }


    private void setupUsername() {
        mUsername = ParseUser.getCurrentUser().getUsername();
    }

    private void sendMessage() {
        //Firebase.goOnline();

        EditText inputText = (EditText) findViewById(R.id.messageInput);
        String message = inputText.getText().toString();
        if(imagePreview.getVisibility() == View.VISIBLE){
            Bitmap bmp = ((BitmapDrawable) attachedImage.getDrawable()).getBitmap();
            ByteArrayOutputStream bYtE = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, bYtE);
            bmp.recycle();
            byte[] byteArray = bYtE.toByteArray();
            String imageData = Base64.encodeToString(byteArray, Base64.DEFAULT);

            ChatMessage chatMsg = new ChatMessage(mUsername, message, System.currentTimeMillis());
            /*try {
                JSONObject json = new JSONObject();
                json.put(ChatConfig.JSON_USER, chatMsg.getAuthor());
                json.put(ChatConfig.JSON_MSG,  chatMsg.getMessage());
                json.put(ChatConfig.JSON_TIME, chatMsg.getTimeStamp());
                publish(ChatConfig.JSON_GROUP, json);
                sendNotification(notificationChannel);

            } catch (JSONException e){ e.printStackTrace();
            }*/

            mChatListAdapter.mModels.add(chatMsg);
            notifyAndSmartScroll(false);

            inputText.setText("");
            imagePreview.setVisibility(View.GONE);
            attachedImage.setImageBitmap(null);
        }
        else if (!message.equals("")) {
            ChatMessage chatMsg = new ChatMessage(mUsername, message, System.currentTimeMillis());
            /*try {
                JSONObject json = new JSONObject();
                json.put(ChatConfig.JSON_USER, chatMsg.getAuthor());
                json.put(ChatConfig.JSON_MSG,  chatMsg.getMessage());
                json.put(ChatConfig.JSON_TIME, chatMsg.getTimeStamp());
                publish(ChatConfig.JSON_GROUP, json);
                sendNotification(notificationChannel);
            } catch (JSONException e){ e.printStackTrace();
            }*/

            mChatListAdapter.mModels.add(chatMsg);
            notifyAndSmartScroll(false);

            inputText.setText("");
        }
    }

    void notifyAndSmartScroll(boolean isOldQuery){
        if(isOldQuery) {
            int prevPos = mLayoutManager.findFirstVisibleItemPosition();
            int offset = 0;

            if (prevPos >= 0 && listView.getChildAt(prevPos) != null) {
                offset = listView.getChildAt(prevPos).getTop() - listView.getPaddingTop();
            }

            mChatListAdapter.notifyDataSetChanged();
            Log.d("__CHAT_KS", "smartNotifyAndScroll() old Query");
            mLayoutManager.scrollToPositionWithOffset(mChatListAdapter.getItemCount() - lastTotalCount, offset);
        }
        else{
            Log.d("__CHAT_KS", "smartNotifyAndScroll() new Query");
            mChatListAdapter.notifyDataSetChanged();
            mLayoutManager.scrollToPosition(mChatListAdapter.getItemCount() - 1);
        }
    }

}

















