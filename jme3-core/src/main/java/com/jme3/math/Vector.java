package com.jme3.math;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.management.InvalidAttributeValueException;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
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
		return this.mult(scalar, this);
	}
	
	public Vector mult(float[] v, Vector dest) {
		if (dest == null) dest = new Vector(this.values.length);
		float[] res = new float[this.values.length];
		for (int x = 0; x < this.values.length; x++) {
			res[x] = this.values[x] * v[x];
		}
		return dest.set(res);
	}
	
	public Vector mult(Vector v) {
		return mult(v.getValues(), null);
	}
	
	public Vector mult(Vector v, Vector dest) {
		return mult(v.getValues(), dest);
	}
	
	public Vector divide(float scalar) {
		scalar = 1f / scalar;
		return this.mult(scalar);
	}
	
	public Vector divideLocal(float scalar) {
		scalar = 1f / scalar;
		return this.multLocal(scalar);
	}
	
	public Vector divide(Vector v) {
		float[] res = new float[this.values.length];
		for (int x = 0; x < this.values.length; x++) {
			res[x] = this.values[x] / v.getValues()[x];
		}
		return new Vector(res);
	}
	
	public Vector negate() {
		return this.mult(-1f);
	}
	
	public Vector negateLocal() {
		return this.multLocal(-1f);
	}
	
	public Vector subtract(float[] v) {
		float[] res = new float[this.values.length];
		for (int x = 0; x < this.values.length; x++) {
			res[x] = this.values[x] - v[x];
		}
		return new Vector(res);
	}
	
	public Vector subtract(Vector v) {
		return this.subtract(v.getValues());
	}
	
	public Vector subtractLocal(float[] v) {
		return this.set(this.subtract(v));
	}
	
	public Vector subtractLocal(Vector v) {
		return this.set(this.subtract(v));
	}
	
	public Vector subtract(Vector v, Vector dest) {
		return dest.set(this.subtract(v));
	}
	
	public Vector normalize() {
		float length = lengthSquared();
		
		if (length != 1f && length != 0f) {
			length = 1.0f / this.length();
			return mult(length);
		}
		
		return clone();
	}
	
	public Vector normalizeLocal() {
		return this.set(this.normalize());
	}
	
	public Vector max(Vector v) {
		float[] res = new float[this.values.length];
		for (int x = 0; x < this.values.length; x++) {
			res[x] = (v.getValues()[x] > this.values[x] ? v.getValues()[x] : this.values[x]);
		}
		return new Vector(res);
	}
	
	public Vector maxLocal(Vector v) {
		return this.set(this.max(v));
	}
	
	public Vector min(Vector v) {
		float[] res = new float[this.values.length];
		for (int x = 0; x < this.values.length; x++) {
			res[x] = (v.getValues()[x] < this.values[x] ? v.getValues()[x] : this.values[x]);
		}
		return new Vector(res);
	}
	
	public Vector minLocal(Vector v) {
		return this.set(this.min(v));
	}	
	
	public float angleBetween(Vector v) {
		float dotProduct = dot(v);
		float angle = FastMath.acos(dotProduct);
		return angle;
	}
	
	public Vector interpolateLocal(Vector finalVec, float changeAmnt) {
		return interpolateLocal(this, finalVec, changeAmnt);
	}
	
	public Vector interpolateLocal(Vector beginVec, Vector finalVec, float changeAmnt) {
		for (int x = 0; x < this.values.length; x++) {
			this.values[x] = (1 - changeAmnt) * beginVec.getValues()[x] + changeAmnt * finalVec.getValues()[x];
		}
		return this;
	}
	
	public static boolean isValidVector(Vector v) {
		if (v == null) return false;
		for (int x = 0; x < v.getValues().length; x++) {
			if (Float.isNaN(v.getValues()[x]) || Float.isInfinite(v.getValues()[x])) 
				return false;
		}
		return true;
	}
	
	@Override
	public Vector clone() {
		try {
			return (Vector) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
	
	public float[] toArray(float[] floats) {
		if (floats == null) // Also when invalid length?
			floats = new float[this.values.length];
		
		for (int x = 0; x < this.values.length; x++) {
			floats[x] = this.values[x];
		}
		
		return floats;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Vector)) { return false; }

        if (this == o) { return true; }
        
        Vector comp = (Vector) o;
        for (int x = 0; x < this.values.length; x++) {
        	if (Float.compare(this.values[x], comp.getValues()[x]) != 0) return false;
        }
        return true;
	}
	
	public int hashCode() {
		int hash = 37;
		for (int x = 0; x < this.values.length; x++) { 
			hash += 37 * hash + Float.floatToIntBits(this.values[x]); 
		}
		return hash;
	}
	
	public String toString() {
		String res = "(";
		for (int x = 0; x < this.values.length; x++) { 
			res += this.values[x] + (x < this.values.length - 1 ? ", " : ""); 
		}
		return res + ")";
	}
	
	public float[] getValues() {
		return this.values;
	}
	
	public int getSize() {
		return this.values.length;
	}
	
	@Override
	public void write(JmeExporter e) throws IOException {
		OutputCapsule capsule = e.getCapsule(this);
		for (int x = 0; x < this.values.length; x++) {
			capsule.write(this.values[x], "" + x + "", 0);
		}
	}

	@Override
	public void read(JmeImporter e) throws IOException {
		InputCapsule capsule = e.getCapsule(this);
		for (int x = 0; x < this.values.length; x++) {
			this.values[x] = capsule.readFloat("" + x + "", 0);
		}
	}

	public float get(int index) {
		if (index < this.values.length) {
			return this.values[index];
		}
		throw new IllegalArgumentException("index must be between 0 and " + (this.values.length - 1));
	}
	
	public void set(int index, float f) {
		if (index < this.values.length) {
			this.values[index] = f;
		}
		throw new IllegalArgumentException("index must be between 0 and " + (this.values.length - 1));
	}
}
