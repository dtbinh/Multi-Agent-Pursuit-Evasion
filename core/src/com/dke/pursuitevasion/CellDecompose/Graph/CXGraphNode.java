package com.dke.pursuitevasion.CellDecompose.Graph;

import java.io.*;

/**
 * Created by chenxi on 4/16/17.
 */
public class CXGraphNode implements Serializable {
    public int nodeNumber;
    public CXPoint location;

    public CXGraphNode(){};
    public CXGraphNode(CXPoint location,int nodeNumber){
        this.location = location;
        this.nodeNumber = nodeNumber;
    }
    public CXGraphNode(CXPoint location){
        this.location  = location;
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
}
