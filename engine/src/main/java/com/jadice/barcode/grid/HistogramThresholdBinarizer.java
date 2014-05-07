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
package com.jadice.barcode.grid;

import java.awt.geom.AffineTransform;
import java.util.List;

import com.jadice.barcode.Result;

/**
 * A {@link Binarizer} implementation which uses some heuristics to derive a global threshold from
 * the given {@link LuminanceGrid}. This threshold is used to binarize the image.
 */
public class HistogramThresholdBinarizer extends FixedlThresholdBinarizer {

  private static final int histogramSamplingInterval = 10;

  public HistogramThresholdBinarizer(LuminanceGrid grid) {
    super(grid);
    setThreshold(calcThreshold(null));
  }

  private int[] getHistogram() {
    int histogram[] = new int[256];

    int width = grid.getWidth();
    int height = grid.getHeight();

    for (int y = 0; y < height; y += histogramSamplingInterval)
      for (int x = 0; x < width; x += histogramSamplingInterval) {
        int value = grid.getPixelLuminance(x, y);
        if (value < 0)
          value = 0;
        if (value > 255)
          value = 255;
        histogram[value]++;
      }

    return histogram;
  }

  /**
   * This is where the black/white threshold of the histogram is tuned. There's some magic taking
   * place here.
   * 
   * @param results
   */
  private int calcThreshold(List<Result> results) {
    final int histogram[] = getHistogram();
    int totalSamples = 0;
    final int maxHistogramIndex = -1;

    // results.addLine(Marker.Feature.HISTOGRAM, x0, y0, x0 + histogram.length, y0);

    float max = 0;
    for (final int h : histogram)
      if (h > max)
        max = h;

    // boolean enableDebugging = options.getOptions(BaseSettings.class).isDebuggingEnabled();
    for (int i = 0; i < histogram.length; i++) {
      totalSamples += histogram[i];

      // if (enableDebugging)
      // results.addLine(Marker.Feature.HISTOGRAM, x0 + i, y0, x0 + i, y0 - histogram[i] / max *
      // 100f);
    }

    // Eliminate all histogram values which make up more than 25% of the
    // sum. This is not optimal. A better (but more complicated) solution
    // would be to eliminate the top x% of all values.
    final int totalSamples4 = totalSamples / 4;
    for (int i = 0; i < histogram.length; i++)
      if (histogram[i] > totalSamples4) {
        totalSamples -= histogram[i] - 1;
        histogram[i] = 1;
      }

    // find the threshold to use.
    final int totalSamples2 = totalSamples / 2;
    totalSamples = 0;
    int threshold;
    int lastNonzeroIndex = 0;
    for (threshold = 0; threshold < histogram.length; threshold++)
      if (threshold != maxHistogramIndex) {
        if ((totalSamples += histogram[threshold]) > totalSamples2) {
          threshold = (threshold + lastNonzeroIndex) / 2 + 1;
          break;
        }
        if (histogram[threshold] != 0)
          lastNonzeroIndex = threshold;
      }

    // if (enableDebugging)
    // results.addLine(Marker.Feature.HISTOGRAM_THRESHOLD, x0 + threshold, y0, x0 + threshold, y0 -
    // 100);

    return threshold * channels;
  }

  @Override
  public AffineTransform getInverseTransform() {
    return grid.getInverseTransform();
  }

  @Override
  public int getPixelLuminance(int x, int y) {
    return samplePixel(x, y) ? 0 : 255;
  }
}
