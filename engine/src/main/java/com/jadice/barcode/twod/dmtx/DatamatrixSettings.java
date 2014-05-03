package com.jadice.barcode.twod.dmtx;

import com.jadice.barcode.Settings;

public class DatamatrixSettings implements Settings {
  private int minExtent;

  public int getMinExtent() {
    return minExtent;
  }

  public void setMinExtent(int minExtent) {
    this.minExtent = minExtent;
  }
}
