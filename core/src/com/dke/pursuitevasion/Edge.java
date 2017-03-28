package com.dke.pursuitevasion;

/**
 * Created by Envy on 3/28/2017.
 */
public class Edge
{
    public final int index0;
    public final int index1;
    public Edge(int index0, int index1)
    {
        this.index0 = index0;
        this.index1 = index1;
    }
    @Override
    public String toString()
    {
        return "("+index0+","+index1+")";
    }
    @Override
    public int hashCode()
    {
        return index0 ^ index1;
    }
    @Override
    public boolean equals(Object object)
    {
        if (this == object)
        {
            return true;
        }
        if (object == null)
        {
            return false;
        }
        if (getClass() != object.getClass())
        {
            return false;
        }
        Edge that = (Edge) object;
        return
                (this.index0 == that.index0 &&
                        this.index1 == that.index1) ||
                        (this.index0 == that.index1 &&
                                this.index1 == that.index0);
    }
}