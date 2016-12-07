package com.camera;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.google.zxing.client.android.CaptureActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Point;

import java.io.File;
import java.util.List;
import java.util.Map;


/**
 * Created by Administrator on 2016/11/19.
 */
public class CameraActivity extends Activity implements View.OnClickListener {
    private int mPicCount = 0;
    private TextView tv_cancle;
    private TextView tv_complet;
    private ImageViewDraw iv_photo;
    private int ivHeight;
    private int ivWidth;
    private TextView tv_photo;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    //第一次调用需要调用初始化
                    //                    initView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private String codeResult;
    private TextView tv_scan;
    private int inSampleSize;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        initView();
    }

    private void initView() {
        tv_cancle = (TextView) findViewById(R.id.tv_cancle);
        tv_cancle.setOnClickListener(this);
        tv_complet = (TextView) findViewById(R.id.tv_complet);
        tv_complet.setOnClickListener(this);
        tv_photo = (TextView) findViewById(R.id.tv_photo);
        tv_photo.setOnClickListener(this);
        iv_photo = (ImageViewDraw) findViewById(R.id.iv_photo);
        tv_scan = (TextView) findViewById(R.id.tv_scan);
        tv_scan.setOnClickListener(this);
        ViewTreeObserver vto = iv_photo.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                ivHeight = iv_photo.getMeasuredHeight();
                ivWidth = iv_photo.getMeasuredWidth();
                return true;
            }
        });

    }


    private String getPointsSizeString() {
        List<Point> rect = iv_photo.getQRRectPoints();
        JSONArray jsonArray = new JSONArray();
        JSONObject tmpObj = null;
        try {
            for (Point p : rect) {
                tmpObj = new JSONObject();
                tmpObj.put("x", p.x * inSampleSize + "");
                tmpObj.put("y", p.y * inSampleSize);
                jsonArray.put(tmpObj);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonArray.toString();
    }

    private void processQRBimap(Bitmap drawingCache) {
        List<Point> rect = iv_photo.getQRRectPoints();
        Point p1 = rect.get(0);
        Point p3 = rect.get(2);
        int w = (int) (p3.x - p1.x);
        int h = (int) (p3.y - p1.y);
        Bitmap bitmap = Bitmap.createBitmap(drawingCache, (int) p1.x, (int) p1.y, w, h);
        ImageUtils.saveBitmap(getPhotopath(), bitmap);
        //        codeResult= QRCodeUtil.scanningImage(getPhotopath());
    }

    private void startCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File out = new File(getPhotopath());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Uri uri = Uri.fromFile(out);
        // 获取拍照后未压缩的原图片，并保存在uri路径中
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, 1);
    }

    private void startQRScan() {
        Intent intent = new Intent();
        intent.setClass(this, CaptureActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        this.startActivityForResult(intent, 1);
    }

    private void process(Bitmap drawingCache) {
        ImageProcess imageProcess = new ImageProcess(drawingCache);
        Map<String, Bitmap> mps = imageProcess.doSplitInvoice(drawingCache, iv_photo
                .getMaxRectPoints());
        UploadUtil.postBitmaps(mps);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //        Bitmap bitmap=ImageUtils.decodeSampledBitmapFromResource(getResources(),R
        // .drawable.image2,iv_photo.getWidth(),iv_photo.getHeight(),options);
        //        Bitmap bitmap = (Bitmap)data.getExtras().get("data");
        //        Bitmap bitmap= BitmapFactory.decodeResource(getResources(),R.drawable
        // .image_stand);
        if (mPicCount == 0) {
            if (data != null) {
                codeResult = data.getStringExtra("result");
            }
            //            iv_photo.setDrawPonintsFlag(2);
            //            codeResult= QRCodeUtil.scanningImage(getPhotopath());
            //            startCamera();
        } else {
            Bitmap bitmap = ImageUtils.decodeSampledBitmapFromFile(getPhotopath(), ivWidth,
                    ivHeight, options);
            iv_photo.setImageBitmap(bitmap);
            iv_photo.setPoints(StandImageUtils.initRect);
            iv_photo.setDrawPonintsFlag(1);
            inSampleSize = options.inSampleSize;
        }

        //        File file = new File(Environment.getExternalStorageDirectory() + "/myImage/");
        //        file.mkdirs();
        //        String fileName = Environment.getExternalStorageDirectory() + "/myImage/" +
        // System.currentTimeMillis() + ".jpg";
        //        try {
        //            FileOutputStream b = new FileOutputStream(fileName);
        //            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, b);
        //            b.flush();
        //            b.close();
        //            Toast.makeText(getApplicationContext(), "照片已保存到：" + fileName, Toast
        // .LENGTH_LONG).show();
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //        }
    }


    /**
     * 获取原图片存储路径
     *
     * @return
     */
    private String getPhotopath() {
        // 照片全路径
        String fileName = "";
        // 文件夹路径
        String pathUrl = Environment.getExternalStorageDirectory() + "/finger/";
        String imageName = "imageOrg" + mPicCount + ".jpg";
        File file = new File(pathUrl);
        if (!file.exists()) {
            file.mkdirs();// 创建文件夹
        }
        fileName = pathUrl + imageName;
        return fileName;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this,
                    mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("count", mPicCount);
        outState.putInt("ivWidth", ivWidth);
        outState.putInt("ivHeight", ivHeight);
        if (codeResult != null) {
            outState.putString("codeResult", codeResult);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mPicCount = savedInstanceState.getInt("count");
        ivWidth = savedInstanceState.getInt("ivWidth");
        ivHeight = savedInstanceState.getInt("ivHeight");
        if (mPicCount == 1) {
            codeResult = savedInstanceState.getString("codeResult");
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        if (v == tv_photo) {
            mPicCount = 1;
            startCamera();
        } else if (v == tv_complet) {
            if (mPicCount >= 1) {
                UploadUtil.postBitmapAndQRCode(getPhotopath(), codeResult, getPointsSizeString());
            } else if (mPicCount == 0) {
                //                    iv_photo.setDrawingCacheEnabled(true);
                //                    iv_photo.setDrawPonintsFlag(0);
                //                    iv_photo.setDrawingCacheQuality(ImageView
                // .DRAWING_CACHE_QUALITY_HIGH);
                //                    process(iv_photo.getDrawingCache());
                //                    processQRBimap(iv_photo.getDrawingCache());
                //                    iv_photo.setDrawingCacheEnabled(false);
                //                    mPicCount++;
            }
        } else if (v == tv_scan) {
            mPicCount = 0;
            startQRScan();
        }
    }
}
