package pontus;

import Util.Vector2D;

/**
 * Created by Pontus on 2016-02-07.
 */
public class TestClass {
    public static void main(String[] args){
        TestClass test = new TestClass();
        test.test();

    }

    public void test(){
        testVector();
    }

    private void testVector(){
        Vector2D a = new Vector2D(1,0);
        Vector2D b = new Vector2D(0,-1);
        Vector2D c = new Vector2D(-1,0);
        Vector2D d = new Vector2D(-5,1);

        System.out.println(a.dot(b) == 0);
        System.out.println(a.minAngle(b) == 90);
        System.out.println(Vector2D.dot(a,b) == 0);
        System.out.println(Vector2D.minAngle(a,b) == 90);
        System.out.println(a.length() == 1);
        System.out.println(b.length() == 1);
        System.out.println(Vector2D.getLength(a) == 1);
        System.out.println(Vector2D.getLength(b) == 1);
        System.out.println(a.getHeading() == 90);
        System.out.println(b.getHeading() == 0);
        System.out.println(c.getHeading() == -90);
        System.out.println(d.getHeading());
    }
}
