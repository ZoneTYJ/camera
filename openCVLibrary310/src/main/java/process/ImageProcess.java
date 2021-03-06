package process;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tangyijian on 2016/11/15.
 */
public class ImageProcess {
    private Bitmap mBitmap;
    public ImageProcess(Bitmap bitmap){
//        mBitmap=bitmap;
    }

    public Mat getMat(){
        Mat dstMat =new Mat();
        Utils.bitmapToMat(mBitmap, dstMat);
        return dstMat;
    }

    public Mat getMat(Bitmap bitmap){
        Mat dstMat =new Mat();
        Utils.bitmapToMat(bitmap, dstMat);
        return dstMat;
    }

    public Bitmap getMat2Bitmap(Mat mat){
        Bitmap bitmap=Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }
    public Bitmap doCanny(Bitmap bmp){
        Mat orgMat=getMat(bmp);
        Mat mat=doCanny(orgMat);
        return getMat2Bitmap(mat);
    }

    public Rect doContours(Bitmap bmp){
        //        Mat preMat=colorFilter(getMat());
        Mat orgMat=getMat(bmp);
        return doContours(orgMat);
//        Mat mat=doContours(orgMat);
//        return getMat2Bitmap(mat);
    }

    public Rect doContours(){
        return doContours(mBitmap);
    }

    private Mat doCanny(Mat frame)
    {
        // init
        Mat grayImage = new Mat();
        Mat detectedEdges = new Mat();

        // convert to grayscale
        Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);

        // reduce noise with a 3x3 kernel
        Imgproc.blur(grayImage, detectedEdges, new Size(3, 3));

        // canny detector, with ratio of lower:upper threshold of 3:1
        double threshold=30;
        Imgproc.Canny(detectedEdges, detectedEdges, threshold, threshold * 3);

        // using Canny's output as a mask, display the result
        Mat dest = new Mat();
        detectedEdges.copyTo(dest);

        return detectedEdges;
    }

    private Rect doContours(Mat frame)
    {
        // init
        Mat grayImage = new Mat();
        Mat detectedEdges = new Mat();

        // convert to grayscale
        Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);
        // reduce noise with a 3x3 kernel
        Imgproc.blur(grayImage, detectedEdges, new Size(3, 3));

        //二值化
        Imgproc.adaptiveThreshold(detectedEdges, detectedEdges, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV,
                7, 7);

        detectedEdges=doDilate(detectedEdges);
        detectedEdges=doDilate(detectedEdges);
//        detectedEdges=doDilate(detectedEdges);

        List<MatOfPoint> list=new ArrayList<>();
        Mat hierarchy=new Mat();
        Imgproc.findContours(detectedEdges, list, hierarchy, Imgproc.RETR_EXTERNAL , Imgproc
                .CHAIN_APPROX_SIMPLE);
        List<Point> selectPoints = new ArrayList<Point>();
        List<MatOfPoint> checkPoints = new ArrayList<MatOfPoint>();
        int SampleTime = 20;
        double maxContourarea=-1;
        MatOfPoint maxcheckPoints=null;

        if (list != null && list.size() > 0) {
            Log.i("tag", "轮廓个数=" + list.size());
            Log.i("tag", "点个数=" + list.get(0).size().height);
            // Log.i("tag","..="+contours.get(1).size().height);

            for(MatOfPoint bean:list){
                MatOfPoint2f mat2f=new MatOfPoint2f(bean.toArray());
                double contourarea = Imgproc.contourArea(mat2f);
                if(contourarea>100 &&contourarea>maxContourarea){
//                    Imgproc.approxPolyDP(mat2f,mat2f,Imgproc.arcLength(mat2f,true)*0.02,true);
                    maxContourarea=contourarea;
                    MatOfPoint mop=new MatOfPoint(mat2f.toArray());
                    maxcheckPoints=mop;
                }
//                org.opencv.core.Point[] points = bean.toArray();
//                int a=0;

            }
//            for (Point point : points) {// 遍历每一个point
//                // Log.i("tag", "x=" + point.x + ",y=" + point.y);
//                count++;
//                if (count == SampleTime) {// SampleTime个点采样一个点
//                    count = 0;
//                    Point selectPoint = new Point(point.x, point.y);
//                    selectPoints.add(selectPoint);
//                    byte[] temp_byte = new byte[4];
//                    temp_byte = intToByteArray((int) point.x);
//                    keyPoints[num][0] = temp_byte[0];
//                    keyPoints[num][1] = temp_byte[1];
//                    keyPoints[num][2] = temp_byte[2];
//                    keyPoints[num][3] = temp_byte[3];
//                    temp_byte = intToByteArray((int) point.y);
//                    keyPoints[num][4] = temp_byte[0];
//                    keyPoints[num][5] = temp_byte[1];
//                    keyPoints[num][6] = temp_byte[2];
//                    keyPoints[num][7] = temp_byte[3];
//                    num++;
//                }
//            }
            Log.i("tag", "存储完毕！");
        }
        checkPoints.add(maxcheckPoints);
