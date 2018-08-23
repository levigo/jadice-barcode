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
 * @struct DmtxVector2
 * @brief DmtxVector2
 */
class Vector2 {
  double x;
  double y;

  public Vector2() {
  }

  public Vector2(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public void set(Vector2 v) {
    this.x = v.x;
    this.y = v.y;
  }

  @Override
  public Vector2 clone() {
    return new Vector2(x, y);
  }

  /**
     *
     *
     */
  double rightAngleTrueness(Vector2 c1, Vector2 c2, double angle) {
    Vector2 vA = clone().subtract(c1);
    vA.normalize();

    Vector2 vB = c2.clone().subtract(c1);
    vB.normalize();

    final Matrix3 m = new Matrix3();
    m.setToRotate(angle);
    vB.multiply(m);

    return vA.dot(vB);
  }

  /**
 *
 *
 */
  Vector2 add(Vector2 v2) {
    x += v2.x;
    y += v2.y;
    return this;
  }

  /**
 *
 *
 */
  Vector2 subtract(Vector2 v2) {
    x -= v2.x;
    y -= v2.y;
    return this;
  }

  /**
 *
 *
 */
  Vector2 scale(double s) {
    x *= s;
    y *= s;
    return this;
  }

  /**
 *
 *
 */
  double cross(Vector2 v2) {
    return x * v2.y - y * v2.x;
  }

  /**
 *
 *
 */
  double normalize() {
    double mag;

    mag = magnitude();

    if (mag <= DatamatrixDecoder.DmtxAlmostZero)
      return -1.0; /* XXX this doesn't look clean */

    scale(1 / mag);

    return mag;
  }

  /**
 *
 *
 */
  double dot(Vector2 v2) {
    return this.x * v2.x + this.y * v2.y;
  }

  /**
 *
 *
 */
  double magnitude() {
    return Math.sqrt(x * x + y * y);
  }

  /**
   * \brief Multiply vector and matrix in place \param v Vector (input and output) \param m Matrix
   * to be multiplied \return DmtxPass | DmtxFail
   */
  void multiply(Matrix3 m) {
    final Vector2 vOut = new Vector2();

    multiply(m, vOut);

    x = vOut.x;
    y = vOut.y;
  }

  /**
   * \brief Multiply vector and matrix \param vOut Vector (output) \param vIn Vector (input) \param
   * m Matrix to be multiplied \return DmtxPass | DmtxFail
   */
  void multiply(Matrix3 m, Vector2 vOut) {
    double w = this.x * m.m[0][2] + this.y * m.m[1][2] + m.m[2][2];
    if (Math.abs(w) <= DatamatrixDecoder.DmtxAlmostZero) {
      vOut.x = Double.MAX_VALUE;
      vOut.y = Double.MAX_VALUE;
      throw new IllegalStateException();
    }

    double x = (this.x * m.m[0][0] + this.y * m.m[1][0] + m.m[2][0]) / w;
    double y = (this.x * m.m[0][1] + this.y * m.m[1][1] + m.m[2][1]) / w;

    vOut.x = x;
    vOut.y = y;
  }
}