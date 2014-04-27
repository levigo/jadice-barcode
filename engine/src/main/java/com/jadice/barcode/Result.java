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
import java.awt.Shape;
import java.awt.geom.AffineTransform;

/**
 * A Result represents a single detected and decoded bar code.
 */
public class Result {
  /** The code which produced the result */
  private final Symbology symbology;

  /** The shape of the detection area */
  private Shape shape = new Rectangle();

  /** The detected code string */
  private final String codeString;

  /** Whether the code had a checksum */
  private final boolean hasChecksum;

  /** Whether the checksum was Ok */
  private final boolean checksumOK;

  /** The angle at which the code was read */
  private double angle;

  public Result(Symbology symbology, Shape shape, String codeString, boolean hasChecksum, boolean checksumOK,
      double angle) {
    this.shape = shape;
    this.symbology = symbology;
    this.codeString = codeString;
    this.hasChecksum = hasChecksum;
    this.checksumOK = checksumOK;
    this.angle = angle;
  }

  /**
   * Returns the shape of the detection.
   * 
   * @return
   */
  public Shape getShape() {
    return shape;
  }

  /**
   * Returns whether the detection is valid.
   * 
   * @return
   */
  public boolean isValid() {
    return true;
  }

  /**
   * Returns whether the checksum for the detection was OK.
   * 
   * @return
   */
  public boolean isChecksumOK() {
    return checksumOK;
  }

  /**
   * Returns the code the given detection was for.
   * 
   * @return
   */
  public Symbology getSymbology() {
    return symbology;
  }

  /**
   * Returns the detected code string.
   * 
   * @return
   */
  public String getCodeString() {
    return codeString;
  }

  /**
   * Return, whether the code has a checksum.
   * 
   * @return
   */
  public boolean isHasCheckSum() {
    return hasChecksum;
  }

  /**
   * Return the angle at which the code was detected.
   * 
   * @return
   */
  public double getAngle() {
    return angle;
  }

  /**
   * Rotate the result by the given angle.
   * 
   * @param d
   */
  protected void rotate(double angle, double x, double y) {
    this.angle = (this.angle + angle) % (2 * Math.PI);
    shape = AffineTransform.getRotateInstance(angle, x, y).createTransformedShape(shape);
  }

  protected void translate(int dx, int dy) {
    shape = AffineTransform.getTranslateInstance(dx, dy).createTransformedShape(shape);
  }

  /**
   * @param inverseTransform
   */
  public void transform(AffineTransform transform) {
    shape = transform.createTransformedShape(shape);

    // the following calculation assumes a transform without shearing
    final double transformAngle = Math.atan2(transform.getShearX(), transform.getScaleX());
    angle = (angle + transformAngle) % (2 * Math.PI);
  }

  @Override
  public String toString() {
    return "DetectionResult [" + symbology + ": " + codeString + "; " + shape.getBounds() + "; " + angle + "]";
  }
}
