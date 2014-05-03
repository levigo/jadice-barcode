package com.jadice.barcode.twod.dmtx;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.Shape;
import java.awt.geom.GeneralPath;

import com.jadice.barcode.DiagnosticSettings;
import com.jadice.barcode.Options;
import com.jadice.barcode.Marker.Feature;
import com.jadice.barcode.twod.dmtx.Decode.FlagGrid;

/**
 * @struct DmtxRegion
 * @brief DmtxRegion
 */
public class Region {
  /**
   * @struct DmtxFollow
   * @brief DmtxFollow
   */
  private class Follow {
    int step;
    Point loc = new Point();

    public Follow(Point loc) {
      this.loc = loc.clone();
      step = 0;
    }

    @Override
    public Region.Follow clone() {
      final Region.Follow f = new Follow(loc);
      f.step = step;
      f.loc = loc.clone();
      return f;
    }

    /**
     *
     *
     */
    void followStep(int sign) {
      assert Math.abs(sign) == 1;
      assert dec.getFlagGrid().isAnySet(loc, 0x40);

      int factor = stepsTotal + 1;
      int stepMod;
      if (sign > 0)
        stepMod = (factor + this.step % factor) % factor;
      else
        stepMod = (factor - this.step % factor) % factor;

      /* End of positive trail -- magic jump */
      if (sign > 0 && stepMod == jumpToNeg)
        loc.setTo(finalNeg);
      else if (sign < 0 && stepMod == jumpToPos)
        loc.setTo(finalPos);
      else {
        int flags = dec.getFlagGrid().get(loc);
        int patternIdx = sign < 0 ? flags & 0x07 : (flags & 0x38) >> 3;
        this.loc.x += dmtxPatternX[patternIdx];
        this.loc.y += dmtxPatternY[patternIdx];
      }

      step += sign;
    }

    /**
     *
     *
     */
    void followStep2(int sign) {
      assert Math.abs(sign) == 1;
      FlagGrid flags = dec.getFlagGrid();
      assert flags.isAnySet(loc, 0x40);

      int patternIdx = sign < 0 ? flags.get(loc) & 0x07 : (flags.get(loc) & 0x38) >> 3;
      this.loc.x += dmtxPatternX[patternIdx];
      this.loc.y += dmtxPatternY[patternIdx];

      step += sign;
    }
  }

  private enum Edge {
    DmtxEdgeTop, DmtxEdgeBottom, DmtxEdgeLeft, DmtxEdgeRight
  }

  private class BestLine {
    int angle;
    int hOffset;
    int mag;
    int stepBeg;
    int stepPos;
    int stepNeg;
    long distSq;
    double devn;
    Point locBeg;
    Point locPos;
    Point locNeg;

    @Override
    public BestLine clone() {
      final BestLine l = new BestLine();
      l.angle = angle;
      l.hOffset = hOffset;
      l.mag = mag;
      l.stepBeg = stepBeg;
      l.stepPos = stepPos;
      l.stepNeg = stepNeg;
      l.distSq = distSq;
      l.devn = devn;
      l.locBeg = locBeg.clone();
      l.locPos = locPos.clone();
      l.locNeg = locNeg.clone();
      return l;
    }

    @Override
    public String toString() {
      return "BestLine [angle=" + angle + ", hOffset=" + hOffset + ", mag=" + mag + ", stepBeg=" + stepBeg
          + ", stepPos=" + stepPos + ", stepNeg=" + stepNeg + ", distSq=" + distSq + ", devn=" + devn + ", locBeg="
          + locBeg + ", locPos=" + locPos + ", locNeg=" + locNeg + "]";
    }
  }

  public class PointFlow {
    int colorPlane;
    Direction arrive;
    final Direction depart;
    final int magnitude;
    final Point position;

    public PointFlow(int colorPlane, Direction arrive, Direction depart, int mag, Point loc) {
      this.colorPlane = colorPlane;
      this.arrive = arrive;
      this.depart = depart;
      this.magnitude = mag;
      this.position = loc;
    }

    @Override
    public PointFlow clone() {
      return new PointFlow(colorPlane, arrive, depart, magnitude, position.clone());
    }

    /**
     * 
     */
    PointFlow findStrongestNeighbor(int sign) {
      final Point loc = new Point();

      Direction attempt = sign < 0 ? this.depart : this.depart.opposite();

      int occupied = 0;
      PointFlow strongestFlow = blankEdge;
      for (Direction direction : Direction.values()) {
        loc.x = this.position.x + direction.dx;
        loc.y = this.position.y + direction.dy;

        if (!dec.getFlagGrid().isValid(loc))
          continue;

        if (dec.getFlagGrid().isAnySet(loc, 0x80))
          if (++occupied > 2)
            return blankEdge;
          else
            continue;

        int attemptDiff = abs(attempt.ordinal() - direction.ordinal());
        if (attemptDiff > 4)
          attemptDiff = 8 - attemptDiff;
        if (attemptDiff > 1)
          continue;

        PointFlow flow = getPointFlow(this.colorPlane, loc, direction);

        if (flow.magnitude > strongestFlow.magnitude || flow.magnitude == strongestFlow.magnitude
            && direction.isManhattan())
          strongestFlow = flow;
      }

      return strongestFlow;
    }

    @Override
    public String toString() {
      return "PointFlow [plane=" + colorPlane + ", from " + arrive + " to " + depart + ", mag=" + magnitude + ", loc="
          + position + "]";
    }
  }

  /**
   * \brief Scan individual pixel for presence of barcode edge \param dec Pointer to DmtxDecode
   * information struct \param loc Pixel location \return Detected region (if any)
   */
  static Region scan(Decode dec, Point loc, Options options) {
    if (dec.getFlagGrid().isAnySet(loc, 0x80))
      return null;

    DiagnosticSettings diag = options.getOptions(DiagnosticSettings.class);
    if (diag.isMarkupEnabled())
      diag.getTransientMarkers().clear();

    final Region reg = new Region(dec, diag);

    /* Test for presence of any reasonable edge at this location */
    final PointFlow flowBegin = reg.detectEdge(loc);
    if (flowBegin.magnitude < (int) (dec.getEdgeThresh() * 7.65 + 0.5))
      return null;

    /* Determine barcode orientation */
    if (!reg.matrixRegionOrientation(flowBegin))
      return null;
    if (!reg.updateXfrms())
      return null;

    /* Define top edge */
    reg.matrixRegionAlignCalibEdge(Region.Edge.DmtxEdgeTop);
    if (!reg.updateXfrms())
      return null;

    /* Define right edge */
    reg.matrixRegionAlignCalibEdge(Region.Edge.DmtxEdgeRight);
    if (!reg.updateXfrms())
      return null;

    Region.CALLBACK_MATRIX(reg);

    /* Calculate the best fitting symbol size */
    if (!reg.matrixRegionFindSize())
      return null;

    if (diag.isMarkupEnabled())
      diag.addTransientPoint(Feature.DETECTION_SCAN, loc.x, dec.yMax - loc.y);

    /* Found a valid matrix region */
    return reg;
  }

  /* Trail blazing values */
  private int jumpToPos; /* */
  private int jumpToNeg; /* */
  private int stepsTotal; /* */
  private Point finalPos; /* */
  private Point finalNeg; /* */
  private Point boundMin; /* */
  private Point boundMax; /* */
  private PointFlow flowBegin; /* */

  /* Orientation values */
  private int polarity; /* */
  @SuppressWarnings("unused")
  private int stepR;
  @SuppressWarnings("unused")
  private int stepT;
  private Point locR; /* remove if stepR works above */
  private Point locT; /* remove if stepT works above */

  /* Region fitting values */
  private int leftKnown; /* known == 1; unknown == 0 */
  private int leftAngle; /* hough angle of left edge */
  private Point leftLoc; /* known (arbitrary) location on left edge */
  private BestLine leftLine; /* */
  private int bottomKnown; /* known == 1; unknown == 0 */
  private int bottomAngle; /* hough angle of bottom edge */
  private Point bottomLoc; /* known (arbitrary) location on bottom edge */
  private BestLine bottomLine; /* */
  private int topKnown; /* known == 1; unknown == 0 */
  private int topAngle; /* hough angle of top edge */
  private Point topLoc; /* known (arbitrary) location on top edge */
  private int rightKnown; /* known == 1; unknown == 0 */
  private int rightAngle; /* hough angle of right edge */
  private Point rightLoc; /* known (arbitrary) location on right edge */

  /* Region calibration values */
  private int onColor; /* */
  private int offColor; /* */
  private SymbolSize sizeIdx; /*
                               * Index of arrays that store Data Matrix constants
                               */
  private int symbolRows; /*
                           * Number of total rows in symbol including alignment patterns
                           */
  private int symbolCols; /*
                           * Number of total columns in symbol including alignment patterns
                           */
  @SuppressWarnings("unused")
  private int mappingRows; /* Number of data rows in symbol */
  @SuppressWarnings("unused")
  private int mappingCols; /* Number of data columns in symbol */

  /* Transform values */
  /*
   * 3x3 transformation from raw image to fitted barcode grid
   */
  private final Matrix3 raw2fit = new Matrix3();
  /*
   * 3x3 transformation from fitted barcode grid to raw image
   */
  private final Matrix3 fit2raw = new Matrix3();

