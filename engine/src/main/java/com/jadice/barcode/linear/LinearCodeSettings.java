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
package com.jadice.barcode.linear;

import java.util.Arrays;

import com.jadice.barcode.Settings;


/**
 * {@link Settings} partaining to all types of linear (1D) bar codes.
 */
public class LinearCodeSettings implements Settings {
  /**
   * A compass-style enum of directions used to determine the orientations which will be considered
   * during bar code detection.
   */
  public enum Direction {
    NORTH, SOUTH, EAST, WEST
  }

  /** The detection directions to try: east, north, west, south */
  private final boolean directionsToTry[] = new boolean[Direction.values().length];


  /** The initial scan interval to use */
  private int scanInterval = 5;

  private boolean requireValidChecksum = false;

  /**
   * The tolerance to apply to the quiet zones. A tolerance of 100 means that the quiet zone must at
   * least match the size from the code's specification. Tolerances less than 100 can be used to
   * relax the quiet zone checks.
   * 
   * Useful values fall in the range [~30, 150]
   * 
   * Default: 80
   */
  private int quietZoneTolerance = 80;

  /**
   * The radius within which edge detections may be considered to belong to the same edge, expressed
   * in percent of the quiet zone. Setting this to small values may help separate codes which are
   * printed close together.
   * 
   * Although named 'radius' the actual shape is usually a rectangle for simplicity's sake.
   * 
   * Useful values fall in the range [~5, 150]
   * 
   * Default: 90
   */
  private int confidenceRadius = 90;

  /**
   * The bar width tolerance specifies the latitude the detection allows for the mis-match between
   * bars of the same color. The bar width tolerance is expressed relative to a unit width. A value
   * of 50 means that bars may be 50% units wider/narrower to still be recognized as the same unit
   * width.
   * 
   * Useful values fall in the range [0, 80]
   * 
   * Default: 60
   */
  private int barWidthTolerance = 60;

  public LinearCodeSettings() {
    Arrays.fill(directionsToTry, false);
    directionsToTry[Direction.EAST.ordinal()] = true;
  }

  /**
   * Set whether to enable scanning in the given direction. Reducing the set of directions reduces
   * detection time as well as the risk of false positives.
   * 
   * Default: all directions will be tried
   * 
   * @param direction
   * @param enabled
   */
  public void setDirectionEnabled(Direction direction, boolean enabled) {
    directionsToTry[direction.ordinal()] = enabled;
  }

  /**
   * Return whether the given direction is enabled.
   * 
   * @param direction
   * @return
   */
  public boolean isDirectionEnabled(Direction direction) {
    return directionsToTry[direction.ordinal()];
  }

  /**
   * Set whether a valid check sum is required for a bar code detection. This applies to symbologies
   * supporting a check sum only. If set to <code>true</code>, only {@link Results} with valid check
   * sums are returned. Otherwise all Results are returned, however each Result will still indicate
   * whether its check sum is valid.
   * 
   * Default: <code>false</code>
   * 
   * @param requireValidChecksum
   */
  public void setRequireValidChecksum(boolean requireValidChecksum) {
    this.requireValidChecksum = requireValidChecksum;
  }

  /**
   * Return whether a valid check sum is required for a bar code detection.
   * 
   * @return
   */
  public boolean isRequireValidChecksum() {
    return requireValidChecksum;
  }

  /**
   * Set the interval at which to perform initial scans. Initial scans are performed every
   * <code>scanInterval</code> scan lines. Increasing this interval will reduce scanning/detection
   * time, but, especially with low-quality images, the risk of false negatives increases as well.
   * 
   * Default: 5
   * 
   * @param scanInterval
   */
  public void setScanInterval(int scanInterval) {
    this.scanInterval = scanInterval;
  }

  /**
   * Return the initial scan interval.
   * 
   * @return
   */
  public int getScanInterval() {
    return scanInterval;
  }

  /**
   * Return the tolerance with respect to quiet zones.
   * 
   * @return the quietZoneTolerance
   */
  public int getQuietZoneTolerance() {
    return quietZoneTolerance;
  }

  /**
   * Set the tolerance with respect to quiet zones. A tolerance of 100 means that the quiet zone
   * must at least match the size from the code's specification. Tolerances less than 100 can be
   * used to relax the quiet zone checks.
   * 
   * Useful values fall in the range [~30, 150]
   * 
   * Default: 80
   * 
   * @param quietZoneTolerance the quietZoneTolerance to set
   */
  public void setQuietZoneTolerance(int quietZoneTolerance) {
    if (quietZoneTolerance < 10 || quietZoneTolerance > 150)
      throw new IllegalArgumentException("The quiet zone tolerance must be within [10,150]");

    this.quietZoneTolerance = quietZoneTolerance;
  }

  /**
   * @return the barWidthTolerance
   */
  public int getBarWidthTolerance() {
    return barWidthTolerance;
  }

  /**
   * Set the bar width tolerance. It specifies the latitude the detection allows for the mis-match
   * between bars of the same color. The bar width tolerance is expressed relative to a unit width.
   * A value of 50 means that bars may be 50% units wider/narrower to still be recognized as the
   * same unit width.
   * 
   * Useful values fall in the range [0, 80]
   * 
   * Default: 60
   * 
   * @param barWidthTolerance the barWidthTolerance to set
   */
  public void setBarWidthTolerance(int barWidthTolerance) {
    if (barWidthTolerance < 0 || barWidthTolerance > 80)
      throw new IllegalArgumentException("The bar width tolerance must be within [0,80]");

    this.barWidthTolerance = barWidthTolerance;
  }

  /**
   * @return the confidenceRadius
   */
  public int getConfidenceRadius() {
    return confidenceRadius;
  }

  /**
   * @param confidenceRadius the confidenceRadius to set
   */
  public void setConfidenceRadius(int confidenceRadius) {
    this.confidenceRadius = confidenceRadius;
  }
}
