package chat;

import android.app.ListActivity;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ServerValue;
import com.firebase.client.ValueEventListener;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import baseclasses.MyActionBarActivity;
import trumplab.textslate.R;
import trumplabs.schoolapp.ChooserDialog;
import utility.Config;
import utility.Utility;

public class ChatActivity extends MyActionBarActivity implements ChooserDialog.CommunicatorInterface {

    // TODO: change this to your own Firebase URL
    public static final String FIREBASE_URL = "https://devknit.firebaseio.com";

    private String mUsername;
    private Firebase mFirebaseRef;
    private ValueEventListener mConnectedListener;
    private ChatListAdapter mChatListAdapter;

    private String childName;
    private String childId;
    private String classCode;

    LinearLayout imagePreview;
    ImageView attachedImage;
    ImageView removeButton;

    Query moreQuery;
    static int lastTotalCount = -1;
    static ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);

        // Make sure we have a mUsername
        setupUsername();

        lastTotalCount = -1;

        if(getIntent()!= null && getIntent().getExtras() != null)
        {
            classCode = getIntent().getExtras().getString("classCode");
            childName = getIntent().getExtras().getString("childName");
            childId = getIntent().getExtras().getString("childId");
        }

        setTitle("Chatting with " + childName + " as " + classCode);

        // Setup our Firebase mFirebaseRef - chat rooms is <classCode>_<childId>  (childId is just emailId column)
        mFirebaseRef = new Firebase(FIREBASE_URL).child(classCode + "-" + childId);
        mFirebaseRef.keepSynced(true);

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

    @Override
    public void onStart() {
        super.onStart();
        // Setup our view and list adapter. Ensure it scrolls to the bottom as data changes
        listView = (ListView) findViewById(R.id.chatList);

        // Tell our list adapter that we only want 50 messages at a time
        mChatListAdapter = new ChatListAdapter(mFirebaseRef.limit(8), this, R.layout.chat_message, mUsername);
        listView.setAdapter(mChatListAdapter);

        mChatListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                /*if (mChatListAdapter.getCount() > 4) {
                    Log.d("__CHAT_KKP", "DataSetObserver.onChanged() called");
                    listView.setSelection(4);
                }*/
            }
        });

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            int scrollState = SCROLL_STATE_IDLE;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                this.scrollState = scrollState;
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (totalItemCount > 0 && firstVisibleItem == 0) {
                    if (totalItemCount != lastTotalCount) {
                        //remove duplicate listeners by making sure that another listener is added only if total count changes
                        Log.d("__CHAT_KKP", "Top onScrollNew " + listView.getFirstVisiblePosition() + ", lTC=" + lastTotalCount + ", fVI=" + firstVisibleItem + ", vIC=" + visibleItemCount + ", tIC=" + totalItemCount);
                        lastTotalCount = totalItemCount;

                        if (moreQuery != null) {
                            //Log.d("__CHAT_KKP", "removing Listener visibleItemCount=" + visibleItemCount + ", totalItemCount=" + totalItemCount);
                            moreQuery.removeEventListener(mChatListAdapter.mListener);
                            //moreQuery.removeEventListener(mChatListAdapter.mListener); // multiple calls gives no error :P
                        }

                        moreQuery = mFirebaseRef.limit(4).orderByKey().endAt(mChatListAdapter.mKeys.get(0)); //key for first item
                        moreQuery.addChildEventListener(mChatListAdapter.mListener);
                    } else {
                        Log.d("__CHAT_KKQ", "Top onScrollDuplicate " + listView.getFirstVisiblePosition() + ", lTC=" + lastTotalCount + ", fVI=" + firstVisibleItem + ", vIC=" + visibleItemCount + ", tIC=" + totalItemCount);
                    }
                }
            }
        });

        // Finally, a little indication of connection status
        mConnectedListener = mFirebaseRef.getRoot().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("__BA", dataSnapshot + "");
                boolean connected = (Boolean) dataSnapshot.getValue();
                if (connected) {
                    Toast.makeText(ChatActivity.this, "Connected to Firebase", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChatActivity.this, "Disconnected from Firebase", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                // No-op
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        mFirebaseRef.getRoot().child(".info/connected").removeEventListener(mConnectedListener);
        mChatListAdapter.cleanup();
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
        }
    }
}

















