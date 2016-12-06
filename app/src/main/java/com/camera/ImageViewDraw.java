package com.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/11/19.
 */
public class ImageViewDraw extends ImageView {
    private static int deviation=20;
    private List<Point> mOrgPoints;
    private List<Point> mPoints;
    private List<Point> mQRPoints;
    private float startX;
    private float starty;
    private int pointCount;

    private int drawPonintsMode=0;  //0不画点 1画绿色 2画红色
    public void setDrawPonintsFlag(int drawPonintsMode) {
        this.drawPonintsMode = drawPonintsMode;
        invalidate();
    }

    public ImageViewDraw(Context context) {
        super(context);
        initPoints();
    }

    public ImageViewDraw(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPoints();
    }

    public ImageViewDraw(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPoints();
    }

    public void setPoints(List<Point> points){
        mPoints=points;
    }

    public void setPoints(Point[] maxRect){
        mOrgPoints=new ArrayList<>();
        for(int i=0;i<4;i++){
            mOrgPoints.add(maxRect[i]);
        }
        initPoints();
        invalidate();
    }

    public List<Point> getMaxRectPoints(){
        return mPoints;
    }

    public List<Point> getQRRectPoints(){
        return mQRPoints;
    }

    private void initPoints() {
        mPoints=new ArrayList<Point>();
        if(mQRPoints==null){
            mQRPoints=new ArrayList<>();
            mQRPoints.add(new Point(400,400));
            mQRPoints.add(new Point(700,400));
            mQRPoints.add(new Point(700,700));
            mQRPoints.add(new Point(400,700));
        }

        pointCount=-1;
        if(mOrgPoints!=null){
            for (int i = 0; i < 4; i++) {
                mPoints.add(mOrgPoints.get(i));
            }
            drawPonintsMode=1;
        }else {
            for (int i = 0; i < 4; i++) {
                mPoints.add(new Point(0, 0));
            }
            drawPonintsMode=0;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if(pointCount!=-1){
                    float dx=event.getX()-startX;
                    float dy=event.getY()-starty;
                    if(drawPonintsMode==1) {
                        movePoints(mPoints,dx,dy);
//                        Point p = mPoints.get(pointCount);
                    }else if(drawPonintsMode==2) {
                        movePoints(mQRPoints,dx,dy);
//                        Point p = mQRPoints.get(pointCount);
                        if(pointCount==0){
                            oppositePoint(mQRPoints.get(1), dy, mQRPoints.get(3), dx);
                        }else if(pointCount==1){
                            oppositePoint(mQRPoints.get(0), dy, mQRPoints.get(2), dx);
                        }else if(pointCount==2){
                            oppositePoint(mQRPoints.get(3), dy, mQRPoints.get(1), dx);
                        }else if(pointCount==3){
                            oppositePoint(mQRPoints.get(2), dy, mQRPoints.get(0), dx);
                        }

                    }
                    startX=event.getX();
                    starty=event.getY();
                    //  p.x = event.getX();
                    //  p.y = event.getY();
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
//                if(count<4) {
//                    int endX = (int) event.getX();
//                    int endY = (int) event.getY();
//                    setPoint(new Point(endX, endY), count);
//                    count++;
//                }else {
//                    initPoints();
//                }
                pointCount=-1;
                invalidate();
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);

    }

    private void oppositePoint(Point pointY, float dy, Point pointX, float dx) {
        if(dy!=0){
            pointY.y+=dy;
        }
        if(dx!=0){
            pointX.x+=dx;
        }
    }

    private void movePoints(List<Point> points, float dx, float dy) {
        if(pointCount==10){ //全部移动
            for(int i=0;i<4;i++){
                Point p=points.get(i);
                p.y+=dy;
                p.x+=dx;
            }
        }else{
            Point p=points.get(pointCount);
            p.y+=dy;
            p.x+=dx;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:// 记录起始点x轴的值
                startX=event.getX();
                starty=event.getY();
                setPoint(new Point(startX,starty));
                return true;
            default:
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    private void setPoint(Point point, int count) {
        for(int i=0;i<4;i++){
            if(isPointContains(mPoints.get(i),point)){
                point=mPoints.get(i);
            }
        }
    }
    private void setPoint(Point point) {
        if(drawPonintsMode==1) {
            for (int i = 0; i < 4; i++) {
                if (isPointContains(mPoints.get(i), point)) {
                    pointCount = i;
                    return;
                }
            }
            if(isRectContains(mPoints.get(0),mPoints.get(2),point)){
                pointCount=10;
                return;
            }
        }else if(drawPonintsMode==2){
            for (int i = 0; i < 4; i++) {
                if (isPointContains(mQRPoints.get(i), point)) {
                    pointCount = i;
                    return;
                }
            }
            if(isRectContains(mQRPoints.get(0),mQRPoints.get(2),point)){
                pointCount=10;
                return;
            }
        }
        pointCount=-1;
    }

    private boolean isRectContains(Point p0, Point p2, Point point) {
        double x=point.x;
        double y=point.y;
        if(x>=p0.x && x<=p2.x){
            if(y>=p0.y && y<p2.y){
                return true;
            }
        }
        return false;
    }


    private boolean isPointContains(Point orgPont,Point point) {
        if(point.x<=(orgPont.x+deviation) && point.x>=(orgPont.x-deviation)){
            if(point.y<=(orgPont.y+deviation) && point.y>=(orgPont.y-deviation)){
                return true;
            }
        }
        return false;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(drawPonintsMode==1) {
            canvas.save();
            Paint p = new Paint();
            p.setColor(Color.GREEN);
            Path path = new Path();
            float firstx = 0, firsty = 0;
            p.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 4; i++) {
                float x = (float) mPoints.get(i).x;
                float y = (float) mPoints.get(i).y;
                canvas.drawCircle(x, y, deviation, p);
                if (i == 0) {
                    firstx = x;
                    firsty = y;
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            path.lineTo(firstx, firsty);
            p.setStyle(Paint.Style.STROKE);
            canvas.drawPath(path, p);
            canvas.restore();
        }else if(drawPonintsMode==2){
            canvas.save();
            Paint p = new Paint();
            p.setColor(Color.YELLOW);
            Path path = new Path();
            float firstx = 0, firsty = 0;
            p.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 4; i++) {
                float x = (float) mQRPoints.get(i).x;
                float y = (float) mQRPoints.get(i).y;
                canvas.drawCircle(x, y, deviation, p);
                if (i == 0) {
                    firstx = x;
                    firsty = y;
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            path.lineTo(firstx, firsty);
            p.setStyle(Paint.Style.STROKE);
            canvas.drawPath(path, p);
            canvas.restore();
        }
    }

}
