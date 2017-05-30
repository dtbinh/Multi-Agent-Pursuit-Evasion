package com.dke.pursuitevasion.CellDecompose.Graph;

import java.io.*;

/**
 * Created by chenxi on 4/16/17.
 */
public class CXPoint implements Serializable {
    public double x;
    public double y;

    public CXPoint(){}
    public CXPoint(double x, double y){
        this.x = x;
        this.y = y;
    }
    public static Object deepCopy(Object o) throws IOException, ClassNotFoundException {
//      //先序列化，写入到流里
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bo);
        oo.writeObject(o);
        //然后反序列化，从流里读取出来，即完成复制
        ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
        ObjectInputStream oi = new ObjectInputStream(bi);
        return oi.readObject();
    }

    public static double computeTheDistanceFromPointToLine(CXPoint orginalPoint, CXPoint linePoint1, CXPoint linePoint2) {
        boolean returnValue;
        double y1 = linePoint1.y;
        double x1 = linePoint1.x;
        double y2 = linePoint2.y;
        double x2 = linePoint2.x;
        double gradientK = (y2- y1)/(x2 - x1);
        double C = y2 - gradientK*x2;
        double distance = gradientK*orginalPoint.x + (-1)*orginalPoint.y + C;
        return distance;
    }

    public static double distance(CXPoint point1, CXPoint point2){
        double a = point2.y-point1.y;
        double b = point2.x - point1.x;
        double distance = Math.pow(a,2) + Math.pow(b,2);
        return Math.sqrt(distance);
    }

    public static CXPoint convertToOriginalCoordination(CXPoint newCoordinationPoint){
        CXPoint point = new CXPoint();
        point.x = newCoordinationPoint.x - 5;
        point.y = (newCoordinationPoint.y - 5) * -1;
        return point;
    }

    public static CXPoint converToGraphCoordiantion(CXPoint orignialCoordinatePoint){
        CXPoint point = new CXPoint();
        point.x  = orignialCoordinatePoint.x + 5;
        point.y = orignialCoordinatePoint.y * (-1) + 5;
        return point;
    }
}
