package Util;

import java.awt.*;

/**
 * Created by pontu on 2017-03-31.
 */
public class Line {

    private double A,B;

    public Line(double A, double B){
        this.A = A;
        this.B = B;
    }

    public double f(double x){
        return A*x + B;
    }

    public void setA(double A){
        this.A = A;
    }

    public void setAFromHeading(double headingRadians){
        double dX = 10*Math.sin(headingRadians);
        double dY = 10*Math.cos(headingRadians);

        if(dX == 0){
            dX = Double.MIN_VALUE; //As close to zero as possible
        }

        A = dY/dX;
    }

    public void setB(double B){
        this.B = B;
    }

    public void plot(Graphics2D g, double origX, double origY, double length, Color color){
        g.setColor(color);

        double oneSide = length/2;
        g.drawLine((int)(origX - oneSide), (int)(f(-oneSide) + origY), (int)(origX + oneSide), (int)(f(origX + oneSide) + origY));
    }
}
