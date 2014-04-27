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
package com.jadice.barcode.grid.j2d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.jadice.barcode.Marker;
import com.jadice.barcode.Result;
import com.jadice.barcode.Marker.Feature;

/**
 * @author jh
 */
public class Java2DResultPainter {
  private static final int DEBUG_ALPHA = 200;

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

  public static void paintCodeMarkers(Graphics2D g2, List<Result> results) {
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
}
