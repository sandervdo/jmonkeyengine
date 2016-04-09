package com.jme3.terrain.geomipmap;

public final class Utils {
	
    public static int findQuadrant(int x, int y, int size) {
        int split = (size + 1) >> 1;
        int quads = 0;
        if (x < split && y < split)
            quads |= 1;
        if (x < split && y >= split - 1)
            quads |= 2;
        if (x >= split - 1 && y < split)
            quads |= 4;
        if (x >= split - 1 && y >= split - 1)
            quads |= 8;
        return quads;
    }
    
    /**
     * Create just a flat heightmap
     */
    public static float[] generateDefaultHeightMap(int size) {
        float[] heightMap = new float[size*size];
        return heightMap;
    }
    
    /**
     * is the 2d point inside the terrain?
     * @param x local coordinate
     * @param z local coordinate
     * @return 
     */
    public static boolean isInside(int x, int z, int totalSize) {
        if (x < 0 || z < 0 || x > totalSize || z > totalSize)
            return false;
        return true;
    }
    
    
    


}
