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
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.UnsupportedCollisionException;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.*;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.terrain.geomipmap.TerrainQuad.LocationHeight;
import com.jme3.terrain.geomipmap.lodcalc.util.EntropyComputeUtil;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.List;


/**
 * A terrain patch is a leaf in the terrain quad tree. It has a mesh that can change levels of detail (LOD)
 * whenever the view point, or camera, changes. The actual terrain mesh is created by the LODGeomap class.
 * That uses a geo-mipmapping algorithm to change the index buffer of the mesh.
 * The mesh is a triangle strip. In wireframe mode you might notice some strange lines, these are degenerate
 * triangles generated by the geoMipMap algorithm and can be ignored. The video card removes them at almost no cost.
 * 
 * Each patch needs to know its neighbour's LOD so it can seam its edges with them, in case the neighbour has a different
 * LOD. If this doesn't happen, you will see gaps.
 * 
 * The LOD value is most detailed at zero. It gets less detailed the higher the LOD value until you reach maxLod, which
 * is a mathematical limit on the number of times the 'size' of the patch can be divided by two. However there is a -1 to that
 * for now until I add in a custom index buffer calculation for that max level, the current algorithm does not go that far.
 * 
 * You can supply a LodThresholdCalculator for use in determining when the LOD should change. It's API will no doubt change 
 * in the near future. Right now it defaults to just changing LOD every two patch sizes. So if a patch has a size of 65, 
 * then the LOD changes every 130 units away.
 * 
 * @author Brent Owens
 */
public class TerrainPatch extends Geometry {

	protected TerrainPatchLod tpLod;

    protected int size;
    protected int totalSize;

    protected short quadrant = 1;

    // x/z step
    protected Vector stepScale;

    // center of the patch in relation to (0,0,0)
    protected Vector offset;

    // amount the patch has been shifted.
    protected float offsetAmount;

    //protected LodCalculator lodCalculator;
    //protected LodCalculatorFactory lodCalculatorFactory;

    protected TerrainPatch leftNeighbour, topNeighbour, rightNeighbour, bottomNeighbour;
    protected boolean searchedForNeighboursAlready = false;

    // these two vectors are calculated on the GL thread, but used in the outside LOD thread
    protected Vector worldTranslationCached;
    protected Vector worldScaleCached;

    public TerrainPatch() {
        super("TerrainPatch");
        setBatchHint(BatchHint.Never);
    }
    
    public TerrainPatch(String name) {
        super(name);
        setBatchHint(BatchHint.Never);
    }

    public TerrainPatch(String name, int size) {
        this(name, size, new Vector(3), null, Vector.ZERO(3));
    }
    
    public TerrainPatchLod getTpLod() {
    	return this.tpLod;
    }

    /**
     * Constructor instantiates a new <code>TerrainPatch</code> object. The
     * parameters and heightmap data are then processed to generate a
     * <code>TriMesh</code> object for rendering.
     *
     * @param name
     *			the name of the terrain patch.
     * @param size
     *			the size of the heightmap.
     * @param stepScale
     *			the scale for the axes.
     * @param heightMap
     *			the height data.
     * @param origin
     *			the origin offset of the patch.
     */
    public TerrainPatch(String name, int size, Vector stepScale,
                    float[] heightMap, Vector origin) {
        this(name, size, stepScale, heightMap, origin, size, new Vector(2), 0);
    }

    /**
     * Constructor instantiates a new <code>TerrainPatch</code> object. The
     * parameters and heightmap data are then processed to generate a
     * <code>TriMesh</code> object for renderering.
     *
     * @param name
     *			the name of the terrain patch.
     * @param size
     *			the size of the patch.
     * @param stepScale
     *			the scale for the axes.
     * @param heightMap
     *			the height data.
     * @param origin
     *			the origin offset of the patch.
     * @param totalSize
     *			the total size of the terrain. (Higher if the patch is part of
     *			a <code>TerrainQuad</code> tree.
     * @param vector
     *			the offset for texture coordinates.
     * @param offsetAmount
     *			the total offset amount. Used for texture coordinates.
     */
    public TerrainPatch(String name, int size, Vector stepScale,
                    float[] heightMap, Vector origin, int totalSize,
                    Vector vector, float offsetAmount) {
        super(name);
        setBatchHint(BatchHint.Never);
        this.size = size;
        this.stepScale = stepScale;
        this.totalSize = totalSize;
        this.offsetAmount = offsetAmount;
        this.offset = vector;

        setLocalTranslation(origin);

        this.tpLod = new TerrainPatchLod(size, totalSize, heightMap, stepScale, offset, offsetAmount);
        setMesh(this.tpLod.getMesh());
    }

