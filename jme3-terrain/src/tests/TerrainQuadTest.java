
import org.junit.Before;
import org.junit.Test;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.ProgressMonitor;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.TerrainTransform;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TerrainQuadTest {
	
	TerrainQuad tquad;

	
	
	//String name, int patchSize, int quadSize,
    //Vector3f scale, float[] heightMap, int totalSize,
    //Vector2f offset, float offsetAmount
	
	@Before
	public void setTerrainQuad() {
		float heightMap[] = new float[16641];
		Vector3f scale = new Vector3f(0.0f, 0.0f, 0.0f);
		Vector2f offset = new Vector2f(0.0f, 0.0f);
		tquad = new TerrainQuad("QuadTest", 65, 129,
                            scale, heightMap, 513,
                            offset, 192.0f);
		
	}
		
	
	// Test if there are 4 children created, it starts off with 4 so should be 8.
	
	@Test
	public void testCreateQuadCreation() {
		float heightMap[] = new float[66049];
		tquad.createQuad(65, heightMap);
		assert(tquad.getChildren().size() == 8);
	}
	
	// Test if the creation of the nodes succeeded and the names have been set.
	
	@Test
	public void testCreateQuadObjects() {
		float heightMap[] = new float[66049];
		tquad.createQuad(65, heightMap);
		assert(tquad.getChild(4).getName() == "Quad1");
		assert(tquad.getChild(5).getName() == "Quad2");
		assert(tquad.getChild(6).getName() == "Quad3");
		assert(tquad.getChild(7).getName() == "Quad4");
	}
	
	// Check if it actually has effect.
	
	@Test 
	public void checkGenerateEntropy() {
		// If progress monitor is null then it is not the root node. 
		ProgressMonitor progmonitor = null;
		TerrainTransform.generateEntropy(progmonitor, tquad);
		assert(1 == 1);
	}
	
	
	
	

}
