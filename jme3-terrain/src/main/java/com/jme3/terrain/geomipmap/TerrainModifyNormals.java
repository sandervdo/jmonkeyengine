package com.jme3.terrain.geomipmap;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

public class TerrainModifyNormals {
	
    protected static void updateNormals(TerrainQuad tq) {

        if (needToRecalculateNormals(tq)) {
            //TODO background-thread this if it ends up being expensive
            fixNormals(tq.getAffectedAreaBBox(), tq); // the affected patches
            TerrainQuadrants.fixNormalEdges(tq.getAffectedAreaBBox(), tq); // the edges between the patches
            
            setNormalRecalcNeeded(null, tq); // set to false
        }
    }
    

    /**
     * Find what terrain patches need normal recalculations and update
     * their normals;
     */
    
    protected static void fixNormals(BoundingBox affectedArea, TerrainQuad tq) {
        if (tq.getChildren() == null)
            return;

        // go through the children and see if they collide with the affectedAreaBBox
        // if they do, then update their normals
        for (int x = tq.getChildren().size(); --x >= 0;) {
            Spatial child = tq.getChildren() .get(x);
            if (child instanceof TerrainQuad) {
                if (affectedArea != null && affectedArea.intersects(((TerrainQuad) child).getWorldBound()) )
                    fixNormals(affectedArea, (TerrainQuad) child);
            } else if (child instanceof TerrainPatch) {
                if (affectedArea != null && affectedArea.intersects(((TerrainPatch) child).getWorldBound()) )
                    ((TerrainPatch) child).updateNormals(); // recalculate the patch's normals
            }
        }
    }
    
    /**
     * Signal if the normal vectors for the terrain need to be recalculated.
     * Does this by looking at the affectedAreaBBox bounding box. If the bbox
     * exists already, then it will grow the box to fit the new changedPoint.
     * If the affectedAreaBBox is null, then it will create one of unit size.
     *
     * @param needToRecalculateNormals if null, will cause needToRecalculateNormals() to return false
     */
    
    protected static void setNormalRecalcNeeded(Vector2f changedPoint, TerrainQuad tq) {
        if (changedPoint == null) { // set needToRecalculateNormals() to false
            tq.setAffectedAreaBBox(null);
            return;
        }

        if (tq.getAffectedAreaBBox() == null) {
            tq.setAffectedAreaBBox(new BoundingBox(new Vector3f(changedPoint.x, 0, changedPoint.y), 1f, Float.MAX_VALUE, 1f)); // unit length
        } else {
            // adjust size of box to be larger
        	tq.getAffectedAreaBBox().mergeLocal(new BoundingBox(new Vector3f(changedPoint.x, 0, changedPoint.y), 1f, Float.MAX_VALUE, 1f));
        }
    }
    
    protected static boolean needToRecalculateNormals(TerrainQuad tq) {
        if (tq.getAffectedAreaBBox() != null)
            return true;
        if (!tq.getLastScale().equals(tq.getWorldScale())) {
            tq.setAffectedAreaBBox(new BoundingBox(tq.getWorldTranslation(), Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE));
            tq.setLastScale(tq.getWorldScale());
            return true;
        }
        return false;
    }

}
