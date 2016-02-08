package pontus;

import java.awt.*;
import java.util.Vector;

/**
 * Created by Pontus on 2016-02-07.
 */
public class Vector2D
{
    private double x,y;

    public Vector2D(double x, double y){
        this.x = x;
        this.y = y;
    }

    public Vector2D(Point point){
        x = point.getX();
        y = point.getY();
    }

    public Vector2D(Vector<Double> vector){
        if(vector.size() >= 2){
            x = vector.get(0);
            y = vector.get(1);
        }else{
            x = 0;
            y = 0;
        }
    }

    public double getX(){return x;}
    public double getY(){return y;}

    //Dot product
    public double dot(Vector2D vector){
        return x*vector.getX() + y*vector.getY();
    }

    //Static version of the dot product
    public static double dot(Vector2D a, Vector2D b){
        return a.getX()*b.getX() + a.getY()*b.getY();
    }

    //Returns length of vector
    public double length(){
        return Math.sqrt(Math.pow(x,2) + Math.pow(y,2));
    }

    public static double getLength(Vector2D vector){
        return Math.sqrt(Math.pow(vector.getX(),2) + Math.pow(vector.getY(),2));
    }

    public double minAngle(Vector2D vector){
        //Linear Algebra!! A dot B = ||A|| * ||B|| * cosv
        //cosv = A dot B / ||A|| * ||B||
        //v = arccos((A dot B)/(||A|| * ||B||))

        return Math.toDegrees(Math.acos((dot(vector)/(length()*vector.length()))));
    }

    public double minAngleRadians(Vector2D vector){
        return Math.acos((dot(vector)/(length()*vector.length())));
    }

    public static double minAngle(Vector2D a, Vector2D b){
        return Math.toDegrees(Math.acos((dot(a,b)/(a.length()*b.length()))));
    }

    public static double minAngleRadians(Vector2D a, Vector2D b){
        return Math.acos((dot(a,b)/(a.length()*b.length())));
    }

    public void paintVector(Graphics2D g, double startX, double startY){
        g.setColor(Color.green);
        g.setStroke(new BasicStroke(2));
        g.drawLine((int)startX, (int)startY, (int)(startX + x), (int)(startY + y));
    }

    @Override
    public String toString(){
        return "[" + x + ";" + y + "]";
    }
}