//        Mat dest = new Mat(frame.rows(), frame.cols(), frame.type());
//        Imgproc.drawContours(dest, checkPoints, -1, new Scalar(255,255,255));
       return  calcuMaxRect(maxcheckPoints);
//        return dest;

    }

    public Bitmap doLinearTransform(Bitmap bitmap){
        Mat mat=getMat(bitmap);
        Mat hist=CreateGrayImageHist(mat);
        int maxHist=calcMaxHist(hist);
        mat=doBinary(mat,maxHist-15);
//        Mat mat= doErode(getMat(bitmap));
//        mat=doLinearTransform(mat,0,250,150);
        return getMat2Bitmap(mat);
    }

    private Mat doErode(Mat mat){
        Mat dest=new Mat();
        Imgproc.erode(mat, dest, new Mat());
        return dest;
    }

    public Bitmap ContraValue(){
        Mat mat=ContraValue(getMat());
        return getMat2Bitmap(mat);
    }
    //对比度平衡
    private Mat ContraValue(Mat mat){
        List<Mat> mv=new ArrayList<>();
        List<Mat> dstMv=new ArrayList<>();
        Core.split(mat, mv);
        for(Mat ma:mv){
            Mat dstma=new Mat();
            Imgproc.equalizeHist(ma,dstma);
            dstMv.add(dstma);
        }
        Mat dest=new Mat();
        Core.merge(dstMv, dest);
        return dest;
    }

    private Mat doDilate(Mat mat){
        Mat dest=new Mat();
        Imgproc.dilate(mat, dest, new Mat());
        return dest;
    }


    private Mat colorFilter(Mat mat){
        Mat hsvImage=new Mat();
        Imgproc.cvtColor(mat, hsvImage, Imgproc.COLOR_RGB2HSV);
        CvType.depth(24);
        byte[] data={(byte) 255, (byte) 255, (byte) 255, (byte) 255};
        for(int i=0;i<mat.height();i++){
            for(int j=0;j<mat.width();j++){
                int h= (int) hsvImage.get(i,j)[0];
                int s=(int) hsvImage.get(i,j)[1];
                if(((h>0 && h<24)||(h>140 && h<180))){
                    mat.put(i,j,data);
                }
            }
        }
//        Scalar scalar=new Scalar(255,255,255,255);
//        Mat blueImgae=new Mat(mat.height(),mat.width(),24,scalar);
//        Imgproc.cvtColor(mat, hsvImage, Imgproc.COLOR_BGR2HSV);
//        byte[] blueData=new byte[4];
//        for(int i=0;i<blueImgae.height();i++){
//            for(int j=0;j<blueImgae.width();j++){
//                int h= (int) hsvImage.get(i,j)[0];
//                if(((h>0 && h<24)||(h>140 && h<180))){
//                    mat.get(i,j,blueData);
//                    blueImgae.put(i,j,blueData);
//                }
//            }
//        }
//        blueImgae=doCanny(mat);
        return mat;
    }


    private Mat doLinearTransform(Mat mat,int brightness,int contrast,int maxHist){
        double B = brightness / 255.;
        double C = contrast / 255. ;
        double M = Math.tan((45 + 44 * C) / 180 * Math.PI);
        byte[] data=new byte[4];
        for(int i=0;i<mat.height();i++){
            for(int j=0;j<mat.width();j++){
                mat.get(i,j,data);
                for(int m=0;m<3;m++){
                   int temp= (int) (((data[m]&0xff) - maxHist * (1 - B)) * M);
                    if(temp>255){
                        temp=255;
                    }
                    if(temp<0){
                        temp=0;
                    }
                    data[m]= (byte) temp;
                }
                mat.put(i,j,data);
            }
        }
        return mat;
    }

    private Mat doBinary(Mat mat,int maxHist){
        byte[] data=new byte[4];
        for(int i=0;i<mat.height();i++){
            for(int j=0;j<mat.width();j++){
                mat.get(i,j,data);
                for(int m=0;m<3;m++){
                    int temp= (int) ((data[m]&0xff)-maxHist);
                    if(temp<0){
                        temp=0;
                    }else{
                        temp=255;
                    }
                    data[m]= (byte) temp;
                }
                mat.put(i,j,data);
            }
        }
        return mat;
    }


    public Map<String,Bitmap> doSplitInvoice(Bitmap bmp, List<Point> maxRectPoints) {
        Mat orgMat=getMat(bmp);
        bmp.recycle();
        bmp=null;
        //计算旋转角度,用右上和右下
        Point upper=maxRectPoints.get(1);
        Point undder=maxRectPoints.get(2);
//        double dx=  (upper.x-undder.x);
//        double dy= (upper.y-undder.x);
//        double degree;
//        if(dx<0){ //右上点在右下点的左边
//            degree=-Math.atan2(-dx, -dy);      //正表示逆时针
//        }else {
//            degree=Math.atan2(dx, -dy);
//        }
        //计算缩放比例 用右上和右下 假设等比缩放
        double distance= Math.pow((upper.x - undder.x), 2) + Math.pow((upper.y - undder.y), 2);
        distance = Math.sqrt(distance);
        double scale=StandImageUtils.getYdistance(StandImageUtils.maxRect)/distance;


        Map<String,Bitmap> bitmaps=new HashMap<>();
//        Mat mat= doRotationMatrixRect(orgMat, maxRectPoints.get(2), degree, 1, 1);
        Mat mat= doAffineTransform(orgMat, maxRectPoints, StandImageUtils.maxRect,scale);
        //原图变换后
        bitmaps.put("invoice", getMat2Bitmap(mat));
        Rect rect;
        //切图
        rect=new Rect(StandImageUtils.getValues(StandImageUtils.underRightCorner));
        bitmaps.put("invMemo", getMat2Bitmap(new Mat(mat, rect)));

        rect=new Rect(StandImageUtils.getValues(StandImageUtils.upperLeftCorner));
        bitmaps.put("invLhead", getMat2Bitmap(new Mat(mat, rect)));

        rect=new Rect(StandImageUtils.getValues(StandImageUtils.upperRightCorner));
        bitmaps.put("invRhead",getMat2Bitmap(new Mat(mat, rect)));

        return bitmaps;
    }

    private Mat doRotationMatrixRect(Mat orgMat, Point center, double degree, double scaleX,
                                     double scaleY) {
        Mat array=Imgproc.getRotationMatrix2D(center,degree,scaleY);
        Mat dst=new Mat();
        Size size=new Size(orgMat.width()*scaleX,orgMat.height()*scaleY);

        Imgproc.warpAffine(orgMat, dst, array, size);
        return dst;
    }


    private Mat doAffineTransform(Mat orgMat,List<Point> maxRectPoints,Point[] points,double scale){
        //取前三个点
        MatOfPoint2f maf=new MatOfPoint2f(maxRectPoints.get(0),maxRectPoints.get(1),maxRectPoints.get(2));
        MatOfPoint2f dsf=new MatOfPoint2f(points[0],points[1],points[2]);
        Mat array=Imgproc.getAffineTransform(maf, dsf);
        Mat dst=new Mat();
        Size size=new Size(points[2].x,points[2].y);
        Imgproc.warpAffine(orgMat, dst, array, size);
        return dst;
    }

   //把标准图上的坐标点转换到 原图上
    private Mat doAffineTransform(Mat orgMat,List<Point> maxRectPoints,Point[] points,Point[] standPoints){
        //取前三个点
        MatOfPoint2f maf=new MatOfPoint2f(maxRectPoints.get(0),maxRectPoints.get(1),maxRectPoints.get(2));
        MatOfPoint2f dsf=new MatOfPoint2f(points[0],points[1],points[2]);
        Mat array=Imgproc.getAffineTransform(dsf,maf);
        Mat dst=new Mat();
//        Imgproc.warpAffine(orgMat, dst, array, orgMat.size());

        MatOfPoint2f stdf=new MatOfPoint2f(standPoints[0],standPoints[1],standPoints[2]);
        Imgproc.warpAffine(stdf, dst, array, orgMat.size());
        //dst 是获取的新左边点
        //todo 在原图像上切割
        return dst;
    }

    private Mat CreateGrayImageHist(Mat frame){
        List<Mat> images = new ArrayList<Mat>();
        Core.split(frame, images);
        MatOfInt histSize = new MatOfInt(256);
        MatOfFloat histRange = new MatOfFloat(0, 256);
        MatOfInt channels = new MatOfInt(0);
        Mat hist = new Mat();
        Imgproc.calcHist(images.subList(0, 1), channels, new Mat(), hist, histSize, histRange, false);
        int a=0;
        return hist;
    }

    private int calcMaxHist(Mat hist){
        int maxHist=0;
        return maxHist;
    }

    private static Rect calcuMaxRect(MatOfPoint maxcheckPoints) {
        List<Point> allPointList = maxcheckPoints.toList();
        //所有点集合中最左边、最上边、最右边、最下边
        double minX = 999999;
        Map<Double, Double> minXMap = new HashMap<>();
        double maxX = 0;
        Map<Double, Double> maxXMap = new HashMap<>();
        double minY = 999999;
        Map<Double, Double> minYMap = new HashMap<>();
        double maxY = 0;
        Map<Double, Double> maxYMap = new HashMap<>();
        //累计次数达到阈值，则置为极值，防止少量噪点影响。
        int threshold = 10;
        int minXTimes = 0;
        int maxXTimes = 0;
        int minYTimes = 0;
        int maxYTimes = 0;
        for (Point point : allPointList) {
            double x = point.x;
            double y = point.y;
            //最小x
            if (x < minX) {
                minX = x;
                minXTimes = 0;
            } else {
                minXTimes++;
            }
            //最大x
            if (x > maxX) {
                maxX = x;
                maxXTimes = 0;
            } else {
                maxXTimes++;
            }
            //最小y
            if (y < minY) {
                minY = y;
                minYTimes = 0;
            } else {
                minYTimes++;
            }
            //最大y
            if (y > maxY) {
                maxY = y;
                maxYTimes = 0;
            } else {
                maxYTimes++;
            }
            if (minXTimes > threshold) { //记录暂时大于阈值的值
                if (minXMap.get(minX) == null) {
                    minXMap.put(minX, minX);
                }
            }
            if (maxXTimes > threshold) { //记录暂时大于阈值的值
                if (maxXMap.get(maxX) == null) {
                    maxXMap.put(maxX, maxX);
                }
            }
            if (minYTimes > threshold) { //记录暂时大于阈值的值
                if (minYMap.get(minY) == null) {
                    minYMap.put(minY, minY);
                }
            }
            if (maxYTimes > threshold) { //记录暂时大于阈值的值
                if (maxYMap.get(maxY) == null) {
                    maxYMap.put(maxY, maxY);
                }
            }
        }
        minX = 999999;
        for (double x : minXMap.values()) {
            if (x < minX) {
                minX = x;
            }
        }
        maxX = 0;
        for (double x : maxXMap.values()) {
            if (x > maxX) {
                maxX = x;
            }
        }
        minY = 999999;
        for (double y : minYMap.values()) {
            if (y < minY) {
                minY = y;
            }
        }
        maxY = 0;
        for (double y : maxYMap.values()) {
            if (y > maxY) {
                maxY = y;
            }
        }
        Rect rect = new Rect((int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY));
        return rect;
    }
}
