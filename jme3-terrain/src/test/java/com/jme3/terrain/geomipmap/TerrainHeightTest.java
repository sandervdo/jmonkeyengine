package com.jme3.terrain.geomipmap;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

public class TerrainHeightTest {
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

}
