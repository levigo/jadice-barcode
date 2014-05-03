package com.jadice.barcode.twod.dmtx;

import static java.lang.Math.max;
import static java.lang.Math.min;

import com.jadice.barcode.Options;
import com.jadice.barcode.grid.Grid;

public class Decode {
  public class FlagGrid {
    private final int width;
    private final int height;
    private final byte cache[];

    FlagGrid(byte cache[], int width, int height) throws OutOfRangeException {
      this.width = width;
      this.height = height;
      this.cache = cache;
    }

    public boolean isValid(PixelLocation pos) throws OutOfRangeException {
      return pos.x >= 0 && pos.x < width && pos.y >= 0 && pos.y < height;
    }

    public int get(PixelLocation pos) {
      if (!isValid(pos))
        throw new OutOfRangeException("Cache location invalid");
      return cache[pos.y * width + pos.x] & 0xff;
    }

    public void setTo(PixelLocation pos, int value) {
      if (!isValid(pos))
        throw new OutOfRangeException("Cache location invalid");
      cache[pos.y * width + pos.x] = (byte) value;
    }

    public void set(PixelLocation pos, int value) {
      if (!isValid(pos))
        throw new OutOfRangeException("Cache location invalid");
      cache[pos.y * width + pos.x] |= (byte) value;
    }

    public void reset(PixelLocation pos) {
      if (!isValid(pos))
        throw new OutOfRangeException("Cache location invalid");
      cache[pos.y * width + pos.x] = 0;
    }

    public void clear(PixelLocation pos, int value) {
      if (!isValid(pos))
        throw new OutOfRangeException("Cache location invalid");
      cache[pos.y * width + pos.x] &= (byte) ~value;
    }

    public boolean isAnySet(PixelLocation pos, int value) {
      if (!isValid(pos))
        throw new OutOfRangeException("Cache location invalid");
      return (cache[pos.y * width + pos.x] & (byte) value) != 0;
    }

    /**
     * \brief Fill the region covered by the quadrilateral given by (p0,p1,p2,p3) in the cache.
     * 
     * @param decode TODO
     * @param p0 TODO
     * @param p1 TODO
     * @param p2 TODO
     * @param p3 TODO
     */
    void fillQuad(PixelLocation p0, PixelLocation p1, PixelLocation p2, PixelLocation p3) {
      PixelLocation pEmpty = new PixelLocation();
      BresenhamLine lines[] = new BresenhamLine[4];
      lines[0] = new BresenhamLine(p0, p1, pEmpty);
      lines[1] = new BresenhamLine(p1, p2, pEmpty);
      lines[2] = new BresenhamLine(p2, p3, pEmpty);
      lines[3] = new BresenhamLine(p3, p0, pEmpty);

      int minY = getYMax();
      int maxY = 0;

      minY = min(minY, p0.y);
      maxY = max(maxY, p0.y);
      minY = min(minY, p1.y);
      maxY = max(maxY, p1.y);
      minY = min(minY, p2.y);
      maxY = max(maxY, p2.y);
      minY = min(minY, p3.y);
      maxY = max(maxY, p3.y);

      int sizeY = maxY - minY + 1;

      int scanlineMin[] = new int[sizeY]; // (int *)malloc(sizeY * sizeof(int));
      int scanlineMax[] = new int[sizeY]; // (int *)calloc(sizeY, sizeof(int));

      for (int i = 0; i < sizeY; i++)
        scanlineMin[i] = getXMax();

      for (int i = 0; i < 4; i++)
        while (lines[i].loc.x != lines[i].loc1.x || lines[i].loc.y != lines[i].loc1.y) {
          int idx = lines[i].loc.y - minY;
          scanlineMin[idx] = min(scanlineMin[idx], lines[i].loc.x);
          scanlineMax[idx] = max(scanlineMax[idx], lines[i].loc.x);
          lines[i].step(1, 0);
        }

      PixelLocation pos = new PixelLocation();
      for (pos.y = minY; pos.y < maxY && pos.y < getYMax(); pos.y++) {
        int idx = pos.y - minY;
        for (pos.x = scanlineMin[idx]; pos.x < scanlineMax[idx] && pos.x < getXMax(); pos.x++)
          set(pos, 0x80);
      }
    }
  }

