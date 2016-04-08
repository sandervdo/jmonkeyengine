package com.jme3.terrain.geomipmap;

import java.util.HashMap;
import java.util.List;

import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.terrain.ProgressMonitor;
import com.jme3.terrain.geomipmap.lodcalc.LodCalculator;
import com.jme3.terrain.geomipmap.picking.BresenhamTerrainPicker;

public class TerrainTransform {
	
    /**
     * Caches the transforms (except rotation) so the LOD calculator,
     * which runs on a separate thread, can access them safely.
     */
	
    public static int collideWithRay(Ray ray, CollisionResults results, TerrainQuad terrainQuad) {
        if (terrainQuad.getPicker() == null)
            terrainQuad.setPicker(new BresenhamTerrainPicker(terrainQuad));

        Vector3f intersection = terrainQuad.getPicker().getTerrainIntersection(ray, results);
        if (intersection != null) {
            if (ray.getLimit() < Float.POSITIVE_INFINITY) {
                if (results.getClosestCollision().getDistance() <= ray.getLimit())
                    return 1; // in range
                else
                    return 0; // out of range
            } else
                return 1;
        } else
            return 0;
    }

	
    public static void cacheTerrainTransforms(TerrainQuad tq) {
        for (int i = tq.getChildren().size(); --i >= 0;) {
            Spatial child = tq.getChildren().get(i);
            if (child instanceof TerrainQuad) {
                ((TerrainQuad) child).cacheTerrainTransforms();
            } else if (child instanceof TerrainPatch) {
                ((TerrainPatch) child).cacheTerrainTransforms();
            }
        }
    }
    
    /**
     * Generate the entropy values for the terrain for the "perspective" LOD
     * calculator. This routine can take a long time to run!
     * @param progressMonitor optional
     */
    
    public static void generateEntropy(ProgressMonitor progressMonitor, TerrainQuad terrainQuad) {
        // only check this on the root quad
        if (terrainQuad.isRootQuad())
            if (progressMonitor != null) {
                int numCalc = (terrainQuad.getTotalSize()-1)/(terrainQuad.getPatchSize()-1); // make it an even number
                progressMonitor.setMonitorMax(numCalc*numCalc);
            }

        if (terrainQuad.getChildren() != null) {
            for (int i = terrainQuad.getChildren().size(); --i >= 0;) {
                Spatial child = terrainQuad.getChildren().get(i);
                if (child instanceof TerrainQuad) {
                        ((TerrainQuad) child).generateEntropy(progressMonitor);
                } else if (child instanceof TerrainPatch) {
                    ((TerrainPatch) child).generateLodEntropies();
                    if (progressMonitor != null)
                        progressMonitor.incrementProgress(1);
                }
            }
        }

        // only do this on the root quad
        if (terrainQuad.isRootQuad())
            if (progressMonitor != null)
                progressMonitor.progressComplete();
    }
    
    
    protected static boolean calculateLod(List<Vector3f> location, HashMap<String,UpdatedTerrainPatch> updates, LodCalculator lodCalculator, TerrainQuad terrainQuad) {

        boolean lodChanged = false;

        if (terrainQuad.getChildren() != null) {
            for (int i = terrainQuad.getChildren().size(); --i >= 0;) {
                Spatial child = terrainQuad.getChildren().get(i);
                if (child instanceof TerrainQuad) {
                    boolean b = calculateLod(location, updates, lodCalculator, ((TerrainQuad) child));
                    if (b)
                        lodChanged = true;
                } else if (child instanceof TerrainPatch) {
                    boolean b = lodCalculator.calculateLod((TerrainPatch) child, location, updates);
                    if (b)
                        lodChanged = true;
                }
            }
        }

        return lodChanged;
    }
    
    /**
     * Reset the cached references of neighbours.
     * TerrainQuad caches neighbours for faster LOD checks.
     * Sometimes you might want to reset this cache (for instance in TerrainGrid)
     */
    public static void resetCachedNeighbours(TerrainQuad terrainQuad) {
        if (terrainQuad.getChildren() != null) {
            for (int x = terrainQuad.getChildren().size(); --x >= 0;) {
                Spatial child = terrainQuad.getChildren().get(x);
                if (child instanceof TerrainQuad) {
                    resetCachedNeighbours(((TerrainQuad) child));
                } else if (child instanceof TerrainPatch) {
                    TerrainPatch patch = (TerrainPatch) child;
                    patch.searchedForNeighboursAlready = false;
                }
            }
        }
    }

}
