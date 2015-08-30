package chat;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import baseclasses.MyActionBarActivity;
import trumplab.textslate.R;

/**
 * Created by ashish on 31/8/15.
 */
public class ChatRoomsActivity extends MyActionBarActivity{
    static Firebase myRoomsRef; //will point to list of my chat rooms in firebase
    static ChildEventListener  myRoomListener;

    static RecyclerView listView;
    static MyRecycleAdapter listViewAdapter;

    static Map<String, Room> roomMap;
    static List<String> roomIdList;

    //if non null, means that it's listener, adapter, etc has been set
    String mUsername;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_rooms_activity);
        if(ParseUser.getCurrentUser() != null) {
            mUsername = ParseUser.getCurrentUser().getUsername();
        }
        else{
            return;
        }

        if(myRoomsRef == null){
            Log.d("__CHAT_CR", "init myRoomsRef and listener");
            myRoomsRef = new Firebase(ChatConfig.FIREBASE_URL).child("Users").child(mUsername).child("rooms");
            myRoomListener = myRoomsRef.addChildEventListener(new MyRoomListener());
            roomIdList = new ArrayList<>();
            roomMap = new HashMap<>();
        }

        listView = (RecyclerView) findViewById(R.id.roomList);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listViewAdapter = new MyRecycleAdapter();
        listView.setAdapter(listViewAdapter);
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        LinearLayout chatHolder;
        TextView roomIdTV;

        public MyViewHolder(View view) {
            super(view);
            chatHolder = (LinearLayout) view.findViewById(R.id.holder);
            roomIdTV = (TextView) view.findViewById(R.id.roomId);
        }
    }

    static class Room{
        String roomId;
        String lastReadTimestamp;
    }

    public class MyRecycleAdapter extends RecyclerView.Adapter<MyViewHolder>{
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
            View row = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.chat_room, viewGroup, false);
            MyViewHolder holder = new MyViewHolder(row);
            return holder;
        }

        @Override
        public int getItemCount() {
            return roomIdList.size();
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, final int position) {
            String roomId = roomIdList.get(position);
            holder.roomIdTV.setText(roomId);
        }
    }

    class MyRoomListener implements ChildEventListener{
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
            String key = dataSnapshot.getKey();

            Log.d("__CHAT_CR", "New onChildAdded : " + dataSnapshot.toString());

            // Insert into the correct location, based on previousChildName
            //todo add to roomMap also
            if (previousChildName == null) {
                roomIdList.add(0, key);
            } else {
                int previousIndex = roomIdList.indexOf(previousChildName);
                int nextIndex = previousIndex + 1;
                if (nextIndex == roomIdList.size()) {
                    roomIdList.add(key);
                } else {
                    roomIdList.add(nextIndex, key);
                }
            }

            notifyAndSmartScroll();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            Log.d("__CHAT_CR", "onChildChanged : " + dataSnapshot.toString());
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            Log.d("__CHAT_CR", "onChildRemoved : " + dataSnapshot.toString());
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
            Log.d("__CHAT_CR", "onChildMoved : " + dataSnapshot.toString());
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
            Log.d("__CHAT_CR", "onChildCancelled");
            Log.e("FirebaseListAdapter", "Listen was cancelled, no more updates will occur");
        }

        void notifyAndSmartScroll(){
            if(listViewAdapter != null) {
                listViewAdapter.notifyDataSetChanged();
            }
        }
    }

}
