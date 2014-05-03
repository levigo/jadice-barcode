package com.jadice.barcode.twod.dmtx;

import static java.lang.Math.abs;

public class BresenhamLine {
  int xStep;
  int yStep;
  int xDelta;
  int yDelta;
  boolean steep;
  int xOut;
  int yOut;
  int travel;
  int outward;
  int error;
  PixelLocation loc;
  PixelLocation loc0;
  PixelLocation loc1;

  @Override
  protected BresenhamLine clone() {
    final BresenhamLine l = new BresenhamLine();
    l.xStep = xStep;
    l.yStep = yStep;
    l.xDelta = xDelta;
    l.yDelta = yDelta;
    l.steep = steep;
    l.xOut = xOut;
    l.yOut = yOut;
    l.travel = travel;
    l.outward = outward;
    l.error = error;
    l.loc = loc.clone();
    l.loc0 = loc0.clone();
    l.loc1 = loc1.clone();
    return l;
  }

  public void copyTo(BresenhamLine l) {
    l.xStep = xStep;
    l.yStep = yStep;
    l.xDelta = xDelta;
    l.yDelta = yDelta;
    l.steep = steep;
    l.xOut = xOut;
    l.yOut = yOut;
    l.travel = travel;
    l.outward = outward;
    l.error = error;
    l.loc = loc.clone();
    l.loc0 = loc0.clone();
    l.loc1 = loc1.clone();
  }

  private BresenhamLine() {
  }

  /**
   *
   *
   */
  BresenhamLine(PixelLocation loc0, PixelLocation loc1, PixelLocation locInside) {
    int cp;
    PixelLocation locBeg, locEnd;

    /* XXX Verify that loc0 and loc1 are inbounds */

    /* Values that stay the same after initialization */
    this.loc0 = loc0.clone();
    this.loc1 = loc1.clone();
    this.xStep = loc0.x < loc1.x ? +1 : -1;
    this.yStep = loc0.y < loc1.y ? +1 : -1;
    this.xDelta = abs(loc1.x - loc0.x);
    this.yDelta = abs(loc1.y - loc0.y);
    this.steep = this.yDelta > this.xDelta;

    /* Take cross product to determine outward step */
    if (this.steep) {
      /* Point first vector up to get correct sign */
      if (loc0.y < loc1.y) {
        locBeg = loc0;
        locEnd = loc1;
      } else {
        locBeg = loc1;
        locEnd = loc0;
      }
      cp = (locEnd.x - locBeg.x) * (locInside.y - locEnd.y) - (locEnd.y - locBeg.y) * (locInside.x - locEnd.x);

      this.xOut = cp > 0 ? +1 : -1;
      this.yOut = 0;
    } else {
      /* Point first vector left to get correct sign */
      if (loc0.x > loc1.x) {
        locBeg = loc0;
        locEnd = loc1;
      } else {
        locBeg = loc1;
        locEnd = loc0;
      }
      cp = (locEnd.x - locBeg.x) * (locInside.y - locEnd.y) - (locEnd.y - locBeg.y) * (locInside.x - locEnd.x);

      this.xOut = 0;
      this.yOut = cp > 0 ? +1 : -1;
    }

    /* Values that change while stepping through line */
    this.loc = loc0.clone();
    this.travel = 0;
    this.outward = 0;
    this.error = this.steep ? this.yDelta / 2 : this.xDelta / 2;

    /*
     * CALLBACK_POINT_PLOT(loc0, 3, 1, 1); CALLBACK_POINT_PLOT(loc1, 3, 1, 1);
     */
  }

  /**
   *
   *
   */
  void getStep(PixelLocation target, int travel[], int outward[]) {
    /* Determine necessary step along and outward from Bresenham line */
    if (this.steep) {
      travel[0] = this.yStep > 0 ? target.y - this.loc.y : this.loc.y - target.y;
      step(travel[0], 0);
      outward[0] = this.xOut > 0 ? target.x - this.loc.x : this.loc.x - target.x;
      assert this.yOut == 0;
    } else {
      travel[0] = this.xStep > 0 ? target.x - this.loc.x : this.loc.x - target.x;
      step(travel[0], 0);
      outward[0] = this.yOut > 0 ? target.y - this.loc.y : this.loc.y - target.y;
      assert this.xOut == 0;
    }
  }

  /**
   *
   *
   */
  void step(int travel, int outward) {
    assert abs(travel) < 2;
    assert abs(outward) >= 0;

    /* Perform forward step */
    final BresenhamLine lineNew = this;
    if (travel > 0) {
      lineNew.travel++;
      if (lineNew.steep) {
        lineNew.loc.y += lineNew.yStep;
        lineNew.error -= lineNew.xDelta;
        if (lineNew.error < 0) {
          lineNew.loc.x += lineNew.xStep;
          lineNew.error += lineNew.yDelta;
        }
      } else {
        lineNew.loc.x += lineNew.xStep;
        lineNew.error -= lineNew.yDelta;
        if (lineNew.error < 0) {
          lineNew.loc.y += lineNew.yStep;
          lineNew.error += lineNew.xDelta;
        }
      }
    } else if (travel < 0) {
      lineNew.travel--;
      if (lineNew.steep) {
        lineNew.loc.y -= lineNew.yStep;
        lineNew.error += lineNew.xDelta;
        if (lineNew.error >= lineNew.yDelta) {
          lineNew.loc.x -= lineNew.xStep;
          lineNew.error -= lineNew.yDelta;
        }
      } else {
        lineNew.loc.x -= lineNew.xStep;
        lineNew.error += lineNew.yDelta;
        if (lineNew.error >= lineNew.xDelta) {
          lineNew.loc.y -= lineNew.yStep;
          lineNew.error -= lineNew.xDelta;
        }
      }
    }

    for (int i = 0; i < outward; i++) {
      /* Outward steps */
      lineNew.outward++;
      lineNew.loc.x += lineNew.xOut;
      lineNew.loc.y += lineNew.yOut;
    }
  }
}