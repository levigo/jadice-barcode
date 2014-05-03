package com.jadice.barcode.twod.dmtx;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.Arrays;

import com.jadice.barcode.DiagnosticSettings;
import com.jadice.barcode.Options;
import com.jadice.barcode.Marker.Feature;
import com.jadice.barcode.twod.dmtx.Decode.FlagGrid;

/**
 * @struct DmtxRegion
 * @brief DmtxRegion
 */
public class Region {
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

    public static Direction get(int direction) {
      return values()[direction % 8];
    }

    public final int dx;
    public final int dy;
    public final int coefficient;

    private Direction opposite;

    private Direction(int dx, int dy, int coefficient) {
      this.dx = dx;
      this.dy = dy;
      this.coefficient = coefficient;
    }

    public boolean isManhattan() {
      return (ordinal() & 1) != 0;
    }

    public Direction opposite() {
      if (opposite == null)
        opposite = get(ordinal() + 4);
      return opposite;
    }
  }

  private enum Edge {
    DmtxEdgeTop, DmtxEdgeBottom, DmtxEdgeLeft, DmtxEdgeRight
  }

  private static class Line {
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
    public Line clone() {
      final Line l = new Line();
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

  public static class PointFlow {
    static PointFlow getPointFlow(Decode dec, int colorPlane, Point loc, Direction arrive) {
      // Sample grid at pattern positions:
      // 6 5 4
      // 7 L 3
      // 0 1 2
      final int colorPattern[] = new int[8];
      for (final Direction d : Direction.values()) {
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
      // 255  X  000
      // 255 255 000
      // 
      // @formatter:on
      Direction compassMax = null;
      int magMax = Integer.MIN_VALUE;
      for (int compass = 0; compass < 4; compass++) {
        int mag = 0;

        /* Add portion from each position in the convolution matrix pattern */
        for (int patternIdx = 0; patternIdx < 8; patternIdx++) {
          final Direction c = Direction.get(patternIdx - compass + 8);
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
      final Direction depart = magMax > 0 ? compassMax.opposite() : compassMax;
      return new PointFlow(dec, colorPlane, arrive, depart, abs(magMax), loc.clone());
    }

    static PointFlow detectEdge(Decode dec, Point loc) {
      final int channelCount = 1; // FIXME dec.getImage().getChannelCount();

      /* Find whether red, green, or blue shows the strongest edge */
      PointFlow flow = blankEdge;
      for (int channel = 0; channel < channelCount; channel++) {
        final PointFlow f = getPointFlow(dec, channel, loc, Region.NEIGHTBOR_NONE);
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

    static final PointFlow blankEdge = new PointFlow(null, 0, null, null, Integer.MIN_VALUE, new Point());

    int colorPlane;
    Direction arrive;
    final Direction depart;
    final int magnitude;
    final Point position;
    private final Decode dec;

    public PointFlow(Decode dec, int colorPlane, Direction arrive, Direction depart, int mag, Point loc) {
      this.dec = dec;
      this.colorPlane = colorPlane;
      this.arrive = arrive;
      this.depart = depart;
      magnitude = mag;
      position = loc;
    }

    @Override
    public PointFlow clone() {
      return new PointFlow(dec, colorPlane, arrive, depart, magnitude, position.clone());
    }

    /**
     * 
     */
    PointFlow findStrongestNeighbor(int sign) {
      final Point loc = new Point();

      final Direction attempt = sign < 0 ? depart : depart.opposite();

      int occupied = 0;
      PointFlow strongestFlow = blankEdge;
      for (final Direction direction : Direction.values()) {
        loc.x = position.x + direction.dx;
        loc.y = position.y + direction.dy;

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

        final PointFlow flow = getPointFlow(dec, colorPlane, loc, direction);

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

    final DiagnosticSettings diag = options.getOptions(DiagnosticSettings.class);

    final Region reg = new Region(dec, diag);

    try {
      /* Test for presence of any reasonable edge at this location */
      final PointFlow flowBegin = PointFlow.detectEdge(dec, loc);
      if (flowBegin.magnitude < (int) (dec.getEdgeThresh() * 7.65 + 0.5))
        return null;

      /* Determine barcode orientation */
      if (!reg.matrixRegionOrientation(flowBegin)) {
        System.out.println(loc + ": matrixRegionOrientation failed: " + reg);
        return null;
      }
      if (!reg.updateXfrms()) {
        System.out.println(loc + ": updateXFrms failed: " + reg);
        return null;
      }

      /* Define top edge */
      reg.matrixRegionAlignCalibEdge(Region.Edge.DmtxEdgeTop);
      if (!reg.updateXfrms()) {
        System.out.println(loc + ": updateXFrms (2nd) failed: " + reg);
        return null;
      }

      /* Define right edge */
      reg.matrixRegionAlignCalibEdge(Region.Edge.DmtxEdgeRight);
      if (!reg.updateXfrms()) {
        System.out.println(loc + ": updateXFrms (3rd) failed: " + reg);
        return null;
      }

      Region.CALLBACK_MATRIX(reg);

      /* Calculate the best fitting symbol size */
      if (!reg.matrixRegionFindSize()) {
        System.out.println(loc + ": matrixRegionFindSize failed: " + reg);
        return null;
      }

      /* Found a valid matrix region */
      System.out.println(loc + ": Region: " + reg);
      return reg;
    } finally {
      System.out.print("");
    }
  }

  private PointFlow flowBegin; /* */
  /* Orientation values */
  private int polarity; /* */
  private int stepR;
  private int stepT;
  private Point locR; /* remove if stepR works above */

  private Point locT; /* remove if stepT works above */

  /* Region fitting values */
  private boolean leftKnown;
  private int leftAngle; // hough angle of left edge
  private Point leftLoc; // arbitrary point on left edge
  private Line leftLine;

  private boolean bottomKnown;
  private int bottomAngle; // hough angle of bottom edge
  private Point bottomLoc; // arbitrary point on bottom edge

  private Line bottomLine;
  private boolean topKnown;
  private int topAngle; /* hough angle of top edge */
  private Point topLoc; /* known (arbitrary) location on top edge */

  private boolean rightKnown;
  private int rightAngle; /* hough angle of right edge */
  private Point rightLoc; /* known (arbitrary) location on right edge */

  /* Region calibration values */
  private int onColor; /* */

  private int offColor; /* */

  /*
   * Index of arrays that store Data Matrix constants
   */
  private SymbolSize symbolSize;

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

  static void CALLBACK_MATRIX(Region reg) {
    // TODO Auto-generated method stub

  }

  static void CALLBACK_POINT_PLOT(Point loc, int i, int j, int k) {
    // TODO Auto-generated method stub

  }

  private final DiagnosticSettings diag;
  private Trail trail;

  public Region(Decode dec, DiagnosticSettings diag) {
    this.dec = dec;
    this.diag = diag;
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

    if (xStart == -1 || xStart == symbolSize.columns || yStart == -1 || yStart == symbolSize.rows)
      state = Region.DmtxModuleOff;

    darkOnLight = offColor > onColor;
    jumpThreshold = Math.abs((int) (0.4 * (onColor - offColor) + 0.5));
    color = readModuleColor(yStart, xStart, symbolSize, flowBegin.colorPlane);
    tModule = darkOnLight ? offColor - color : color - offColor;

    for (x = xStart + xInc, y = yStart + yInc; dir == Direction.E && x < symbolSize.columns || dir == Direction.N
        && y < symbolSize.rows; x += xInc, y += yInc) {

      tPrev = tModule;
      color = readModuleColor(y, x, symbolSize, flowBegin.colorPlane);
      tModule = darkOnLight ? offColor - color : color - offColor;

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
   * \brief Convert fitted Data Matrix region into a decoded message \param dec \param reg \param
   * fix \return Decoded message
   */
  public Message decodeMatrixRegion(int fix) {
    final Message msg = new Message(symbolSize, Message.Format.DmtxFormatMatrix);

    populateArrayFromMatrix(msg);

    /*
     * maybe place remaining logic into new dmtxDecodePopulatedArray() function so other people can
     * pass in their own arrays
     */
    PlaceMod.modulePlacementEcc200(msg.array, msg.code, symbolSize, Region.DmtxModuleOnRed | Region.DmtxModuleOnGreen
        | Region.DmtxModuleOnBlue);

    if (!ReedSolomon.decode(msg.code, symbolSize, fix))
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

    new Codec(msg).DecodeDataStream(symbolSize, null);

    return msg;
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
    final Message rMsg = decodeMatrixRegion(fix);

    flowBegin.colorPlane = 1; /* kind of a hack */
    final Message gMsg = decodeMatrixRegion(fix);

    flowBegin.colorPlane = 2; /* kind of a hack */
    final Message bMsg = decodeMatrixRegion(fix);

    flowBegin.colorPlane = colorPlane;

    final Message oMsg = new Message(symbolSize, Message.Format.DmtxFormatMosaic);

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

  private static class Trail {
    public class Follow {
      int step;
      Point loc = new Point();

      public Follow(Point loc) {
        this.loc = loc.clone();
        step = 0;
      }

      @Override
      public Follow clone() {
        final Follow f = new Follow(loc);
        f.step = step;
        f.loc = loc.clone();
        return f;
      }

      public void followStep(int sign) {
        assert Math.abs(sign) == 1;
        assert dec.getFlagGrid().isAnySet(loc, 0x40);

        final int factor = length + 1;
        int stepMod;
        if (sign > 0)
          stepMod = (factor + step % factor) % factor;
        else
          stepMod = (factor - step % factor) % factor;

        /* End of positive trail -- magic jump */
        if (sign > 0 && stepMod == positiveTrailLength)
          loc.setTo(negativeTrailEnd);
        else if (sign < 0 && stepMod == negativeTrailLength)
          loc.setTo(positiveTrailEnd);
        else {
          final int flags = dec.getFlagGrid().get(loc);
          final int patternIdx = sign < 0 ? flags & 0x07 : (flags & 0x38) >> 3;
          loc.x += dmtxPatternX[patternIdx];
          loc.y += dmtxPatternY[patternIdx];
        }

        step += sign;
      }

      /**
       *
       *
       */
      public void followStep2(int sign) {
        assert Math.abs(sign) == 1;
        final FlagGrid flags = dec.getFlagGrid();
        assert flags.isAnySet(loc, 0x40);

        final int patternIdx = sign < 0 ? flags.get(loc) & 0x07 : (flags.get(loc) & 0x38) >> 3;
        loc.x += dmtxPatternX[patternIdx];
        loc.y += dmtxPatternY[patternIdx];

        step += sign;
      }
    }

    private final PointFlow start;

    private Point positiveTrailEnd;
    private int positiveTrailLength;
    private Point negativeTrailEnd;
    private int negativeTrailLength;
    private final int length;
    private final Point boundMin;
    private final Point boundMax;
    private final Decode dec;

    // vauuuddd
    // --------
    // 0x80 v = visited bit
    // 0x40 a = assigned bit
    // 0x38 u = 3 bits points upstream 0-7
    // 0x07 d = 3 bits points downstream 0-7
    Trail(Decode dec, PointFlow start, int maxDiagonal, DiagnosticSettings diag) {
      this.dec = dec;
      final Point boundMin = start.position.clone();
      final Point boundMax = start.position.clone();

      // Mark location as visited and assigned
      dec.getFlagGrid().setTo(start.position, 0x80 | 0x40);

      this.start = start;

      int negAssigns = 0;
      int posAssigns = 0;
      for (int sign = 1; sign >= -1; sign -= 2) {
        PointFlow flow = start;

        int steps;
        for (steps = 0;; steps++) {
          if (maxDiagonal != LibDMTX.DmtxUndefined
              && (boundMax.x - boundMin.x > maxDiagonal || boundMax.y - boundMin.y > maxDiagonal))
            break;

          /* Find the strongest eligible neighbor */
          final PointFlow flowNext = flow.findStrongestNeighbor(sign);
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
          this.positiveTrailEnd = flow.position.clone();
          this.positiveTrailLength = steps;
        } else {
          this.negativeTrailEnd = flow.position.clone();
          this.negativeTrailLength = steps;
        }
      }
      this.length = negativeTrailLength + positiveTrailLength;
      this.boundMin = boundMin;
      this.boundMax = boundMax;

      /* Clear "visited" bit from trail */
      final int clears = clear(0x80);
      assert posAssigns + negAssigns == clears - 1;
    }

    /**
     * Start following at an offset of the start position of <code>seek</code> steps in positive or
     * negative direction.
     */
    private Follow followSeek(int seek) {
      final Follow follow = new Follow(start.position);

      int sign = seek > 0 ? +1 : -1;
      for (int i = 0; i != seek; i += sign) {
        follow.followStep(sign);
        assert abs(follow.step) <= length;
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

    /**
     *
     *
     */
    private int clear(int clearMask) {
      assert (clearMask | 0xff) == 0xff;

      /* Clear "visited" bit from trail */
      int clears = 0;
      Follow follow = followSeek(0);
      while (abs(follow.step) <= length) {
        assert dec.getFlagGrid().isAnySet(follow.loc, clearMask);
        dec.getFlagGrid().clear(follow.loc, clearMask);
        follow.followStep(+1);
        clears++;
      }

      return clears;
    }

    public int getLength() {
      return length;
    }

    /**
     *
     *
     */
    private Line findBestSolidLine(int step0, int step1, int streamDir, int houghAvoid) {
      int sign = 0;
      int tripSteps = 0;
      /* Always follow path flowing away from the trail start */
      if (step0 != 0) {
        if (step0 > 0) {
          sign = +1;
          tripSteps = (step1 - step0 + length) % length;
        } else {
          sign = -1;
          tripSteps = (step0 - step1 + length) % length;
        }
        if (tripSteps == 0)
          tripSteps = length;
      } else if (step1 != 0) {
        sign = step1 > 0 ? +1 : -1;
        tripSteps = Math.abs(step1);
      } else if (step1 == 0) {
        sign = +1;
        tripSteps = length;
      }
      assert sign == streamDir;

      int houghMin, houghMax;
      final int hough[][] = new int[3][DMTX_HOUGH_RES];
      final boolean houghTest[] = new boolean[DMTX_HOUGH_RES];

      /* Predetermine which angles to test */
      if (houghAvoid == LibDMTX.DmtxUndefined)
        Arrays.fill(houghTest, true);
      else
        for (int i = 0; i < DMTX_HOUGH_RES; i++) {
          houghMin = (houghAvoid + DMTX_HOUGH_RES / 6) % DMTX_HOUGH_RES;
          houghMax = (houghAvoid - DMTX_HOUGH_RES / 6 + DMTX_HOUGH_RES) % DMTX_HOUGH_RES;
          if (houghMin > houghMax)
            houghTest[i] = i > houghMin || i < houghMax;
          else
            houghTest[i] = i > houghMin && i < houghMax;
        }

      int hOffsetBest = 0;
      int angleBest = 0;

      Follow follow = followSeek(step0);
      final Point startPoint = follow.loc.clone();

      final Line line = new Line();
      line.stepBeg = line.stepPos = line.stepNeg = step0;
      line.locBeg = follow.loc.clone();
      line.locPos = follow.loc.clone();
      line.locNeg = follow.loc.clone();

      /* Test each angle for steps along path */
      int hOffsetOutOfRange = 0;
      for (int step = 0; step < tripSteps; step++) {
        int dx = follow.loc.x - startPoint.x;
        int dy = follow.loc.y - startPoint.y;

        /* Increment Hough accumulator */
        for (int i = 0; i < DMTX_HOUGH_RES; i++) {
          if (!houghTest[i])
            continue;

          int dH = rHvX[i] * dy - rHvY[i] * dx;
          if (dH >= -384 && dH <= 384) {
            int hOffset;
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
          } else
            hOffsetOutOfRange++;
        }

        /* CALLBACK_POINT_PLOT(follow.loc, (sign > 1) ? 4 : 3, 1, 2); */

        follow.followStep(sign);
      }


      System.out.println("Hough: outOfRange=" + hOffsetOutOfRange);
      for (int h[] : hough) {
        System.out.println(Arrays.toString(h));
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
    private Line findBestSolidLine2(Point loc0, int tripSteps, int sign, int houghAvoid) {
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
      final Line line = new Line();
      Point rHp;

      angleBest = 0;
      hOffset = hOffsetBest = 0;

      Follow follow = followSeekLoc(loc0);
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

    private void findTravelLimits(Line line) {
      /* line.stepBeg is already known to sit on the best Hough line */
      final Follow followNeg = followSeek(line.stepBeg);
      final Follow followPos = followNeg.clone();
      final Point loc0 = followPos.loc.clone();

      final int cosAngle = rHvX[line.angle];
      final int sinAngle = rHvY[line.angle];

      long distSqMax = 0;
      Point negMax = followPos.loc.clone();
      Point posMax = followPos.loc.clone();

      int negTravel = 0;
      int posTravel = 0;

      int posWander, posWanderMin, posWanderMax, posWanderMinLock, posWanderMaxLock;
      posWander = posWanderMin = posWanderMax = posWanderMinLock = posWanderMaxLock = 0;
      int negWander, negWanderMin, negWanderMax, negWanderMinLock, negWanderMaxLock;
      negWander = negWanderMin = negWanderMax = negWanderMinLock = negWanderMaxLock = 0;

      for (int i = 0; i < length / 2; i++) {
        boolean posRunning = i < 10 || abs(posWander) < abs(posTravel);
        boolean negRunning = i < 10 || abs(negWander) < abs(negTravel);

        int xDiff;
        int yDiff;
        long distSq;
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

    public int largestExtent() {
      return Math.max(boundMax.x - boundMin.x, boundMax.y - boundMin.y);
    }

    public int area() {
      return (boundMax.x - boundMin.x) * (boundMax.y - boundMin.y);
    }

    @Override
    public String toString() {
      return "Trail [start=" + start + ", positiveTrailEnd=" + positiveTrailEnd + ", positiveTrailLength="
          + positiveTrailLength + ", negativeTrailEnd=" + negativeTrailEnd + ", negativeTrailLength="
          + negativeTrailLength + ", length=" + length + ", boundMin=" + boundMin + ", boundMax=" + boundMax + ", dec="
          + dec + "]";
    }
  }

  double getBottomAngle() {
    return bottomAngle;
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

    final GeneralPath path = new GeneralPath();
    path.moveTo(pxBottomLeft.x, dec.getHeight() - pxBottomLeft.y);
    path.lineTo(pxBottomRight.x, dec.getHeight() - pxBottomRight.y);
    path.lineTo(pxTopRight.x, dec.getHeight() - pxTopRight.y);
    path.lineTo(pxTopLeft.x, dec.getHeight() - pxTopLeft.y);
    path.lineTo(pxBottomLeft.x, dec.getHeight() - pxBottomLeft.y);

    return path;
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
    Trail.Follow follow;
    Line bestLine;

    /* Determine pixel coordinates of origin */
    final Vector2 pTmp = new Vector2();
    pTmp.multiply(fit2raw); // FIXME: huh? can only be zero.

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
      streamDir = polarity * -1;
      avoidAngle = leftLine.angle;
      follow = trail.followSeekLoc(locT);
      pTmp.x = 0.8;
      pTmp.y = symbolShape == SymbolSize.RectAuto ? 0.2 : 0.6;
    } else {
      assert edgeLoc == Edge.DmtxEdgeRight;
      streamDir = polarity;
      avoidAngle = bottomLine.angle;
      follow = trail.followSeekLoc(locR);
      pTmp.x = symbolShape == SymbolSize.SquareAuto ? 0.7 : 0.9;
      pTmp.y = 0.8;
    }

    pTmp.multiply(fit2raw);
    loc1.x = (int) (pTmp.x + 0.5);
    loc1.y = (int) (pTmp.y + 0.5);

    loc0 = follow.loc.clone();
    line = new BresenhamLine(loc0, loc1, locOrigin);
    steps = trailBlazeGapped(line.clone(), streamDir);

    bestLine = trail.findBestSolidLine2(loc0, steps, streamDir, avoidAngle);
    if (bestLine.mag < 5)
      ;

    if (edgeLoc == Edge.DmtxEdgeTop) {
      topKnown = true;
      topAngle = bestLine.angle;
      topLoc = bestLine.locBeg;
    } else {
      rightKnown = true;
      rightAngle = bestLine.angle;
      rightLoc = bestLine.locBeg;
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

      symbolRows = SymbolSize.values()[sizeIdx].rows;
      symbolCols = SymbolSize.values()[sizeIdx].columns;
      colorOnAvg = colorOffAvg = 0;

      /* Sum module colors along horizontal calibration bar */
      row = symbolRows - 1;
      for (col = 0; col < symbolCols; col++) {
        color = readModuleColor(row, col, SymbolSize.values()[sizeIdx], flowBegin.colorPlane);
        if ((col & 0x01) != 0x00)
          colorOffAvg += color;
        else
          colorOnAvg += color;
      }

      /* Sum module colors along vertical calibration bar */
      col = symbolCols - 1;
      for (row = 0; row < symbolRows; row++) {
        color = readModuleColor(row, col, SymbolSize.values()[sizeIdx], flowBegin.colorPlane);
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

    symbolSize = SymbolSize.values()[bestSizeIdx];
    onColor = bestColorOnAvg;
    offColor = bestColorOffAvg;

    /* Tally jumps on horizontal calibration bar to verify sizeIdx */
    jumpCount = countJumpTally(0, symbolSize.rows - 1, Direction.E);
    errors = Math.abs(1 + jumpCount - symbolSize.columns);
    if (jumpCount < 0 || errors > 2)
      return false;

    /* Tally jumps on vertical calibration bar to verify sizeIdx */
    jumpCount = countJumpTally(symbolSize.columns - 1, 0, Direction.N);
    errors = Math.abs(1 + jumpCount - symbolSize.rows);
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

    errors = countJumpTally(0, symbolSize.rows, Direction.E);
    if (errors < 0 || errors > 2)
      return false;

    errors = countJumpTally(symbolSize.columns, 0, Direction.N);
    if (errors < 0 || errors > 2)
      return false;

    return true;
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

    flowBegin = begin.clone();
    trail = new Trail(dec, begin, maxDiagonal, diag);

    /* XXX clean this up ... redundant test above */
    if ((maxDiagonal != LibDMTX.DmtxUndefined && trail.largestExtent() > maxDiagonal) || trail.getLength() < 40) {
      trail.clear(0x40);
      return false;
    }

    /* Filter out region candidates that are smaller than expected */
    if (dec.getEdgeMin() != LibDMTX.DmtxUndefined) {
      final int scale = dec.getScale();

      int minArea;
      if (symbolShape == SymbolSize.SquareAuto)
        minArea = dec.getEdgeMin() * dec.getEdgeMin() / (scale * scale);
      else
        minArea = 2 * dec.getEdgeMin() * dec.getEdgeMin() / (scale * scale);

      if (trail.area() < minArea) {
        trail.clear(0x40);
        return false;
      }
    }

    if (diag.isMarkupEnabled())
      diag.addTransientPoint(Feature.DETECTION_SCAN, begin.position.x, dec.yMax - begin.position.y);

    final Line line1x = trail.findBestSolidLine(0, 0, +1, LibDMTX.DmtxUndefined);
    if (line1x.mag < 5) {
      trail.clear(0x40);
      return false;
    }

    System.out.println("line1x initial: " + line1x);

    trail.findTravelLimits(line1x);
    if (line1x.distSq < 100 || line1x.devn * 10 >= Math.sqrt(line1x.distSq)) {
      trail.clear(0x40);
      return false;
    }
    assert line1x.stepPos >= line1x.stepNeg;

    System.out.println("line1x updated: " + line1x);

    Trail.Follow fTmp = trail.followSeek(line1x.stepPos + 5);
    final Line line2p = trail.findBestSolidLine(fTmp.step, line1x.stepNeg, +1, line1x.angle);

    fTmp = trail.followSeek(line1x.stepNeg - 5);
    final Line line2n = trail.findBestSolidLine(fTmp.step, line1x.stepPos, -1, line1x.angle);
    if (max(line2p.mag, line2n.mag) < 5)
      return false;

    Line line2x;
    int cross;
    if (line2p.mag > line2n.mag) {
      line2x = line2p;
      trail.findTravelLimits(line2x);
      if (line2x.distSq < 100 || line2x.devn * 10 >= Math.sqrt(line2x.distSq))
        return false;

      cross = (line1x.locPos.x - line1x.locNeg.x) * (line2x.locPos.y - line2x.locNeg.y)
          - (line1x.locPos.y - line1x.locNeg.y) * (line2x.locPos.x - line2x.locNeg.x);
      if (cross > 0) {
        /* Condition 2 */
        polarity = +1;
        locR = line2x.locPos;
        stepR = line2x.stepPos;
        locT = line1x.locNeg;
        stepT = line1x.stepNeg;
        leftLoc = line1x.locBeg;
        leftAngle = line1x.angle;
        bottomLoc = line2x.locBeg;
        bottomAngle = line2x.angle;
        leftLine = line1x;
        bottomLine = line2x;
      } else {
        /* Condition 3 */
        polarity = -1;
        locR = line1x.locNeg;
        stepR = line1x.stepNeg;
        locT = line2x.locPos;
        stepT = line2x.stepPos;
        leftLoc = line2x.locBeg;
        leftAngle = line2x.angle;
        bottomLoc = line1x.locBeg;
        bottomAngle = line1x.angle;
        leftLine = line2x;
        bottomLine = line1x;
      }
    } else {
      line2x = line2n;
      trail.findTravelLimits(line2x);
      if (line2x.distSq < 100 || line2x.devn / Math.sqrt(line2x.distSq) >= 0.1)
        return false;

      cross = (line1x.locNeg.x - line1x.locPos.x) * (line2x.locNeg.y - line2x.locPos.y)
          - (line1x.locNeg.y - line1x.locPos.y) * (line2x.locNeg.x - line2x.locPos.x);
      if (cross > 0) {
        /* Condition 1 */
        polarity = -1;
        locR = line2x.locNeg;
        stepR = line2x.stepNeg;
        locT = line1x.locPos;
        stepT = line1x.stepPos;
        leftLoc = line1x.locBeg;
        leftAngle = line1x.angle;
        bottomLoc = line2x.locBeg;
        bottomAngle = line2x.angle;
        leftLine = line1x;
        bottomLine = line2x;
      } else {
        /* Condition 4 */
        polarity = +1;
        locR = line1x.locPos;
        stepR = line1x.stepPos;
        locT = line2x.locNeg;
        stepT = line2x.stepNeg;
        leftLoc = line2x.locBeg;
        leftAngle = line2x.angle;
        bottomLoc = line1x.locBeg;
        bottomAngle = line1x.angle;
        leftLine = line2x;
        bottomLine = line1x;
      }
    }
    /*
     * CALLBACK_POINT_PLOT(reg.locR, 2, 1, 1); CALLBACK_POINT_PLOT(reg.locT, 2, 1, 1);
     */

    leftKnown = bottomKnown = true;

    return true;
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
    final int xRegionTotal = symbolSize.horizDataRegions;
    final int yRegionTotal = symbolSize.vertDataRegions;

    /* Capture region dimensions (not including border modules) */
    final int mapWidth = symbolSize.regionCols;
    final int mapHeight = symbolSize.regionRows;

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

    symbolRows = sizeIdx.rows;
    symbolCols = sizeIdx.columns;

    color = 0;
    for (i = 0; i < 5; i++) {

      p.x = 1.0 / symbolCols * (symbolCol + sampleX[i]);
      p.y = 1.0 / symbolRows * (symbolRow + sampleY[i]);

      p.multiply(fit2raw);

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

    final boolean darkOnLight = (offColor > onColor);
    final int jumpThreshold = abs((int) (0.4 * (offColor - onColor) + 0.5));

    assert (jumpThreshold >= 0);

    for (line[0] = lineStart; line[0] < lineStop; line[0]++) {

      /*
       * Capture tModule for each leading border module as normal but decide status based on
       * predictable barcode border pattern
       */

      travel[0] = travelStart;
      int color = readModuleColor(symbolRow[0], symbolCol[0], symbolSize, flowBegin.colorPlane);
      int tModule = (darkOnLight) ? offColor - color : color - offColor;

      int statusModule = (travelStep == 1 || (line[0] & 0x01) == 0) ? Region.DmtxModuleOnRGB : Region.DmtxModuleOff;

      int weight = extent;

      while ((travel[0] += travelStep) != travelStop) {

        final int tPrev = tModule;
        final int statusPrev = statusModule;

        /*
         * For normal data-bearing modules capture color and decide module status based on
         * comparison to previous "known" module
         */

        color = readModuleColor(symbolRow[0], symbolCol[0], symbolSize, flowBegin.colorPlane);
        tModule = (darkOnLight) ? offColor - color : color - offColor;

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
   * recives bresline, and follows strongest neighbor unless it involves ratcheting bresline inward
   * or backward (although back + outward is allowed).
   * 
   */
  private int trailBlazeGapped(BresenhamLine line, int streamDir) {
    final int travel[] = new int[1], outward[] = new int[1];
    final int dirMap[] = {
        0, 1, 2, 7, 8, 3, 6, 5, 4
    };

    final Point loc0 = line.loc.clone();
    PointFlow flow = PointFlow.getPointFlow(dec, flowBegin.colorPlane, loc0, NEIGHTBOR_NONE);
    final int distSqMax = line.xDelta * line.xDelta + line.yDelta * line.yDelta;
    int steps = 0;
    boolean onEdge = true;

    Point beforeStep = loc0;
    if (!dec.getFlagGrid().isValid(loc0))
      return 0;

    dec.getFlagGrid().reset(loc0); /* probably should just overwrite one direction */

    int distSq;
    do {
      if (onEdge) {
        final PointFlow flowNext = flow.findStrongestNeighbor(streamDir);
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
        flow = PointFlow.getPointFlow(dec, flowBegin.colorPlane, line.loc, NEIGHTBOR_NONE);
        if (flow.magnitude > 50)
          onEdge = true;
      }

      final Point afterStep = line.loc;
      if (!dec.getFlagGrid().isValid(afterStep))
        break;

      /* Determine step direction using pure magic */
      final int xStep = afterStep.x - beforeStep.x;
      final int yStep = afterStep.y - beforeStep.y;
      assert abs(xStep) <= 1 && abs(yStep) <= 1;
      final int stepDir = dirMap[3 * yStep + xStep + 4];
      assert stepDir != 8;

      if (streamDir < 0) {
        dec.getFlagGrid().set(beforeStep, 0x40 | stepDir);
        dec.getFlagGrid().setTo(afterStep, (stepDir + 4) % 8 << 3);
      } else {
        dec.getFlagGrid().set(beforeStep, 0x40 | stepDir << 3);
        dec.getFlagGrid().setTo(afterStep, (stepDir + 4) % 8);
      }

      /* Guaranteed to have taken one step since top of loop */
      final int xDiff = line.loc.x - loc0.x;
      final int yDiff = line.loc.y - loc0.y;
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
    m.multiply(msky, raw2fit);

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
    m.multiply(mtxy, fit2raw);

    return true;
  }

  /**
   *
   *
   */
  private boolean updateXfrms() {
    double radians;
    final Ray2 rLeft = new Ray2(), rBottom = new Ray2(), rTop = new Ray2(), rRight = new Ray2();

    assert leftKnown && bottomKnown;

    /* Build ray representing left edge */
    rLeft.p.x = leftLoc.x;
    rLeft.p.y = leftLoc.y;
    radians = leftAngle * Math.PI / DMTX_HOUGH_RES;
    rLeft.v.x = Math.cos(radians);
    rLeft.v.y = Math.sin(radians);
    rLeft.tMin = 0.0;
    rLeft.tMax = rLeft.v.normalize();

    /* Build ray representing bottom edge */
    rBottom.p.x = bottomLoc.x;
    rBottom.p.y = bottomLoc.y;
    radians = bottomAngle * Math.PI / DMTX_HOUGH_RES;
    rBottom.v.x = Math.cos(radians);
    rBottom.v.y = Math.sin(radians);
    rBottom.tMin = 0.0;
    rBottom.tMax = rBottom.v.normalize();

    /* Build ray representing top edge */
    if (topKnown) {
      rTop.p.x = topLoc.x;
      rTop.p.y = topLoc.y;
      radians = topAngle * Math.PI / DMTX_HOUGH_RES;
      rTop.v.x = Math.cos(radians);
      rTop.v.y = Math.sin(radians);
      rTop.tMin = 0.0;
      rTop.tMax = rTop.v.normalize();
    } else {
      rTop.p.x = locT.x;
      rTop.p.y = locT.y;
      radians = bottomAngle * Math.PI / DMTX_HOUGH_RES;
      rTop.v.x = Math.cos(radians);
      rTop.v.y = Math.sin(radians);
      rTop.tMin = 0.0;
      rTop.tMax = rBottom.tMax;
    }

    /* Build ray representing right edge */
    if (rightKnown) {
      rRight.p.x = rightLoc.x;
      rRight.p.y = rightLoc.y;
      radians = rightAngle * Math.PI / DMTX_HOUGH_RES;
      rRight.v.x = Math.cos(radians);
      rRight.v.y = Math.sin(radians);
      rRight.tMin = 0.0;
      rRight.tMax = rRight.v.normalize();
    } else {
      rRight.p.x = locR.x;
      rRight.p.y = locR.y;
      radians = leftAngle * Math.PI / DMTX_HOUGH_RES;
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

  @Override
  public String toString() {
    return "Region [flowBegin=" + flowBegin + ", polarity=" + polarity + ", stepR=" + stepR + ", stepT=" + stepT
        + ", locR=" + locR + ", locT=" + locT + ", leftKnown=" + leftKnown + ", leftAngle=" + leftAngle + ", leftLoc="
        + leftLoc + ", leftLine=" + leftLine + ", bottomKnown=" + bottomKnown + ", bottomAngle=" + bottomAngle
        + ", bottomLoc=" + bottomLoc + ", bottomLine=" + bottomLine + ", topKnown=" + topKnown + ", topAngle="
        + topAngle + ", topLoc=" + topLoc + ", rightKnown=" + rightKnown + ", rightAngle=" + rightAngle + ", rightLoc="
        + rightLoc + ", onColor=" + onColor + ", offColor=" + offColor + ", symbolSize=" + symbolSize + ", raw2fit="
        + raw2fit + ", fit2raw=" + fit2raw + ", trail=" + trail + "]";
  }
}