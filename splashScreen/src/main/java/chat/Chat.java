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
    public String imageData;

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
    @Override
    public String toString(){
        int imageSize = 0;
        if(imageData != null){
            imageSize = imageData.length();
        }

        return author + "@" + time + " : " + message + ", imageSize=" + imageSize;
    }
}
