package process;

import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

/**
 * Created by tangyijian on 2016/12/16.
 */
public class UploadHandler extends Handler {
    public static int UPLOAD =1;
    private final BaseActivity currentActivity;

    public UploadHandler(BaseActivity activity){
        currentActivity=activity;
    }
    @Override
    public void handleMessage(Message msg) {
        String str= (String) msg.obj;
        if(msg.what== UPLOAD){
            currentActivity.hideProgress();
            Toast.makeText(currentActivity.getApplicationContext(),str.toString(),Toast.LENGTH_SHORT).show();
        }

    }

}
