package library;

public class UtilString {
  
  public static boolean isBlank(String str)
  {
    if(str == null || str.trim().equals(""))
      return true;
    
    return false;
  }
  
  public static String parseString(String str)
  {
    str = str.replace("'", " ");
    str = str.replace("\"", " ");
    return str;
  }

  public static String changeFirstToCaps(String displayname)
  {
    if(isBlank(displayname))
      return null;
    
    String[] nameList=displayname.split(" "); 
    String userName ="";
    
    for(int i=0; i<nameList.length;i++)
    {
        String str = nameList[i].trim();

        if(!UtilString.isBlank(str))
            userName += str.substring(0, 1).toUpperCase() + str.substring(1) +" " ;
    }
   
    return userName;
  }
  
}
