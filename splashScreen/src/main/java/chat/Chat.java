package chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author greg
 * @since 6/21/13
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Chat {

    public String message;
    public String author;
    public String time;

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

    @Override
    public String toString(){
        return author + "@" + time + " : " + message;
    }
}
