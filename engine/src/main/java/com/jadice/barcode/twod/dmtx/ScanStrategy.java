package com.jadice.barcode.twod.dmtx;



public interface ScanStrategy {

  /**
   * \brief Return the next good location (which may be the current location), and advance grid
   * progress one position beyond that. If no good locations remain then return DmtxRangeEnd. \param
   * grid \return void
   */
  public abstract boolean getNextScanLocation(PixelLocation p);

}