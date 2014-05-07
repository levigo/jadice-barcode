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
package com.jadice.barcode.grid;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.Collection;

import com.jadice.barcode.Options;
import com.jadice.barcode.linear.LinearCodeSettings;
import com.jadice.barcode.linear.LinearCodeSettings.Direction;


/**
 * {@link BinaryGrid} that rotates an image in order to detect barcodes in in any of the four
 * orientations.
 * 
 * @see Options.Direction
 * 
 * @author Jörg Henne
 * @author Tobias Jenkner
 * 
 */
public class QuadrantRotationGrid implements BinaryGrid {
  /**
   * Factory method that creates <code>RotatingImageDataSources</code> for every orientation that is
   * enabled in {@link Options#directionsToTry} based on the {@link BinaryGrid}s contained in
   * <i>delegates</i>.
   * 
   * @param options - specify the orientations
   * @param sources - imagedatasources that will be wrapped through the created
   *          <code>RotatingImageDataSource</code>.
   * @return a collection of RotatingImageDataSource
   */
  public static Collection<QuadrantRotationGrid> createRotationImageDataSources(Options options,
      Collection<BinaryGrid> sources) {

    Collection<QuadrantRotationGrid> rotatingSources = new ArrayList<QuadrantRotationGrid>();
    for (LinearCodeSettings.Direction d : LinearCodeSettings.Direction.values())
      if (options.getSettings(LinearCodeSettings.class).isDirectionEnabled(d))
        for (BinaryGrid delegate : sources)
          rotatingSources.add(new QuadrantRotationGrid(delegate, d));

    return rotatingSources;
  }

  private final BinaryGrid delegate;
  private Dimension translatedSize = new Dimension();
  private final Direction rotation;

  private final int w;
  private final int h;

  private final AffineTransform transform;
  private AffineTransform inverseTransform;

  /**
   * @param delegate - The {@link BinaryGrid} that provides access to the image data.
   * @param rotation - One of {@link Options#DIRECTION_EAST}, {@link Options#DIRECTION_NORTH},
   *          {@link Options#DIRECTION_WEST}, {@link Options#DIRECTION_SOUTH}.
   * 
   */
  public QuadrantRotationGrid(BinaryGrid delegate, Direction rotation) {
    this.delegate = delegate;
    this.rotation = rotation;
    if (rotation == LinearCodeSettings.Direction.NORTH || rotation == LinearCodeSettings.Direction.SOUTH)
      this.translatedSize = new Dimension(delegate.getHeight(), delegate.getWidth());
    else
      translatedSize = new Dimension(delegate.getWidth(), delegate.getHeight());
    this.w = translatedSize.width;
    this.h = translatedSize.height;

    // create forward transform (image -> detection space)
    transform = new AffineTransform();
    switch (rotation){
      case EAST :
        break; // nothing to do

      case NORTH :
        transform.rotate(Math.PI / 2);
        transform.translate(0, -delegate.getHeight());
        break;

      case WEST :
        transform.rotate(Math.PI);
        transform.translate(-delegate.getWidth(), -delegate.getHeight());
        break;

      case SOUTH :
        transform.rotate(-Math.PI / 2);
        transform.translate(-delegate.getWidth(), 0);
        break;

      default :
        throw new IllegalArgumentException("Unsupported rotation angle");
    }

    // build inverse transform (detection -> image space)
    try {
      inverseTransform = transform.createInverse();
    } catch (NoninvertibleTransformException e) {
      // can't happen...
    }
  }

  /*
   * @see com.levigo.barcode.ImageDataSource#samplePixel(int, int)
   */
  public boolean samplePixel(int x, int y) {
    switch (rotation){
      case EAST :
        return delegate.samplePixel(x, y);
      case NORTH :
        return delegate.samplePixel(y, w - x - 1);
      case WEST :
        return delegate.samplePixel(w - x - 1, h - y - 1);
      case SOUTH :
        return delegate.samplePixel(h - y - 1, x);
    }
    return false;
  }

  public AffineTransform getInverseTransform() {
    return inverseTransform;
  }

  public int getWidth() {
    return translatedSize.width;
  }

  public int getHeight() {
    return translatedSize.height;
  }

  public int getPixelLuminance(int x, int y) {
    return samplePixel(x, y) ? 0 : 255;
  }
}