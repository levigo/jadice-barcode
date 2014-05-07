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
package com.jadice.barcode.j2d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Collection;
import java.util.Set;

import com.jadice.barcode.Marker;
import com.jadice.barcode.Marker.Feature;
import com.jadice.barcode.Result;
import com.jadice.barcode.grid.BinaryGrid;

/**
 * A collection of static utility methods used in conjunction with Java2D-based environments.
 */
public class Java2DUtils {
  private static final int DEBUG_ALPHA = 200;

  /**
   * Paint debug-{@link Marker}s into a Java2D {@link Graphics2D} context.
   * 
   * @param g2 the target graphics context
   * @param debugMarkers the collection of debug markers
   * @param enabledFeatures the {@link Feature}s to paint
   */
  public static void paintDebugMarkers(Graphics2D g2, Collection<Marker> debugMarkers, Set<Feature> enabledFeatures) {
    // create array of enabled flags for quick access
    boolean enableFlags[] = new boolean[Feature.values().length];
    for (Feature feature : enabledFeatures)
      enableFlags[feature.ordinal()] = true;

    Color previous = null;
    for (Marker m : debugMarkers) {
      if (!enableFlags[m.getFeature().ordinal()])
        continue;

      Color color = m.getFeature().color;
      if (color != previous) {
        previous = color;
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), DEBUG_ALPHA));
      }

      final Shape s = m.getShape();
      g2.draw(s);
      // g2.fill(s);
    }
  }

  private static final Color CODE_AREA_COLOR = new Color(80, 80, 255, 80);

  private static final Color VALID_CODE_BACKGROUND = new Color(0, 255, 0, 240);
  private static final Color INVALID_CODE_BACKGROUND = new Color(255, 0, 0, 240);

  /**
   * Paint {@link Result}s ainto a Java2D {@link Graphics2D} context.
   * 
   * @param g2 the graphics context
   * @param results the {@link Result}s to paint
   */
  public static void paintCodeMarkers(Graphics2D g2, Collection<Result> results) {
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    for (Result r : results) {
      String code = r.getSymbology().name() + ": " + r.getCodeString();
      Rectangle2D stringBounds = g2.getFontMetrics().getStringBounds(code, g2);
      Rectangle shapeBounds = r.getShape().getBounds();

      g2.setColor(CODE_AREA_COLOR);
      g2.fill(r.getShape());

      double textCenterX = shapeBounds.getCenterX();
      double textCenterY = shapeBounds.getCenterY();

      AffineTransform current = g2.getTransform();
      g2.translate(textCenterX, textCenterY);
      g2.rotate(-r.getAngle());

      g2.setColor(r.isChecksumOK() ? VALID_CODE_BACKGROUND : INVALID_CODE_BACKGROUND);
      g2.fillRect((int) (-stringBounds.getWidth() / 2) - 1, (int) (-stringBounds.getHeight() / 2),
          (int) stringBounds.getWidth() + 2, (int) stringBounds.getHeight());

      g2.setColor(Color.black);
      g2.drawString(code, (int) (-stringBounds.getWidth() / 2 - (int) stringBounds.getX()),
          (int) (-stringBounds.getHeight() / 2 - (int) stringBounds.getY()));

      g2.setTransform(current);
    }
  }

  /**
   * Create a Java2D {@link BufferedImage} from a given {@link BinaryGrid}. This method is designed
   * for verification and debugging purposes.
   * 
   * @param grid
   * @return
   */
  public static BufferedImage createBinaryGridImage(BinaryGrid grid) {
    BufferedImage img = new BufferedImage(grid.getWidth(), grid.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

    extractPixels(grid, 0, 0, grid.getWidth(), grid.getHeight(),
        ((DataBufferByte) img.getRaster().getDataBuffer()).getData());

    return img;
  }

  private static void extractPixels(BinaryGrid grid, int x0, int y0, int width, int height, byte dst[]) {
    for (int y = 0; y < height; y++)
      for (int x = 0; x < width; x++)
        if (!grid.samplePixel(x, y))
          dst[(y + y0) * width + x + x0] = (byte) 0xff;
  }
}
