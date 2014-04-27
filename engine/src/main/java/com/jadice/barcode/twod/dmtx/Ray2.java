/*
 * This file is part of a java-port of the libdmtx library.
 * 
 * Copyright (C) 2014 levigo solutions gmbh Contact: solutions@levigo.de
 * 
 * 
 * The original library's copyright statement follows:
 * 
 * libdmtx - Data Matrix Encoding/Decoding Library
 * 
 * Copyright (C) 2011 Mike Laughton
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Contact: mike@dragonflylogic.com
 */
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
    assert Math.abs(1.0 - v.magnitude()) <= DatamatrixDecoder.DmtxAlmostZero;

    return v.cross(q.clone().subtract(p));
  }

  /**
   *
   *
   */
  Vector2 pointAlongRay(double t) {
    /* Ray should always have unit length of 1 */
    assert Math.abs(1.0 - v.magnitude()) <= DatamatrixDecoder.DmtxAlmostZero;

    return p.clone().add(v.clone().scale(t));
  }

  /**
   *
   *
   */
  Vector2 intersect(Ray2 p1) {
    double denom = p1.v.cross(v);
    if (Math.abs(denom) <= DatamatrixDecoder.DmtxAlmostZero)
      return null;

    double numer = p1.v.cross(p1.p.clone().subtract(p));

    return pointAlongRay(numer / denom);
  }
}