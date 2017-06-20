package com.dke.pursuitevasion.AI.PotentialField;

/**
 * Created by imabeast on 6/20/17.
 */

public class Triplet {

    private int x,y,value;

    public Triplet(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Triplet (int x, int y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public void setAll(int x, int y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getValue() {
        return value;
    }

    public void printTriplet() {
        System.out.println("x = " + x + ", y = " + y + " value = " + value);
    }

    public String tripletToString() {
        return new String("x = " + x + ", y = " + y + " value = " + value);
    }
}
