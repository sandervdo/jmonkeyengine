package com.jme3.terrain.geomipmap;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import junit.framework.Assert;

public class TerrainCreatePatchTest {

	TerrainQuad tquad;
	Vector3f zero3f; 
	Vector2f zero2f;
	
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
	public void testCreateQuad() {
		float heightMap[] = new float[66049];
		TerrainCreatePatch.createQuad(65, heightMap, tquad);
		Assert.assertEquals(tquad.getChildren().size(), 8);
		
		//Test the creation of new objects. WHen created sucesfully the name is concated by Quad x.
		
		Assert.assertEquals(tquad.getChild(4).getName(), "QuadTestQuad1");
		Assert.assertEquals(tquad.getChild(5).getName(), "QuadTestQuad2");
		Assert.assertEquals(tquad.getChild(6).getName(), "QuadTestQuad3");
		Assert.assertEquals(tquad.getChild(7).getName(), "QuadTestQuad4");
	}
	
	@Test
	public void testTerrainSplit() {
		
	}
}