  private static final Direction NEIGHTBOR_NONE = null;

  public static final int DmtxSymbolSquareCount = 24;
  public static final int DmtxSymbolRectCount = 6;

  // Compass directions
  // 6 5 4
  // 7 X 3
  // 0 1 2
  private enum Direction {
    // @formatter:off
    SW(-1, -1,  0), 
    S ( 0, -1,  1), 
    SE( 1, -1,  2), 
    E ( 1,  0,  1), 
    NE( 1,  1,  0),
    N ( 0,  1, -1),
    NW(-1,  1, -2),
    W (-1,  0, -1);
    // @formatter:on

    public final int dx;
    public final int dy;
    public final int coefficient;
    private Direction opposite;

    private Direction(int dx, int dy, int coefficient) {
      this.dx = dx;
      this.dy = dy;
      this.coefficient = coefficient;
    }

    public Direction opposite() {
      if (opposite == null)
        opposite = get(ordinal() + 4);
      return opposite;
    }

    public boolean isManhattan() {
      return (ordinal() & 1) != 0;
    }

    public static Direction get(int direction) {
      return values()[direction % 8];
    }
  }

  public static final int dmtxPatternX[] = {
      -1, 0, 1, 1, 1, 0, -1, -1
  };
  public static final int dmtxPatternY[] = {
      -1, -1, -1, 0, 1, 1, 1, 0
  };

  static int rHvX[] = {
      256, 256, 256, 256, 255, 255, 255, 254, 254, 253, 252, 251, 250, 249, 248, 247, 246, 245, 243, 242, 241, 239,
      237, 236, 234, 232, 230, 228, 226, 224, 222, 219, 217, 215, 212, 210, 207, 204, 202, 199, 196, 193, 190, 187,
      184, 181, 178, 175, 171, 168, 165, 161, 158, 154, 150, 147, 143, 139, 136, 132, 128, 124, 120, 116, 112, 108,
      104, 100, 96, 92, 88, 83, 79, 75, 71, 66, 62, 58, 53, 49, 44, 40, 36, 31, 27, 22, 18, 13, 9, 4, 0, -4, -9, -13,
      -18, -22, -27, -31, -36, -40, -44, -49, -53, -58, -62, -66, -71, -75, -79, -83, -88, -92, -96, -100, -104, -108,
      -112, -116, -120, -124, -128, -132, -136, -139, -143, -147, -150, -154, -158, -161, -165, -168, -171, -175, -178,
      -181, -184, -187, -190, -193, -196, -199, -202, -204, -207, -210, -212, -215, -217, -219, -222, -224, -226, -228,
      -230, -232, -234, -236, -237, -239, -241, -242, -243, -245, -246, -247, -248, -249, -250, -251, -252, -253, -254,
      -254, -255, -255, -255, -256, -256, -256
  };
  static int rHvY[] = {
      0, 4, 9, 13, 18, 22, 27, 31, 36, 40, 44, 49, 53, 58, 62, 66, 71, 75, 79, 83, 88, 92, 96, 100, 104, 108, 112, 116,
      120, 124, 128, 132, 136, 139, 143, 147, 150, 154, 158, 161, 165, 168, 171, 175, 178, 181, 184, 187, 190, 193,
      196, 199, 202, 204, 207, 210, 212, 215, 217, 219, 222, 224, 226, 228, 230, 232, 234, 236, 237, 239, 241, 242,
      243, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 254, 255, 255, 255, 256, 256, 256, 256, 256, 256, 256,
      255, 255, 255, 254, 254, 253, 252, 251, 250, 249, 248, 247, 246, 245, 243, 242, 241, 239, 237, 236, 234, 232,
      230, 228, 226, 224, 222, 219, 217, 215, 212, 210, 207, 204, 202, 199, 196, 193, 190, 187, 184, 181, 178, 175,
      171, 168, 165, 161, 158, 154, 150, 147, 143, 139, 136, 132, 128, 124, 120, 116, 112, 108, 104, 100, 96, 92, 88,
      83, 79, 75, 71, 66, 62, 58, 53, 49, 44, 40, 36, 31, 27, 22, 18, 13, 9, 4
  };

  private final Decode dec;

  public static final int DmtxModuleOff = 0x00;
  public static final int DmtxModuleOnRed = 0x01;
  public static final int DmtxModuleOnGreen = 0x02;
  public static final int DmtxModuleOnBlue = 0x04;
  public static final int DmtxModuleOnRGB = 0x07; /* OnRed | OnGreen | OnBlue */
  public static final int DmtxModuleOn = 0x07;
  public static final int DmtxModuleUnsure = 0x08;
  public static final int DmtxModuleAssigned = 0x10;
  public static final int DmtxModuleVisited = 0x20;
  public static final int DmtxModuleData = 0x40;

  static final int DMTX_HOUGH_RES = 180;

  private final DiagnosticSettings diag;

  final PointFlow blankEdge = new PointFlow(0, null, null, Integer.MIN_VALUE, new Point());

  public Region(Decode dec, DiagnosticSettings diag) {
    this.dec = dec;
    this.diag = diag;
  }

  static void CALLBACK_MATRIX(Region reg) {
    // TODO Auto-generated method stub

  }

  static void CALLBACK_POINT_PLOT(Point loc, int i, int j, int k) {
    // TODO Auto-generated method stub

  }

  /**
   *
   *
   */
  private boolean matrixRegionOrientation(PointFlow begin) {
    SymbolSize symbolShape;
    if (dec.getSizeIdxExpected() == SymbolSize.SquareAuto
        || dec.getSizeIdxExpected().ordinal() >= SymbolSize.Fixed10x10.ordinal()
        && dec.getSizeIdxExpected().ordinal() <= SymbolSize.Fixed144x144.ordinal())
      symbolShape = SymbolSize.SquareAuto;
    else if (dec.getSizeIdxExpected() == SymbolSize.RectAuto
        || dec.getSizeIdxExpected().ordinal() >= SymbolSize.Fixed8x18.ordinal()
        && dec.getSizeIdxExpected().ordinal() <= SymbolSize.Fixed16x48.ordinal())
      symbolShape = SymbolSize.RectAuto;
    else
      symbolShape = SymbolSize.ShapeAuto;

    int maxDiagonal;
    if (dec.getEdgeMax() != LibDMTX.DmtxUndefined) {
      if (symbolShape == SymbolSize.RectAuto)
        maxDiagonal = (int) (1.23 * dec.getEdgeMax() + 0.5); // sqrt(5/4) + 10%
      else
        maxDiagonal = (int) (1.56 * dec.getEdgeMax() + 0.5); // sqrt(2) + 10%
    } else
      maxDiagonal = LibDMTX.DmtxUndefined;

    /* Follow to end in both directions */
    if (!followEdge(begin, maxDiagonal) || this.stepsTotal < 40) {
      TrailClear(0x40);
      return false;
    }

    /* Filter out region candidates that are smaller than expected */
    if (dec.getEdgeMin() != LibDMTX.DmtxUndefined) {
      int scale = dec.getScale();

      int minArea;
      if (symbolShape == SymbolSize.SquareAuto)
        minArea = dec.getEdgeMin() * dec.getEdgeMin() / (scale * scale);
      else
        minArea = 2 * dec.getEdgeMin() * dec.getEdgeMin() / (scale * scale);

      if ((this.boundMax.x - this.boundMin.x) * (this.boundMax.y - this.boundMin.y) < minArea) {
        TrailClear(0x40);
        return false;
      }
    }

    BestLine line1x = findBestSolidLine(0, 0, +1, LibDMTX.DmtxUndefined);
    if (line1x.mag < 5) {
      TrailClear(0x40);
      return false;
    }

    findTravelLimits(line1x);
    if (line1x.distSq < 100 || line1x.devn * 10 >= Math.sqrt(line1x.distSq)) {
      TrailClear(0x40);
      return false;
    }
    assert line1x.stepPos >= line1x.stepNeg;

    Follow fTmp = followSeek(line1x.stepPos + 5);
    BestLine line2p = findBestSolidLine(fTmp.step, line1x.stepNeg, +1, line1x.angle);

    fTmp = followSeek(line1x.stepNeg - 5);
    BestLine line2n = findBestSolidLine(fTmp.step, line1x.stepPos, -1, line1x.angle);
    if (max(line2p.mag, line2n.mag) < 5)
      return false;

    BestLine line2x;
    int cross;
    if (line2p.mag > line2n.mag) {
      line2x = line2p;
      findTravelLimits(line2x);
      if (line2x.distSq < 100 || line2x.devn * 10 >= Math.sqrt(line2x.distSq))
        return false;

      cross = (line1x.locPos.x - line1x.locNeg.x) * (line2x.locPos.y - line2x.locNeg.y)
          - (line1x.locPos.y - line1x.locNeg.y) * (line2x.locPos.x - line2x.locNeg.x);
      if (cross > 0) {
        /* Condition 2 */
        this.polarity = +1;
        this.locR = line2x.locPos;
        this.stepR = line2x.stepPos;
        this.locT = line1x.locNeg;
        this.stepT = line1x.stepNeg;
        this.leftLoc = line1x.locBeg;
        this.leftAngle = line1x.angle;
        this.bottomLoc = line2x.locBeg;
        this.bottomAngle = line2x.angle;
        this.leftLine = line1x;
        this.bottomLine = line2x;
      } else {
        /* Condition 3 */
        this.polarity = -1;
        this.locR = line1x.locNeg;
        this.stepR = line1x.stepNeg;
        this.locT = line2x.locPos;
        this.stepT = line2x.stepPos;
        this.leftLoc = line2x.locBeg;
        this.leftAngle = line2x.angle;
        this.bottomLoc = line1x.locBeg;
        this.bottomAngle = line1x.angle;
        this.leftLine = line2x;
        this.bottomLine = line1x;
      }
    } else {
      line2x = line2n;
      findTravelLimits(line2x);
      if (line2x.distSq < 100 || line2x.devn / Math.sqrt(line2x.distSq) >= 0.1)
        return false;

      cross = (line1x.locNeg.x - line1x.locPos.x) * (line2x.locNeg.y - line2x.locPos.y)
          - (line1x.locNeg.y - line1x.locPos.y) * (line2x.locNeg.x - line2x.locPos.x);
      if (cross > 0) {
        /* Condition 1 */
        this.polarity = -1;
        this.locR = line2x.locNeg;
        this.stepR = line2x.stepNeg;
        this.locT = line1x.locPos;
        this.stepT = line1x.stepPos;
        this.leftLoc = line1x.locBeg;
        this.leftAngle = line1x.angle;
        this.bottomLoc = line2x.locBeg;
        this.bottomAngle = line2x.angle;
        this.leftLine = line1x;
        this.bottomLine = line2x;
      } else {
        /* Condition 4 */
        this.polarity = +1;
        this.locR = line1x.locPos;
        this.stepR = line1x.stepPos;
        this.locT = line2x.locNeg;
        this.stepT = line2x.stepNeg;
        this.leftLoc = line2x.locBeg;
        this.leftAngle = line2x.angle;
        this.bottomLoc = line1x.locBeg;
        this.bottomAngle = line1x.angle;
        this.leftLine = line2x;
        this.bottomLine = line1x;
      }
    }
    /*
     * CALLBACK_POINT_PLOT(reg.locR, 2, 1, 1); CALLBACK_POINT_PLOT(reg.locT, 2, 1, 1);
     */

    this.leftKnown = this.bottomKnown = 1;

    return true;
  }