  private static int timeout;

  /* Options */
  private int edgeMin;
  private int edgeMax;
  private double squareDevn;
  private SymbolSize sizeIdxExpected;
  private int edgeThresh;

  /* Image modifiers */
  private final int xMin;
  final int xMax;
  private final int yMin;
  final int yMax;
  private final int scale;

  /* Internals */
  private final byte cache[];
  private final Grid grid;
  private final ScanStrategy scanStrategy;
  private final long startTime;
  public static final long DMTX_USEC_PER_SEC = 1000000;

  private final Options options;

  private final FlagGrid flagGrid;

  /**
   * \brief Initialize decode struct with default values \param img \return Initialized DmtxDecode
   * struct
   */
  Decode(Grid img, ScanStrategy scanStrategy, int scale, Options options) {
    this.grid = img;
    this.scanStrategy = scanStrategy;
    this.options = options;

    int width = grid.getWidth() / scale;
    int height = grid.getHeight() / scale;

    this.setEdgeMin(LibDMTX.DmtxUndefined);
    this.setEdgeMax(LibDMTX.DmtxUndefined);
    this.setSquareDevn(Math.cos(50 * (Math.PI / 180)));
    this.setSizeIdxExpected(SymbolSize.ShapeAuto);
    this.setEdgeThresh(10);

    this.xMin = 0;
    this.xMax = width - 1;
    this.yMin = 0;
    this.yMax = height - 1;
    this.scale = scale;

    cache = new byte[width * height];
    flagGrid = new FlagGrid(cache, width, height);

    this.startTime = System.currentTimeMillis();
  }

  public int getXMin() {
    return this.xMin;
  }

  public int getXMax() {
    return this.xMax;
  }

  public int getYMin() {
    return this.yMin;
  }

  public int getYMax() {
    return this.yMax;
  }

  public int getWidth() {
    return grid.getWidth() / this.scale;
  }

  public int getHeight() {
    return grid.getHeight() / this.scale;
  }

  /**
   * @throws OutOfRangeException
   * 
   * 
   */
  int getPixelValue(int x, int y, int channel) throws OutOfRangeException {
    int xUnscaled = x * this.scale;
    int yUnscaled = y * this.scale;

    /* Remove spherical lens distortion */
    /*
     * int width, height; double radiusPow2, radiusPow4; double factor; DmtxVector2 pointShifted;
     * DmtxVector2 correctedPoint;
     * 
     * width = dmtxImageGetProp(img, DmtxPropWidth); height = dmtxImageGetProp(img, DmtxPropHeight);
     * 
     * pointShifted.X = point.X - width/2.0; pointShifted.Y = point.Y - height/2.0;
     * 
     * radiusPow2 = pointShifted.X * pointShifted.X + pointShifted.Y * pointShifted.Y; radiusPow4 =
     * radiusPow2 * radiusPow2;
     * 
     * factor = 1 + (k1 * radiusPow2) + (k2 * radiusPow4);
     * 
     * correctedPoint.X = pointShifted.X * factor + width/2.0; correctedPoint.Y = pointShifted.Y *
     * factor + height/2.0;
     * 
     * return correctedPoint;
     */

    // if (xUnscaled < 0 || yUnscaled < 0 || xUnscaled >= grid.getWidth() || yUnscaled >=
    // grid.getHeight())
    // throw new OutOfRangeException("");

    int v = grid.getPixelLuminance(xUnscaled, grid.getHeight() - yUnscaled - 1);
    // System.out.println(xUnscaled + "/" + yUnscaled + "[" + channel + "]: " + v);
    return v; // FIXME , channel);
  }

  public void setSizeIdxExpected(SymbolSize sizeIdxExpected) {
    this.sizeIdxExpected = sizeIdxExpected;
  }

