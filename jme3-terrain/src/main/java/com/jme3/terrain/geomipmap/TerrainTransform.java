package com.jme3.terrain.geomipmap;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
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

}
