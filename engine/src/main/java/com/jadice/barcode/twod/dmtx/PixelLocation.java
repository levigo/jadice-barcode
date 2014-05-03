package com.jadice.barcode.twod.dmtx;

/**
 * @struct DmtxPixelLoc
 * @brief DmtxPixelLoc
 */
public class PixelLocation {
  int x;
  int y;

  public PixelLocation() {
  }

  public PixelLocation(PixelLocation loc) {
    x = loc.x;
    y = loc.y;
  }

  @Override
  public PixelLocation clone() {
    return new PixelLocation(this);
  }

  /**
   *
   *
   */
  public long distanceSquared(PixelLocation b) {
    int xDelta = x - b.x;
    int yDelta = y - b.y;
    return xDelta * xDelta + yDelta * yDelta;
  }

  @Override
  public String toString() {
    return "(" + x + "," + y + ")";
  }
}