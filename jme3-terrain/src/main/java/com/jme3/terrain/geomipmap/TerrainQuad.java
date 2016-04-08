/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.terrain.geomipmap;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireBox;
import com.jme3.terrain.ProgressMonitor;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.lodcalc.LodCalculator;
import com.jme3.terrain.geomipmap.picking.BresenhamTerrainPicker;
import com.jme3.terrain.geomipmap.picking.TerrainPickData;
import com.jme3.terrain.geomipmap.picking.TerrainPicker;
import com.jme3.util.SafeArrayList;
import com.jme3.util.TangentBinormalGenerator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * TerrainQuad is a heightfield-based terrain system. Heightfield terrain is fast and can
 * render large areas, and allows for easy Level of Detail control. However it does not
 * permit caves easily.
 * TerrainQuad is a quad tree, meaning that the root quad has four children, and each of
 * those children have four children. All the way down until you reach the bottom, the actual
 * geometry, the TerrainPatches.
 * If you look at a TerrainQuad in wireframe mode with the TerrainLODControl attached, you will
 * see blocks that change their LOD level together; these are the TerrainPatches. The TerrainQuad
 * is just an organizational structure for the TerrainPatches so patches that are not in the
 * view frustum get culled quickly.
 * TerrainQuads size are a power of 2, plus 1. So 513x513, or 1025x1025 etc.
 * Each point in the terrain is one unit apart from its neighbour. So a 513x513 terrain
 * will be 513 units wide and 513 units long.
 * Patch size can be specified on the terrain. This sets how large each geometry (TerrainPatch)
 * is. It also must be a power of 2 plus 1 so the terrain can be subdivided equally.
 * </p>
 * <p>
 * The height of the terrain can be modified at runtime using setHeight()
 * </p>
 * <p>
 * A terrain quad is a node in the quad tree of the terrain system.
 * The root terrain quad will be the only one that receives the update() call every frame
 * and it will determine if there has been any LOD change.
 * </p><p>
 * The leaves of the terrain quad tree are Terrain Patches. These have the real geometry mesh.
 * </p><p>
 * Heightmap coordinates start from the bottom left of the world and work towards the
 * top right.
 * </p><pre>
 *  +x
 *  ^
 *  | ......N = length of heightmap
 *  | :     :
 *  | :     :
 *  | 0.....:
 *  +---------&gt; +z
 * (world coordinates)
 * </pre>
 * @author Brent Owens
 */
public class TerrainQuad extends Node implements Terrain {
    protected Vector2f offset;

    protected int totalSize; // the size of this entire terrain tree (on one side)

    protected int size; // size of this quad, can be between totalSize and patchSize

    protected int patchSize; // size of the individual patches

    protected Vector3f stepScale;

    protected float offsetAmount;

    protected int quadrant = 0; // 1=upper left, 2=lower left, 3=upper right, 4=lower right
    private int maxLod = -1;
    private BoundingBox affectedAreaBBox; // only set in the root quad

    private TerrainPicker picker;
    private Vector3f lastScale = Vector3f.UNIT_XYZ;

    protected NeighbourFinder neighbourFinder;
    
    public TerrainQuad() {
        super("Terrain");
    }

    /**
     * Creates a terrain with:
     * <ul>
     * <li>the total, real-world, size of the terrain</li>
     * <li>the patchSize, or the size of each geometry tile of the terrain</li>
     * <li>the heightmap that defines the height of the terrain</li>
     * </ul>
     * <p>
     * A TerrainQuad of totalSize 513x513 will be 513 units wide and 513 units long.
     * PatchSize is just used to subdivide the terrain into tiles that can be culled.
     * </p>
     * @param name the name of the scene element. This is required for
     * identification and comparison purposes.
     * @param patchSize size of the individual patches (geometry). Power of 2 plus 1, 
     * must be smaller than totalSize. (eg. 33, 65...)
     * @param totalSize the size of this entire terrain (on one side). Power of 2 plus 1 
     * (eg. 513, 1025, 2049...)
     * @param heightMap The height map to generate the terrain from (a flat
     * height map will be generated if this is null). The size of one side of the heightmap 
     * must match the totalSize. So a 513x513 heightmap is needed for a terrain with totalSize of 513.
     */
    public TerrainQuad(String name, int patchSize, int totalSize, float[] heightMap) {
        this(name, patchSize, totalSize, Vector3f.UNIT_XYZ, heightMap);
                
        affectedAreaBBox = new BoundingBox(new Vector3f(0,0,0), size*2, Float.MAX_VALUE, size*2);
        fixNormalEdges(affectedAreaBBox);
        addControl(new NormalRecalcControl(this));
    }
    
    /**
     * 
     * @param name the name of the scene element. This is required for
     * identification and comparison purposes.
     * @param patchSize size of the individual patches
     * @param quadSize
     * @param totalSize the size of this entire terrain tree (on one side)
     * @param heightMap The height map to generate the terrain from (a flat
     * height map will be generated if this is null)
     */
    @Deprecated
    public TerrainQuad(String name, int patchSize, int quadSize, int totalSize, float[] heightMap) {
        this(name, patchSize, totalSize, quadSize, Vector3f.UNIT_XYZ, heightMap);
    }

