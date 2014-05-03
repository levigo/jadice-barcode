package com.jadice.barcode.twod.dmtx;

import com.levigo.barcode.Decoder;
import com.levigo.barcode.Settings;
import com.levigo.barcode.Symbology;

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
