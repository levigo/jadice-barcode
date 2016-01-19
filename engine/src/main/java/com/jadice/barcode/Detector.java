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
import com.jadice.barcode.grid.Grid;
import com.jadice.barcode.grid.HistogramThresholdBinarizer;
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

    List<Integer> thresholds = options.getSettings(BaseSettings.class).getThresholds();
    int barcodeCountLimit = options.getSettings(BaseSettings.class).getBarcodeCountLimit();
    if (!thresholds.isEmpty()) {
      for (int threshold : thresholds) {
        mergeNoDuplicates(decodeAtThreshold(options, grid, threshold), results);
        // break when expected number of barcodes are found
        if (barcodeCountLimit != BaseSettings.NO_BARCODELIMIT && results.size() >= barcodeCountLimit)
          break;
      }
    } else {
      results.addAll(decodeAtThreshold(options, grid, BaseSettings.AUTO_THRESHOLD));
    }
    return results;
  }

  private static List<Result> decodeAtThreshold(Options options, Grid grid, int threshold) {
    List<Result> results = new ArrayList<Result>();


    LuminanceGrid lumGrid = null;
    if (grid instanceof LuminanceGrid)
      lumGrid = (LuminanceGrid) grid;

    BinaryGrid binaryGrid = grid instanceof BinaryGrid //
        ? ((BinaryGrid) grid) //
        : prepareBinaryGrid(options, grid, threshold);

    BaseSettings baseSettings = options.getSettings(BaseSettings.class);
    LinearCodeSettings linearCodeSettings = options.getSettings(LinearCodeSettings.class);

    // build image variations
    List<BinaryGrid> binarySources = new LinkedList<BinaryGrid>();
    if (!baseSettings.getRegions().isEmpty()) {
      for (Rectangle region : baseSettings.getRegions()) {
        ROIGrid regionGrid = new ROIGrid(binaryGrid, region);
        binarySources.addAll(getBinarySourcesForActiveDirections(regionGrid, linearCodeSettings));
      }
    } else {
      binarySources.addAll(getBinarySourcesForActiveDirections(binaryGrid, linearCodeSettings));
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
   * Returns the binary sources for all active directions.
   */
  private static List<BinaryGrid> getBinarySourcesForActiveDirections(BinaryGrid binaryGrid,
      LinearCodeSettings linearCodeSettings) {
    List<BinaryGrid> binarySources = new LinkedList<BinaryGrid>();
    for (Direction d : Direction.values()) {
      if (linearCodeSettings.isDirectionEnabled(d))
        binarySources.add(d == Direction.EAST ? binaryGrid : new QuadrantRotationGrid(binaryGrid, d));
    }
    return binarySources;
  }

  /**
   * Merge another list of results, applying the given transform to them.
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

  /**
   * Merge another list of results, excluding any duplicates.
   */
  private static void mergeNoDuplicates(Collection<Result> src, Collection<Result> dst) {
    for (final Result s : src) {
      boolean duplicate = false;
      for (final Result d : dst) {
        // already found
        if (d.getShape().intersects(s.getShape().getBounds())) {
          duplicate = true;
          break;
        }
      }
      if (!duplicate) {
        dst.add(s);
      }
    }
  }

  public static BinaryGrid prepareBinaryGrid(Options options, Grid grid, int threshold) {
    BinaryGrid binaryGrid;

    if (grid instanceof BinaryGrid)
      return (BinaryGrid) grid;
    else if (grid instanceof LuminanceGrid) {
      if (threshold != BaseSettings.AUTO_THRESHOLD)
        binaryGrid = new FixedlThresholdBinarizer((LuminanceGrid) grid);
      else
        binaryGrid = new HistogramThresholdBinarizer((LuminanceGrid) grid);
    } else
      throw new IllegalArgumentException("Unsupported type of grid: " + grid.getClass());

    if (binaryGrid instanceof FixedlThresholdBinarizer)
      ((FixedlThresholdBinarizer) binaryGrid).setThreshold(threshold * 255 / 100);

    // binaryGrid = new CachingBinaryGrid(binaryGrid);

    return binaryGrid;
  }
}
