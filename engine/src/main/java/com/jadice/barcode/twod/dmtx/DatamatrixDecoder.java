/*
 * This file is part of a java-port of the libdmtx library.
 * 
 * Copyright (C) 2014 levigo solutions gmbh Contact: solutions@levigo.de
 * 
 * 
 * The original library's copyright statement follows:
 * 
 * libdmtx - Data Matrix Encoding/Decoding Library
 * 
 * Copyright (C) 2011 Mike Laughton
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Contact: mike@dragonflylogic.com
 */
package com.jadice.barcode.twod.dmtx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.jadice.barcode.LuminanceDecoder;
import com.jadice.barcode.Options;
import com.jadice.barcode.Result;
import com.jadice.barcode.Symbology;
import com.jadice.barcode.grid.LuminanceGrid;

public class DatamatrixDecoder implements LuminanceDecoder {

  private Options options;
  public static final int DmtxUndefined = -1;
  static final double DmtxAlmostZero = 0.000001;

  @Override
  public void setOptions(Options options) {
    this.options = options;
  }

  @Override
  public Symbology getSymbology() {
    return new Datamatrix();
  }

  @Override
  public Collection<Result> detect(LuminanceGrid grid) {
    // FIXME: support more options
    ScanGrid scanGrid = new ScanGrid(grid, options);
    scanGrid.setMinExtent(options.getOptions(DatamatrixSettings.class).getMinExtent());

    Decode dec = new Decode(grid, scanGrid, 1, options);


    List<Result> r = new ArrayList<Result>();
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
