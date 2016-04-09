package com.jme3.terrainTests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.GeoMap;
import com.jme3.terrain.ProgressMonitor;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.TerrainTransform;

public class TerrainQuadTest {
	
	TerrainQuad tquad;
	Vector3f zero3f; 
	Vector2f zero2f;

	
	
	//String name, int patchSize, int quadSize,
    //Vector3f scale, float[] heightMap, int totalSize,
    //Vector2f offset, float offsetAmount
	
	@Before
	public void setTerrainQuad() {
		float heightMap[] = new float[16641];
		zero3f = new Vector3f(0.0f, 0.0f, 0.0f);
		zero2f = new Vector2f(0.0f, 0.0f);
		tquad = new TerrainQuad("QuadTest", 65, 129,
                            zero3f, heightMap, 513,
                            zero2f, 192.0f);
		
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
	
	// Should return either 1 or 0.
	
	@Test 
	public void testCollideWithRay() {
		Ray ray = new Ray();
		ray.setDirection(zero3f);
		CollisionResults collResults = new CollisionResults();
		int ret = TerrainTransform.collideWithRay(ray, collResults, tquad);
		assert(ret == 0 || ret == 1);
	}
	
	@Test
	public void testCalculateLod() {
		assert(10 == 0);
		
	}
	

	
	@Test 
	public void checkGenerateEntropy() {
		ProgressMonitor progMonitor = null;
		TerrainTransform.generateEntropy(progMonitor, tquad);
		// This is hard to test as it is a recursive function that does modify progressmonitor which is really difficult to initiate. But it should execute in this scenario so it is a small test.
	}
	
	
	
	

}
