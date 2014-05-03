package com.jadice.barcode.twod.dmtx;


class Matrix3 {
  private static final Matrix3 IDENTITY = new Matrix3();
  static {
    IDENTITY.m = new double[][]{
        new double[]{
            1, 0, 0
        }, new double[]{
            0, 1, 0
        }, new double[]{
            0, 0, 1
        },
    };
  }

  double m[][] = new double[3][3];

  @Override
  public Matrix3 clone() {
    final Matrix3 clone = new Matrix3();
    for (int i = 0; i < m.length; i++)
      System.arraycopy(m[i], 0, clone.m[i], 0, m[i].length);
    return clone;
  }

  /**
   * \brief Copy matrix contents \param m0 Copy target \param m1 Copy source \return void
   */
  void set(Matrix3 m1) {
    for (int i = 0; i < m1.m.length; i++)
      m[i] = m1.m[i].clone();
  }

  /**
   * \brief Generate identity transformation matrix \param m Generated matrix \return void
   * 
   * | 1 0 0 | m = | 0 1 0 | | 0 0 1 |
   * 
   * Transform "m" (doesn't change anything) |\ (0,1) x----o +--+ \ (0,1) x----o | | | \ | | | | | /
   * | | +----* +--+ / +----* (0,0) (1,0) |/ (0,0) (1,0)
   * 
   */
  void reset() {
    set(IDENTITY);
  }

  /**
   * \brief Generate translate transformation matrix \param m Generated matrix \param tx \param ty
   * \return void
   * 
   * | 1 0 0 | m = | 0 1 0 | | tx ty 1 |
   * 
   * Transform "m" _____ (tx,1+ty) x----o (1+tx,1+ty) \ | | | (0,1) x----o / | (0,1) +-|--+ | | | /
   * /\| | +----* (1+tx,ty) | | \ / | | +----* ` +----+ (0,0) (1,0) (0,0) (1,0)
   * 
   */
  void setToTranslate(double tx, double ty) {
    reset();
    m[2][0] = tx;
    m[2][1] = ty;
  }

  /**
   * \brief Generate rotate transformation \param m Generated matrix \param angle \return void
   * 
   * | cos(a) sin(a) 0 | m = | -sin(a) cos(a) 0 | | 0 0 1 | o Transform "m" / ` ___ / ` (0,1) x----o
   * |/ \ x * (cos(a),sin(a)) | | '-- | ` / | | ___/ ` / a +----* `+ - - - - - - (0,0) (1,0) (0,0)
   * 
   */
  void setToRotate(double angle) {
    double sinAngle, cosAngle;

    sinAngle = Math.sin(angle);
    cosAngle = Math.cos(angle);

    reset();
    m[0][0] = cosAngle;
    m[0][1] = sinAngle;
    m[1][0] = -sinAngle;
    m[1][1] = cosAngle;
  }

  /**
   * \brief Generate scale transformation matrix \param m Generated matrix \param sx \param sy
   * \return void
   * 
   * | sx 0 0 | m = | 0 sy 0 | | 0 0 1 |
   * 
   * Transform "m" _____ (0,sy) x-------o (sx,sy) \ | | | (0,1) x----o / | (0,1) +----+ | | | / /\|
   * | | | | | \ / | | | +----* ` +----+--* (0,0) (1,0) (0,0) (sx,0)
   * 
   */
  void setToScale(double sx, double sy) {
    reset();
    m[0][0] = sx;
    m[1][1] = sy;
  }

  /**
   * \brief Generate shear transformation matrix \param m Generated matrix \param shx \param shy
   * \return void
   * 
   * | 0 shy 0 | m = | shx 0 0 | | 0 0 1 |
   */
  void setToShear(double shx, double shy) {
    reset();
    m[1][0] = shx;
    m[0][1] = shy;
  }

  /**
   * \brief Generate top line skew transformation \param m \param b0 \param b1 \param sz \return
   * void
   * 
   * | b1/b0 0 (b1-b0)/(sz*b0) | m = | 0 sz/b0 0 | | 0 0 1 |
   * 
   * (sz,b1) o /| Transform "m" / | / | +--+ / | | | (0,b0) x | | | | | +-+ +-+ (0,sz) +----+ \ /
   * (0,sz) x----o | | \ / | | | | \/ | | +----+ +----+ (0,0) (sz,0) (0,0) (sz,0)
   * 
   */
  void setToLineSkewTop(double b0, double b1, double sz) {
    assert b0 >= LibDMTX.DmtxAlmostZero;

    reset();
    m[0][0] = b1 / b0;
    m[1][1] = sz / b0;
    m[0][2] = (b1 - b0) / (sz * b0);
  }

  /**
   * \brief Generate top line skew transformation (inverse) \param m \param b0 \param b1 \param sz
   * \return void
   */
  void setToLineSkewTopInv(double b0, double b1, double sz) {
    assert b1 >= LibDMTX.DmtxAlmostZero;

    reset();
    m[0][0] = b0 / b1;
    m[1][1] = b0 / sz;
    m[0][2] = (b0 - b1) / (sz * b1);
  }

  /**
   * \brief Generate side line skew transformation \param m \param b0 \param b1 \param sz \return
   * void
   */
  void setToLineSkewSide(double b0, double b1, double sz) {
    assert b0 >= LibDMTX.DmtxAlmostZero;

    reset();
    m[0][0] = sz / b0;
    m[1][1] = b1 / b0;
    m[1][2] = (b1 - b0) / (sz * b0);
  }

  /**
   * \brief Generate side line skew transformation (inverse) \param m \param b0 \param b1 \param sz
   * \return void
   */
  void setToLineSkewSideInv(double b0, double b1, double sz) {
    assert b1 >= LibDMTX.DmtxAlmostZero;

    reset();
    m[0][0] = b0 / sz;
    m[1][1] = b0 / b1;
    m[1][2] = (b0 - b1) / (sz * b1);
  }

  /**
   * \brief Multiply two matrices to create a third \param mOut \param m0 \param m1 \return void
   */
  void multiply(Matrix3 m1, Matrix3 mOut) {
    int i, j, k;
    double val;

    for (i = 0; i < 3; i++)
      for (j = 0; j < 3; j++) {
        val = 0.0;
        for (k = 0; k < 3; k++)
          val += this.m[i][k] * m1.m[k][j];
        mOut.m[i][j] = val;
      }
  }

  /**
   * \brief Multiply two matrices in place \param m0 \param m1 \return void
   */
  void multiply(Matrix3 m1) {
    clone().multiply(m1, this);
  }

  /**
   * \brief Print matrix contents to STDOUT \param m \return void
   */
  void print() {
    System.out.println(String.format("%8.8f\t%8.8f\t%8.8f\n", m[0][0], m[0][1], m[0][2]));
    System.out.println(String.format("%8.8f\t%8.8f\t%8.8f\n", m[1][0], m[1][1], m[1][2]));
    System.out.println(String.format("%8.8f\t%8.8f\t%8.8f\n", m[2][0], m[2][1], m[2][2]));
  }

  /**
   * \brief Generate shear transformation matrix \param m Generated matrix \param shx \param shy
   * \return void
   * 
   * | 0 shy 0 | m = | shx 0 0 | | 0 0 1 |
   */
  void dmtxMatrix3Shear(double shx, double shy) {
    reset();
    m[1][0] = shx;
    m[0][1] = shy;
  };
}