  /**
   *
   *
   */
  private boolean updateCorners(Vector2 p00, Vector2 p10, Vector2 p11, Vector2 p01) {
    double xMax, yMax;
    double tx, ty, phi, shx, scx, scy, skx, sky;
    double dimOT, dimOR, dimTX, dimRX, ratio;
    final Matrix3 m = new Matrix3(), mtxy = new Matrix3(), mphi = new Matrix3(), mshx = new Matrix3(), mscx = new Matrix3(), mscy = new Matrix3(), mscxy = new Matrix3(), msky = new Matrix3(), mskx = new Matrix3();

    xMax = dec.getWidth() - 1;
    yMax = dec.getHeight() - 1;

    if (p00.x < 0.0 || p00.y < 0.0 || p00.x > xMax || p00.y > yMax || p01.x < 0.0 || p01.y < 0.0 || p01.x > xMax
        || p01.y > yMax || p10.x < 0.0 || p10.y < 0.0 || p10.x > xMax || p10.y > yMax)
      return false;

    final Vector2 vOT = p01.clone().subtract(p00);
    dimOT = vOT.magnitude();
    /*
     * XXX could use MagSquared()
     */
    final Vector2 vOR = p10.clone().subtract(p00);
    dimOR = vOR.magnitude();
    final Vector2 vTX = p11.clone().subtract(p01);
    dimTX = vTX.magnitude();
    final Vector2 vRX = p11.clone().subtract(p10);
    dimRX = vRX.magnitude();

    /* Verify that sides are reasonably long */
    if (dimOT <= 8.0 || dimOR <= 8.0 || dimTX <= 8.0 || dimRX <= 8.0)
      return false;

    /* Verify that the 4 corners define a reasonably fat quadrilateral */
    ratio = dimOT / dimRX;
    if (ratio <= 0.5 || ratio >= 2.0)
      return false;

    ratio = dimOR / dimTX;
    if (ratio <= 0.5 || ratio >= 2.0)
      return false;

    /* Verify this is not a bowtie shape */
    if (vOR.cross(vRX) <= 0.0 || vOT.cross(vTX) >= 0.0)
      return false;

    if (p00.rightAngleTrueness(p10, p11, Math.PI / 2) <= dec.getSquareDevn())
      return false;
    if (p10.rightAngleTrueness(p11, p01, Math.PI / 2) <= dec.getSquareDevn())
      return false;

    /* Calculate values needed for transformations */
    tx = -1 * p00.x;
    ty = -1 * p00.y;
    mtxy.setToTranslate(tx, ty);

    phi = Math.atan2(vOT.x, vOT.y);
    mphi.setToRotate(phi);
    mtxy.multiply(mphi, m);

    final Vector2 vTmp = p10.clone();
    p10.multiply(m, vTmp);
    shx = -vTmp.y / vTmp.x;
    mshx.setToShear(0.0, shx);
    m.multiply(mshx);

    scx = 1.0 / vTmp.x;
    mscx.setToScale(scx, 1.0);
    m.multiply(mscx);

    p11.multiply(m, vTmp);
    scy = 1.0 / vTmp.y;
    mscy.setToScale(1.0, scy);
    m.multiply(mscy);

    p11.multiply(m, vTmp);
    skx = vTmp.x;
    mskx.setToLineSkewSide(1.0, skx, 1.0);
    m.multiply(mskx);

    p01.multiply(m, vTmp);
    sky = vTmp.y;
    msky.setToLineSkewTop(sky, 1.0, 1.0);
    m.multiply(msky, this.raw2fit);

    /* Create inverse matrix by reverse (avoid straight matrix inversion) */
    msky.setToLineSkewTopInv(sky, 1.0, 1.0);
    mskx.setToLineSkewSideInv(1.0, skx, 1.0);
    msky.multiply(mskx, m);

    mscxy.setToScale(1.0 / scx, 1.0 / scy);
    m.multiply(mscxy);

    mshx.setToShear(0.0, -shx);
    m.multiply(mshx);

    mphi.setToRotate(-phi);
    m.multiply(mphi);

    mtxy.setToTranslate(-tx, -ty);
    m.multiply(mtxy, this.fit2raw);

    return true;
  }

  /**
   *
   *
   */
  private boolean updateXfrms() {
    double radians;
    final Ray2 rLeft = new Ray2(), rBottom = new Ray2(), rTop = new Ray2(), rRight = new Ray2();

    assert this.leftKnown != 0 && this.bottomKnown != 0;

    /* Build ray representing left edge */
    rLeft.p.x = this.leftLoc.x;
    rLeft.p.y = this.leftLoc.y;
    radians = this.leftAngle * Math.PI / DMTX_HOUGH_RES;
    rLeft.v.x = Math.cos(radians);
    rLeft.v.y = Math.sin(radians);
    rLeft.tMin = 0.0;
    rLeft.tMax = rLeft.v.normalize();

    /* Build ray representing bottom edge */
    rBottom.p.x = this.bottomLoc.x;
    rBottom.p.y = this.bottomLoc.y;
    radians = this.bottomAngle * Math.PI / DMTX_HOUGH_RES;
    rBottom.v.x = Math.cos(radians);
    rBottom.v.y = Math.sin(radians);
    rBottom.tMin = 0.0;
    rBottom.tMax = rBottom.v.normalize();

    /* Build ray representing top edge */
    if (this.topKnown != 0) {
      rTop.p.x = this.topLoc.x;
      rTop.p.y = this.topLoc.y;
      radians = this.topAngle * Math.PI / DMTX_HOUGH_RES;
      rTop.v.x = Math.cos(radians);
      rTop.v.y = Math.sin(radians);
      rTop.tMin = 0.0;
      rTop.tMax = rTop.v.normalize();
    } else {
      rTop.p.x = this.locT.x;
      rTop.p.y = this.locT.y;
      radians = this.bottomAngle * Math.PI / DMTX_HOUGH_RES;
      rTop.v.x = Math.cos(radians);
      rTop.v.y = Math.sin(radians);
      rTop.tMin = 0.0;
      rTop.tMax = rBottom.tMax;
    }

    /* Build ray representing right edge */
    if (this.rightKnown != 0) {
      rRight.p.x = this.rightLoc.x;
      rRight.p.y = this.rightLoc.y;
      radians = this.rightAngle * Math.PI / DMTX_HOUGH_RES;
      rRight.v.x = Math.cos(radians);
      rRight.v.y = Math.sin(radians);
      rRight.tMin = 0.0;
      rRight.tMax = rRight.v.normalize();
    } else {
      rRight.p.x = this.locR.x;
      rRight.p.y = this.locR.y;
      radians = this.leftAngle * Math.PI / DMTX_HOUGH_RES;
      rRight.v.x = Math.cos(radians);
      rRight.v.y = Math.sin(radians);
      rRight.tMin = 0.0;
      rRight.tMax = rLeft.tMax;
    }

    /* Calculate 4 corners, real or imagined */
    final Vector2 p00 = rLeft.intersect(rBottom);
    if (null == p00)
      return false;

    final Vector2 p10 = rBottom.intersect(rRight);
    if (null == p10)
      return false;

    final Vector2 p11 = rRight.intersect(rTop);
    if (null == p11)
      return false;

    final Vector2 p01 = rTop.intersect(rLeft);
    if (null == p01)
      return false;

    if (!updateCorners(p00, p10, p11, p01))
      return false;

    return true;
  }

