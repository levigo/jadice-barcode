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

    if (mag <= LibDMTX.DmtxAlmostZero)
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
    if (Math.abs(w) <= LibDMTX.DmtxAlmostZero) {
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