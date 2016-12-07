/*
 * Copyright 2009 ZXing authors
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

package com.zxing.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import com.google.zxing.LuminanceSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This object extends LuminanceSource around an array of YUV data returned from the camera driver,
 * with the option to crop to a rectangle within the full data. This can be used to exclude
 * superfluous pixels around the perimeter and speed up decoding.
 * <p/>
 * It works for any pixel format where the Y channel is planar and appears first, including
 * YCbCr_420_SP and YCbCr_422_SP.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class PlanarYUVLuminanceSource extends LuminanceSource {
    private final byte[] yuvData;
    private final int dataWidth;
    private final int dataHeight;
    private final int left;
    private final int top;

    public PlanarYUVLuminanceSource(byte[] yuvData, int dataWidth, int dataHeight, int left, int
            top,
                                    int width, int height) {
        super(width, height);

        if (left + width > dataWidth || top + height > dataHeight) {
            throw new IllegalArgumentException("Crop rectangle does not fit within image data.");
        }

        this.yuvData = yuvData;
        this.dataWidth = dataWidth;
        this.dataHeight = dataHeight;
        this.left = left;
        this.top = top;
    }

    @Override
    public byte[] getRow(int y, byte[] row) {
        if (y < 0 || y >= getHeight()) {
            throw new IllegalArgumentException("Requested row is outside the image: " + y);
        }
        int width = getWidth();
        if (row == null || row.length < width) {
            row = new byte[width];
        }
        int offset = (y + top) * dataWidth + left;
        System.arraycopy(yuvData, offset, row, 0, width);
        return row;
    }

    @Override
    public byte[] getMatrix() {
        int width = getWidth();
        int height = getHeight();

        // If the caller asks for the entire underlying image, save the copy and give them the
        // original data. The docs specifically warn that result.length must be ignored.
        if (width == dataWidth && height == dataHeight) {
            return yuvData;
        }

        int area = width * height;
        byte[] matrix = new byte[area];
        int inputOffset = top * dataWidth + left;

        // If the width matches the full width of the underlying data, perform a single copy.
        if (width == dataWidth) {
            System.arraycopy(yuvData, inputOffset, matrix, 0, area);
            return matrix;
        }

        // Otherwise copy one cropped row at a time.
        byte[] yuv = yuvData;
        for (int y = 0; y < height; y++) {
            int outputOffset = y * width;
            System.arraycopy(yuv, inputOffset, matrix, outputOffset, width);
            inputOffset += dataWidth;
        }
        return matrix;
    }

    @Override
    public boolean isCropSupported() {
        return true;
    }

    public int getDataWidth() {
        return dataWidth;
    }

    public int getDataHeight() {
        return dataHeight;
    }

    public Bitmap renderCroppedGreyscaleBitmap() {
        int width = getWidth();
        int height = getHeight();
        int[] pixels = new int[width * height];
        byte[] yuv = yuvData;
        int inputOffset = top * dataWidth + left;
        Log.e("top size", top+"left"+left);

        for (int y = 0; y < height; y++) {
            int outputOffset = y * width;
            for (int x = 0; x < width; x++) {
                int grey = yuv[inputOffset + x] & 0xff;
                pixels[outputOffset + x] = 0xFF000000 | (grey * 0x00010101);
            }
            inputOffset += dataWidth;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }


    public Bitmap getRGBredBitmap() throws IOException {
        int width = getWidth();
        int height = getHeight();
        byte[] rgbBuf = new byte[width * height];
        int inputOffset = top * dataWidth + left;
        //    decodeYUV420SP(rgbBuf,yuv,width,height);
        //    int[] pixels=convertByteToColor(rgbBuf);
        //    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        //    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        //    return bitmap;
        Point size=CameraManager.get().getConfigManager().getCameraResolution();
        YuvImage yuvimage = new YuvImage(yuvData, ImageFormat.NV21,size.y, size.x, null);//旋转了
        //data是onPreviewFrame参数提供
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, size.y, size.x), 100, baos);//100最高
        byte[] rawImage = baos.toByteArray();
        BitmapRegionDecoder bitmapRegionDecoder= BitmapRegionDecoder.newInstance(rawImage, 0,
                rawImage.length, false);
        BitmapFactory.Options opt=new BitmapFactory.Options();
        return bitmapRegionDecoder.decodeRegion(new Rect(left,top,dataWidth,dataHeight),opt);

    }

    private void YUVToGray(int[] pixels, byte[] data, int w, int h, int inputOffset) {
//        int inputOffset = 0;
        for (int y = 0; y < h; y++) {
            int outputOffset = y * w;
            for (int x = 0; x < w; x++) {
                int grey = data[inputOffset + x] & 0xff;
                pixels[outputOffset + x] = 0xFF000000 | (grey * 0x00010101);
            }
            inputOffset += w;
        }
    }


    private void decodeYUV420SP(byte[] rgbBuf, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;
        if (rgbBuf == null)
            throw new NullPointerException("buffer 'rgbBuf' is null");
        if (rgbBuf.length < frameSize * 3)
            throw new IllegalArgumentException("buffer 'rgbBuf' size " + rgbBuf.length
                    + " < minimum " + frameSize * 3);

        if (yuv420sp == null)
            throw new NullPointerException("buffer 'yuv420sp' is null");

        if (yuv420sp.length < frameSize * 3 / 2)
            throw new IllegalArgumentException("buffer 'yuv420sp' size " + yuv420sp.length
                    + " < minimum " + frameSize * 3 / 2);

        int i = 0, y = 0;
        int uvp = 0, u = 0, v = 0;
        int y1192 = 0, r = 0, g = 0, b = 0;

        for (int j = 0, yp = 0; j < height; j++) {
            uvp = frameSize + (j >> 1) * width;
            u = 0;
            v = 0;
            for (i = 0; i < width; i++, yp++) {
                y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                y1192 = 1192 * y;
                r = (y1192 + 1634 * v);
                g = (y1192 - 833 * v - 400 * u);
                b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                rgbBuf[yp * 3] = (byte) (r >> 10);
                //        rgbBuf[yp * 3 + 1] = (byte) (g >> 10);
                //        rgbBuf[yp * 3 + 2] = (byte) (b >> 10);
                rgbBuf[yp * 3 + 1] = 0;
                rgbBuf[yp * 3 + 2] = 0;
            }
        }
    }


    private int[] convertByteToColor(byte[] data) {
        int size = data.length;
        if (size == 0) {
            return null;
        }
        // 理论上data的长度应该是3的倍数，这里做个兼容
        int arg = 0;
        if (size % 3 != 0) {
            arg = 1;
        }
        int[] color = new int[size / 3 + arg];
        if (arg == 0) { // 正好是3的倍数
            for (int i = 0; i < color.length; ++i) {
                color[i] = (data[i * 3] << 16 & 0x00FF0000) | (data[i * 3 + 1] << 8 & 0x0000FF00)
                        | (data[i * 3 + 2] & 0x000000FF) | 0xFF000000;
            }
        } else { // 不是3的倍数
            for (int i = 0; i < color.length - 1; ++i) {
                color[i] = (data[i * 3] << 16 & 0x00FF0000) | (data[i * 3 + 1] << 8 & 0x0000FF00)
                        | (data[i * 3 + 2] & 0x000000FF) | 0xFF000000;
            }
            color[color.length - 1] = 0xFF000000; // 最后一个像素用黑色填充
        }
        return color;
    }
}
