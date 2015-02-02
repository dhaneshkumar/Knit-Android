package additionals;

import android.support.v7.app.ActionBarActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import trumplabs.schoolapp.Application;

/**
 * Created by dhanesh on 20/1/15.
 */
public class ReadSchoolFile extends ActionBarActivity{




    public  List<String> getSchoolsList() throws IOException {
        List<String> schoolList = new ArrayList<String>();


        InputStream iS = Application.getAppContext().getAssets().open("school_list.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(iS));
        try {
            String line = br.readLine();

            while (line != null) {

                schoolList.add(line);

                line = br.readLine();
            }
        } finally {
            br.close();
        }

        return schoolList;
    }


}