    /**
     * This calculation is slow, so don't use it often.
     */
    public void generateLodEntropies() {
        float[] entropies = new float[getMaxLod()+1];
        for (int i = 0; i <= getMaxLod(); i++){
            int curLod = (int) Math.pow(2, i);
            IndexBuffer idxB = tpLod.getLODGeomap().writeIndexArrayLodDiff(curLod, false, false, false, false, totalSize);
            Buffer ib;
            if (idxB.getBuffer() instanceof IntBuffer)
                ib = (IntBuffer)idxB.getBuffer();
            else
                ib = (ShortBuffer)idxB.getBuffer();
            entropies[i] = EntropyComputeUtil.computeLodEntropy(mesh, ib);
        }

        tpLod.lodEntropy = entropies;
    }

    public float[] getLodEntropies(){
        if (tpLod.lodEntropy == null){
            generateLodEntropies();
        }
        return tpLod.lodEntropy;
    }

    @Deprecated
    public FloatBuffer getHeightmap() {
        return BufferUtils.createFloatBuffer(tpLod.getLODGeomap().getHeightArray());
    }
    
    public float[] getHeightMap() {
        return tpLod.getLODGeomap().getHeightArray();
    }

    /**
     * The maximum lod supported by this terrain patch.
     * If the patch size is 32 then the returned value would be log2(32)-2 = 3
     * You can then use that value, 3, to see how many times you can divide 32 by 2
     * before the terrain gets too un-detailed (can't stitch it any further).
     * @return the maximum LOD
     */
    public int getMaxLod() {
        if (tpLod.getMaxLod() < 0)
            tpLod.setMaxLod(Math.max(1, (int) (FastMath.log(size-1)/FastMath.log(2)) -1)); // -1 forces our minimum of 4 triangles wide

        return tpLod.getMaxLod();
    }

    protected void reIndexGeometry(HashMap<String,UpdatedTerrainPatch> updated, boolean useVariableLod) {

        UpdatedTerrainPatch utp = updated.get(getName());

        if (utp != null && utp.isReIndexNeeded() ) {
            int pow = (int) Math.pow(2, utp.getNewLod());
            boolean left = utp.getLeftLod() > utp.getNewLod();
            boolean top = utp.getTopLod() > utp.getNewLod();
            boolean right = utp.getRightLod() > utp.getNewLod();
            boolean bottom = utp.getBottomLod() > utp.getNewLod();

            IndexBuffer idxB;
            if (useVariableLod)
                idxB = tpLod.getLODGeomap().writeIndexArrayLodVariable(pow, (int) Math.pow(2, utp.getRightLod()), (int) Math.pow(2, utp.getTopLod()), (int) Math.pow(2, utp.getLeftLod()), (int) Math.pow(2, utp.getBottomLod()), totalSize);
            else
                idxB = tpLod.getLODGeomap().writeIndexArrayLodDiff(pow, right, top, left, bottom, totalSize);
            
            Buffer b;
            if (idxB.getBuffer() instanceof IntBuffer)
                b = (IntBuffer)idxB.getBuffer();
            else
                b = (ShortBuffer)idxB.getBuffer();
            utp.setNewIndexBuffer(b);
        }

    }


    public Vector2f getTex(float x, float z, Vector2f store) {
        if (x < 0 || z < 0 || x >= size || z >= size) {
            store.set(Vector2f.ZERO);
            return store;
        }
        int idx = (int) (z * size + x);
        return store.set(getMesh().getFloatBuffer(Type.TexCoord).get(idx*2),
                         getMesh().getFloatBuffer(Type.TexCoord).get(idx*2+1) );
    }
    

    
    /**
     * Get the triangle of this geometry at the specified local coordinate.
     * @param x local to the terrain patch
     * @param z local to the terrain patch
     * @return the triangle in world coordinates, or null if the point does intersect this patch on the XZ axis
     */
    public Triangle getTriangle(float x, float z) {
        return tpLod.getLODGeomap().getTriangleAtPoint(x, z, getWorldScale() , getWorldTranslation());
    }

