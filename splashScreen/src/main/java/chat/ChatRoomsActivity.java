package chat;

import android.content.Intent;
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
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import baseclasses.MyActionBarActivity;
import trumplab.textslate.R;
import trumplabs.schoolapp.Constants;
import trumplabs.schoolapp.MemberDetails;
import utility.Queries;
import utility.Utility;

/**
 * Created by ashish on 31/8/15.
 */
public class ChatRoomsActivity extends MyActionBarActivity{
    static Firebase myRoomsRef; //will point to list of my chat rooms in firebase
    static ChildEventListener  myRoomListener;

    static RecyclerView listView;
    static MyRecycleAdapter listViewAdapter;

    static Map<String, RoomDetail> roomMap;
    static List<String> roomIdList;

    //if non null, means that it's listener, adapter, etc has been set
    static String mUsername;
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

        initChatRooms(mUsername);

        listView = (RecyclerView) findViewById(R.id.roomList);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listViewAdapter = new MyRecycleAdapter();
        listView.setAdapter(listViewAdapter);
    }

    public static void initChatRooms(String user){
        if(myRoomsRef == null){
            mUsername = user;
            Log.d("__CHAT_CR", "init myRoomsRef and listener");
            myRoomsRef = new Firebase(ChatConfig.FIREBASE_URL).child("Users").child(mUsername).child("rooms");
            myRoomListener = myRoomsRef.addChildEventListener(new MyRoomListener());
            roomIdList = new ArrayList<>();
            roomMap = new HashMap<>();
        }
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        LinearLayout chatHolder;
        TextView roomIdTV;
        TextView newMsgsTV;

        public MyViewHolder(View view) {
            super(view);
            chatHolder = (LinearLayout) view.findViewById(R.id.holder);
            roomIdTV = (TextView) view.findViewById(R.id.roomId);
            newMsgsTV = (TextView) view.findViewById(R.id.newMsgs);
        }
    }

    static class RoomDetail {
        public int newMsgs;
        Room room;

        String opponentName;
        String opponentParseUsername;

        public RoomDetail(int n, Room r){
            newMsgs = n;
            room = r;
        }
    }

    static class Room{
        public String chatAs;
        public String lastSeenMsgKey;

        public Room(){

        }
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
            final RoomDetail roomDetail = roomMap.get(roomId);

            holder.roomIdTV.setText(roomId);
            holder.newMsgsTV.setText(roomDetail.newMsgs + ""); //pass string, int will be treated as resId(crash)

            String[] tokens = roomId.split("-");
            Log.d("__CHAT_CR_opp", "roomId split into=" + Arrays.toString(tokens));
            if(tokens.length != 2) {
                return;
            }

            final String classCode = tokens[0];
            final String parentParseUsername = tokens[1];

            //chatAs replacement
            if(parentParseUsername.equals(mUsername)){
                roomDetail.room.chatAs = ChatConfig.NON_TEACHER;
            }
            else{
                roomDetail.room.chatAs = ChatConfig.TEACHER;
            }

            if(roomDetail.opponentParseUsername == null){
                if (roomDetail.room.chatAs.equals(ChatConfig.TEACHER)) {
                    //search in subscriber
                    MemberDetails member = Queries.getMember(classCode, parentParseUsername);
                    if(member != null){
                        roomDetail.opponentName = member.getChildName();
                        roomDetail.opponentParseUsername = member.getChildId();
                        Log.d("__CHAT_CR_opp", "(teacher)found " + roomDetail.opponentName + "; " + tokens[1] + "==" + roomDetail.opponentParseUsername);
                    }
                }
                else{
                    //search for Codegroup object
                    ParseObject codegroup = Queries.getCodegroupObject(classCode);
                    if(codegroup != null){
                        roomDetail.opponentName = codegroup.getString(Constants.Codegroup.CREATOR);
                        roomDetail.opponentParseUsername = codegroup.getString(Constants.Codegroup.SENDER_ID);
                        Log.d("__CHAT_CR_opp", "(non_teacher) found " + roomDetail.opponentName + "; " + roomDetail.opponentParseUsername);
                    }
                }
            }

            if(roomDetail.opponentParseUsername != null){
                //set click listener
                holder.roomIdTV.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ChatRoomsActivity.this, ChatActivityRecyclerView.class);
                        intent.putExtra("classCode", classCode);
                        intent.putExtra("chatAs", roomDetail.room.chatAs);
                        intent.putExtra("opponentName", roomDetail.opponentName);
                        intent.putExtra("opponentParseUsername", roomDetail.opponentParseUsername);
                        startActivity(intent);
                    }
                });
            }
            else{
                //should not happen
                holder.roomIdTV.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Utility.toast("Sorry chat details not available at the moment !. Please restart the app and try again");
                    }
                });
            }
        }
    }

    static class MyRoomListener implements ChildEventListener{
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
            String key = dataSnapshot.getKey();

            Log.d("__CHAT_CR", "New onChildAdded : " + dataSnapshot.toString());
            Room newModel = dataSnapshot.getValue(Room.class);

            // Insert into the correct location, based on previousChildName
            //todo add to roomMap also
            roomMap.put(key, new RoomDetail(0, newModel));
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

            //add new msg listener
            Firebase newMsgsRef = new Firebase(ChatConfig.FIREBASE_URL).child("Chats").child(key);
            if(newModel.lastSeenMsgKey != null){
                Log.d("__CHAT_CR_msg", "Add listener startAt=" + newModel.lastSeenMsgKey);
                newMsgsRef.orderByKey().startAt(newModel.lastSeenMsgKey).addChildEventListener(new NewMessageListener(key));
            }
            else{
                Log.d("__CHAT_CR_msg", "Add listener startAt=null");
                newMsgsRef.orderByKey().addChildEventListener(new NewMessageListener(key));
            }

            notifyAdapter();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            Log.d("__CHAT_CR", "onChildChanged : " + dataSnapshot.toString());

            String key = dataSnapshot.getKey();
            Room newModel = dataSnapshot.getValue(Room.class);

            RoomDetail roomDetail = roomMap.get(key);
            roomDetail.room = newModel; //keep the new msgs count intact

            roomMap.put(key, roomDetail);

            notifyAdapter();
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

        static void notifyAdapter(){
            if(listViewAdapter != null) {
                listViewAdapter.notifyDataSetChanged();
            }
        }
    }

    static class NewMessageListener implements ChildEventListener{
        String roomId;

        public NewMessageListener(String roomId){
            this.roomId = roomId;
        }

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
            Log.d("__CHAT_CR_msg", "onChildAdded@" + roomId + ":" + dataSnapshot.toString());
            String key = dataSnapshot.getKey();
            Chat model = dataSnapshot.getValue(Chat.class);

            boolean received = true;
            if(model.getAuthor().equals(mUsername)){
                received = false;
            }

            if(received){
                //new received message, increment the count
                RoomDetail roomDetail = roomMap.get(roomId);
                Log.d("__CHAT_CR_msg", "last=" + roomDetail.room.lastSeenMsgKey + ", this=" + key);
                if(roomDetail.room.lastSeenMsgKey == null || key.compareTo(roomDetail.room.lastSeenMsgKey) > 0){ //current key > last seen key
                    roomDetail.newMsgs++;
                    MyRoomListener.notifyAdapter();
                }
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {

        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }

}
