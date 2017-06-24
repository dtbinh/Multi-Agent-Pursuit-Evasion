package com.dke.pursuitevasion.CellDecompose.Model;


import com.dke.pursuitevasion.CellDecompose.Graph.CXPoint;

import java.awt.*;
import java.util.ArrayList;

/**
 * Created by chenxi on 4/20/17.
 */
public class CXExtendInfoModel {
    public CXPoint extendedVertices;  // The vertical intersection
    public Point edgeNumber;       // The edge which intersected with original point
    public Boolean isUp;           // Extend direction
    public int extendedVerticesNumber;

    public CXExtendInfoModel(){}
}
