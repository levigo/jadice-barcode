/**
 * jadice barcode engine - a Java-based barcode decoding engine
 * 
 * Copyright (C) 1995-${year} levigo holding gmbh. All Rights Reserved.
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
 * Contact: solutions@levigo.de
 */
package com.jadice.barcode.twod.dmtx_zxing;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.DataMatrixReader;
import com.jadice.barcode.LuminanceDecoder;
import com.jadice.barcode.Options;
import com.jadice.barcode.Result;
import com.jadice.barcode.Symbology;
import com.jadice.barcode.grid.LuminanceGrid;

/**
 * This is a wrapper around the Zebra Crossing (ZXing) Datamatrix recognizer so we can use the
 * latter with a single API.
 *
 */
public class ZXingDatamatrixDecoder implements LuminanceDecoder {

  private Options options;
  public static final int DmtxUndefined = -1;
  static final double DmtxAlmostZero = 0.000001;

  @Override
  public void setOptions(final Options options) {
    this.options = options;
  }

  @Override
  public Symbology getSymbology() {
    return new ZXingDatamatrix();
  }

  @Override
  public Collection<Result> detect(final LuminanceGrid grid) {
    DataMatrixReader r = new DataMatrixReader();

    try {
      com.google.zxing.Result result = r.decode(
          new BinaryBitmap(new HybridBinarizer(new LuminanceSource(grid.getWidth(), grid.getHeight()) {
            @Override
            public byte[] getRow(final int y, final byte[] row) {
              int w = grid.getWidth();

              byte r[] = row;
              if (null == r || r.length < w)
                r = new byte[w];

              for (int x = 0; x < w; x++) {
                r[x] = (byte) grid.getLuminance(x, y);
              }

              return r;
            }

            @Override
            public byte[] getMatrix() {
              int w = grid.getWidth();
              int h = grid.getHeight();
              byte m[] = new byte[w * h];

              for (int y = 0; y < h; y++) {
                int y0 = y * w;
                for (int x = 0; x < w; x++) {
                  m[y0 + x] = (byte) grid.getLuminance(x, y);
                }
              }

              return m;
            }
          })));

      Rectangle2D rectangle = null;
      for (ResultPoint p : result.getResultPoints()) {
        if (null == rectangle)
          rectangle = new Rectangle2D.Float(p.getX(), p.getY(), 0, 0);
        else
          rectangle.add(p.getX(), p.getY());
      }

      return Collections.singleton(new Result(getSymbology(), rectangle, result.getText(), true, true, 0));
    } catch (NotFoundException | ChecksumException | FormatException e) {
      // ignore and return as not found
    }

    return Collections.emptyList();
  }
}
