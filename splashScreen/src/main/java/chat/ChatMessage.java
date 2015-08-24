package chat;

/**
 * Created by GleasonK on 7/11/15.
 *
 * ChatMessage is used to hold information that is transmitted using PubNub.
 * A message in this app has a author, message, and timestamp.
 */
public class ChatMessage {
    private String author;
    private String message;
    private long timeStamp;
    private String imageData;

    public ChatMessage(String username, String message, long timeStamp){
        this.author = username;
        this.message   = message;
        this.timeStamp = timeStamp;
        this.imageData = "";
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getImageData(){
        return imageData;
    }

    @Override
    public String toString(){
        return author + "@" + timeStamp + ":" + message;
    }
}
