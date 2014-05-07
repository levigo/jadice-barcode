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
package com.jadice.barcode;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.jadice.barcode.grid.BinaryGrid;
import com.jadice.barcode.grid.FixedlThresholdBinarizer;
import com.jadice.barcode.grid.GlobalThresholdBinarizer;
import com.jadice.barcode.grid.Grid;
import com.jadice.barcode.grid.LuminanceGrid;
import com.jadice.barcode.grid.QuadrantRotationGrid;
import com.jadice.barcode.grid.ROIGrid;
import com.jadice.barcode.linear.LinearCodeSettings;
import com.jadice.barcode.linear.LinearCodeSettings.Direction;

/**
 * This class provides the main entry point for bar code detection and decoding.
 */
public class Detector {
  /**
   * Make an attempt to decode bar codes in the given {@link Grid}. The decoding process is governed
   * by the given options.
   * 
   * @param options
   * @param grid
   * @return
   */
  public static List<Result> decode(Options options, Grid grid) {
    // globally aggregated results go here
    List<Result> results = new ArrayList<Result>();

    LuminanceGrid lumGrid = null;
    if (grid instanceof LuminanceGrid)
      lumGrid = (LuminanceGrid) grid;

    BinaryGrid binaryGrid = grid instanceof BinaryGrid //
        ? ((BinaryGrid) grid) //
        : prepareBinaryGrid(options, grid);

    BaseSettings baseSettings = options.getOptions(BaseSettings.class);
    LinearCodeSettings linearCodeSettings = options.getOptions(LinearCodeSettings.class);

    // build image variations
    List<BinaryGrid> binarySources = new LinkedList<BinaryGrid>();
    for (Direction d : Direction.values()) {
      if (linearCodeSettings.isDirectionEnabled(d))
        if (baseSettings.getRegions().isEmpty())
          binarySources.add(d == Direction.EAST ? binaryGrid : new QuadrantRotationGrid(binaryGrid, d));
        else
          for (Rectangle region : baseSettings.getRegions())
            binarySources.add(new ROIGrid(d == Direction.EAST ? binaryGrid : new QuadrantRotationGrid(binaryGrid, d),
                region));
    }

    // build list of codes to try
    List<Decoder> decoders = new LinkedList<Decoder>();
    for (Class<? extends Symbology> sc : baseSettings.getAvailableSymbologies())
      if (baseSettings.isSymbologyEnabled(sc))
        try {
          final Symbology s = sc.newInstance();
          if (s.canDecode()) {
            Decoder decoder = s.createDecoder();
            decoder.setOptions(options);
            decoders.add(decoder);
          }
        } catch (Exception e) {
          // won't happen.
        }

    // iterate over source variations, then codes
    for (BinaryGrid src : binarySources)
      for (Decoder d : decoders)
        if (d instanceof BinaryDecoder)
          merge(((BinaryDecoder) d).detect(src), src.getInverseTransform(), results);
        else if (d instanceof LuminanceDecoder)
          merge(((LuminanceDecoder) d).detect(lumGrid), src.getInverseTransform(), results);

    return results;
  }

  /**
   * Merge another list of resuls, applying the given transform to them.
   * 
   * @param transform
   * @param r
   */
  private static void merge(Collection<Result> src, AffineTransform transform, Collection<Result> dst) {
    for (final Result c : src) {
      c.transform(transform);
      dst.add(c);
    }
  }

  public static BinaryGrid prepareBinaryGrid(Options options, Grid grid) {
    BinaryGrid binaryGrid;
    int threshold = options.getOptions(BaseSettings.class).getThreshold();
    if (grid instanceof BinaryGrid)
      return (BinaryGrid) grid;
    else if (grid instanceof LuminanceGrid) {
      if (threshold != BaseSettings.AUTO_THRESHOLD)
        binaryGrid = new FixedlThresholdBinarizer((LuminanceGrid) grid);
      else
        binaryGrid = new GlobalThresholdBinarizer((LuminanceGrid) grid);
    } else
      throw new IllegalArgumentException("Unsupported type of grid: " + grid.getClass());

    if (binaryGrid instanceof FixedlThresholdBinarizer)
      ((FixedlThresholdBinarizer) binaryGrid).setThreshold(threshold * 255 / 100);

    // binaryGrid = new CachingBinaryGrid(binaryGrid);

    return binaryGrid;
  }
}
