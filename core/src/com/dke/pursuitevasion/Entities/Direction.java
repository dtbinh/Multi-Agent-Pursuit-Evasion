package com.dke.pursuitevasion.Entities;

/**
 * Created by Nicola Gheza on 08/05/2017.
 */
public enum Direction
{
    CLOCKWISE(1), COUNTERCLOCKWISE(-1);

    private int value;

    private Direction(int value)
    {
        this.value = value;
    }

    public Direction invert()
    {
        if(value == 1)
            return COUNTERCLOCKWISE;
        else
            return CLOCKWISE;
    }

    public int value()
    {
        return value;
    }
};