    /**
     * Get the triangles at the specified grid point. Probably only 2 triangles
     * @param x local to the terrain patch
     * @param z local to the terrain patch
     * @return the triangles in world coordinates, or null if the point does intersect this patch on the XZ axis
     */
    public Triangle[] getGridTriangles(float x, float z) {
        return tpLod.getLODGeomap().getGridTrianglesAtPoint(x, z, getWorldScale() , getWorldTranslation());
    }


    protected float getHeight(int x, int z, float xm, float zm) {
        return tpLod.getLODGeomap().getHeight(x,z,xm,zm);
    }
    
    /**
     * Locks the mesh (sets it static) to improve performance.
     * But it it not editable then. Set unlock to make it editable.
     */
    public void lockMesh() {
        getMesh().setStatic();
    }

    /**
     * Unlocks the mesh (sets it dynamic) to make it editable.
     * It will be editable but performance will be reduced.
     * Call lockMesh to improve performance.
     */
    public void unlockMesh() {
        getMesh().setDynamic();
    }
	
    /**
     * Returns the offset amount this terrain patch uses for textures.
     *
     * @return The current offset amount.
     */
    public float getOffsetAmount() {
        return offsetAmount;
    }

    /**
     * Returns the step scale that stretches the height map.
     *
     * @return The current step scale.
     */
    public Vector getStepScale() {
        return stepScale;
    }

    /**
     * Returns the total size of the terrain.
     *
     * @return The terrain's total size.
     */
    public int getTotalSize() {
        return totalSize;
    }

    /**
     * Returns the size of this terrain patch.
     *
     * @return The current patch size.
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the current offset amount. This is used when building texture
     * coordinates.
     *
     * @return The current offset amount.
     */
    public Vector getOffset() {
        return offset;
    }

    /**
     * Sets the value for the current offset amount to use when building texture
     * coordinates. Note that this does <b>NOT </b> rebuild the terrain at all.
     * This is mostly used for outside constructors of terrain patches.
     *
     * @param offset
     *			The new texture offset.
     */
    public void setOffset(Vector offset) {
        this.offset = offset;
    }

    /**
     * Sets the size of this terrain patch. Note that this does <b>NOT </b>
     * rebuild the terrain at all. This is mostly used for outside constructors
     * of terrain patches.
     *
     * @param size
     *			The new size.
     */
    public void setSize(int size) {
        this.size = size;

        tpLod.setMaxLod(-1); // reset it
    }

    /**
     * Sets the total size of the terrain . Note that this does <b>NOT </b>
     * rebuild the terrain at all. This is mostly used for outside constructors
     * of terrain patches.
     *
     * @param totalSize
     *			The new total size.
     */
    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    /**
     * Sets the step scale of this terrain patch's height map. Note that this
     * does <b>NOT </b> rebuild the terrain at all. This is mostly used for
     * outside constructors of terrain patches.
     *
     * @param stepScale
     *			The new step scale.
     */
    public void setStepScale(Vector stepScale) {
        this.stepScale = stepScale;
    }

    /**
     * Sets the offset of this terrain texture map. Note that this does <b>NOT
     * </b> rebuild the terrain at all. This is mostly used for outside
     * constructors of terrain patches.
     *
     * @param offsetAmount
     *			The new texture offset.
     */
    public void setOffsetAmount(float offsetAmount) {
        this.offsetAmount = offsetAmount;
    }

    /**
     * @return Returns the quadrant.
     */
    public short getQuadrant() {
        return quadrant;
    }

    /**
     * @param quadrant
     *			The quadrant to set.
     */
    public void setQuadrant(short quadrant) {
        this.quadrant = quadrant;
    }

    public int getLod() {
        return tpLod.getLod();
    }

    public void setLod(int lod) {
        tpLod.setLod(lod);
    }

    public int getPreviousLod() {
        return tpLod.getPreviousLod();
    }

    public void setPreviousLod(int previousLod) {
        tpLod.setPreviousLod(previousLod);
    }

    protected int getLodLeft() {
        return tpLod.getLodLeft();
    }

    protected void setLodLeft(int lodLeft) {
        tpLod.setLodLeft(lodLeft);
    }

    protected int getLodTop() {
        return tpLod.getLodTop();
    }