  /**
   * \brief Read color of Data Matrix module location \param dec \param reg \param symbolRow \param
   * symbolCol \param sizeIdx \return Averaged module color
   */
  private int readModuleColor(int symbolRow, int symbolCol, SymbolSize sizeIdx, int colorPlane) {
    int i;
    int symbolRows, symbolCols;
    int color, colorTmp;
    final double sampleX[] = {
        0.5, 0.4, 0.5, 0.6, 0.5
    };
    final double sampleY[] = {
        0.5, 0.5, 0.4, 0.5, 0.6
    };
    final Vector2 p = new Vector2();

    symbolRows = sizeIdx.symbolRows;
    symbolCols = sizeIdx.symbolCols;

    color = 0;
    for (i = 0; i < 5; i++) {

      p.x = 1.0 / symbolCols * (symbolCol + sampleX[i]);
      p.y = 1.0 / symbolRows * (symbolRow + sampleY[i]);

      p.multiply(this.fit2raw);

      try {
        colorTmp = dec.getPixelValue((int) (p.x + 0.5), (int) (p.y + 0.5), colorPlane);
        color += colorTmp;
      } catch (final OutOfRangeException e) {
        // FIXME
      }
    }

    return color / 5;
  }

  /**
   * \brief Increment counters used to determine module values \param img \param reg \param tally
   * \param xOrigin \param yOrigin \param mapWidth \param mapHeight \param dir \return void
   */
  private void tallyModuleJumps(int tally[][], int xOrigin, int yOrigin, int mapWidth, int mapHeight, Direction dir) {
    assert (dir == Direction.N || dir == Direction.W || dir == Direction.S || dir == Direction.E);

    final int travelStep = (dir == Direction.N || dir == Direction.E) ? 1 : -1;

    /*
     * Abstract row and column progress using pointers to allow grid traversal in all 4 directions
     * using same logic
     */
    final int symbolRow[] = new int[1], symbolCol[] = new int[1];
    int line[], travel[];
    int extent;
    int lineStart;
    int lineStop;
    int travelStart;
    int travelStop;
    if (dir == Direction.W || dir == Direction.E) {
      line = symbolRow;
      travel = symbolCol;
      extent = mapWidth;
      lineStart = yOrigin;
      lineStop = yOrigin + mapHeight;
      travelStart = (travelStep == 1) ? xOrigin - 1 : xOrigin + mapWidth;
      travelStop = (travelStep == 1) ? xOrigin + mapWidth : xOrigin - 1;
    } else {
      assert (dir == Direction.N || dir == Direction.S);
      line = symbolCol;
      travel = symbolRow;
      extent = mapHeight;
      lineStart = xOrigin;
      lineStop = xOrigin + mapWidth;
      travelStart = (travelStep == 1) ? yOrigin - 1 : yOrigin + mapHeight;
      travelStop = (travelStep == 1) ? yOrigin + mapHeight : yOrigin - 1;
    }

    final boolean darkOnLight = (this.offColor > this.onColor);
    final int jumpThreshold = abs((int) (0.4 * (this.offColor - this.onColor) + 0.5));

    assert (jumpThreshold >= 0);

    for (line[0] = lineStart; line[0] < lineStop; line[0]++) {

      /*
       * Capture tModule for each leading border module as normal but decide status based on
       * predictable barcode border pattern
       */

      travel[0] = travelStart;
      int color = this.readModuleColor(symbolRow[0], symbolCol[0], this.sizeIdx, this.flowBegin.colorPlane);
      int tModule = (darkOnLight) ? this.offColor - color : color - this.offColor;

      int statusModule = (travelStep == 1 || (line[0] & 0x01) == 0) ? Region.DmtxModuleOnRGB : Region.DmtxModuleOff;

      int weight = extent;

      while ((travel[0] += travelStep) != travelStop) {

        final int tPrev = tModule;
        final int statusPrev = statusModule;

        /*
         * For normal data-bearing modules capture color and decide module status based on
         * comparison to previous "known" module
         */

        color = this.readModuleColor(symbolRow[0], symbolCol[0], this.sizeIdx, this.flowBegin.colorPlane);
        tModule = (darkOnLight) ? this.offColor - color : color - this.offColor;

        if (statusPrev == Region.DmtxModuleOnRGB) {
          if (tModule < tPrev - jumpThreshold)
            statusModule = Region.DmtxModuleOff;
          else
            statusModule = Region.DmtxModuleOnRGB;
        } else if (statusPrev == Region.DmtxModuleOff)
          if (tModule > tPrev + jumpThreshold)
            statusModule = Region.DmtxModuleOnRGB;
          else
            statusModule = Region.DmtxModuleOff;

        final int mapRow = symbolRow[0] - yOrigin;
        final int mapCol = symbolCol[0] - xOrigin;
        assert (mapRow < 24 && mapCol < 24);

        if (statusModule == Region.DmtxModuleOnRGB)
          tally[mapRow][mapCol] += (2 * weight);

        weight--;
      }

      assert (weight == 0);
    }
  }

  /**
   * \brief Determine barcode size, expressed in modules \param image \param reg \return DmtxPass |
   * DmtxFail
   */
  private boolean matrixRegionFindSize() {
    int row, col;
    SymbolSize sizeIdxBeg;
    SymbolSize sizeIdxEnd;
    int sizeIdx, bestSizeIdx;
    int symbolRows, symbolCols;
    int jumpCount, errors;
    int color;
    int colorOnAvg, bestColorOnAvg;
    int colorOffAvg, bestColorOffAvg;
    int contrast, bestContrast;
    dec.getImage();
    bestSizeIdx = LibDMTX.DmtxUndefined;
    bestContrast = 0;
    bestColorOnAvg = bestColorOffAvg = 0;

    if (dec.getSizeIdxExpected() == SymbolSize.ShapeAuto) {
      sizeIdxBeg = SymbolSize.values()[0];
      sizeIdxEnd = SymbolSize.values()[DmtxSymbolSquareCount + DmtxSymbolRectCount];
    } else if (dec.getSizeIdxExpected() == SymbolSize.SquareAuto) {
      sizeIdxBeg = SymbolSize.values()[0];
      sizeIdxEnd = SymbolSize.values()[DmtxSymbolSquareCount];
    } else if (dec.getSizeIdxExpected() == SymbolSize.RectAuto) {
      sizeIdxBeg = SymbolSize.values()[DmtxSymbolSquareCount];
      sizeIdxEnd = SymbolSize.values()[DmtxSymbolSquareCount + DmtxSymbolRectCount];
    } else {
      sizeIdxBeg = dec.getSizeIdxExpected();
      sizeIdxEnd = SymbolSize.values()[dec.getSizeIdxExpected().ordinal() + 1];
    }

    /* Test each barcode size to find best contrast in calibration modules */
    for (sizeIdx = sizeIdxBeg.ordinal(); sizeIdx < sizeIdxEnd.ordinal(); sizeIdx++) {

      symbolRows = SymbolSize.values()[sizeIdx].symbolRows;
      symbolCols = SymbolSize.values()[sizeIdx].symbolCols;
      colorOnAvg = colorOffAvg = 0;

      /* Sum module colors along horizontal calibration bar */
      row = symbolRows - 1;
      for (col = 0; col < symbolCols; col++) {
        color = this.readModuleColor(row, col, SymbolSize.values()[sizeIdx], this.flowBegin.colorPlane);
        if ((col & 0x01) != 0x00)
          colorOffAvg += color;
        else
          colorOnAvg += color;
      }

      /* Sum module colors along vertical calibration bar */
      col = symbolCols - 1;
      for (row = 0; row < symbolRows; row++) {
        color = this.readModuleColor(row, col, SymbolSize.values()[sizeIdx], this.flowBegin.colorPlane);
        if ((row & 0x01) != 0x00)
          colorOffAvg += color;
        else
          colorOnAvg += color;
      }

      colorOnAvg = colorOnAvg * 2 / (symbolRows + symbolCols);
      colorOffAvg = colorOffAvg * 2 / (symbolRows + symbolCols);

      contrast = Math.abs(colorOnAvg - colorOffAvg);
      if (contrast < 20)
        continue;

      if (contrast > bestContrast) {
        bestContrast = contrast;
        bestSizeIdx = sizeIdx;
        bestColorOnAvg = colorOnAvg;
        bestColorOffAvg = colorOffAvg;
      }
    }

    /* If no sizes produced acceptable contrast then call it quits */
    if (bestSizeIdx == LibDMTX.DmtxUndefined || bestContrast < 20)
      return false;

    this.sizeIdx = SymbolSize.values()[bestSizeIdx];
    this.onColor = bestColorOnAvg;
    this.offColor = bestColorOffAvg;

    this.symbolRows = this.sizeIdx.symbolRows;
    this.symbolCols = this.sizeIdx.symbolCols;
    this.mappingRows = this.sizeIdx.mappingMatrixRows;
    this.mappingCols = this.sizeIdx.mappingMatrixCols;

    /* Tally jumps on horizontal calibration bar to verify sizeIdx */
    jumpCount = countJumpTally(0, this.symbolRows - 1, Direction.E);
    errors = Math.abs(1 + jumpCount - this.symbolCols);
    if (jumpCount < 0 || errors > 2)
      return false;

    /* Tally jumps on vertical calibration bar to verify sizeIdx */
    jumpCount = countJumpTally(this.symbolCols - 1, 0, Direction.N);
    errors = Math.abs(1 + jumpCount - this.symbolRows);
    if (jumpCount < 0 || errors > 2)
      return false;

    /* Tally jumps on horizontal finder bar to verify sizeIdx */
    errors = countJumpTally(0, 0, Direction.E);
    if (jumpCount < 0 || errors > 2)
      return false;

    /* Tally jumps on vertical finder bar to verify sizeIdx */
    errors = countJumpTally(0, 0, Direction.N);
    if (errors < 0 || errors > 2)
      return false;

    /* Tally jumps on surrounding whitespace, else fail */
    errors = countJumpTally(0, -1, Direction.E);
    if (errors < 0 || errors > 2)
      return false;

    errors = countJumpTally(-1, 0, Direction.N);
    if (errors < 0 || errors > 2)
      return false;

    errors = countJumpTally(0, this.symbolRows, Direction.E);
    if (errors < 0 || errors > 2)
      return false;

    errors = countJumpTally(this.symbolCols, 0, Direction.N);
    if (errors < 0 || errors > 2)
      return false;

    return true;
  }

