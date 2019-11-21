package com.jadice.barcode.twod.dmtx_zxing;

import com.jadice.barcode.Settings;

public class ZXingDatamatrixSettings implements Settings {
  private int minExtent;

  public int getMinExtent() {
    return minExtent;
  }

  public void setMinExtent(final int minExtent) {
    this.minExtent = minExtent;
  }
}
