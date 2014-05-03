package com.jadice.barcode.twod.dmtx;

import com.levigo.barcode.LuminanceDecoder;
import com.levigo.barcode.Options;
import com.levigo.barcode.Result;
import com.levigo.barcode.Results;
import com.levigo.barcode.Symbology;
import com.levigo.barcode.grid.LuminanceGrid;

public class DatamatrixDecoder implements LuminanceDecoder {

  private Options options;

  @Override
  public void setOptions(Options options) {
    this.options = options;
  }

  @Override
  public Symbology getSymbology() {
    return new Datamatrix();
  }

  @Override
  public Results detect(LuminanceGrid grid) {
    // FIXME: support more options
    ScanGrid scanGrid = new ScanGrid(grid, options);
    scanGrid.setMinExtent(options.getOptions(DatamatrixSettings.class).getMinExtent());

    Decode dec = new Decode(grid, scanGrid, 1, options);


    Results r = new Results();
    Region reg;
    while ((reg = dec.findNextRegion()) != null) {
      Message msg = reg.decodeMatrixRegion(-1);
      if (msg != null) {
        String txt = new String(msg.output, 0, msg.outputIdx);
        r.add(new Result(getSymbology(), reg.getShape(), txt, true, true, reg.getBottomAngle()));
        break; // FIXME: support finding more than one code
      }
    }

    return r;
  }
}