    /**
     * 
     * @param name the name of the scene element. This is required for
     * identification and comparison purposes.
     * @param patchSize size of the individual patches
     * @param size size of this quad, can be between totalSize and patchSize
     * @param scale
     * @param heightMap The height map to generate the terrain from (a flat
     * height map will be generated if this is null)
     */
    @Deprecated
    public TerrainQuad(String name, int patchSize, int size, Vector3f scale, float[] heightMap) {
        this(name, patchSize, size, scale, heightMap, size, new Vector2f(), 0);
        //affectedAreaBBox = new BoundingBox(new Vector3f(0,0,0), size*2, Float.MAX_VALUE, size*2);
        //fixNormalEdges(affectedAreaBBox);
        //addControl(new NormalRecalcControl(this));
    }
    
    /**
     * 
     * @param name the name of the scene element. This is required for
     * identification and comparison purposes.
     * @param patchSize size of the individual patches
     * @param totalSize the size of this entire terrain tree (on one side)
     * @param quadSize
     * @param scale
     * @param heightMap The height map to generate the terrain from (a flat
     * height map will be generated if this is null)
     */
    @Deprecated
    public TerrainQuad(String name, int patchSize, int totalSize, int quadSize, Vector3f scale, float[] heightMap) {
        this(name, patchSize, quadSize, scale, heightMap, totalSize, new Vector2f(), 0);
        //affectedAreaBBox = new BoundingBox(new Vector3f(0,0,0), totalSize*2, Float.MAX_VALUE, totalSize*2);
        //fixNormalEdges(affectedAreaBBox);
        //addControl(new NormalRecalcControl(this));
    }
    

    public TerrainQuad(String name, int patchSize, int quadSize,
                            Vector3f scale, float[] heightMap, int totalSize,
                            Vector2f offset, float offsetAmount)
    {
    	
        super(name);        
        
        if (heightMap == null)
            heightMap = Utils.generateDefaultHeightMap(quadSize);
        
        if (!FastMath.isPowerOfTwo(quadSize - 1)) {
            throw new RuntimeException("size given: " + quadSize + "  Terrain quad sizes may only be (2^N + 1)");
        }
        if (FastMath.sqrt(heightMap.length) > quadSize) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Heightmap size is larger than the terrain size. Make sure your heightmap image is the same size as the terrain!");
        }
        
        TerrainQuadObject quadObj = new TerrainQuadObject();
        