    protected void setLodTop(int lodTop) {
        tpLod.setLodTop(lodTop);
    }

    protected int getLodRight() {
        return tpLod.getLodRight();
    }

    protected void setLodRight(int lodRight) {
        tpLod.setLodRight(lodRight);
    }

    protected int getLodBottom() {
        return tpLod.getLodBottom();
    }

    protected void setLodBottom(int lodBottom) {
        tpLod.setLodBottom(lodBottom);
    }
    
    /*public void setLodCalculator(LodCalculatorFactory lodCalculatorFactory) {
        this.lodCalculatorFactory = lodCalculatorFactory;
        setLodCalculator(lodCalculatorFactory.createCalculator(this));
    }*/

    @Override
    public int collideWith(Collidable other, CollisionResults results) throws UnsupportedCollisionException {
        if (refreshFlags != 0)
            throw new IllegalStateException("Scene graph must be updated" +
                                            " before checking collision");

        if (other instanceof BoundingVolume)
            if (!getWorldBound().intersects((BoundingVolume)other))
                return 0;
        
        if(other instanceof Ray)
            return TerrainPatchCollision.collideWithRay((Ray)other, results);
        else if (other instanceof BoundingVolume)
            return TerrainPatchCollision.collideWithBoundingVolume((BoundingVolume)other, results, this);
        else {
            throw new UnsupportedCollisionException("TerrainPatch cannnot collide with "+other.getClass().getName());
        }
    }




    protected Vector worldCoordinateToLocal(Vector loc) {
        Vector translated = new Vector(3);
        translated.setX(loc.getX()/getWorldScale().getX() - getWorldTranslation().getX());
        translated.setY(loc.getY()/getWorldScale().getY() - getWorldTranslation().getY());
        translated.setZ(loc.getZ()/getWorldScale().getZ() - getWorldTranslation().getZ());
        return translated;
    }

    /**
     * This most definitely is not optimized.
     */
    private int collideWithBoundingBox(BoundingBox bbox, CollisionResults results) {
        
        // test the four corners, for cases where the bbox dimensions are less than the terrain grid size, which is probably most of the time
        Vector topLeft = worldCoordinateToLocal(new Vector(bbox.getCenter().getX()-bbox.getXExtent(), 0, bbox.getCenter().getZ()-bbox.getZExtent()));
        Vector topRight = worldCoordinateToLocal(new Vector(bbox.getCenter().getX()+bbox.getXExtent(), 0, bbox.getCenter().getZ()-bbox.getZExtent()));
        Vector bottomLeft = worldCoordinateToLocal(new Vector(bbox.getCenter().getX()-bbox.getXExtent(), 0, bbox.getCenter().getZ()+bbox.getZExtent()));
        Vector bottomRight = worldCoordinateToLocal(new Vector(bbox.getCenter().getX()+bbox.getXExtent(), 0, bbox.getCenter().getZ()+bbox.getZExtent()));

        Triangle t = getTriangle(topLeft.getX(), topLeft.getZ());
        if (t != null && bbox.collideWith(t, results) > 0)
            return 1;
        t = getTriangle(topRight.getX(), topRight.getZ());
        if (t != null && bbox.collideWith(t, results) > 0)
            return 1;
        t = getTriangle(bottomLeft.getX(), bottomLeft.getZ());
        if (t != null && bbox.collideWith(t, results) > 0)
            return 1;
        t = getTriangle(bottomRight.getX(), bottomRight.getZ());
        if (t != null && bbox.collideWith(t, results) > 0)
            return 1;
        
        // box is larger than the points on the terrain, so test against the points
        for (float z=topLeft.getZ(); z<bottomLeft.getZ(); z+=1) {
            for (float x=topLeft.getX(); x<topRight.getX(); x+=1) {
                
                if (x < 0 || z < 0 || x >= size || z >= size)
                    continue;
                t = getTriangle(x,z);
                if (t != null && bbox.collideWith(t, results) > 0)
                    return 1;
            }
        }

        return 0;
    }


