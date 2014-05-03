package com.jadice.barcode.twod.dmtx;

/**
 * @struct DmtxPixelLoc
 * @brief DmtxPixelLoc
 */
public class Point {
  int x;
  int y;

  public Point() {
  }

  public Point(Point loc) {
    x = loc.x;
    y = loc.y;
  }

  public Point(int x, int y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public Point clone() {
    return new Point(this);
  }

  /**
   *
   *
   */
  public long distanceSquared(Point b) {
    int xDelta = x - b.x;
    int yDelta = y - b.y;
    return xDelta * xDelta + yDelta * yDelta;
  }

  @Override
  public String toString() {
    return "(" + x + "," + y + ")";
  }

  public void setTo(Point l) {
    this.x = l.x;
    this.y = l.y;
  }
}