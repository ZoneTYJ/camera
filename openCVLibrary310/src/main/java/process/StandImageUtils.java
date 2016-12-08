package process;

import org.opencv.core.Point;

/**
 * Created by Administrator on 2016/11/20.
 */
public class StandImageUtils {
    public static Point[] initRect={new Point(55,260),new Point(490,260),new Point(490,480),new Point(55,480)};//初始默认大小
    //左上 右上 右下 左下
    public static Point[] maxRect={new Point(110,535),new Point(980,535),new Point(980,960),new Point(110,960)};//中间框
    public static Point[] upperRightCorner={new Point(700,430),new Point(980,430),new Point(980,520),new Point(700,520)};//右上角切图
    public static Point[] upperLeftCorner={new Point(200,430),new Point(380,430),new Point(380,530),new Point(200,530)};//左上角切图
    public static Point[] underRightCorner={new Point(635,875),new Point(975,875),new Point(975,955),new Point(635,955)};//右下角切图


    public static double getYdistance(Point[] points){
        return points[2].y-points[1].y;
    }

    public static double[] getValues(Point[] points) {
        double[] values={points[0].x, points[0].y, points[1].x - points[0].x, points[2].y - points[0].y};
        return values;
    }
}