    @Override
    public void write(JmeExporter ex) throws IOException {
        // the mesh is removed, and reloaded when read() is called
        // this reduces the save size to 10% by not saving the mesh
        Mesh temp = getMesh();
        mesh = null;
        
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(size, "size", 16);
        oc.write(totalSize, "totalSize", 16);
        oc.write(quadrant, "quadrant", (short)0);
        oc.write(stepScale, "stepScale", Vector3f.UNIT_XYZ);
        oc.write(offset, "offset", Vector3f.UNIT_XYZ);
        oc.write(offsetAmount, "offsetAmount", 0);
        //oc.write(lodCalculator, "lodCalculator", null);
        //oc.write(lodCalculatorFactory, "lodCalculatorFactory", null);
        oc.write(tpLod.getLodEntropies(), "lodEntropy", null);
        oc.write(tpLod.getLODGeomap(), "geomap", null);
        
        setMesh(temp);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        size = ic.readInt("size", 16);
        totalSize = ic.readInt("totalSize", 16);
        quadrant = ic.readShort("quadrant", (short)0);
        stepScale = (Vector) ic.readSavable("stepScale", Vector.ONES(3));
        offset = (Vector) ic.readSavable("offset", Vector.ONES(3));
        offsetAmount = ic.readFloat("offsetAmount", 0);
        //lodCalculator = (LodCalculator) ic.readSavable("lodCalculator", new DistanceLodCalculator());
        //lodCalculator.setTerrainPatch(this);
        //lodCalculatorFactory = (LodCalculatorFactory) ic.readSavable("lodCalculatorFactory", null);
        tpLod.setLodEntropies(ic.readFloatArray("lodEntropy", null));
        tpLod.setLODGeomap((LODGeomap) ic.readSavable("geomap", null));
        
        Mesh regen = tpLod.getLODGeomap().createMesh(stepScale, new Vector2f(1,1), offset, offsetAmount, totalSize, false);
        setMesh(regen);
        //TangentBinormalGenerator.generate(this); // note that this will be removed
        ensurePositiveVolumeBBox();
    }

    @Override
    public TerrainPatch clone() {
        TerrainPatch clone = new TerrainPatch();
        clone.name = name.toString();
        clone.size = size;
        clone.totalSize = totalSize;
        clone.quadrant = quadrant;
        clone.stepScale = stepScale.clone();
        clone.offset = offset.clone();
        clone.offsetAmount = offsetAmount;
        //clone.lodCalculator = lodCalculator.clone();
        //clone.lodCalculator.setTerrainPatch(clone);
        //clone.setLodCalculator(lodCalculatorFactory.clone());
//        clone.geomap = new LODGeomap(size, geomap.getHeightArray());
        clone.tpLod = new TerrainPatchLod(clone.tpLod.size, clone.tpLod.totalSize, clone.tpLod.heightMap, stepScale, offset, offsetAmount);
        clone.setLocalTranslation(getLocalTranslation().clone());
//        Mesh m = clone.geomap.createMesh(clone.stepScale, Vector2f.UNIT_XY, clone.offset, clone.offsetAmount, clone.totalSize, false);
//        clone.setMesh(m);
        clone.setMaterial(material.clone());
        return clone;
    }

    protected void ensurePositiveVolumeBBox() {
        if (getModelBound() instanceof BoundingBox) {
            if (((BoundingBox)getModelBound()).getYExtent() < 0.001f) {
                // a correction so the box always has a volume
                ((BoundingBox)getModelBound()).setYExtent(0.001f);
                updateWorldBound();
            }
        }
    }

    /**
     * Caches the transforms (except rotation) so the LOD calculator,
     * which runs on a separate thread, can access them safely.
     */
    protected void cacheTerrainTransforms() {
        this.worldScaleCached = getWorldScale().clone();
        this.worldTranslationCached = getWorldTranslation().clone();
    }

    public Vector getWorldScaleCached() {
        return worldScaleCached;
    }

    public Vector getWorldTranslationCached() {
        return worldTranslationCached;
    }

    /**
     * Removes any references when the terrain is being removed.
     */
    protected void clearCaches() {
        if (leftNeighbour != null) {
            leftNeighbour.rightNeighbour = null;
            leftNeighbour = null;
        }
        if (rightNeighbour != null) {
            rightNeighbour.leftNeighbour = null;
            rightNeighbour = null;
        }
        if (topNeighbour != null) {
            topNeighbour.bottomNeighbour = null;
            topNeighbour = null;
        }
        if (bottomNeighbour != null) {
            bottomNeighbour.topNeighbour = null;
            bottomNeighbour = null;
        }
    }


}
