package com.jadice.barcode.twod.dmtx_zxing;

import com.jadice.barcode.Decoder;
import com.jadice.barcode.Settings;
import com.jadice.barcode.Symbology;

public class ZXingDatamatrix implements Symbology {

  @Override
  public Class<? extends Settings> getSettingsClass() {
    return ZXingDatamatrixSettings.class;
  }

  @Override
  public String name() {
    return "Datamatrix (ZXing)";
  }

  @Override
  public boolean canDecode() {
    try {
      // can only decode if zxin is on the class path
      Class.forName("com.google.zxing.datamatrix.DataMatrixReader");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public Decoder createDecoder() {
    return new ZXingDatamatrixDecoder();
  }
}
