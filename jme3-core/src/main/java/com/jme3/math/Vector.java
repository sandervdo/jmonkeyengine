package com.jme3.math;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.management.InvalidAttributeValueException;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;

public class Vector implements Savable, Cloneable, java.io.Serializable {

	static final long serialVersionUID = 1;
	
	private static final Logger logger = Logger.getLogger(Vector4f.class.getName());
	
	private float[] values;
	
	public Vector(int size) {
		this.values = new float[size];
	}
	
	public Vector(float[] v) {
		this.values = new float[v.length];
		for (int x = 0; x < v.length; x++) { this.values[x] = v[x]; }
	}
	
	public Vector(Vector v) {
		new Vector(v.getValues());
	}
	
	/**
	 * Preset with ZERO
	 */
	public void makeZERO() { Arrays.fill(values, 0f); }
	public final static Vector ZERO(int size) { Vector v = new Vector(size); v.makeZERO(); return v; }
	
	/**
	 * Preset with NAN
	 */
	public void makeNAN() { Arrays.fill(values, Float.NaN); }
	public final static Vector NAN(int size) { Vector v = new Vector(size); v.makeNAN(); return v; }
	
	/**
	 * Preset with ONES
	 */
	public void makeONES() { Arrays.fill(values, 1f); }
	public final static Vector ONES(int size) { Vector v = new Vector(size); v.makeONES(); return v; }
	
	/**
	 * Preset with UNIT at X
	 */
	public void makeUNIT(int x) { Arrays.fill(values, 0); values[x] = 1; }
	public final static Vector UNIT(int size, int u) { Vector v = new Vector(size); v.makeUNIT(u); return v; }

	/**
	 * Preset with POSITIVE_INF
	 */
	public void makePOSITIVE_INF() { Arrays.fill(values, Float.POSITIVE_INFINITY); }
	public final static Vector POSITIVE_INF(int size) { Vector v = new Vector(size); v.makePOSITIVE_INF(); return v; }
	
	/**
	 * Preset with NEGATIVE_INF
	 */
	public void makeNEGATIVE_INF() { Arrays.fill(values, Float.NEGATIVE_INFINITY); }
	public final static Vector NEGATIVE_INF(int size) { Vector v = new Vector(size); v.makeNEGATIVE_INF(); return v; }
	
	public Vector set(float[] newValues) {
		for (int x = 0; x < this.values.length; x++) {
			this.values[x] = newValues[x];
		}
		return this;
	}
	
	public Vector set(Vector v) {
		return this.set(v.getValues());
	}
	
	public Vector add(float[] v) {
		Vector res = new Vector(this.getSize()).set(this);
		float[] newValues = new float[this.getSize()];
		for (int x = 0; x < res.getValues().length; x++) {
			newValues[x] = v[x] + this.values[x];
		}
		return res.set(newValues);		
	}
	
	public Vector add(Vector v) {
		if (v == null) { logger.warning("Provided Vector is null, null returned."); return null; }
		return this.add(v.getValues());
	}
	
	public Vector add(Vector v, Vector res) {
		float[] newValues = new float[this.getSize()];
		for (int x = 0; x < this.values.length; x++) {
			newValues[x] = this.values[x] + v.getValues()[x];
		}
		return res.set(newValues);
	}
	
	public Vector addLocal(Vector v) {
		return this.add(v, this);
	}
	
	public Vector addLocal(float[] v) {
		float[] newValues = new float[this.getSize()];
		for (int x = 0; x < this.values.length; x++) {
			newValues[x] = v[x] + this.values[x];
		}
		return this.set(newValues);			
	}
	
	public Vector mult(float scalar) {
		float[] res = new float[this.getSize()];
		for (int x = 0; x < this.values.length; x++) {
			res[x] = this.values[x] * scalar;
		}
		return new Vector(res);
	}
	
	public Vector mult(float scalar, Vector res) {
		return res.set(this.mult(scalar));
	}
	
	public Vector multLocal(float scalar) {
		return this.set(this.mult(scalar));
	}
	
	public Vector scaleAdd(float scalar, Vector a) {
		return scaleAdd(scalar, this, a);
	}
	
	public Vector scaleAdd(float scalar, Vector v, Vector a) {
		return this.set(v.mult(scalar).add(a));
	}
	
	public float dot(Vector v) {
		float res = 0f;
		for (int x = 0; x < this.values.length; x ++) {
			res += this.values[x] * v.getValues()[x];
		}
		return res;
	}
	
//	public Vector project(Vector other) {
//		float n = this.dot(other);
//		float d = other.lengthSquared();
//		return new Vector(other).normalizeLocal().multLocal();
//	}
	
	public boolean isUnitVector() {
		float len = length();
		return 0.99f < len && len < 1.01f;
	}
	
	public float length() {
		return FastMath.sqrt(lengthSquared());
	}
	
	public float lengthSquared() {
		float res = 0f;
		for (int x = 0; x < this.values.length; x++) {
			res += this.values[x] * this.values[x];
		}
		return res;
	}
	
	public float distanceSquared(Vector v) {
		float res = 0f;
		for (int x = 0; x < this.values.length; x++) {
			res += (this.values[x] - v.getValues()[x]) * (this.values[x] - v.getValues()[x]); 
		}
		return res;
	}
	
	public float distance(Vector v) {
		return FastMath.sqrt(distanceSquared(v));
	}
	
	public float[] getValues() {
		return this.values;
	}
	
	public int getSize() {
		return this.values.length;
	}
	
	@Override
	public void write(JmeExporter ex) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void read(JmeImporter im) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
