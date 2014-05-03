package com.jadice.barcode.twod.dmtx;


/**
 * @struct DmtxRay2
 * @brief DmtxRay2
 */
public class Ray2 {
  double tMin;
  double tMax;
  Vector2 p = new Vector2();
  Vector2 v = new Vector2();

  /**
   *
   *
   */
  double distanceAlongRay(Vector2 q) {
    return q.clone().subtract(p).dot(v);
  }

  /**
   *
   *
   */
  double distanceFromRay(Vector2 q) {
    /* Assumes that v is a unit vector */
    assert Math.abs(1.0 - v.magnitude()) <= LibDMTX.DmtxAlmostZero;

    return v.cross(q.clone().subtract(p));
  }

  /**
   *
   *
   */
  Vector2 pointAlongRay(double t) {
    /* Ray should always have unit length of 1 */
    assert Math.abs(1.0 - v.magnitude()) <= LibDMTX.DmtxAlmostZero;

    return p.clone().add(v.clone().scale(t));
  }

  /**
   *
   *
   */
  Vector2 intersect(Ray2 p1) {
    double denom = p1.v.cross(v);
    if (Math.abs(denom) <= LibDMTX.DmtxAlmostZero)
      return null;

    double numer = p1.v.cross(p1.p.clone().subtract(p));

    return pointAlongRay(numer / denom);
  }
}