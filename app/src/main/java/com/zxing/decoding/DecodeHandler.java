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

package com.zxing.decoding;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.camera.ImageProcess;
import com.camera.R;
import com.camera.UploadUtil;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.zxing.activity.MipcaActivityCapture;
import com.zxing.camera.CameraManager;
import com.zxing.camera.PlanarYUVLuminanceSource;

import java.util.Hashtable;

final class DecodeHandler extends Handler {

  private static final String TAG = DecodeHandler.class.getSimpleName();

  private final MipcaActivityCapture activity;
  private final MultiFormatReader multiFormatReader;

  DecodeHandler(MipcaActivityCapture activity, Hashtable<DecodeHintType, Object> hints) {
    multiFormatReader = new MultiFormatReader();
    //todo 修改代码
    hints=new Hashtable<>();
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
//        hints.put(DecodeHintType.POSSIBLE_FORMATS, BarcodeFormat.QR_CODE);
    multiFormatReader.setHints(hints);
    this.activity = activity;
  }

  @Override
  public void handleMessage(Message message) {
    switch (message.what) {
      case R.id.decode:
        //Log.d(TAG, "Got decode message");
        decode((byte[]) message.obj, message.arg1, message.arg2,false);
        break;
      case R.id.quit:
        Looper.myLooper().quit();
        break;
      case R.id.save_decode:
        decode((byte[]) message.obj, message.arg1, message.arg2, true);
        break;
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
  private void decode(byte[] data, int width, int height,boolean saveFlag) {
    long start = System.currentTimeMillis();
    Result rawResult = null;
    
    //modify here
    byte[] rotatedData = new byte[data.length];
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++)
            rotatedData[x * height + height - y - 1] = data[x + y * width];
    }
    int tmp = width; // Here we are swapping, that's the difference to #11
    width = height;
    height = tmp;
    try {
      PlanarYUVLuminanceSource sourceYUV = CameraManager.get().buildLuminanceSource(rotatedData, width, height);
      Bitmap bitmap=sourceYUV.renderCroppedGreyscaleBitmap();
      if(bitmap!=null) {
                ImageProcess imageProcess = new ImageProcess(bitmap);
                bitmap = imageProcess.doErode(bitmap);
        if (saveFlag) {
          UploadUtil.saveBitmap2Card(bitmap);
        }
        RGBLuminanceSource source = BitmapConverRGBLum(bitmap);
        BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
        rawResult = multiFormatReader.decodeWithState(bitmap1);
      }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      multiFormatReader.reset();
    }

    if (rawResult != null) {
      long end = System.currentTimeMillis();
      Log.d(TAG, "Found barcode (" + (end - start) + " ms):\n" + rawResult.toString());
      Message message = Message.obtain(activity.getHandler(), R.id.decode_succeeded, rawResult);
      Bundle bundle = new Bundle();
//      bundle.putParcelable(DecodeThread.BARCODE_BITMAP, source.renderCroppedGreyscaleBitmap());
      message.setData(bundle);
      //Log.d(TAG, "Sending decode succeeded message...");
      message.sendToTarget();
    } else {
      Message message = Message.obtain(activity.getHandler(), R.id.decode_failed);
      message.sendToTarget();
    }
  }

  private RGBLuminanceSource BitmapConverRGBLum(Bitmap bitmap) {
    int w=bitmap.getWidth();
    int h=bitmap.getHeight();
    int[] pixels=new int[w*h];
    bitmap.getPixels(pixels,0,w,0,0,w,h);
    RGBLuminanceSource source=new RGBLuminanceSource(w,h,pixels);
    return source;
  }
}