  public SymbolSize getSizeIdxExpected() {
    return sizeIdxExpected;
  }

  public void setEdgeThresh(int edgeThresh) {
    this.edgeThresh = edgeThresh;
  }

  public int getEdgeThresh() {
    return edgeThresh;
  }

  public Grid getImage() {
    return grid;
  }

  public void setEdgeMax(int edgeMax) {
    this.edgeMax = edgeMax;
  }

  public int getEdgeMax() {
    return edgeMax;
  }

  public void setEdgeMin(int edgeMin) {
    this.edgeMin = edgeMin;
  }

  public int getEdgeMin() {
    return edgeMin;
  }

  public void setSquareDevn(double squareDevn) {
    this.squareDevn = squareDevn;
  }

  public double getSquareDevn() {
    return squareDevn;
  }

  /**
   * \brief Find next barcode region \param dec Pointer to DmtxDecode information struct \param
   * timeout Pointer to timeout time (null if none) \return Detected region (if found)
   */
  public Region findNextRegion() {
    final PixelLocation loc = new PixelLocation();

    /* Continue until we find a region or run out of chances */
    for (;;) {
      if (!scanStrategy.getNextScanLocation(loc))
        break;

      /* Scan location for presence of valid barcode region */
      Region reg = Region.scan(this, loc, options);
      if (reg != null)
        return reg;

      /* Ran out of time? */
      if (timeExceeded())
        break;
    }

    return null;
  }

  private boolean timeExceeded() {
    return 0 != timeout && System.currentTimeMillis() > startTime + timeout;
  }

  /**
   * \brief Fill the region covered by the quadrilateral given by (p0,p1,p2,p3) in the cache.
   */
  void cacheFillQuad(PixelLocation p0, PixelLocation p1, PixelLocation p2, PixelLocation p3) {
    PixelLocation pEmpty = new PixelLocation();
    BresenhamLine lines[] = new BresenhamLine[4];
    lines[0] = new BresenhamLine(p0, p1, pEmpty);
    lines[1] = new BresenhamLine(p1, p2, pEmpty);
    lines[2] = new BresenhamLine(p2, p3, pEmpty);
    lines[3] = new BresenhamLine(p3, p0, pEmpty);

    int minY = getYMax();
    int maxY = 0;

    minY = min(minY, p0.y);
    maxY = max(maxY, p0.y);
    minY = min(minY, p1.y);
    maxY = max(maxY, p1.y);
    minY = min(minY, p2.y);
    maxY = max(maxY, p2.y);
    minY = min(minY, p3.y);
    maxY = max(maxY, p3.y);

    int sizeY = maxY - minY + 1;

    int scanlineMin[] = new int[sizeY]; // (int *)malloc(sizeY * sizeof(int));
    int scanlineMax[] = new int[sizeY]; // (int *)calloc(sizeY, sizeof(int));

    for (int i = 0; i < sizeY; i++)
      scanlineMin[i] = getXMax();

    for (int i = 0; i < 4; i++)
      while (lines[i].loc.x != lines[i].loc1.x || lines[i].loc.y != lines[i].loc1.y) {
        int idx = lines[i].loc.y - minY;
        scanlineMin[idx] = min(scanlineMin[idx], lines[i].loc.x);
        scanlineMax[idx] = max(scanlineMax[idx], lines[i].loc.x);
        lines[i].step(1, 0);
      }

    PixelLocation pos = new PixelLocation();
    for (pos.y = minY; pos.y < maxY && pos.y < getYMax(); pos.y++) {
      int idx = pos.y - minY;
      for (pos.x = scanlineMin[idx]; pos.x < scanlineMax[idx] && pos.x < getXMax(); pos.x++)
        if (getFlagGrid().isValid(pos))
          getFlagGrid().set(pos, 0x80);
    }
  }

  public int getScale() {
    return scale;
  }

  public FlagGrid getFlagGrid() {
    return flagGrid;
  }
}