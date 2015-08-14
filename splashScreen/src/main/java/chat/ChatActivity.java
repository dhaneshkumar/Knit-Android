package chat;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ServerValue;
import com.firebase.client.ValueEventListener;
import com.parse.ParseUser;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import library.UtilString;
import trumplab.textslate.R;

public class ChatActivity extends ListActivity {

    // TODO: change this to your own Firebase URL
    public static final String FIREBASE_URL = "https://devknit.firebaseio.com";

    private String mUsername;
    private Firebase mFirebaseRef;
    private ValueEventListener mConnectedListener;
    private ChatListAdapter mChatListAdapter;

    private String childName;
    private String childId;
    private String classCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);

        // Make sure we have a mUsername
        setupUsername();


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

    }

    @Override
    public void onStart() {
        super.onStart();
        // Setup our view and list adapter. Ensure it scrolls to the bottom as data changes
        final ListView listView = getListView();
        // Tell our list adapter that we only want 50 messages at a time
        mChatListAdapter = new ChatListAdapter(mFirebaseRef.limit(100), this, R.layout.chat_message, mUsername);
        listView.setAdapter(mChatListAdapter);
        mChatListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(mChatListAdapter.getCount() - 1);
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
        if (!input.equals("")) {
            Map<String, Object> data = new HashMap<>();
            data.put("message", input);
            data.put("author", mUsername);
            data.put("time", ServerValue.TIMESTAMP);

            // Create a new, auto-generated child of that chat location, and save our chat data there
            mFirebaseRef.push().setValue(data);
            inputText.setText("");
        }
    }
}

















