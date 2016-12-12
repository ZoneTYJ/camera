/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.ByteArrayOutputStream;
import java.util.Map;

final class DecodeHandler extends Handler {

  private static final String TAG = DecodeHandler.class.getSimpleName();

  private final CaptureActivity activity;
  private final MultiFormatReader multiFormatReader;
  private boolean running = true;

  private boolean decodeOpenCv = true;
  private int countPic=0;

  DecodeHandler(CaptureActivity activity, Map<DecodeHintType,Object> hints) {
    multiFormatReader = new MultiFormatReader();
    multiFormatReader.setHints(hints);
    this.activity = activity;
  }

  @Override
  public void handleMessage(Message message) {
    if (!running) {
      return;
    }
    if (message.what == R.id.decode) {
      decode((byte[]) message.obj, message.arg1, message.arg2);

    } else if (message.what == R.id.quit) {
      running = false;
      Looper.myLooper().quit();

    }
  }

  /**
   * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
   * reuse the same reader objects from one decode to the next.
   *
   * @param data   The YUV preview frame.
   * @param width  The width of the preview frame.
   * @param height The height of the preview frame.
   */
  private void decode(byte[] data, int width, int height) {
    long start = System.currentTimeMillis();
    Result rawResult = null;
//    Rect rect = activity.getCameraManager().getFramingRectInPreview();
//    Bitmap bmp=decodeToBitMap(data, width, height, rect);
    PlanarYUVLuminanceSource sourceYUV = activity.getCameraManager().buildLuminanceSource(data, width, height);
    try {
      if (decodeOpenCv) {//使用openCV
        decodeOpenCv = false;
        Bitmap bitmap=DoBinaryBitmap(sourceYUV);
//        Bitmap bitmap=DoBinaryRGBBitmap(bmp);
        if (bitmap != null) {
//          UploadUtil.saveBitmap2Card(bitmap, countPic++);
          RGBLuminanceSource source = BitmapConverRGBLum(bitmap);
          BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
          rawResult = multiFormatReader.decodeWithState(bitmap1);
        }
      } else {//图片直接扫描查询
        decodeOpenCv = true;
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(sourceYUV));
        //解析不出来时候会报异常
        rawResult = multiFormatReader.decodeWithState(bitmap);

      }
    } catch (ReaderException re) {
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      multiFormatReader.reset();
    }

    Handler handler = activity.getHandler();
    if (rawResult != null) {
      // Don't log the barcode contents for security.
      long end = System.currentTimeMillis();
      Log.d(TAG, "Found barcode in " + (end - start) + " ms");
      if (handler != null) {
        Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
        Bundle bundle = new Bundle();
//        bundleThumbnail(sourceYUV, bundle);
        message.setData(bundle);
        message.sendToTarget();
      }
    } else {
      if (null != handler) {
        Message message = Message.obtain(handler, R.id.decode_failed);
        message.sendToTarget();
      }
    }
  }

  private static void bundleThumbnail(PlanarYUVLuminanceSource source, Bundle bundle) {
    int[] pixels = source.renderThumbnail();
    int width = source.getThumbnailWidth();
    int height = source.getThumbnailHeight();
    Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
    ByteArrayOutputStream out = new ByteArrayOutputStream();    
    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
    bundle.putByteArray(DecodeThread.BARCODE_BITMAP, out.toByteArray());
    bundle.putFloat(DecodeThread.BARCODE_SCALED_FACTOR, (float) width / source.getWidth());
  }

  private Bitmap getThumbnail(PlanarYUVLuminanceSource source) {
    int[] pixels = source.renderThumbnail();
    int width = source.getThumbnailWidth();
    int height = source.getThumbnailHeight();
    return Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
  }


  private RGBLuminanceSource BitmapConverRGBLum(Bitmap bitmap) {
    int w = bitmap.getWidth();
    int h = bitmap.getHeight();
    int left=w/6*2;
    int top=h/10*2;
    w=w-left*2;
    h=h-top*2;
    int[] pixels = new int[w * h];
    bitmap.getPixels(pixels, 0, w,left, top, w, h);
    creatBitmap(pixels,w,h);
    RGBLuminanceSource source = new RGBLuminanceSource(w, h, pixels);
    return source;
  }


  private void creatBitmap(int[] pixels, int width, int height) {
    Bitmap bitmap=Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
//    int c=-countPic++;
//    UploadUtil.saveBitmap2Card(bitmap, c);
  }


  private  Bitmap DoBinaryBitmap(PlanarYUVLuminanceSource source){
    int pixels[]=source.renderThumbnail();
    int maxhist=getMaxHist(pixels);
    return getBinary(pixels,maxhist-12,source.getThumbnailWidth(),source.getThumbnailHeight());
  }

  private Bitmap DoBinaryRGBBitmap(Bitmap bmp) {
    int w=bmp.getWidth();
    int h=bmp.getHeight();
    int pixels[]=new int[w*h];
    bmp.getPixels(pixels, 0, w, 0, 0, w, h);
    int maxhist=getMaxHist(pixels);
    return getBinary(pixels,maxhist-12,w,h);
  }
  private int getMaxHist(int[] pixels){
    int[] hits=new int[256];
    int maxCount=-1;
    int maxHist=-1;
    for(int i=0;i<pixels.length;i+=5){
      int grey=pixels[i]&0x00FF0000>>16;
      int count=++hits[grey];
      if(count>maxCount){
        maxCount=count;
        maxHist=grey;
      }
    }
    return maxHist;
  }

  private Bitmap getBinary(int[] pixels,int maxhist,int width,int height) {
    for(int i=0;i<pixels.length;i++){
      int grey=pixels[i]&0x00FF0000>>16;
      int color;
      if(grey<=maxhist){
        color=0;
      }else {
        color=0xFFFFFF;
      }
      pixels[i]=pixels[i]&0xFF000000|color;
    }
    return Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
  }


  private Bitmap decodeToBitMap(byte[] data, int width,int height,Rect rect) {
    YuvImage image=new YuvImage(data, ImageFormat.NV21,width,height,null);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    BitmapRegionDecoder bitmapRegionDecoder = null;
    Bitmap bmp=null;
    try {
      image.compressToJpeg(new Rect(0, 0, width, height), 100, stream);
      stream.close();
      bitmapRegionDecoder=BitmapRegionDecoder.newInstance(stream.toByteArray(), 0, stream.size(), false);
    } catch (Exception ex) {
      Log.e("Sys", "Error:" + ex.getMessage());
    }
    if(bitmapRegionDecoder!=null) {
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inPreferredConfig = Bitmap.Config.ARGB_8888;
      bmp= bitmapRegionDecoder.decodeRegion(rect, options);
    }
    return bmp;
  }

}
