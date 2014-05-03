package com.jadice.barcode.twod.dmtx;

import com.jadice.barcode.Decoder;
import com.jadice.barcode.Settings;
import com.jadice.barcode.Symbology;

public class Datamatrix implements Symbology {

  @Override
  public Class<? extends Settings> getSettingsClass() {
    return DatamatrixSettings.class;
  }

  @Override
  public String name() {
    return "Datamatrix";
  }

  @Override
  public boolean canDecode() {
    return true;
  }

  @Override
  public Decoder createDecoder() {
    return new DatamatrixDecoder();
  }
}
