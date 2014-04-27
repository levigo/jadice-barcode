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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jadice.barcode.Marker.Feature;

/**
 * Instances of this class are used during the decoding process in order to collect diagnostic
 * information used to debug the decoding process.
 */
public class DiagnosticSettings implements Settings {
  public interface TransientMarkerListener {
    void markersChanged();
  }

  private boolean markupEnabled;

  private final List<Marker> debugMarkers = new ArrayList<Marker>();

  private final Queue<Marker> transientMarkers = new ConcurrentLinkedQueue<Marker>();

  private final List<TransientMarkerListener> listeners = new CopyOnWriteArrayList<DiagnosticSettings.TransientMarkerListener>();

  /**
   * @return the debugSymbols
   */
  public List<Marker> getDebugMarkers() {
    return debugMarkers;
  }

  public void addPoint(Feature f, int x, int y) {
    debugMarkers.add(new Marker(f, new Rectangle(x - 1, y - 1, 2, 2)));
  }

  public void addLine(Feature f, double x0, double y0, double x1, double y1) {
    debugMarkers.add(new Marker(f, new Line2D.Double(x0, y0, x1, y1)));
  }

  public void addLine(Feature f, int x0, int y0, int x1, int y1) {
    debugMarkers.add(new Marker(f, new Line2D.Float(x0, y0, x1, y1)));
  }

  public void add(Feature f, Point p) {
    debugMarkers.add(new Marker(f, new Rectangle(p.x - 1, p.y - 1, 2, 2)));
  }

  public void add(Shape s, Feature f) {
    debugMarkers.add(new Marker(f, s));
  }

  public void addLine(Feature feature, Point from, Point to) {
    debugMarkers.add(new Marker(feature, new Line2D.Float(from, to)));
  }

  public void addTransientMarkerListener(TransientMarkerListener l) {
    listeners.add(l);
  }

  public void removeTransientMarkerListener(TransientMarkerListener l) {
    listeners.remove(l);
  }

  private void markersChanged() {
    for (TransientMarkerListener l : listeners)
      l.markersChanged();
  }

  public Marker addTransientPoint(Feature f, int x, int y) {
    Marker m = new Marker(f, new Rectangle(x - 1, y - 1, 2, 2));
    getTransientMarkers().add(m);
    markersChanged();
    return m;
  }


  public Marker addTransientLine(Feature f, double x0, double y0, double x1, double y1) {
    Marker m = new Marker(f, new Line2D.Double(x0, y0, x1, y1));
    getTransientMarkers().add(m);
    markersChanged();
    return m;
  }

  public Marker addTransientLine(Feature f, int x0, int y0, int x1, int y1) {
    Marker m = new Marker(f, new Line2D.Float(x0, y0, x1, y1));
    getTransientMarkers().add(m);
    markersChanged();
    return m;
  }

  public Marker addTransient(Feature f, Point p) {
    Marker m = new Marker(f, new Rectangle(p.x - 1, p.y - 1, 2, 2));
    getTransientMarkers().add(m);
    markersChanged();
    return m;
  }

  public Marker addTransient(Shape s, Feature f) {
    Marker m = new Marker(f, s);
    getTransientMarkers().add(m);
    markersChanged();
    return m;
  }

  public Marker addTransientLine(Feature feature, Point from, Point to) {
    Marker m = new Marker(feature, new Line2D.Float(from, to));
    getTransientMarkers().add(m);
    markersChanged();
    return m;
  }

  public void removeTransient(Marker m) {
    getTransientMarkers().remove(m);
    markersChanged();
  }

  public void merge(DiagnosticSettings r, AffineTransform transform) {
    for (final Marker m : r.debugMarkers) {
      m.transform(transform);
      debugMarkers.add(m);
    }
  }

  public boolean isMarkupEnabled() {
    return markupEnabled;
  }

  public void setMarkupEnabled(boolean markupEnabled) {
    this.markupEnabled = markupEnabled;
  }

  public Queue<Marker> getTransientMarkers() {
    return transientMarkers;
  }
}
