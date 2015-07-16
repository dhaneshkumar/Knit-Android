package BackGroundProcesses;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by ashish on 17/7/15.
 * This class is a proxy for AsyncTask but its constructor can be called in threads also(No looper issue)
 * This is because this class creates actual AsyncTask instance only when execute() method is called which is done
 * only in GUI thread and hence safe.
 *
 * So now threads which need to use just helper methods of implemented AsyncTask e.g Inbox.doInBackgroundCore() or SeenHandler.syncSeenJob()
 * can do so safely by just calling the constructor and then calling the methods
 */
public abstract class AsyncTaskProxy<Params, Progress, Result> {
    Task task;
    class Task extends AsyncTask<Params, Progress, Result> {
        @Override
        protected Result doInBackground(Params... params) {
            return AsyncTaskProxy.this.doInBackground(params);
        }

        @Override
        protected void onPostExecute(Result result) {
            AsyncTaskProxy.this.onPostExecute(result);
            super.onPostExecute(result);
        }

    }

    public void execute(Params... params){
        task = new Task();
        task.execute(params);
    }

    abstract Result doInBackground(Params... params);

    void onPostExecute(Result result){

    }
}