  /**
   * \brief Count the number of number of transitions between light and dark \param img \param reg
   * \param xStart \param yStart \param dir \return Jump count
   */
  private int countJumpTally(int xStart, int yStart, Region.Direction dir) {
    int x, xInc = 0;
    int y, yInc = 0;
    int state = Region.DmtxModuleOn;
    int jumpCount = 0;
    int jumpThreshold;
    int tModule, tPrev;
    boolean darkOnLight;
    int color;

    assert xStart == 0 || yStart == 0;
    assert dir == Direction.E || dir == Direction.N;

    if (dir == Direction.E)
      xInc = 1;
    else
      yInc = 1;

    if (xStart == -1 || xStart == this.symbolCols || yStart == -1 || yStart == this.symbolRows)
      state = Region.DmtxModuleOff;

    darkOnLight = this.offColor > this.onColor;
    jumpThreshold = Math.abs((int) (0.4 * (this.onColor - this.offColor) + 0.5));
    color = this.readModuleColor(yStart, xStart, this.sizeIdx, this.flowBegin.colorPlane);
    tModule = darkOnLight ? this.offColor - color : color - this.offColor;

    for (x = xStart + xInc, y = yStart + yInc; dir == Direction.E && x < this.symbolCols || dir == Direction.N
        && y < this.symbolRows; x += xInc, y += yInc) {

      tPrev = tModule;
      color = this.readModuleColor(y, x, this.sizeIdx, this.flowBegin.colorPlane);
      tModule = darkOnLight ? this.offColor - color : color - this.offColor;

      if (state == Region.DmtxModuleOff) {
        if (tModule > tPrev + jumpThreshold) {
          jumpCount++;
          state = Region.DmtxModuleOn;
        }
      } else if (tModule < tPrev - jumpThreshold) {
        jumpCount++;
        state = Region.DmtxModuleOff;
      }
    }

    return jumpCount;
  }

  /**
   *
   *
   */
  private Region.Follow followSeek(int seek) {
    int i;
    int sign;
    Region.Follow follow = new Follow(this.flowBegin.position);

    sign = seek > 0 ? +1 : -1;
    for (i = 0; i != seek; i += sign) {
      follow.followStep(sign);
      assert abs(follow.step) <= this.stepsTotal;
    }

    return follow;
  }

  /**
   *
   *
   */
  private Follow followSeekLoc(Point loc) {
    return new Follow(loc);
  }

  // vauuuddd
  // --------
  // 0x80 v = visited bit
  // 0x40 a = assigned bit
  // 0x38 u = 3 bits points upstream 0-7
  // 0x07 d = 3 bits points downstream 0-7
  private boolean followEdge(PointFlow flowBegin, int maxDiagonal) {
    Point boundMin = flowBegin.position.clone();
    Point boundMax = flowBegin.position.clone();
    dec.getFlagGrid().setTo(flowBegin.position, 0x80 | 0x40); /*
                                                               * Mark location as visited and
                                                               * assigned
                                                               */

    this.flowBegin = flowBegin;

    int negAssigns = 0;
    int posAssigns = 0;
    for (int sign = 1; sign >= -1; sign -= 2) {
      PointFlow flow = flowBegin;

      int steps;
      for (steps = 0;; steps++) {
        if (maxDiagonal != LibDMTX.DmtxUndefined
            && (boundMax.x - boundMin.x > maxDiagonal || boundMax.y - boundMin.y > maxDiagonal))
          break;

        /* Find the strongest eligible neighbor */
        PointFlow flowNext = flow.findStrongestNeighbor(sign);
        // System.out.printf("sign: %d, steps: %d, flowNext: %d,%d,%d,%d,%d/%d, flow: %d,%d,%d,%d,%d/%d\n",
        // sign, steps,
        // flowNext.arrive, flowNext.depart, flowNext.mag, flowNext.plane, flowNext.loc.X,
        // flowNext.loc.Y,
        // flow.arrive, flow.depart, flow.mag, flow.plane, flow.loc.X, flow.loc.Y);
        if (flowNext.magnitude < 50)
          break;

        if (diag.isMarkupEnabled())
          diag.addTransientPoint(sign > 0 ? Feature.START : Feature.STOP, flowNext.position.x, dec.yMax
              - flowNext.position.y);

        /* Get the neighbor's cache location */
        assert !dec.getFlagGrid().isAnySet(flowNext.position, 0x80);

        /*
         * Mark departure from current location. If flowing downstream (sign < 0) then departure
         * vector here is the arrival vector of the next location. Upstream flow uses the opposite
         * rule.
         */
        dec.getFlagGrid().set(flow.position, sign < 0 ? flowNext.arrive.ordinal() : flowNext.arrive.ordinal() << 3);

        /* Mark known direction for next location */
        /*
         * If testing downstream (sign < 0) then next upstream is opposite of next arrival
         */
        /*
         * If testing upstream (sign > 0) then next downstream is opposite of next arrival
         */
        dec.getFlagGrid().setTo(flowNext.position,
            sign < 0 ? flowNext.arrive.opposite().ordinal() << 3 : flowNext.arrive.opposite().ordinal());
        dec.getFlagGrid().set(flowNext.position, 0x80 | 0x40); // Mark location

        // as visited
        // and assigned

        if (sign > 0)
          posAssigns++;
        else
          negAssigns++;
        flow = flowNext;

        if (flow.position.x > boundMax.x)
          boundMax.x = flow.position.x;
        else if (flow.position.x < boundMin.x)
          boundMin.x = flow.position.x;
        if (flow.position.y > boundMax.y)
          boundMax.y = flow.position.y;
        else if (flow.position.y < boundMin.y)
          boundMin.y = flow.position.y;

        /* CALLBACK_POINT_PLOT(flow.loc, (sign > 0) ? 2 : 3, 1, 2); */
      }

      if (sign > 0) {
        this.finalPos = flow.position.clone();
        this.jumpToNeg = steps;
      } else {
        this.finalNeg = flow.position.clone();
        this.jumpToPos = steps;
      }
    }
    this.stepsTotal = this.jumpToPos + this.jumpToNeg;
    this.boundMin = boundMin;
    this.boundMax = boundMax;

    /* Clear "visited" bit from trail */
    int clears = TrailClear(0x80);
    assert posAssigns + negAssigns == clears - 1;

    /* XXX clean this up ... redundant test above */
    if (maxDiagonal != LibDMTX.DmtxUndefined
        && (boundMax.x - boundMin.x > maxDiagonal || boundMax.y - boundMin.y > maxDiagonal))
      return false;

    return true;
  }

