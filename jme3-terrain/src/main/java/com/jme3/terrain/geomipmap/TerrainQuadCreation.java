package com.jme3.terrain.geomipmap;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

public class TerrainQuadCreation {
	
	/*
	
    public static float[] createHeightSubBlock(float[] heightMap, int x,
		int y, int side) {
		float[] rVal = new float[side * side];
		int bsize = (int) FastMath.sqrt(heightMap.length);
		int count = 0;
		for (int i = y; i < side + y; i++) {
		    for (int j = x; j < side + x; j++) {
		        if (j < bsize && i < bsize)
		            rVal[count] = heightMap[j + (i * bsize)];
		        count++;
		    }
		}
		return rVal;
    }
	
    protected void setDefaultOffset(Vector2f tempOffset, Vector3f origin) {
    	tempOffset = new Vector2f();
    	tempOffset.x = offset.x;
    	tempOffset.y = offset.y;
    	tempOffset.x += origin.x;
    	tempOffset.y += origin.z;
    }
    
    protected  void attachQuad(float[] heightBlock, Vector3f origin, Vector2f tempOffset, int quaddrant, String name, int split, int blockSize) {	
    	TerrainQuad quad = new TerrainQuad(getName() + name, blockSize,
                split, stepScale, heightBlock, totalSize, tempOffset,
                offsetAmount);
    	
    	quad.setLocalTranslation(origin);
        quad.quadrant = quaddrant;
        this.attachChild(quad);
    }
    
    /**
     * Quadrants, world coordinates, and heightmap coordinates (Y-up):
     * 
     *         -z
     *      -u | 
     *    -v  1|3 
     *  -x ----+---- x
     *        2|4 u
     *         | v
     *         z
     * <code>createQuad</code> generates four new quads from this quad.
     * The heightmap's top left (0,0) coordinate is at the bottom, -x,-z
     * coordinate of the terrain, so it grows in the positive x.z direction.
     */
    
    /*
	
    protected static void createQuad(int blockSize, float[] heightMap, TerrainQuad terrainQuad) {
        // create 4 terrain quads
        int quarterSize = size >> 2;

        int split = (size + 1) >> 1;

        Vector2f tempOffset = new Vector2f();
        offsetAmount += quarterSize;

        //if (lodCalculator == null)
        //    lodCalculator = createDefaultLodCalculator(); // set a default one

        // 1 upper left of heightmap, upper left quad
        float[] heightBlock1 = createHeightSubBlock(heightMap, 0, 0, split);
    	Vector3f origin1 = new Vector3f(-quarterSize * stepScale.x, 0,
                -quarterSize * stepScale.z);
    	
    	setDefaultOffset(tempOffset, origin1);
        
        attachQuad(heightBlock1, origin1, tempOffset, 1, "Quad1", split, blockSize);

        // 2 lower left of heightmap, lower left quad
        float[] heightBlock2 = createHeightSubBlock(heightMap, 0, split - 1,
                        split);
        
    	Vector3f origin2 = new Vector3f(-quarterSize * stepScale.x, 0,
                quarterSize * stepScale.z);
    	
    	setDefaultOffset(tempOffset, origin2);
        
        attachQuad(heightBlock2, origin2, tempOffset, 2, "Quad2", split, blockSize);


        // 3 upper right of heightmap, upper right quad
        float[] heightBlock3 = createHeightSubBlock(heightMap, split - 1, 0,
                        split);

    	Vector3f origin3 = new Vector3f(quarterSize * stepScale.x, 0,
                -quarterSize * stepScale.z);
    	
    	setDefaultOffset(tempOffset, origin3);
        
        attachQuad(heightBlock3, origin3, tempOffset, 3, "Quad3", split, blockSize);
        
        // 4 lower right of heightmap, lower right quad
        float[] heightBlock4 = createHeightSubBlock(heightMap, split - 1,
                        split - 1, split);

    	Vector3f origin4 = new Vector3f(quarterSize * stepScale.x, 0,
                quarterSize * stepScale.z);
    	
    	setDefaultOffset(tempOffset, origin4);
        
        attachQuad(heightBlock4, origin4, tempOffset, 4, "Quad4", split, blockSize);

    }
    
    */

}
