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
/**
 * 
 */
package com.jadice.barcode;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

/**
 * Markers are used to convey diagnostic information about the decoding process.
 */
public class Marker {
  /**
   * A Feature specifies the particular type of marker.
   */
  public enum Feature {
    START(Color.BLUE), //
    START_EDGE(new Color(100, 100, 255)), //
    START_RESCAN(new Color(100, 0, 255)), //
    STOP(Color.RED), //
    STOP_EDGE(new Color(255, 100, 100)), //
    STOP_RESCAN(new Color(255, 100, 0)), //
    EDGE(Color.GREEN), //
    EDGE_ASSOCIATION(Color.MAGENTA), //
    METHOD2(Color.DARK_GRAY), //
    HISTOGRAM(Color.CYAN), //
    HISTOGRAM_THRESHOLD(Color.BLACK), //
    SCAN(Color.LIGHT_GRAY), //
    INITIAL_SCAN(new Color(120, 120, 255)), //
    DETECTION_SCAN(new Color(255, 120, 120)), //
    INVALID_EDGE(new Color(120, 120, 120)), //
    SINGLETON_EDGE(new Color(120, 255, 120));

    public final Color color;

    private Feature(Color color) {
      this.color = color;
    }
  }

  private final Marker.Feature feature;
  private Shape shape;

  public Marker(Marker.Feature feature, Shape shape) {
    super();
    this.feature = feature;
    this.shape = shape;
  }

  public void transform(AffineTransform transform) {
    if (transform != null)
      shape = transform.createTransformedShape(shape);
  }

  /**
   * @return the shape
   */
  public Shape getShape() {
    return shape;
  }

  /**
   * @param shape the shape to set
   */
  public void setShape(Shape shape) {
    this.shape = shape;
  }

  /**
   * @return the feature
   */
  public Marker.Feature getFeature() {
    return feature;
  }
}