  /**
   * recives bresline, and follows strongest neighbor unless it involves ratcheting bresline inward
   * or backward (although back + outward is allowed).
   * 
   */
  private int trailBlazeGapped(BresenhamLine line, int streamDir) {
    final int travel[] = new int[1], outward[] = new int[1];
    final int dirMap[] = {
        0, 1, 2, 7, 8, 3, 6, 5, 4
    };

    Point loc0 = line.loc.clone();
    PointFlow flow = getPointFlow(this.flowBegin.colorPlane, loc0, NEIGHTBOR_NONE);
    int distSqMax = line.xDelta * line.xDelta + line.yDelta * line.yDelta;
    int steps = 0;
    boolean onEdge = true;

    Point beforeStep = loc0;
    if (!dec.getFlagGrid().isValid(loc0))
      return 0;

    dec.getFlagGrid().reset(loc0); /* probably should just overwrite one direction */

    int distSq;
    do {
      if (onEdge) {
        PointFlow flowNext = flow.findStrongestNeighbor(streamDir);
        if (flowNext.magnitude == LibDMTX.DmtxUndefined)
          break;

        line.clone().getStep(flowNext.position, travel, outward);
        if (flowNext.magnitude < 50 || outward[0] < 0 || outward[0] == 0 && travel[0] < 0)
          onEdge = false;
        else {
          line.step(travel[0], outward[0]);
          flow = flowNext.clone();
        }
      }

      if (onEdge == false) {
        line.step(1, 0);
        flow = getPointFlow(this.flowBegin.colorPlane, line.loc, NEIGHTBOR_NONE);
        if (flow.magnitude > 50)
          onEdge = true;
      }

      Point afterStep = line.loc;
      if (!dec.getFlagGrid().isValid(afterStep))
        break;

      /* Determine step direction using pure magic */
      int xStep = afterStep.x - beforeStep.x;
      int yStep = afterStep.y - beforeStep.y;
      assert abs(xStep) <= 1 && abs(yStep) <= 1;
      int stepDir = dirMap[3 * yStep + xStep + 4];
      assert stepDir != 8;

      if (streamDir < 0) {
        dec.getFlagGrid().set(beforeStep, 0x40 | stepDir);
        dec.getFlagGrid().setTo(afterStep, (stepDir + 4) % 8 << 3);
      } else {
        dec.getFlagGrid().set(beforeStep, 0x40 | stepDir << 3);
        dec.getFlagGrid().setTo(afterStep, (stepDir + 4) % 8);
      }

      /* Guaranteed to have taken one step since top of loop */
      int xDiff = line.loc.x - loc0.x;
      int yDiff = line.loc.y - loc0.y;
      distSq = xDiff * xDiff + yDiff * yDiff;

      beforeStep = line.loc.clone();
      steps++;

      // System.out.printf("TPG: dsq: %d, dsqm: %d diff: %d/%d\n", distSq, distSqMax, xDiff,
      // yDiff);
    } while (distSq < distSqMax);

    return steps;
  }

  /**
   *
   *
   */
  private int TrailClear(int clearMask) {
    int clears;
    Region.Follow follow;

    assert (clearMask | 0xff) == 0xff;

    /* Clear "visited" bit from trail */
    clears = 0;
    follow = followSeek(0);
    while (abs(follow.step) <= this.stepsTotal) {
      assert dec.getFlagGrid().isAnySet(follow.loc, clearMask);
      dec.getFlagGrid().clear(follow.loc, clearMask);
      follow.followStep(+1);
      clears++;
    }

    return clears;
  }

  /**
   *
   *
   */
  private BestLine findBestSolidLine(int step0, int step1, int streamDir, int houghAvoid) {
    final int hough[][] = new int[3][DMTX_HOUGH_RES];
    int houghMin, houghMax;
    final byte houghTest[] = new byte[DMTX_HOUGH_RES];
    int i;
    int step;
    int sign = 0;
    int tripSteps = 0;
    int angleBest;
    int hOffset, hOffsetBest;
    int xDiff, yDiff;
    int dH;
    Region.Follow follow;
    final BestLine line = new BestLine();
    Point rHp;

    angleBest = 0;
    hOffset = hOffsetBest = 0;

    /* Always follow path flowing away from the trail start */
    if (step0 != 0) {
      if (step0 > 0) {
        sign = +1;
        tripSteps = (step1 - step0 + this.stepsTotal) % this.stepsTotal;
      } else {
        sign = -1;
        tripSteps = (step0 - step1 + this.stepsTotal) % this.stepsTotal;
      }
      if (tripSteps == 0)
        tripSteps = this.stepsTotal;
    } else if (step1 != 0) {
      sign = step1 > 0 ? +1 : -1;
      tripSteps = Math.abs(step1);
    } else if (step1 == 0) {
      sign = +1;
      tripSteps = this.stepsTotal;
    }
    assert sign == streamDir;

    follow = followSeek(step0);
    rHp = follow.loc.clone();

    line.stepBeg = line.stepPos = line.stepNeg = step0;
    line.locBeg = follow.loc.clone();
    line.locPos = follow.loc.clone();
    line.locNeg = follow.loc.clone();

    /* Predetermine which angles to test */
    for (i = 0; i < DMTX_HOUGH_RES; i++)
      if (houghAvoid == LibDMTX.DmtxUndefined)
        houghTest[i] = 1;
      else {
        houghMin = (houghAvoid + DMTX_HOUGH_RES / 6) % DMTX_HOUGH_RES;
        houghMax = (houghAvoid - DMTX_HOUGH_RES / 6 + DMTX_HOUGH_RES) % DMTX_HOUGH_RES;
        if (houghMin > houghMax)
          houghTest[i] = (byte) (i > houghMin || i < houghMax ? 1 : 0);
        else
          houghTest[i] = (byte) (i > houghMin && i < houghMax ? 1 : 0);
      }

    /* Test each angle for steps along path */
    for (step = 0; step < tripSteps; step++) {

      xDiff = follow.loc.x - rHp.x;
      yDiff = follow.loc.y - rHp.y;

      /* Increment Hough accumulator */
      for (i = 0; i < DMTX_HOUGH_RES; i++) {

        if (houghTest[i] == 0)
          continue;

        dH = rHvX[i] * yDiff - rHvY[i] * xDiff;
        if (dH >= -384 && dH <= 384) {

          if (dH > 128)
            hOffset = 2;
          else if (dH >= -128)
            hOffset = 1;
          else
            hOffset = 0;

          hough[hOffset][i]++;

          /* New angle takes over lead */
          if (hough[hOffset][i] > hough[hOffsetBest][angleBest]) {
            angleBest = i;
            hOffsetBest = hOffset;
          }
        }
      }

      /* CALLBACK_POINT_PLOT(follow.loc, (sign > 1) ? 4 : 3, 1, 2); */

      follow.followStep(sign);
    }

    line.angle = angleBest;
    line.hOffset = hOffsetBest;
    line.mag = hough[hOffsetBest][angleBest];

    return line;
  }

  /**
   *
   *
   */
  private BestLine findBestSolidLine2(Point loc0, int tripSteps, int sign, int houghAvoid) {
    final int hough[][] = new int[3][DMTX_HOUGH_RES];
    int houghMin, houghMax;
    final byte houghTest[] = new byte[DMTX_HOUGH_RES];
    int i;
    int step;
    int angleBest;
    int hOffset, hOffsetBest;
    int xDiff, yDiff;
    int dH;
    new Ray2();
    final BestLine line = new BestLine();
    Point rHp;
    Region.Follow follow;

    angleBest = 0;
    hOffset = hOffsetBest = 0;

    follow = followSeekLoc(loc0);
    rHp = line.locBeg = line.locPos = line.locNeg = follow.loc.clone();
    line.stepBeg = line.stepPos = line.stepNeg = 0;

    /* Predetermine which angles to test */
    for (i = 0; i < DMTX_HOUGH_RES; i++)
      if (houghAvoid == LibDMTX.DmtxUndefined)
        houghTest[i] = 1;
      else {
        houghMin = (houghAvoid + DMTX_HOUGH_RES / 6) % DMTX_HOUGH_RES;
        houghMax = (houghAvoid - DMTX_HOUGH_RES / 6 + DMTX_HOUGH_RES) % DMTX_HOUGH_RES;
        if (houghMin > houghMax)
          houghTest[i] = (byte) (i > houghMin || i < houghMax ? 1 : 0);
        else
          houghTest[i] = (byte) (i > houghMin && i < houghMax ? 1 : 0);
      }

    /* Test each angle for steps along path */
    for (step = 0; step < tripSteps; step++) {

      xDiff = follow.loc.x - rHp.x;
      yDiff = follow.loc.y - rHp.y;

      /* Increment Hough accumulator */
      for (i = 0; i < DMTX_HOUGH_RES; i++) {

        if (houghTest[i] == 0)
          continue;

        dH = rHvX[i] * yDiff - rHvY[i] * xDiff;
        if (dH >= -384 && dH <= 384) {
          if (dH > 128)
            hOffset = 2;
          else if (dH >= -128)
            hOffset = 1;
          else
            hOffset = 0;

          hough[hOffset][i]++;

          /* New angle takes over lead */
          if (hough[hOffset][i] > hough[hOffsetBest][angleBest]) {
            angleBest = i;
            hOffsetBest = hOffset;
          }
        }
      }

      /* CALLBACK_POINT_PLOT(follow.loc, (sign > 1) ? 4 : 3, 1, 2); */

      follow.followStep2(sign);
    }

    line.angle = angleBest;
    line.hOffset = hOffsetBest;
    line.mag = hough[hOffsetBest][angleBest];

    return line;
  }

