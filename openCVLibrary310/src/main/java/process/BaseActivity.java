package process;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;

/**
 * Created by tangyijian on 2016/12/16.
 */
public class BaseActivity extends Activity {
    protected Dialog mLoadingDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLoadingDialog = new ProgressDialog(this);
    }

    public void showProgress(){
        mLoadingDialog.show();
    }

    public void hideProgress(){
        mLoadingDialog.dismiss();
    }
}
