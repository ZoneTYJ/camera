package com.camera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Rect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import process.ImageProcess;
import process.ImageUtils;

public class MainActivity extends Activity {
    private static final String  TAG                 = "OCVcamera::Activity";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //第一次调用需要调用初始化
                    initView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    private void initView() {
        Bitmap bmp= ImageUtils.decodeSampledBitmapFromResource(getResources(), R.drawable.image_stand, getApplicationContext());
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                options.inPreferredConfig = Bitmap.Config.RGB_565;
//        options.inJustDecodeBounds=true;
//                Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.image1, options);
//        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.image1);
        ImageView imageView= (ImageView) findViewById(R.id.iv);
//        imageView.setImageBitmap(bmp);
        ImageProcess imageProcess=new ImageProcess(bmp);

        ImageView iv_show= (ImageView) findViewById(R.id.iv_show);
        Rect rect=imageProcess.doContours(bmp);
//        Bitmap bitmap=imageProcess.doContours(bmp);
//        iv_show.setImageBitmap(bitmap);
//        UploadUtil.saveMyBitmap("processPic", bitmap);
    }


    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public static String bitmapToBase64(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


}