        this.offset = offset;
        this.offsetAmount = offsetAmount;
        this.totalSize = totalSize;
        this.size = quadSize;
        this.patchSize = patchSize;
        this.stepScale = scale;
        split(patchSize, heightMap);
    }

    public void setNeighbourFinder(NeighbourFinder neighbourFinder) {
        this.neighbourFinder = neighbourFinder;
        TerrainTransform.resetCachedNeighbours(this);
    }

    /**
     * Forces the recalculation of all normals on the terrain.
     */
    public void recalculateAllNormals() {
        affectedAreaBBox = new BoundingBox(new Vector3f(0,0,0), totalSize*2, Float.MAX_VALUE, totalSize*2);
    }
    
    public SafeArrayList<Spatial> getChildren() {
    	return children;
    }
    
    public void setChildren(SafeArrayList<Spatial> child) {
    	this.children = child;
    }
    
    public void setQuadrant(int quad) {
    	this.quadrant = quad;
    }
    
    
    public Node getRemoteParent() {
    	return getParent();
    }
    
    public NeighbourFinder getNeighbourFinder() {
    	return this.neighbourFinder;
    }
    
    public void setPicker(TerrainPicker tp) {
    	this.picker = tp;
    }
    
    public TerrainPicker getPicker() {
    	return picker;
    }
    
   
    
    

    /**
     * update the normals if there were any height changes recently.
     * Should only be called on the root quad
     */
    protected void updateNormals() {

        if (needToRecalculateNormals()) {
            //TODO background-thread this if it ends up being expensive
            fixNormals(affectedAreaBBox); // the affected patches
            fixNormalEdges(affectedAreaBBox); // the edges between the patches
            
            setNormalRecalcNeeded(null); // set to false
        }
    }
    
    /**
     * Caches the transforms (except rotation) so the LOD calculator,
     * which runs on a separate thread, can access them safely.
     */
    protected void cacheTerrainTransforms() {
    	TerrainTransform.cacheTerrainTransforms(this);
    }

    /**
     * Generate the entropy values for the terrain for the "perspective" LOD
     * calculator. This routine can take a long time to run!
     * @param progressMonitor optional
     */
    public void generateEntropy(ProgressMonitor progressMonitor) {
    	TerrainTransform.generateEntropy(progressMonitor, this);
    }

    protected boolean isRootQuad() {
        return (getParent() != null && !(getParent() instanceof TerrainQuad) );
    }

    public Material getMaterial() {
        return getMaterial(null);
    }
    
    public Material getMaterial(Vector3f worldLocation) {
        // get the material from one of the children. They all share the same material
        if (children != null) {
            for (int i = children.size(); --i >= 0;) {
                Spatial child = children.get(i);
                if (child instanceof TerrainQuad) {
                    return ((TerrainQuad)child).getMaterial(worldLocation);
                } else if (child instanceof TerrainPatch) {
                    return ((TerrainPatch)child).getMaterial();
                }
            }
        }
        return null;
    }

    public int getNumMajorSubdivisions() {
        return 1;
    }
    

    protected synchronized void findNeighboursLod(HashMap<String,UpdatedTerrainPatch> updated) {
    	TerrainNeighbours.findNeighboursLod(updated, this);
    }


    
    /**
     * Find any neighbours that should have their edges seamed because another neighbour
     * changed its LOD to a greater value (less detailed)
     */
    protected synchronized void fixEdges(HashMap<String,UpdatedTerrainPatch> updated) {
    	TerrainNeighbours.fixEdges(updated, this);
    }

    protected synchronized void reIndexPages(HashMap<String,UpdatedTerrainPatch> updated, boolean usesVariableLod) {
        if (children != null) {
            for (int i = children.size(); --i >= 0;) {
                Spatial child = children.get(i);
                if (child instanceof TerrainQuad) {
                    ((TerrainQuad) child).reIndexPages(updated, usesVariableLod);
                } else if (child instanceof TerrainPatch) {
                    ((TerrainPatch) child).reIndexGeometry(updated, usesVariableLod);
                }
            }
        }
    }

    /**
     * <code>split</code> divides the heightmap data for four children. The
     * children are either quads or patches. This is dependent on the size of the
     * children. If the child's size is less than or equal to the set block
     * size, then patches are created, otherwise, quads are created.
     *
     * @param blockSize
     *			the blocks size to test against.
     * @param heightMap
     *			the height data.
     */
    protected void split(int blockSize, float[] heightMap) {
        if ((size >> 1) + 1 <= blockSize) {
            createQuadPatch(heightMap);
        } else {
            createQuad(blockSize, heightMap);
        }

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
    
    
    public void createQuad(int blockSize, float[] heightMap) {
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

    public void generateDebugTangents(Material mat) {
        for (int x = children.size(); --x >= 0;) {
            Spatial child = children.get(x);
            if (child instanceof TerrainQuad) {
                ((TerrainQuad)child).generateDebugTangents(mat);
            } else if (child instanceof TerrainPatch) {
                Geometry debug = new Geometry( "Debug " + name,
                    TangentBinormalGenerator.genTbnLines( ((TerrainPatch)child).getMesh(), 0.8f));
                attachChild(debug);
                debug.setLocalTranslation(child.getLocalTranslation());
                debug.setCullHint(CullHint.Never);
                debug.setMaterial(mat);
            }
        }
    }

    /**
     * <code>createQuadPatch</code> creates four child patches from this quad.
     */
    
    protected Vector2f createBasicOffset() {
    	Vector2f tempOffset = new Vector2f();
        tempOffset.x = offset.x;
        tempOffset.y = offset.y;
        return tempOffset;
    }
    
    protected void addPatch(String name, int quadrant, int split, float[] heightBlock, Vector3f origin, Vector2f tempOffset) {
    	TerrainPatch patch = new TerrainPatch(getName() + "Patch1", split,
                stepScale, heightBlock, origin, totalSize, tempOffset,
                offsetAmount);
    	patch.setQuadrant((short) quadrant);
    	this.attachChild(patch);
    	patch.setModelBound(new BoundingBox());
    	patch.updateModelBound();
        //patch.setLodCalculator(lodCalculator);
        //TangentBinormalGenerator.generate(patch);
    }
    
    protected void createQuadPatch(float[] heightMap) {
        // create 4 terrain patches
        int quarterSize = size >> 2;
        int halfSize = size >> 1;
        int split = (size + 1) >> 1;

        //if (lodCalculator == null)
        //    lodCalculator = createDefaultLodCalculator(); // set a default one

        offsetAmount += quarterSize;

        // 1 lower left
        float[] heightBlock1 = createHeightSubBlock(heightMap, 0, 0, split);

        Vector3f origin1 = new Vector3f(-halfSize * stepScale.x, 0, -halfSize
                        * stepScale.z);

        Vector2f tempOffset1 = createBasicOffset();
        tempOffset1.x += origin1.x / 2;
        tempOffset1.y += origin1.z / 2;

        addPatch("Patch1", 1, split, heightBlock1, origin1, tempOffset1);

        // 2 upper left
        float[] heightBlock2 = createHeightSubBlock(heightMap, 0, split - 1,
                        split);

        Vector3f origin2 = new Vector3f(-halfSize * stepScale.x, 0, 0);

        Vector2f tempOffset2 = createBasicOffset();
        tempOffset2.x += origin1.x / 2;
        tempOffset2.y += quarterSize * stepScale.z;

        addPatch("Patch2", 2, split, heightBlock2, origin2, tempOffset2);

        // 3 lower right
        float[] heightBlock3 = createHeightSubBlock(heightMap, split - 1, 0,
                        split);

        Vector3f origin3 = new Vector3f(0, 0, -halfSize * stepScale.z);

        Vector2f tempOffset3 = createBasicOffset();
        tempOffset3.x += quarterSize * stepScale.x;
        tempOffset3.y += origin3.z / 2;

        addPatch("Patch3", 3, split, heightBlock3, origin3, tempOffset3);

        // 4 upper right
        float[] heightBlock4 = createHeightSubBlock(heightMap, split - 1,
                        split - 1, split);

        Vector3f origin4 = new Vector3f(0, 0, 0);

        Vector2f tempOffset4 = createBasicOffset();
        tempOffset4.x += quarterSize * stepScale.x;
        tempOffset4.y += quarterSize * stepScale.z;

        addPatch("Patch4", 4, split, heightBlock4, origin4, tempOffset4);
    }

    public float[] createHeightSubBlock(float[] heightMap, int x,
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

    /**
     * A handy method that will attach all bounding boxes of this terrain
     * to the node you supply.
     * Useful to visualize the bounding boxes when debugging.
     *
     * @param parent that will get the bounding box shapes of the terrain attached to
     */
    public void attachBoundChildren(Node parent) {
        for (int i = 0; i < this.getQuantity(); i++) {
            if (this.getChild(i) instanceof TerrainQuad) {
                ((TerrainQuad) getChild(i)).attachBoundChildren(parent);
            } else if (this.getChild(i) instanceof TerrainPatch) {
                BoundingVolume bv = getChild(i).getWorldBound();
                if (bv instanceof BoundingBox) {
                    attachBoundingBox((BoundingBox)bv, parent);
                }
            }
        }
        BoundingVolume bv = getWorldBound();
        if (bv instanceof BoundingBox) {
            attachBoundingBox((BoundingBox)bv, parent);
        }
    }

    /**
     * used by attachBoundChildren()
     */
    private void attachBoundingBox(BoundingBox bb, Node parent) {
        WireBox wb = new WireBox(bb.getXExtent(), bb.getYExtent(), bb.getZExtent());
        Geometry g = new Geometry();
        g.setMesh(wb);
        g.setLocalTranslation(bb.getCenter());
        parent.attachChild(g);
    }

    /**
     * Signal if the normal vectors for the terrain need to be recalculated.
     * Does this by looking at the affectedAreaBBox bounding box. If the bbox
     * exists already, then it will grow the box to fit the new changedPoint.
     * If the affectedAreaBBox is null, then it will create one of unit size.
     *
     * @param needToRecalculateNormals if null, will cause needToRecalculateNormals() to return false
     */
    protected void setNormalRecalcNeeded(Vector2f changedPoint) {
        if (changedPoint == null) { // set needToRecalculateNormals() to false
            affectedAreaBBox = null;
            return;
        }

        if (affectedAreaBBox == null) {
            affectedAreaBBox = new BoundingBox(new Vector3f(changedPoint.x, 0, changedPoint.y), 1f, Float.MAX_VALUE, 1f); // unit length
        } else {
            // adjust size of box to be larger
            affectedAreaBBox.mergeLocal(new BoundingBox(new Vector3f(changedPoint.x, 0, changedPoint.y), 1f, Float.MAX_VALUE, 1f));
        }
    }

    protected boolean needToRecalculateNormals() {
        if (affectedAreaBBox != null)
            return true;
        if (!lastScale.equals(getWorldScale())) {
            affectedAreaBBox = new BoundingBox(getWorldTranslation(), Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
            lastScale = getWorldScale();
            return true;
        }
        return false;
    }
    
    /**
     * This will cause all normals for this terrain quad to be recalculated
     */
    protected void setNeedToRecalculateNormals() {
        affectedAreaBBox = new BoundingBox(getWorldTranslation(), Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
    }

    public float getHeightmapHeight(Vector2f xz) {
        // offset
        int halfSize = totalSize / 2;
        int x = Math.round((xz.x / getWorldScale().x) + halfSize);
        int z = Math.round((xz.y / getWorldScale().z) + halfSize);

        if (!isInside(x, z))
            return Float.NaN;
        return getHeightmapHeight(x, z);
    }
    

    /**
     * This will just get the heightmap value at the supplied point,
     * not an interpolated (actual) height value.
     */   
    
    protected float getHeightmapHeight(int x, int z) {
    	System.out.println("running 1");
    	QuadPoint quad = new QuadPoint(x, z, children, size);
    	return quad.calculateHeightMap(x,z);
    }

    protected Vector3f getMeshNormal(int x, int z) {
    	System.out.println("running 2");
    	QuadPoint quad = new QuadPoint(x, z, children, size);
    	return quad.calculateMeshNormal(x,z);
    }

    /**
     * is the 2d point inside the terrain?
     * @param x local coordinate
     * @param z local coordinate
     */
    private boolean isInside(int x, int z) {
        if (x < 0 || z < 0 || x > totalSize || z > totalSize)
            return false;
        return true;
    }

    /**
     * Used for searching for a child and keeping
     * track of its quadrant
     */

    private QuadrantChild findMatchingChild(int x, int z) {
    	System.out.println("running 3");
    	QuadPoint quad = new QuadPoint(x, z, children, size);
    	return quad.calculateQuadrant(x,z);
    }
    
    /**
     * Get the interpolated height of the terrain at the specified point.
     * @param xz the location to get the height for
     * @return Float.NAN if the value does not exist, or the coordinates are outside of the terrain
     */
    public float getHeight(Vector2f xz) {
        // offset
        float x = (float)(((xz.x - getWorldTranslation().x) / getWorldScale().x) + (float)(totalSize-1) / 2f);
        float z = (float)(((xz.y - getWorldTranslation().z) / getWorldScale().z) + (float)(totalSize-1) / 2f);
        if (!isInside((int)x, (int)z))
            return Float.NaN;
        float height = getHeight((int)x, (int)z, (x%1f), (z%1f));
        height *= getWorldScale().y;
        return height;
    }

    /*
     * gets an interpolated value at the specified point
     */
    protected float getHeight(int x, int z, float xm, float zm) {
        
        QuadrantChild match = findMatchingChild(x,z);
        if (match != null) {
            if (match.child instanceof TerrainQuad) {
                return ((TerrainQuad) match.child).getHeight(match.col, match.row, xm, zm);
            } else if (match.child instanceof TerrainPatch) {
                return ((TerrainPatch) match.child).getHeight(match.col, match.row, xm, zm);
            }
        }
        return Float.NaN;
    }

    public Vector3f getNormal(Vector2f xz) {
        // offset
        float x = (float)(((xz.x - getWorldTranslation().x) / getWorldScale().x) + (float)(totalSize-1) / 2f);
        float z = (float)(((xz.y - getWorldTranslation().z) / getWorldScale().z) + (float)(totalSize-1) / 2f);
        Vector3f normal = getNormal(x, z, xz);
        
        return normal;
    }
    
    protected Vector3f getNormal(float x, float z, Vector2f xz) {
        x-=0.5f;
        z-=0.5f;
        float col = FastMath.floor(x);
        float row = FastMath.floor(z);
        boolean onX = false;
        if(1 - (x - col)-(z - row) < 0) // what triangle to interpolate on
            onX = true;
        // v1--v2  ^
        // |  / |  |
        // | /  |  |
        // v3--v4  | Z
        //         |
        // <-------Y
        //     X 
        Vector3f n1 = getMeshNormal((int) FastMath.ceil(x), (int) FastMath.ceil(z));
        Vector3f n2 = getMeshNormal((int) FastMath.floor(x), (int) FastMath.ceil(z));
        Vector3f n3 = getMeshNormal((int) FastMath.ceil(x), (int) FastMath.floor(z));
        Vector3f n4 = getMeshNormal((int) FastMath.floor(x), (int) FastMath.floor(z));
        
        return n1.add(n2).add(n3).add(n4).normalize();
    }
    
    public void setHeight(Vector2f xz, float height) {
        List<Vector2f> coord = new ArrayList<Vector2f>();
        coord.add(xz);
        List<Float> h = new ArrayList<Float>();
        h.add(height);

        setHeight(coord, h);
    }

    public void adjustHeight(Vector2f xz, float delta) {
        List<Vector2f> coord = new ArrayList<Vector2f>();
        coord.add(xz);
        List<Float> h = new ArrayList<Float>();
        h.add(delta);

        adjustHeight(coord, h);
    }

    public void setHeight(List<Vector2f> xz, List<Float> height) {
        setHeight(xz, height, true);
    }

    public void adjustHeight(List<Vector2f> xz, List<Float> height) {
        setHeight(xz, height, false);
    }

    protected void setHeight(List<Vector2f> xz, List<Float> height, boolean overrideHeight) {
        if (xz.size() != height.size())
            throw new IllegalArgumentException("Both lists must be the same length!");

        int halfSize = totalSize / 2;

        List<LocationHeight> locations = new ArrayList<LocationHeight>();

        // offset
        for (int i=0; i<xz.size(); i++) {
            int x = Math.round((xz.get(i).x / getWorldScale().x) + halfSize);
            int z = Math.round((xz.get(i).y / getWorldScale().z) + halfSize);
            if (!isInside(x, z))
                continue;
            locations.add(new LocationHeight(x,z,height.get(i)));
        }

        setHeight(locations, overrideHeight); // adjust height of the actual mesh

        // signal that the normals need updating
        for (int i=0; i<xz.size(); i++)
            setNormalRecalcNeeded(xz.get(i) );
    }

    protected class LocationHeight {
        int x;
        int z;
        float h;

        LocationHeight(){}

        LocationHeight(int x, int z, float h){
            this.x = x;
            this.z = z;
            this.h = h;
        }
    }

    protected void setHeight(List<LocationHeight> locations, boolean overrideHeight) {
        if (children == null)
            return;

        List<LocationHeight> quadLH1 = new ArrayList<LocationHeight>();
        List<LocationHeight> quadLH2 = new ArrayList<LocationHeight>();
        List<LocationHeight> quadLH3 = new ArrayList<LocationHeight>();
        List<LocationHeight> quadLH4 = new ArrayList<LocationHeight>();
        Spatial quad1 = null;
        Spatial quad2 = null;
        Spatial quad3 = null;
        Spatial quad4 = null;

        // get the child quadrants
        for (int i = children.size(); --i >= 0;) {
            Spatial spat = children.get(i);
            int childQuadrant = 0;
            if (spat instanceof TerrainQuad) {
                childQuadrant = ((TerrainQuad) spat).getQuadrant();
            } else if (spat instanceof TerrainPatch) {
                childQuadrant = ((TerrainPatch) spat).getQuadrant();
            }

            if (childQuadrant == 1)
                quad1 = spat;
            else if (childQuadrant == 2)
                quad2 = spat;
            else if (childQuadrant == 3)
                quad3 = spat;
            else if (childQuadrant == 4)
                quad4 = spat;
        }

        int split = (size + 1) >> 1;

        // distribute each locationHeight into the quadrant it intersects
        for (LocationHeight lh : locations) {
            int quad = Utils.findQuadrant(lh.x, lh.z, size);
            int col = lh.x;
            int row = lh.z;

            if ((quad & 1) != 0) {
                quadLH1.add(lh);
            }
            if ((quad & 2) != 0) {
                row = lh.z - split + 1;
                quadLH2.add(new LocationHeight(lh.x, row, lh.h));
            }
            if ((quad & 4) != 0) {
                col = lh.x - split + 1;
                quadLH3.add(new LocationHeight(col, lh.z, lh.h));
            }
            if ((quad & 8) != 0) {
                col = lh.x - split + 1;
                row = lh.z - split + 1;
                quadLH4.add(new LocationHeight(col, row, lh.h));
            }
        }

        // send the locations to the children
        if (!quadLH1.isEmpty()) {
            if (quad1 instanceof TerrainQuad)
                ((TerrainQuad)quad1).setHeight(quadLH1, overrideHeight);
            else if(quad1 instanceof TerrainPatch)
                ((TerrainPatch)quad1).setHeight(quadLH1, overrideHeight);
        }

        if (!quadLH2.isEmpty()) {
            if (quad2 instanceof TerrainQuad)
                ((TerrainQuad)quad2).setHeight(quadLH2, overrideHeight);
            else if(quad2 instanceof TerrainPatch)
                ((TerrainPatch)quad2).setHeight(quadLH2, overrideHeight);
        }

        if (!quadLH3.isEmpty()) {
            if (quad3 instanceof TerrainQuad)
                ((TerrainQuad)quad3).setHeight(quadLH3, overrideHeight);
            else if(quad3 instanceof TerrainPatch)
                ((TerrainPatch)quad3).setHeight(quadLH3, overrideHeight);
        }

        if (!quadLH4.isEmpty()) {
            if (quad4 instanceof TerrainQuad)
                ((TerrainQuad)quad4).setHeight(quadLH4, overrideHeight);
            else if(quad4 instanceof TerrainPatch)
                ((TerrainPatch)quad4).setHeight(quadLH4, overrideHeight);
        }
    }

    protected boolean isPointOnTerrain(int x, int z) {
        return (x >= 0 && x <= totalSize && z >= 0 && z <= totalSize);
    }

    
    public int getTerrainSize() {
        return totalSize;
    }


    // a position can be in multiple quadrants, so use a bit anded value.


    /**
     * lock or unlock the meshes of this terrain.
     * Locked meshes are uneditable but have better performance.
     * @param locked or unlocked
     */
    public void setLocked(boolean locked) {
        for (int i = 0; i < this.getQuantity(); i++) {
            if (this.getChild(i) instanceof TerrainQuad) {
                ((TerrainQuad) getChild(i)).setLocked(locked);
            } else if (this.getChild(i) instanceof TerrainPatch) {
                if (locked)
                    ((TerrainPatch) getChild(i)).lockMesh();
                else
                    ((TerrainPatch) getChild(i)).unlockMesh();
            }
        }
    }


    public int getQuadrant() {
        return quadrant;
    }

    public void setQuadrant(short quadrant) {
        this.quadrant = quadrant;
    }


    protected TerrainPatch getPatch(int quad) {
        if (children != null)
            for (int x = children.size(); --x >= 0;) {
                Spatial child = children.get(x);
                if (child instanceof TerrainPatch) {
                    TerrainPatch tb = (TerrainPatch) child;
                    if (tb.getQuadrant() == quad)
                        return tb;
                }
            }
        return null;
    }

    protected TerrainQuad getQuad(int quad) {
        if (quad == 0)
            return this;
        if (children != null)
            for (int x = children.size(); --x >= 0;) {
                Spatial child = children.get(x);
                if (child instanceof TerrainQuad) {
                    TerrainQuad tq = (TerrainQuad) child;
                    if (tq.getQuadrant() == quad)
                        return tq;
                }
            }
        return null;
    }

    protected TerrainQuad findRightQuad() {
    	return TerrainNeighbours.findRightQuad(this);
    }

    protected TerrainQuad findDownQuad() {
	return TerrainNeighbours.findDownQuad(this);
    }

    protected TerrainQuad findTopQuad() {
    	return TerrainNeighbours.findTopQuad(this);
    }

    protected TerrainQuad findLeftQuad() {
    	return TerrainNeighbours.findLeftQuad(this);
    }

    /**
     * Find what terrain patches need normal recalculations and update
     * their normals;
     */
    protected void fixNormals(BoundingBox affectedArea) {
        if (children == null)
            return;

        // go through the children and see if they collide with the affectedAreaBBox
        // if they do, then update their normals
        for (int x = children.size(); --x >= 0;) {
            Spatial child = children.get(x);
            if (child instanceof TerrainQuad) {
                if (affectedArea != null && affectedArea.intersects(((TerrainQuad) child).getWorldBound()) )
                    ((TerrainQuad) child).fixNormals(affectedArea);
            } else if (child instanceof TerrainPatch) {
                if (affectedArea != null && affectedArea.intersects(((TerrainPatch) child).getWorldBound()) )
                    ((TerrainPatch) child).updateNormals(); // recalculate the patch's normals
            }
        }
    }

    /**
     * fix the normals on the edge of the terrain patches.
     */
    protected void fixNormalEdges(BoundingBox affectedArea) {
    	TerrainNeighbours.fixNormalEdges(affectedArea, this);
    }



    @Override
    public int collideWith(Collidable other, CollisionResults results){
        int total = 0;

        if (other instanceof Ray)
            return TerrainTransform.collideWithRay((Ray)other, results, this);

        // if it didn't collide with this bbox, return
        if (other instanceof BoundingVolume)
            if (!this.getWorldBound().intersects((BoundingVolume)other))
                return total;

        for (Spatial child : children){
            total += child.collideWith(other, results);
        }
        return total;
    }

    /**
     * Gather the terrain patches that intersect the given ray (toTest).
     * This only tests the bounding boxes
     * @param toTest
     * @param results
     */
    public void findPick(Ray toTest, List<TerrainPickData> results) {

        if (getWorldBound() != null) {
            if (getWorldBound().intersects(toTest)) {
                // further checking needed.
                for (int i = 0; i < getQuantity(); i++) {
                    if (children.get(i) instanceof TerrainPatch) {
                        TerrainPatch tp = (TerrainPatch) children.get(i);
                        tp.ensurePositiveVolumeBBox();
                        if (tp.getWorldBound().intersects(toTest)) {
                            CollisionResults cr = new CollisionResults();
                            toTest.collideWith(tp.getWorldBound(), cr);
                            if (cr != null && cr.getClosestCollision() != null) {
                                cr.getClosestCollision().getDistance();
                                results.add(new TerrainPickData(tp, cr.getClosestCollision()));
                            }
                        }
                    }
                    else if (children.get(i) instanceof TerrainQuad) {
                        ((TerrainQuad) children.get(i)).findPick(toTest, results);
                    }
                }
            }
        }
    }


    /**
     * Retrieve all Terrain Patches from all children and store them
     * in the 'holder' list
     * @param holder must not be null, will be populated when returns
     */
    public void getAllTerrainPatches(List<TerrainPatch> holder) {
        if (children != null) {
            for (int i = children.size(); --i >= 0;) {
                Spatial child = children.get(i);
                if (child instanceof TerrainQuad) {
                    ((TerrainQuad) child).getAllTerrainPatches(holder);
                } else if (child instanceof TerrainPatch) {
                    holder.add((TerrainPatch)child);
                }
            }
        }
    }

    public void getAllTerrainPatchesWithTranslation(Map<TerrainPatch,Vector3f> holder, Vector3f translation) {
        if (children != null) {
            for (int i = children.size(); --i >= 0;) {
                Spatial child = children.get(i);
                if (child instanceof TerrainQuad) {
                    ((TerrainQuad) child).getAllTerrainPatchesWithTranslation(holder, translation.clone().add(child.getLocalTranslation()));
                } else if (child instanceof TerrainPatch) {
                    //if (holder.size() < 4)
                    holder.put((TerrainPatch)child, translation.clone().add(child.getLocalTranslation()));
                }
            }
        }
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule c = e.getCapsule(this);
        size = c.readInt("size", 0);
        stepScale = (Vector3f) c.readSavable("stepScale", null);
        offset = (Vector2f) c.readSavable("offset", new Vector2f(0,0));
        offsetAmount = c.readFloat("offsetAmount", 0);
        quadrant = c.readInt("quadrant", 0);
        totalSize = c.readInt("totalSize", 0);
        //lodCalculator = (LodCalculator) c.readSavable("lodCalculator", createDefaultLodCalculator());
        //lodCalculatorFactory = (LodCalculatorFactory) c.readSavable("lodCalculatorFactory", null);
        
        if ( !(getParent() instanceof TerrainQuad) ) {
            BoundingBox all = new BoundingBox(getWorldTranslation(), totalSize, totalSize, totalSize);
            affectedAreaBBox = all;
            updateNormals();
        }
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule c = e.getCapsule(this);
        c.write(size, "size", 0);
        c.write(totalSize, "totalSize", 0);
        c.write(stepScale, "stepScale", null);
        c.write(offset, "offset", new Vector2f(0,0));
        c.write(offsetAmount, "offsetAmount", 0);
        c.write(quadrant, "quadrant", 0);
        //c.write(lodCalculatorFactory, "lodCalculatorFactory", null);
        //c.write(lodCalculator, "lodCalculator", null);
    }

    @Override
    public TerrainQuad clone() {
        return this.clone(true);
    }

	@Override
    public TerrainQuad clone(boolean cloneMaterials) {
        TerrainQuad quadClone = (TerrainQuad) super.clone(cloneMaterials);
        quadClone.name = name.toString();
        quadClone.size = size;
        quadClone.totalSize = totalSize;
        if (stepScale != null) {
            quadClone.stepScale = stepScale.clone();
        }
        if (offset != null) {
            quadClone.offset = offset.clone();
        }
        quadClone.offsetAmount = offsetAmount;
        quadClone.quadrant = quadrant;
        //quadClone.lodCalculatorFactory = lodCalculatorFactory.clone();
        //quadClone.lodCalculator = lodCalculator.clone();
        
        TerrainLodControl lodControlCloned = this.getControl(TerrainLodControl.class);
        TerrainLodControl lodControl = quadClone.getControl(TerrainLodControl.class);
        
        if (lodControlCloned != null && !(getParent() instanceof TerrainQuad)) {
            //lodControlCloned.setLodCalculator(lodControl.getLodCalculator().clone());
        }
        NormalRecalcControl normalControl = getControl(NormalRecalcControl.class);
        if (normalControl != null)
            normalControl.setTerrain(this);

        return quadClone;
    }
        
    @Override
    protected void setParent(Node parent) {
        super.setParent(parent);
        if (parent == null) {
            // if the terrain is being detached
            clearCaches();
        }
    }
    
    /**
     * Removes any cached references this terrain is holding, in particular
     * the TerrainPatch's neighbour references.
     * This is called automatically when the root terrainQuad is detached from
     * its parent or if setParent(null) is called.
     */
    public void clearCaches() {
        if (children != null) {
            for (int i = children.size(); --i >= 0;) {
                Spatial child = children.get(i);
                if (child instanceof TerrainQuad) {
                    ((TerrainQuad) child).clearCaches();
                } else if (child instanceof TerrainPatch) {
                    ((TerrainPatch) child).clearCaches();
                }
            }
        }
    }
    
    public int getMaxLod() {
        if (maxLod < 0)
            maxLod = Math.max(1, (int) (FastMath.log(size-1)/FastMath.log(2)) -1); // -1 forces our minimum of 4 triangles wide

        return maxLod;
    }

    public int getPatchSize() {
        return patchSize;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public float[] getHeightMap() {

        float[] hm = null;
        int length = ((size-1)/2)+1;
        int area = size*size;
        hm = new float[area];

        if (getChildren() != null && !getChildren().isEmpty()) {
            float[] ul=null, ur=null, bl=null, br=null;
            // get the child heightmaps
            if (getChild(0) instanceof TerrainPatch) {
                for (Spatial s : getChildren()) {
                    if ( ((TerrainPatch)s).getQuadrant() == 1)
                        ul = ((TerrainPatch)s).getHeightMap();
                    else if(((TerrainPatch) s).getQuadrant() == 2)
                        bl = ((TerrainPatch)s).getHeightMap();
                    else if(((TerrainPatch) s).getQuadrant() == 3)
                        ur = ((TerrainPatch)s).getHeightMap();
                    else if(((TerrainPatch) s).getQuadrant() == 4)
                        br = ((TerrainPatch)s).getHeightMap();
                }
            }
            else {
                ul = getQuad(1).getHeightMap();
                bl = getQuad(2).getHeightMap();
                ur = getQuad(3).getHeightMap();
                br = getQuad(4).getHeightMap();
            }

            // combine them into a single heightmap


            // first upper blocks
            for (int y=0; y<length; y++) { // rows
                for (int x1=0; x1<length; x1++) {
                    int row = y*size;
                    hm[row+x1] = ul[y*length+x1];
                }
                for (int x2=1; x2<length; x2++) {
                    int row = y*size + length;
                    hm[row+x2-1] = ur[y*length + x2];
                }
            }
            // second lower blocks
            int rowOffset = size*length;
            for (int y=1; y<length; y++) { // rows
                for (int x1=0; x1<length; x1++) {
                    int row = (y-1)*size;
                    hm[rowOffset+row+x1] = bl[y*length+x1];
                }
                for (int x2=1; x2<length; x2++) {
                    int row = (y-1)*size + length;
                    hm[rowOffset+row+x2-1] = br[y*length + x2];
                }
            }
        }

        return hm;
    }

}

