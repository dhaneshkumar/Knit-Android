package chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * @author greg
 * @since 6/21/13
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Chat {

    public String message;
    public String author;
    public String time;
    public String imageData;

    public Boolean sent;
    public Boolean delivered;
    public Boolean seen;

    // Required default constructor for Firebase object mapping
//    @SuppressWarnings("unused")
    public Chat() {
    }

    public String getMessage() {
        return message;
    }

    public String getAuthor() {
        return author;
    }

    public String getTime() {
        return time;
    }

    public String getImageData(){
        return imageData;
    }

    public Boolean getSent(){
        return sent;
    }

    public Boolean getDelivered(){
        return delivered;
    }

    public Boolean getSeen(){
        return seen;
    }

    public String getStatus(){
        if(seen != null){
            return "[seen]";
        }
        else if(delivered != null){
            return "[delivered]";
        }
        else if(sent != null){
            return "[sent to server]";
        }
        else {
            return "[pending]";
        }
    }

    @Override
    public String toString(){
        int imageSize = 0;
        if(imageData != null){
            imageSize = imageData.length();
        }

        return author + "@" + time + " : " + message + ", imageSize=" + imageSize;
    }

    static class ConnectionStatus{
        public Boolean online;
        public Long lastOnline;
    }

    //Because ServerValue.TIMESTAMP placeholder is of type Map
    static class ConnectionStatusProxy{
        public Boolean online;
        public Map<String, String> lastOnline;

        ConnectionStatusProxy(Boolean online, Map<String, String> lastOnline){
            this.online = online;
            this.lastOnline = lastOnline;
        }
    }
}