  /**
   *
   *
   */
  private void findTravelLimits(BestLine line) {
    int i;
    long distSq;
    long distSqMax;
    int xDiff, yDiff;
    boolean posRunning, negRunning;
    int posTravel, negTravel;
    int posWander, posWanderMin, posWanderMax, posWanderMinLock, posWanderMaxLock;
    int negWander, negWanderMin, negWanderMax, negWanderMinLock, negWanderMaxLock;

    /* line.stepBeg is already known to sit on the best Hough line */
    Follow followNeg = followSeek(line.stepBeg);
    Follow followPos = followNeg.clone();
    final Point loc0 = followPos.loc.clone();

    final int cosAngle = rHvX[line.angle];
    final int sinAngle = rHvY[line.angle];

    distSqMax = 0;
    Point negMax = followPos.loc.clone();
    Point posMax = followPos.loc.clone();

    posTravel = negTravel = 0;
    posWander = posWanderMin = posWanderMax = posWanderMinLock = posWanderMaxLock = 0;
    negWander = negWanderMin = negWanderMax = negWanderMinLock = negWanderMaxLock = 0;

    for (i = 0; i < this.stepsTotal / 2; i++) {

      posRunning = i < 10 || abs(posWander) < abs(posTravel);
      negRunning = i < 10 || abs(negWander) < abs(negTravel);

      if (posRunning) {
        xDiff = followPos.loc.x - loc0.x;
        yDiff = followPos.loc.y - loc0.y;
        posTravel = cosAngle * xDiff + sinAngle * yDiff;
        posWander = cosAngle * yDiff - sinAngle * xDiff;

        if (posWander >= -3 * 256 && posWander <= 3 * 256) {
          distSq = followPos.loc.distanceSquared(negMax);
          if (distSq > distSqMax) {
            posMax = followPos.loc.clone();
            distSqMax = distSq;
            line.stepPos = followPos.step;
            line.locPos = followPos.loc.clone();
            posWanderMinLock = posWanderMin;
            posWanderMaxLock = posWanderMax;
          }
        } else {
          posWanderMin = min(posWanderMin, posWander);
          posWanderMax = max(posWanderMax, posWander);
        }
      } else if (!negRunning)
        break;

      if (negRunning) {
        xDiff = followNeg.loc.x - loc0.x;
        yDiff = followNeg.loc.y - loc0.y;
        negTravel = cosAngle * xDiff + sinAngle * yDiff;
        negWander = cosAngle * yDiff - sinAngle * xDiff;

        if (negWander >= -3 * 256 && negWander < 3 * 256) {
          distSq = followNeg.loc.distanceSquared(posMax);
          if (distSq > distSqMax) {
            negMax = followNeg.loc.clone();
            distSqMax = distSq;
            line.stepNeg = followNeg.step;
            line.locNeg = followNeg.loc.clone();
            negWanderMinLock = negWanderMin;
            negWanderMaxLock = negWanderMax;
          }
        } else {
          negWanderMin = min(negWanderMin, negWander);
          negWanderMax = max(negWanderMax, negWander);
        }
      } else if (!posRunning)
        break;

      /*
       * CALLBACK_POINT_PLOT(followPos.loc, 2, 1, 2); CALLBACK_POINT_PLOT(followNeg.loc, 4, 1, 2);
       */

      followPos.followStep(+1);
      followNeg.followStep(-1);
    }
    line.devn = max(posWanderMaxLock - posWanderMinLock, negWanderMaxLock - negWanderMinLock) / 256;
    line.distSq = distSqMax;

    /*
     * CALLBACK_POINT_PLOT(posMax, 2, 1, 1); CALLBACK_POINT_PLOT(negMax, 2, 1, 1);
     */
  }

  /**
   *
   *
   */
  private void matrixRegionAlignCalibEdge(Region.Edge edgeLoc) {
    int streamDir;
    int steps;
    int avoidAngle;
    SymbolSize symbolShape;
    Point loc0;
    final Point loc1 = new Point(), locOrigin = new Point();
    BresenhamLine line;
    Region.Follow follow;
    BestLine bestLine;

    /* Determine pixel coordinates of origin */
    final Vector2 pTmp = new Vector2();
    pTmp.multiply(this.fit2raw); // FIXME: huh? can only be zero.

    locOrigin.x = (int) (pTmp.x + 0.5);
    locOrigin.y = (int) (pTmp.y + 0.5);

    if (dec.getSizeIdxExpected() == SymbolSize.SquareAuto
        || dec.getSizeIdxExpected().ordinal() >= SymbolSize.Fixed10x10.ordinal()
        && dec.getSizeIdxExpected().ordinal() <= SymbolSize.Fixed144x144.ordinal())
      symbolShape = SymbolSize.SquareAuto;
    else if (dec.getSizeIdxExpected() == SymbolSize.RectAuto
        || dec.getSizeIdxExpected().ordinal() >= SymbolSize.Fixed8x18.ordinal()
        && dec.getSizeIdxExpected().ordinal() <= SymbolSize.Fixed16x48.ordinal())
      symbolShape = SymbolSize.RectAuto;
    else
      symbolShape = SymbolSize.ShapeAuto;

    /* Determine end locations of test line */
    if (edgeLoc == Edge.DmtxEdgeTop) {
      streamDir = this.polarity * -1;
      avoidAngle = this.leftLine.angle;
      follow = followSeekLoc(this.locT);
      pTmp.x = 0.8;
      pTmp.y = symbolShape == SymbolSize.RectAuto ? 0.2 : 0.6;
    } else {
      assert edgeLoc == Edge.DmtxEdgeRight;
      streamDir = this.polarity;
      avoidAngle = this.bottomLine.angle;
      follow = followSeekLoc(this.locR);
      pTmp.x = symbolShape == SymbolSize.SquareAuto ? 0.7 : 0.9;
      pTmp.y = 0.8;
    }

    pTmp.multiply(this.fit2raw);
    loc1.x = (int) (pTmp.x + 0.5);
    loc1.y = (int) (pTmp.y + 0.5);

    loc0 = follow.loc.clone();
    line = new BresenhamLine(loc0, loc1, locOrigin);
    steps = trailBlazeGapped(line.clone(), streamDir);

    bestLine = findBestSolidLine2(loc0, steps, streamDir, avoidAngle);
    if (bestLine.mag < 5)
      ;

    if (edgeLoc == Edge.DmtxEdgeTop) {
      this.topKnown = 1;
      this.topAngle = bestLine.angle;
      this.topLoc = bestLine.locBeg;
    } else {
      this.rightKnown = 1;
      this.rightAngle = bestLine.angle;
      this.rightLoc = bestLine.locBeg;
    }
  }

  /**
   * \brief Convert fitted Data Mosaic region into a decoded message \param dec \param reg \param
   * fix \return Decoded message
   */
  public Message decodeMosaicRegion(int fix) {
    final int colorPlane = flowBegin.colorPlane;

    /**
     * Consider performing a color cube fit here to identify exact RGB of all 6 "cube-like" corners
     * based on pixels located within region. Then force each sample pixel to the "cube-like" corner
     * based o which one is nearest "sqrt(dr^2+dg^2+db^2)" (except sqrt is unnecessary). colorPlane
     * = reg.flowBegin.plane;
     * 
     * To find RGB values of primary colors, perform something like a histogram except instead of
     * going from black to color N, go from (127,127,127) to color. Use color bins along with
     * distance to identify value. An additional method will be required to get actual RGB instead
     * of just a plane in 3D.
     */

    flowBegin.colorPlane = 0; /* kind of a hack */
    final Message rMsg = this.decodeMatrixRegion(fix);

    flowBegin.colorPlane = 1; /* kind of a hack */
    final Message gMsg = this.decodeMatrixRegion(fix);

    flowBegin.colorPlane = 2; /* kind of a hack */
    final Message bMsg = this.decodeMatrixRegion(fix);

    flowBegin.colorPlane = colorPlane;

    final Message oMsg = new Message(sizeIdx, Message.Format.DmtxFormatMosaic);

    if (oMsg == null || rMsg == null || gMsg == null || bMsg == null)
      return null;

    int offset = 0;
    System.arraycopy(rMsg.output, 0, oMsg.output, offset, rMsg.outputIdx);
    offset += rMsg.outputIdx;
    System.arraycopy(gMsg.output, 0, oMsg.output, offset, gMsg.outputIdx);
    offset += gMsg.outputIdx;
    System.arraycopy(bMsg.output, 0, oMsg.output, offset, bMsg.outputIdx);
    offset += bMsg.outputIdx;

    oMsg.outputIdx = offset;

    return oMsg;
  }

