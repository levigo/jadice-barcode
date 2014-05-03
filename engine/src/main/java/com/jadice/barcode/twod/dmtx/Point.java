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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + x;
    result = prime * result + y;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Point other = (Point) obj;
    if (x != other.x)
      return false;
    if (y != other.y)
      return false;
    return true;
  }
}