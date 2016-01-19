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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.imageio.spi.ServiceRegistry;

/**
 * The basic {@link Settings} which either apply to all symbologies or control the global decoding
 * process.
 */
public class BaseSettings implements Settings {
  private final Set<Class<? extends Symbology>> enabledSymbologies = new HashSet<Class<? extends Symbology>>();

  public static final int AUTO_THRESHOLD = -1;

  private List<Integer> thresholds = new ArrayList<Integer>();

  /**
   * The overprint tolerance specifies the latitude the detection allows for the mis-match between
   * black and white bar widths. An overprint tolerance of 30 means that a black bar may be 30%
   * wider or narrower than a white bar of the same unit width. Please note that although the
   * parameter is named overprintTolerance, "underprinting" is also accepted although it is far less
   * common in practice.
   * 
   * Useful values fall in the range [0, 150]
   * 
   * Default: 80
   */
  private int overprintTolerance = 100;

  /**
   * The list of regions-of-interest for the detection.
   */
  private final List<Rectangle> regions = new ArrayList<Rectangle>();

  public static final int NO_BARCODELIMIT = 0;

  /**
   * The limit of found barcodes after which the detection stops and returns the already detected
   * barcode results.
   * 
   * Default: {@link NO_BARCODELIMIT}
   */
  private int barcodeCountLimit = NO_BARCODELIMIT;

  public BaseSettings() {
  }

  public List<Class<? extends Symbology>> getAvailableSymbologies() {
    Iterator<Symbology> i = ServiceRegistry.lookupProviders(Symbology.class, getClass().getClassLoader());
    ArrayList<Class<? extends Symbology>> result = new ArrayList<Class<? extends Symbology>>();
    while (i.hasNext())
      result.add(i.next().getClass());
    return result;
  }

  /**
   * Set whether the given symbology shall be enabled (i.e. an attempt shall be made to decode a
   * bar-code based on it).
   * 
   * @param s the {@link Symbology}
   * @param <code>true</code> to enable the symology
   */
  public void setSymbologyEnabled(Class<? extends Symbology> s, boolean enabled) {
    if (enabled)
      enabledSymbologies.add(s);
    else
      enabledSymbologies.remove(s);
  }

  /**
   * Return whether the given symbology is enabled (i.e. an attempt will be made to decode a
   * bar-code based on it).
   * 
   * @param s the {@link Symbology}
   * @returns <code>true</code> if enabled
   */
  public boolean isSymbologyEnabled(Class<? extends Symbology> s) {
    return enabledSymbologies.contains(s);
  }

  /**
   * Return the overprint tolerance.
   * 
   * @return the overprintTolerance
   */
  public int getOverprintTolerance() {
    return overprintTolerance;
  }

  /**
   * Set the overprint tolerance.
   * 
   * The overprint tolerance specifies the latitude the detection allows for the mis-match between
   * black and white bar widths. An overprint tolerance of 30 means that a black bar may be 30%
   * wider or narrower than a white bar of the same unit width. Please note that although the
   * parameter is named overprintTolerance, "underprinting" is also accepted although it is far less
   * common in practice.
   * 
   * Useful values fall in the range [0, 150]
   * 
   * Default: 80
   * 
   * @param overprintTolerance the overprintTolerance to set
   */
  public void setOverprintTolerance(int overprintTolerance) {
    if (overprintTolerance < 0 || overprintTolerance > 150)
      throw new IllegalArgumentException("The overprint tolerance must be within [0,150]");

    this.overprintTolerance = overprintTolerance;
  }

  /**
   * Set the regions-of-interest (ROIs). Code detection only considers those parts of the image
   * which intersect at least one ROI. Detection performance is positively affected by a smaller
   * regions of interest. It is therefore desirable, to limit the regions to those areas where codes
   * are know to reside.
   * 
   * @param regions the regions-of-interest to set
   */
  public void setRegions(List<Rectangle> regions) {
    this.regions.clear();
    this.regions.addAll(regions);
  }

  /**
   * Set the regions-of-interest (ROIs) to a single, given region.
   * 
   * @see #setRegions(List)
   * @param region the one region-of-interest to set
   */
  public void setRegion(Rectangle region) {
    this.regions.clear();
    this.regions.add(region);
  }

  /**
   * Set the thresholds used to binarize the image. If no threshold is set, {@link #AUTO_THRESHOLD}
   * is used, in order to let the decoder automatically determine a useful threshold.
   * 
   * The thresholds must be between [0, 100].
   * 
   * Default: {@link #AUTO_THRESHOLD}
   * 
   * @return the thresholds to set
   */
  public void setThresholds(List<Integer> thresholds) {
    this.thresholds.clear();
    this.thresholds.addAll(thresholds);
  }

  /**
   * Set the threshold used to binarize the image. The threshold must be between 0 and 100. The
   * threshold may be set to the constant {@link #AUTO_THRESHOLD}, in order to let the decoder
   * automatically determine a useful threshold.
   * 
   * The threshold must be between [0, 100].
   * 
   * Default: {@link #AUTO_THRESHOLD}
   * 
   * @param threshold the threshold to set
   */
  public void setThreshold(int threshold) {
    this.thresholds.clear();
    this.thresholds.add(threshold);
  }

  /**
   * Set the limit of found barcodes after which the detection stops and returns the already
   * detected barcode results.
   * 
   * Default: {@link NO_BARCODELIMIT}
   * 
   * @param the barcodeCountLimit
   */
  public void setBarcodeCountLimit(int limit) {
    this.barcodeCountLimit = limit;
  }

  /**
   * Get the regions-of-interest (ROIs).
   * 
   * @returns the regions
   */
  public List<Rectangle> getRegions() {
    return regions;
  }

  /**
   * Return the thresholds used to binarize the image.
   * 
   * @return the thresholds
   */
  public List<Integer> getThresholds() {
    return thresholds;
  }

  /**
   * Returns the primary threshold used to binarize the image.
   * 
   * @return the thresholds
   */
  public int getThreshold() {
    return thresholds.isEmpty() ? AUTO_THRESHOLD : thresholds.get(0);
  }

  /**
   * The limit of found barcodes after which the detection stops and returns the already detected
   * barcode results.
   * 
   * Default: {@link NO_BARCODELIMIT}
   * 
   * @return the barcodeLimit
   */
  public int getBarcodeCountLimit() {
    return barcodeCountLimit;
  }
}