  /**
   * \brief Convert fitted Data Matrix region into a decoded message \param dec \param reg \param
   * fix \return Decoded message
   */
  public Message decodeMatrixRegion(int fix) {
    final Message msg = new Message(sizeIdx, Message.Format.DmtxFormatMatrix);

    populateArrayFromMatrix(msg);

    /*
     * maybe place remaining logic into new dmtxDecodePopulatedArray() function so other people can
     * pass in their own arrays
     */
    PlaceMod.modulePlacementEcc200(msg.array, msg.code, sizeIdx, Region.DmtxModuleOnRed | Region.DmtxModuleOnGreen
        | Region.DmtxModuleOnBlue);

    if (!ReedSolomon.decode(msg.code, sizeIdx, fix))
      return null;

    final Vector2 topLeft = new Vector2(), topRight = new Vector2(), bottomLeft = new Vector2(), bottomRight = new Vector2();
    topLeft.x = bottomLeft.x = topLeft.y = topRight.y = -0.1;
    topRight.x = bottomRight.x = bottomLeft.y = bottomRight.y = 1.1;

    topLeft.multiply(fit2raw);
    topRight.multiply(fit2raw);
    bottomLeft.multiply(fit2raw);
    bottomRight.multiply(fit2raw);

    final Point pxTopLeft = new Point(), pxTopRight = new Point(), pxBottomLeft = new Point(), pxBottomRight = new Point();
    pxTopLeft.x = (int) (0.5 + topLeft.x);
    pxTopLeft.y = (int) (0.5 + topLeft.y);
    pxBottomLeft.x = (int) (0.5 + bottomLeft.x);
    pxBottomLeft.y = (int) (0.5 + bottomLeft.y);
    pxTopRight.x = (int) (0.5 + topRight.x);
    pxTopRight.y = (int) (0.5 + topRight.y);
    pxBottomRight.x = (int) (0.5 + bottomRight.x);
    pxBottomRight.y = (int) (0.5 + bottomRight.y);

    dec.cacheFillQuad(pxTopLeft, pxTopRight, pxBottomRight, pxBottomLeft);

    new Codec(msg).DecodeDataStream(sizeIdx, null);

    return msg;
  }

  /**
   * \brief Populate array with codeword values based on module colors \param msg \param img \param
   * reg \return DmtxPass | DmtxFail
   */
  private void populateArrayFromMatrix(Message msg) {
    final int tally[][] = new int[24][24];

    /*
     * Large enough to map largest single region
     */

    /* memset(msg.array, 0x00, msg.arraySize); */

    /* Capture number of regions present in barcode */
    final int xRegionTotal = sizeIdx.horizDataRegions;
    final int yRegionTotal = sizeIdx.vertDataRegions;

    /* Capture region dimensions (not including border modules) */
    final int mapWidth = sizeIdx.regionCols;
    final int mapHeight = sizeIdx.regionRows;

    final int weightFactor = 2 * (mapHeight + mapWidth + 2);
    assert (weightFactor > 0);

    /* Tally module changes for each region in each direction */
    for (int yRegionCount = 0; yRegionCount < yRegionTotal; yRegionCount++) {

      /* Y location of mapping region origin in symbol coordinates */
      final int yOrigin = yRegionCount * (mapHeight + 2) + 1;

      for (int xRegionCount = 0; xRegionCount < xRegionTotal; xRegionCount++) {

        /* X location of mapping region origin in symbol coordinates */
        final int xOrigin = xRegionCount * (mapWidth + 2) + 1;

        for (int i = 0; i < tally.length; i++)
          tally[i] = new int[24];
        tallyModuleJumps(tally, xOrigin, yOrigin, mapWidth, mapHeight, Direction.N);
        tallyModuleJumps(tally, xOrigin, yOrigin, mapWidth, mapHeight, Direction.W);
        tallyModuleJumps(tally, xOrigin, yOrigin, mapWidth, mapHeight, Direction.S);
        tallyModuleJumps(tally, xOrigin, yOrigin, mapWidth, mapHeight, Direction.E);

        int mapCol;
        /* Decide module status based on final tallies */
        for (int mapRow = 0; mapRow < mapHeight; mapRow++)
          for (mapCol = 0; mapCol < mapWidth; mapCol++) {

            int rowTmp = (yRegionCount * mapHeight) + mapRow;
            rowTmp = yRegionTotal * mapHeight - rowTmp - 1;
            final int colTmp = (xRegionCount * mapWidth) + mapCol;
            final int idx = (rowTmp * xRegionTotal * mapWidth) + colTmp;

            if (tally[mapRow][mapCol] / (double) weightFactor >= 0.5)
              msg.array[idx] = Region.DmtxModuleOnRGB;
            else
              msg.array[idx] = Region.DmtxModuleOff;

            msg.array[idx] |= Region.DmtxModuleAssigned;
          }
      }
    }
  }

  /**
   *
   *
   */
  private PointFlow detectEdge(Point loc) {
    final int channelCount = 1; // FIXME dec.getImage().getChannelCount();

    /* Find whether red, green, or blue shows the strongest edge */
    PointFlow flow = blankEdge;
    for (int channel = 0; channel < channelCount; channel++) {
      PointFlow f = getPointFlow(channel, loc, Region.NEIGHTBOR_NONE);
      if (f.magnitude > flow.magnitude)
        flow = f;
    }

    if (flow.magnitude < 10)
      return blankEdge;

    final PointFlow flowPos = flow.findStrongestNeighbor(+1);
    final PointFlow flowNeg = flow.findStrongestNeighbor(-1);
    if (flowPos.magnitude != 0 && flowNeg.magnitude != 0) {
      final PointFlow flowPosBack = flowPos.findStrongestNeighbor(-1);
      final PointFlow flowNegBack = flowNeg.findStrongestNeighbor(+1);
      if (flowPos.arrive == flowPosBack.arrive.opposite() && flowNeg.arrive == flowNegBack.arrive.opposite()) {
        flow.arrive = Region.NEIGHTBOR_NONE;
        Region.CALLBACK_POINT_PLOT(flow.position, 1, 1, 1);
        return flow;
      }
    }

    return blankEdge;
  }

  Shape getShape() {
    // merge this code with the one from decodeMatrixRegion?
    final Vector2 topLeft = new Vector2(), topRight = new Vector2(), bottomLeft = new Vector2(), bottomRight = new Vector2();
    topLeft.x = bottomLeft.x = topLeft.y = topRight.y = -0.1;
    topRight.x = bottomRight.x = bottomLeft.y = bottomRight.y = 1.1;

    topLeft.multiply(fit2raw);
    topRight.multiply(fit2raw);
    bottomLeft.multiply(fit2raw);
    bottomRight.multiply(fit2raw);

    final Point pxTopLeft = new Point(), pxTopRight = new Point(), pxBottomLeft = new Point(), pxBottomRight = new Point();
    pxTopLeft.x = (int) (0.5 + topLeft.x);
    pxTopLeft.y = (int) (0.5 + topLeft.y);
    pxBottomLeft.x = (int) (0.5 + bottomLeft.x);
    pxBottomLeft.y = (int) (0.5 + bottomLeft.y);
    pxTopRight.x = (int) (0.5 + topRight.x);
    pxTopRight.y = (int) (0.5 + topRight.y);
    pxBottomRight.x = (int) (0.5 + bottomRight.x);
    pxBottomRight.y = (int) (0.5 + bottomRight.y);

    GeneralPath path = new GeneralPath();
    path.moveTo(pxBottomLeft.x, dec.getHeight() - pxBottomLeft.y);
    path.lineTo(pxBottomRight.x, dec.getHeight() - pxBottomRight.y);
    path.lineTo(pxTopRight.x, dec.getHeight() - pxTopRight.y);
    path.lineTo(pxTopLeft.x, dec.getHeight() - pxTopLeft.y);
    path.lineTo(pxBottomLeft.x, dec.getHeight() - pxBottomLeft.y);

    return path;
  }

  double getBottomAngle() {
    return bottomAngle;
  }

  /**
   *
   *
   */
  PointFlow getPointFlow(int colorPlane, Point loc, Direction arrive) {
    // Sample grid at pattern positions:
    // 6 5 4
    // 7 L 3
    // 0 1 2
    final int colorPattern[] = new int[8];
    for (Direction d : Direction.values()) {
      try {
        colorPattern[d.ordinal()] = dec.getPixelValue( //
            loc.x + d.dx, //
            loc.y + d.dy, //
            colorPlane);
      } catch (final OutOfRangeException e) {
        return blankEdge;
      }
    }

    // Calculate this pixel's flow intensity for each direction (-45, 0, 45, 90)
    // @formatter:off
    // Coefficients:
    //         -2 -1  0     -1  0  1      0  1  2      1  2  1
    //         -1  L  1     -2  L  2     -1  L  1      0  L  0
    //          0  1  2     -1  0  1     -2 -1  0     -1 -2 -1
    //
    // Compass:    0            1            2            3
    //
    // 255 255 000
    // 255 XXX 000
    // 255 255 000
    // 
    // @formatter:on
    Direction compassMax = null;
    int magMax = Integer.MIN_VALUE;
    for (int compass = 0; compass < 4; compass++) {
      int mag = 0;

      /* Add portion from each position in the convolution matrix pattern */
      for (int patternIdx = 0; patternIdx < 8; patternIdx++) {
        Direction c = Direction.get(patternIdx - compass + 8);
        if (c.coefficient != 0)
          mag += colorPattern[patternIdx] * c.coefficient;
      }

      /* Identify strongest compass flow */
      if (abs(mag) > abs(magMax)) {
        compassMax = Direction.get(compass);
        magMax = mag;
      }
    }

    // @formatter:off
    // Convert signed compass direction into unique flow directions:
    // 5  3  2
    // 6  X  1
    // 7  8  0
    // @formatter:on
    Direction depart = magMax > 0 ? compassMax.opposite() : compassMax;
    return new PointFlow(colorPlane, arrive, depart, abs(magMax), loc.clone());
  }
}