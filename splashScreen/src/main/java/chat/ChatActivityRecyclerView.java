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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ServerValue;
import com.firebase.client.ValueEventListener;
import com.onesignal.OneSignal;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import baseclasses.MyActionBarActivity;
import library.UtilString;
import trumplab.textslate.R;
import trumplabs.schoolapp.Application;
import trumplabs.schoolapp.ChooserDialog;
import utility.Utility;

public class ChatActivityRecyclerView extends MyActionBarActivity implements ChooserDialog.CommunicatorInterface {
    boolean channelSet = false;

    private String channelId;
    private String coreTitle;
    private String mUsername;

    private String opponentStatus;
    static final String NEVER = "[last : Never]";
    static final String ONLINE = "[online]";

    private String opponentOneSignalId;

    private Firebase mFirebaseRef;

    private Firebase mConnectedRef;
    private ValueEventListener mConnectedListener;

    private Firebase opponentStatusRef;
    private ValueEventListener opponentStatusListener;

    Query newQuery;
    public ChildEventListener mNewListener; //for new messages

    Query oldQuery;
    public ChildEventListener mOldListener; //to fetch old messages as we scroll up
    //Firebase end

    private ReclycleAdapter mChatListAdapter;
    private LinearLayoutManager mLayoutManager;

    private String classCode;
    private String opponentName;
    private String opponentParseUsername;
    private String chatAs; //either ChatConfig.TEACHER or ChatConfig.NON_TEACHER

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
            chatAs = getIntent().getExtras().getString("chatAs");
            classCode = getIntent().getExtras().getString("classCode");
            opponentName = getIntent().getExtras().getString("opponentName");
            opponentParseUsername = getIntent().getExtras().getString("opponentParseUsername");
        }


        coreTitle = opponentName;
        opponentStatus = NEVER;
        setTitle(coreTitle + opponentStatus);

        // Setup our Firebase mFirebaseRef - chat rooms is <classCode>_<childId>  (childId is just emailId column)
        if(chatAs.equals(ChatConfig.TEACHER)) {
            //childId is parent's parse username
            channelId = classCode + "-" + opponentParseUsername;
        }
        else{
            //childId is just my parse username
            channelId = classCode + "-" + mUsername;
        }

        Log.d("__CHAT onCreate", "me=" + mUsername + ", oppName=" + opponentName + ", oppParseUN=" + opponentParseUsername);
        Log.d("__CHAT onCreate", "channelId=" + channelId);

        mFirebaseRef = new Firebase(ChatConfig.FIREBASE_URL).child("Chats").child(channelId);
        mFirebaseRef.keepSynced(true);

        // Setup our input methods. Enter key on the keyboard or pushing the send button
        EditText inputText = (EditText) findViewById(R.id.messageInput);

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

        //add base query listener
        mNewListener = new MyChildEventListener(listView, mChatListAdapter, false);
        mOldListener = new MyChildEventListener(listView, mChatListAdapter, true);

        //set new msg count to 0
        ChatRoomsActivity.RoomDetail roomDetail = ChatRoomsActivity.roomMap.get(channelId);

        if(roomDetail != null){
            roomDetail.newMsgs = 0;
            ChatRoomsActivity.MyRoomListener.notifyAdapter();
        }

        newQuery = mFirebaseRef.limit(8);
        newQuery.addChildEventListener(mNewListener);

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

                        if (oldQuery != null) {
                            //Log.d("__CHAT_KKP", "removing Listener visibleItemCount=" + visibleItemCount + ", totalItemCount=" + totalItemCount);
                            oldQuery.removeEventListener(mOldListener);
                        }

                        oldQuery = mFirebaseRef.limit(4).orderByKey().endAt(mChatListAdapter.mKeys.get(0)); //key for first item
                        oldQuery.addChildEventListener(mOldListener);
                    } else {
                        Log.d("__CHAT_KKQ", "Top onScrollDuplicate " + firstVisibleItem + ", lTC=" + lastTotalCount + ", fVI=" + firstVisibleItem + ", vIC=" + visibleItemCount + ", tIC=" + totalItemCount);
                    }
                }
            }
        });

        mFirebaseRef.getRoot().child("Users").child(opponentParseUsername).child("oneSignalId").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                opponentOneSignalId = (String) dataSnapshot.getValue();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

        mConnectedRef = mFirebaseRef.getRoot().child(".info/connected");
        // Finally, a little indication of connection status
        mConnectedListener = mConnectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("__CHAT myConn", dataSnapshot + "");
                boolean connected = (Boolean) dataSnapshot.getValue();
                if (connected) {
                    Firebase myStatusRef = mFirebaseRef.getRoot().child("Users").child(mUsername).child("status");
                    Toast.makeText(ChatActivityRecyclerView.this, "Connected to Firebase", Toast.LENGTH_SHORT).show();
                    myStatusRef.setValue(new Chat.ConnectionStatusProxy(true, null));
                    myStatusRef.onDisconnect().setValue(new Chat.ConnectionStatusProxy(null, ServerValue.TIMESTAMP));
                } else {
                    Toast.makeText(ChatActivityRecyclerView.this, "Disconnected from Firebase", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                // No-op
            }
        });

        opponentStatusRef = mFirebaseRef.getRoot().child("Users").child(opponentParseUsername).child("status");
        opponentStatusListener = opponentStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //don't take this value into "boolean" as it could be null and would crash while convering null Boolean to boolean
                Log.d("__CHAT opponent", dataSnapshot + "");
                Chat.ConnectionStatus connStatus = dataSnapshot.getValue(Chat.ConnectionStatus.class);

                if (connStatus != null) {
                    if (connStatus.online != null) {
                        opponentStatus = ONLINE;
                    } else if (connStatus.lastOnline != null) {
                        String timeString = new SimpleDateFormat(ChatConfig.LAST_SEEN_FORMAT).format(new Date(connStatus.lastOnline));
                        opponentStatus = "[last : " + timeString + "]";
                    } else {
                        opponentStatus = NEVER;
                    }
                } else {
                    opponentStatus = NEVER;
                }

                Application.applicationHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (ChatActivityRecyclerView.this != null) {
                            ChatActivityRecyclerView.this.setTitle(coreTitle + opponentStatus);
                        }
                    }
                });
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    class MyChildEventListener implements ChildEventListener{
        RecyclerView list;
        ReclycleAdapter adapter;
        boolean isOldQuery;

        public MyChildEventListener(RecyclerView list, ReclycleAdapter adapter, boolean isOldQuery){
            this.list = list;
            this.adapter = adapter;
            this.isOldQuery = isOldQuery;
        }

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {

            String key = dataSnapshot.getKey();
            int myIndex = adapter.mKeys.indexOf(key);

            if(myIndex != -1){
                //ignore as it is duplicate at top of list
                Log.d("__CHAT_K", "Duplicate onChildAdded : " + dataSnapshot.toString() + " priority=" + dataSnapshot.getPriority());
                return;
            }

            Log.d("__CHAT_K", "New onChildAdded : " + dataSnapshot.toString() + " priority=" + dataSnapshot.getPriority());

            //Log.d("__CHAT", dataSnapshot.toString());
            Chat model = dataSnapshot.getValue(Chat.class);
            //Log.d("__CHAT", model + "");

            //set as delivered if received msg
            boolean received = true;
            if(model.getAuthor().equals(mUsername)){
                received = false;
            }

            if(received && model.getDelivered() == null){
                model.delivered = true; //important to prevent infi loop
                mFirebaseRef.child(key).child("delivered").setValue(true);

                //set lastSeenMsgKey in Room
                ChatRoomsActivity.RoomDetail roomDetail = ChatRoomsActivity.roomMap.get(channelId);
                if(roomDetail != null && roomDetail.room != null){
                    try {
                        if (roomDetail.room.lastSeenMsgKey == null || key.compareTo(roomDetail.room.lastSeenMsgKey) > 0) {//this msg key >  stored seen key
                            new Firebase(ChatConfig.FIREBASE_URL).child("Users").child(mUsername)
                                    .child("rooms").child(channelId).child("lastSeenMsgKey").setValue(key);
                        }
                    }
                    catch (NumberFormatException e){
                        e.printStackTrace();
                    }
                }
            }

            // Insert into the correct location, based on previousChildName
            if (previousChildName == null) {
                adapter.mModels.add(0, model);
                adapter.mKeys.add(0, key);
            } else {
                int previousIndex = adapter.mKeys.indexOf(previousChildName);
                int nextIndex = previousIndex + 1;
                if (nextIndex == adapter.mModels.size()) {
                    adapter.mModels.add(model);
                    adapter.mKeys.add(key);
                } else {
                    adapter.mModels.add(nextIndex, model);
                    adapter.mKeys.add(nextIndex, key);
                }
            }

            notifyAndSmartScroll();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            Log.d("__CHAT_K", "onChildChanged : " + dataSnapshot.toString());
            // One of the mModels changed. Replace it in our list and name mapping
            String key = dataSnapshot.getKey();
            Chat newModel = dataSnapshot.getValue(Chat.class);
            int index = adapter.mKeys.indexOf(key);

            Chat oldModel = adapter.mModels.get(index);
            if(oldModel.getSent() == null){
                //first time
                newModel.sent = true; //important to prevent infi loop
                mFirebaseRef.child(key).child("sent").setValue(true);
            }
            adapter.mModels.set(index, newModel);

            notifyAndSmartScroll();
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            if(true){
                return; // exprerimental so as to show window > 10
            }

            Log.d("__CHAT_K", "onChildRemoved : " + dataSnapshot.toString());
            // A model was removed from the list. Remove it from our list and the name mapping
            String key = dataSnapshot.getKey();
            int index = adapter.mKeys.indexOf(key);

            adapter.mKeys.remove(index);
            adapter.mModels.remove(index);

            notifyAndSmartScroll();
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
            Log.d("__CHAT_K", "onChildMoved : " + dataSnapshot.toString());

            // A model changed position in the list. Update our list accordingly
            String key = dataSnapshot.getKey();
            Chat newModel = dataSnapshot.getValue(Chat.class);
            int index = adapter.mKeys.indexOf(key);
            adapter.mModels.remove(index);
            adapter.mKeys.remove(index);
            if (previousChildName == null) {
                adapter.mModels.add(0, newModel);
                adapter.mKeys.add(0, key);
            } else {
                int previousIndex = adapter.mKeys.indexOf(previousChildName);
                int nextIndex = previousIndex + 1;
                if (nextIndex == adapter.mModels.size()) {
                    adapter.mModels.add(newModel);
                    adapter.mKeys.add(key);
                } else {
                    adapter.mModels.add(nextIndex, newModel);
                    adapter.mKeys.add(nextIndex, key);
                }
            }

            notifyAndSmartScroll();
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
            Log.d("__CHAT_K", "onChildCancelled");
            Log.e("FirebaseListAdapter", "Listen was cancelled, no more updates will occur");
        }

        void notifyAndSmartScroll(){
            if(isOldQuery) {
                int prevPos = mLayoutManager.findFirstVisibleItemPosition();
                int offset = 0;

                if (prevPos >= 0 && listView.getChildAt(prevPos) != null) {
                    offset = listView.getChildAt(prevPos).getTop() - listView.getPaddingTop();
                }

                adapter.notifyDataSetChanged();
                Log.d("__CHAT_KS", "smartNotifyAndScroll() old Query");
                mLayoutManager.scrollToPositionWithOffset(adapter.getItemCount() - lastTotalCount, offset);
            }
            else{
                Log.d("__CHAT_KS", "smartNotifyAndScroll() new Query");
                adapter.notifyDataSetChanged();
                mLayoutManager.scrollToPosition(adapter.getItemCount()-1);
            }
        }
    }

    public class ReclycleAdapter extends RecyclerView.Adapter<ViewHolder>{
        public List<Chat> mModels;
        public List<String> mKeys;

        public ReclycleAdapter(){
            mModels = new ArrayList<>();
            mKeys = new ArrayList<>();

            /*Chat c = new Chat();
            c.time = String.valueOf(0L);
            c.imageData = "";
            c.message = "dummy first msg";
            c.author = "ghost";

            mModels.add(c);
            mKeys.add("A");*/
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
            Chat chat = mModels.get(position);
            String key = mKeys.get(position);

            // Map a Chat object to an entry in our listview
            String author = chat.getAuthor();
            Long time = Long.valueOf(chat.getTime());

            boolean received = true;
            if(author.equals(mUsername)){
                received = false;
            }

            //set as delivered if received and seeing it for first time
            if(received && chat.getSeen() == null){
                chat.delivered = true; //important to prevent infi loop
                mFirebaseRef.child(key).child("seen").setValue(true);
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

            String timeString = new SimpleDateFormat(ChatConfig.MSG_TS_FORMAT).format(new Date(time));

            authorTV.setText(timeString + ": ");
            // If the message was sent by this user, color it differently
            if (author != null && author.equals(mUsername)) {
                authorTV.setTextColor(Color.RED);
            } else {
                authorTV.setTextColor(Color.BLUE);
            }

            if(received) {
                messageTV.setText(chat.getMessage());
            }
            else{
                messageTV.setText(chat.getMessage() + " " + chat.getStatus());
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        //remove all listeners
        newQuery.removeEventListener(mNewListener);
        if(oldQuery != null){
            oldQuery.removeEventListener(mOldListener);
        }

        mConnectedRef.removeEventListener(mConnectedListener);
        opponentStatusRef.removeEventListener(opponentStatusListener);
    }

    private void setupUsername() {
        mUsername = ParseUser.getCurrentUser().getUsername();
    }

    private void sendMessage() {
        //Firebase.goOnline();

        EditText inputText = (EditText) findViewById(R.id.messageInput);
        String input = inputText.getText().toString();
        if(imagePreview.getVisibility() == View.VISIBLE){
            Bitmap bmp = ((BitmapDrawable) attachedImage.getDrawable()).getBitmap();
            ByteArrayOutputStream bYtE = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, bYtE);
            bmp.recycle();
            byte[] byteArray = bYtE.toByteArray();
            String imageData = Base64.encodeToString(byteArray, Base64.DEFAULT);

            Map<String, Object> data = new HashMap<>();
            data.put("message", input);
            data.put("author", mUsername);
            data.put("time", ServerValue.TIMESTAMP);
            data.put("imageData", imageData);

            // Create a new, auto-generated child of that chat location, and save our chat data there
            mFirebaseRef.push().setValue(data);
            inputText.setText("");
            imagePreview.setVisibility(View.GONE);
            attachedImage.setImageBitmap(null);
        }
        else if (!input.equals("")) {
            Map<String, Object> data = new HashMap<>();
            data.put("message", input);
            data.put("author", mUsername);
            data.put("time", ServerValue.TIMESTAMP);
            data.put("imageData", "");

            // Create a new, auto-generated child of that chat location, and save our chat data there
            mFirebaseRef.push().setValue(data);
            inputText.setText("");

            sendNotification(input);

            if(!channelSet){
                channelSet = true;
                //add channel to mine and opponent so as to notify him
                Firebase myRef = new Firebase(ChatConfig.FIREBASE_URL).child("Users").child(mUsername).child("rooms");
                Firebase oppRef = new Firebase(ChatConfig.FIREBASE_URL).child("Users").child(opponentParseUsername).child("rooms");

                myRef.child(channelId).child("active").setValue(true);
                oppRef.child(channelId).child("active").setValue(true);
            }
        }
    }

    void sendNotification(String input){
        if(opponentOneSignalId == null){
            Utility.toast(opponentParseUsername + " not yet on OneSignal");
            return;
        }

        try {
            JSONObject contents = new JSONObject();
            contents.put("en", "");

            JSONObject headings = new JSONObject();
            headings.put("en", "");

            JSONObject data = new JSONObject();
            data.put("channel", channelId);
            data.put("msgTitle", mUsername);
            data.put("msgContent", input);

            JSONArray playerIds = new JSONArray();
            playerIds.put(opponentOneSignalId);

            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("contents", contents); //actual content
            jsonMap.put("include_player_ids",playerIds); //receipients
            jsonMap.put("headings", headings); //title
            jsonMap.put("data", data); //extra json
            jsonMap.put("android_background_data", true); //so that com.onesignal.BackgroundBroadcast.RECEIVE broadcast is called

            JSONObject notificationJSON = new JSONObject(jsonMap);

            Log.d("__CHAT noti", notificationJSON + "");

            OneSignal.postNotification(notificationJSON, new OneSignal.PostNotificationResponseHandler() {
                @Override
                public void onSuccess(JSONObject jsonObject) {
                    Application.applicationHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Utility.toast("notification sent");
                        }
                    });
                }

                @Override
                public void onFailure(JSONObject jsonObject) {
                    Application.applicationHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Utility.toast("notification failed");
                        }
                    });
                }
            });
        }
        catch (JSONException e){
            e.printStackTrace();
        }
    }
}

















