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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.jadice.barcode.DiagnosticSettings;
import com.jadice.barcode.Marker;
import com.jadice.barcode.Options;
import com.jadice.barcode.Result;
import com.jadice.barcode.grid.BinaryGrid;
import com.jadice.barcode.linear.OneDDecoder.EdgeDetection;

/**
 * @author jh
 */
public class Edge {
  private static final Logger logger = Logger.getLogger(Edge.class);

  private static final int MIN_HITS_THRESHOLD = 3;
  private static final double MAX_ANGULAR_MISMATCH = 10.0; // degrees
  private static final double EDGE_WIDTH_TOLERANCE = .25;
  private static final double UNIT_WIDTH_TOLERANCE = .25;
  private static final double MAX_CENTER_ANGLE_DIFFERENCE = 40.0;
  private static final double MAX_NORMALIZED_PROJECTION_DISTANCE = 10.0;
  @SuppressWarnings("unused")
  private static final float OVERPRINT_TOLERANCE = .25f;

  private final int moduleWidth;

  private final boolean isBlack;

  /** The rectangle against which to test new hits */
  private final Rectangle confidenceRectangle;

  /** The fuzz factor (i.e. tolerance) with which to accept hits */
  private int quietZoneWidth = 0;

  /** Whether this is a start/stop edge */
  private final boolean isStartEdge;

  /** The collection of detection hits */
  private final List<EdgeDetection> hits = new ArrayList<EdgeDetection>();

  private float avgWidth = 0;

  /** This edge's partner edge */
  private Edge partner;

  private float gradient;

  private float offset;

  private Line2D line;

  private final int width;
  private float avgOverprint = Float.NaN;
  @SuppressWarnings("unused")
  private final Options options;
  private final int confidenceZoneSize;

  private final boolean enableDiagMarkup;

  private final DiagnosticSettings diagSettings;

  /**
   * Construct a new edge based on a hit.
   * 
   * @param options
   * 
   * @param x
   * @param y
   * @param totalWidthInWindow
   * @param isStartCode
   * @param quietZoneWidth
   * @param ed
   */
  Edge(Options options, int moduleWidth, boolean isStartCode, boolean isBlack, int quietZoneWidth, EdgeDetection ed) {
    this.options = options;
    isStartEdge = isStartCode;
    this.isBlack = isBlack;
    this.quietZoneWidth = quietZoneWidth;
    width = ed.width;
    this.moduleWidth = moduleWidth;

    confidenceZoneSize = width * options.getSettings(LinearCodeSettings.class).getConfidenceRadius() * 2 / 100;

    diagSettings = options.getSettings(DiagnosticSettings.class);
    enableDiagMarkup = diagSettings.isMarkupEnabled();

    confidenceRectangle = new Rectangle(ed.x - confidenceZoneSize / 2, ed.y - confidenceZoneSize / 2,
        confidenceZoneSize, confidenceZoneSize);

    hits.add(ed);
  }

  /**
   * @param x
   * @param y
   * @param ed TODO
   * @param totalWidthInWindow
   * @param isStartCode
   * @return
   */
  boolean testDetection(EdgeDetection ed) {
    final float d = (float) ed.width / width;
    final float abs = Math.abs(1f - d);
    if (abs > EDGE_WIDTH_TOLERANCE)
      return false;

    // if (Math.abs(getOverprintEstimate() - ed.overprint) >
    // OVERPRINT_TOLERANCE)
    // return false;

    if (confidenceRectangle.contains(ed.x, ed.y)) {
      hits.add(ed);

      confidenceRectangle.add(new Rectangle(ed.x - confidenceZoneSize / 2, ed.y - confidenceZoneSize / 2,
          confidenceZoneSize, confidenceZoneSize));

      avgWidth = -1;
      avgOverprint = Float.NaN;
      return true;
    } else
      return false;
  }

  float getOverprintEstimate() {
    if (Float.isNaN(avgOverprint)) {
      float sum = 0f;
      for (final EdgeDetection d : hits)
        sum += d.overprint;
      avgOverprint = sum / hits.size();
    }

    return avgOverprint;
  }

  float getAverageWidth() {
    if (avgWidth < 0) {
      int sum = 0;
      for (final EdgeDetection d : hits)
        sum += d.width;
      avgWidth = (float) sum / hits.size();
    }

    return avgWidth;
  }

  /**
   * @return
   */
  Edge getPartner() {
    return partner;
  }

  /**
   * @param edge
   */
  void setPartner(Edge edge) {
    partner = edge;
  }

  /**
   * Post-process the hits. Update offset and gradient etc. Return whether the edge seems to be
   * sane.
   */
  boolean pruneHits() {
    if (hits.size() < MIN_HITS_THRESHOLD)
      return false;

    // invalidate cached stuff
    line = null;

    // perform first rough linear curve fitting of hits.
    // since the gradient is expected to be almost vertical, we interpolate
    // y over x.
    // all our confidence values are equal.

    // calculate a few sums
    int numberOfPoints = hits.size();
    float sumX = 0;
    float sumY = 0;
    float sumXY = 0;
    @SuppressWarnings("unused")
    float sumXSQ = 0;
    float sumYSQ = 0;

    for (final EdgeDetection hit : hits) {
      sumX += hit.x;
      sumY += hit.y;
      sumXY += hit.x * hit.y;
      sumXSQ += hit.x * hit.x;
      sumYSQ += hit.y * hit.y;
    }

    // calculate first fit
    gradient = (numberOfPoints * sumXY - sumX * sumY) / (numberOfPoints * sumYSQ - sumY * sumY);
    offset = (sumX * sumYSQ - sumY * sumXY) / (numberOfPoints * sumYSQ - sumY * sumY);

    final float unitWidth = getAverageWidth() / moduleWidth;
    final float distanceThreshold = unitWidth;
    do {
      final float divident = (float) Math.sqrt(gradient * gradient + 1);
      float maxDistance = 0.0f;
      EdgeDetection maxDistanceHit = null;
      for (final EdgeDetection hit : hits) {
        final float distance = Math.abs((gradient * hit.y - hit.x + offset) / divident);
        if (distance > maxDistance) {
          maxDistance = distance;
          maxDistanceHit = hit;
        }
      }

      // check max distance.
      if (maxDistance > distanceThreshold) {
        // dump it!
        sumX -= maxDistanceHit.x;
        sumY -= maxDistanceHit.y;
        sumXY -= maxDistanceHit.x * maxDistanceHit.y;
        sumXSQ -= maxDistanceHit.x * maxDistanceHit.x;
        sumYSQ -= maxDistanceHit.y * maxDistanceHit.y;
        numberOfPoints--;

        // update fit
        gradient = (numberOfPoints * sumXY - sumX * sumY) / (numberOfPoints * sumYSQ - sumY * sumY);
        offset = (sumX * sumYSQ - sumY * sumXY) / (numberOfPoints * sumYSQ - sumY * sumY);

        hits.remove(maxDistanceHit);

        avgWidth = -1;
        avgOverprint = Float.NaN;
      } else
        break;
    } while (hits.size() > MIN_HITS_THRESHOLD);

    return hits.size() > MIN_HITS_THRESHOLD;
  }

  /*
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (null == line)
      return "Incomplete edge";

    final Point2D p1 = line.getP1();
    final Point2D p2 = line.getP2();
    return "Edge " + (isStartEdge ? "start " : "stop ") + p1.getX() + "/" + p1.getY() + "-" + p2.getX() + "/"
        + p2.getY();
  }

  /**
   * @return
   */
  boolean isStartEdge() {
    return isStartEdge;
  }

  /**
   * Calculate a confidence value with which the given partner edge is a possible partner for this
   * edge.
   * 
   * @return
   */
  int getPartnerConfidence(Edge possiblePartner) {
    final int confidence = Integer.MIN_VALUE;

    // check gradient. Don't accept more than a three-degree mismatch
    final double angularMismatch = Math.abs(Math.atan(possiblePartner.gradient) - Math.atan(gradient));
    if (angularMismatch * 360.0 / (2 * Math.PI) > MAX_ANGULAR_MISMATCH)
      return confidence;

    // check that the edges are not reversed
    final Point2D partnerEdgeCenter = possiblePartner.getEdgeCenter();
    final Point2D edgeCenter = getEdgeCenter();
    if (partnerEdgeCenter.getX() - edgeCenter.getX() < 0)
      return confidence;

    // projection distances
    // distance of partner's center to intersection with my normal
    final double distance1 = getProjectionDistance(this, possiblePartner);
    // distance of my center to intersection with my partner's normal
    final double distance2 = getProjectionDistance(possiblePartner, this);

    // center distance
    final double centerDistance = partnerEdgeCenter.distance(edgeCenter);

    // if center angle and normal angle differ too much -> reject
    final double centerAngle = Math.atan((edgeCenter.getY() - partnerEdgeCenter.getY())
        / (partnerEdgeCenter.getX() - edgeCenter.getX()))
        * 360.0 / (2 * Math.PI);

    if (Math.abs(Math.atan(gradient) * 360.0 / (2 * Math.PI) - centerAngle) > MAX_CENTER_ANGLE_DIFFERENCE)
      return confidence;

    // test unit width of edges
    final double widthRatio = getAverageWidth() / moduleWidth
        / (possiblePartner.getAverageWidth() / possiblePartner.moduleWidth);

    if (Math.abs(1.0 - widthRatio) > UNIT_WIDTH_TOLERANCE)
      return confidence;

    final double myLength = getLine().getP1().distance(getLine().getP2());
    final double possiblePartnerLength = possiblePartner.getLine().getP1().distance(possiblePartner.getLine().getP2());
    final double normalizedProjectionDistance = (distance1 + distance2) / (myLength + possiblePartnerLength);
    if (normalizedProjectionDistance > MAX_NORMALIZED_PROJECTION_DISTANCE)
      return confidence;

    logger.debug(toString() + "\n" + possiblePartner + "\nam=" + angularMismatch + " d1=" + distance1 + " d2="
        + distance2 + " cad=" + Math.abs(Math.atan(gradient) * 360.0 / (2 * Math.PI) - centerAngle) + " wr="
        + widthRatio + " fooFactor=" + normalizedProjectionDistance + " conf="
        + (-centerDistance - distance1 * distance1 - distance2 * distance2));

    return (int) (-centerDistance - distance1 * distance1 - distance2 * distance2) + hits.size();
  }

  private double getProjectionDistance(Edge edge1, Edge edge2) {
    final double m2 = edge2.gradient;
    final double b2 = edge2.offset;

    Point2D intersection;
    if (edge1.gradient == 0)
      intersection = new Point2D.Double(m2 * edge1.getEdgeCenter().getY() + b2, edge1.getEdgeCenter().getY());
    else {
      final Point2D p1 = edge1.getEdgeCenter();
      final double m1 = -1.0 / edge1.gradient;
      final double b1 = p1.getX() - m1 * p1.getY();

      final double y = (b2 - b1) / (m1 - m2);
      intersection = new Point2D.Double(m1 * y + b1, y);
    }
    return edge2.getEdgeCenter().distance(intersection);
  }

  /**
   * @return
   */
  private Point2D getEdgeCenter() {
    final Line2D l = getLine();
    return new Point2D.Double((l.getX1() + l.getX2()) / 2, (l.getY1() + l.getY2()) / 2);
  }

  /**
   * @return
   */
  public Line2D getLine() {
    if (null == line) {
      final EdgeDetection[] extremeHits = getExtremeHits();
      line = new Line2D.Double(new Point(extremeHits[0].x, extremeHits[0].y), new Point(extremeHits[1].x,
          extremeHits[1].y));
    }
    return line;
  }

  /**
   * @param code
   * @param image
   * @param results
   * @param options TODO
   * @param results TODO
   */
  public boolean reScan(OneDDecoder code, BinaryGrid image, Options detection, List<Result> results) {
    if (!isStartEdge)
      return false;

    final OneDDecoder.CodeGeometry geometry = code.getCodeGeometry();

    // find minimum and maximum y's and remember the corresponding hits
    final EdgeDetection p1[] = getExtremeHits();
    final EdgeDetection p2[] = partner.getExtremeHits();

    final int barWidths[] = new int[image.getWidth()];

    // FIXME: the diagonal scans don't work because of the
    // incorrect widths in reScanStart/reScanStop (see FIXMEs there)

    // we scan start/stop four-times in order to achieve the maximum possible
    // coverage: up/down and linear/cross-wise.
    // start
    reScanStart(code, image, geometry, p1[1], p2[1], barWidths, true, detection, results);
    reScanStart(code, image, geometry, p1[0], p2[0], barWidths, false, detection, results);
    // reScanStart(code, image, geometry, p1[1], p2[0], barWidths, true);
    // reScanStart(code, image, geometry, p1[0], p2[1], barWidths, false);

    // stop
    reScanStop(code, image, geometry, p1[1], p2[1], barWidths, true, detection, results);
    reScanStop(code, image, geometry, p1[0], p2[0], barWidths, false, detection, results);
    // reScanStop(code, image, geometry, p1[0], p2[1], barWidths, true);
    // reScanStop(code, image, geometry, p1[1], p2[0], barWidths, false);

    // re-prune, in order to update gradient etc.
    return pruneHits() && partner.pruneHits();
  }

  private void reScanStart(OneDDecoder code, BinaryGrid image, OneDDecoder.CodeGeometry geometry, EdgeDetection d1,
      EdgeDetection d2, int[] barWidths, boolean up, Options detection, List<Result> results) {

    final Point p1 = new Point(d1.x, d1.y);
    final Point p2 = new Point(d2.x, d2.y);

    final int increment = up ? 1 : -1;
    final int f = confidenceZoneSize / 2 * increment;
    int yFuzz = p1.y + f;
    for (; up ? p1.y <= yFuzz : p1.y >= yFuzz; p1.y += increment) {
      p1.x = (int) (gradient * p1.y + offset);

      // position x right before the start/end of the bar
      while (isBlack != image.samplePixel(p1.x, p1.y) && p1.x < image.getWidth())
        p1.x++;
      while (isBlack == image.samplePixel(p1.x, p1.y) && p1.x >= 0)
        p1.x--;

      if (p1.x >= p2.x)
        continue;

      // scan
      final int barCount = code.scanBars(image, barWidths, p1, p2);

      // try to detect start/stop
      if (isStartEdge && barCount > geometry.startCodeWindowSize) {
        final EdgeDetection ed = code.detectStart(barWidths, 1, barCount, p1.x, p1.y);
        if (ed != null) {
          if (enableDiagMarkup)
            diagSettings.addLine(Marker.Feature.START_RESCAN, p1, p2);
          // we fake the width here, because the scan might run diagonally.
          // this distorts the bar widths which causes them to fail the
          // detection test. we have, however, a rather good confidence
          // that this is actually a good scan during rescan, so that we
          // can "assume" the "correct" width here.
          // FIXME:
          testDetection(ed);
          yFuzz = p1.y + f;
        }
      }
    }
  }

  private void reScanStop(OneDDecoder code, BinaryGrid image, OneDDecoder.CodeGeometry geometry, EdgeDetection d1,
      EdgeDetection d2, int[] barWidths, boolean up, Options detection, List<Result> results) {

    final Point p1 = new Point(d1.x, d1.y);
    final Point p2 = new Point(d2.x, d2.y);

    final int increment = up ? 1 : -1;
    final int f = confidenceZoneSize / 2 * increment;
    int yFuzz = p2.y + f;
    for (; up ? p2.y <= yFuzz : p2.y >= yFuzz; p2.y += increment) {
      p2.x = (int) (partner.gradient * p2.y + partner.offset);
      p2.x = Math.min(p2.x, image.getWidth());

      // position x right before/after the start/end of the bar
      while (isBlack != image.samplePixel(p2.x, p2.y) && p2.x > 0)
        p2.x--;
      while (isBlack == image.samplePixel(p2.x, p2.y) && p2.x < image.getWidth())
        p2.x++;

      if (p2.x < p1.x)
        continue;

      // scan
      final int barCount = code.scanBars(image, barWidths, p1, p2);

      // try to detect start/stop
      boolean detectedSomething = false;
      if (isStartEdge && barCount > geometry.stopCodeWindowSize) {
        EdgeDetection ed = code.detectStop(
            barWidths,
            barCount - geometry.stopCodeWindowSize - 1,
            barCount,
            p2.x
                - code.sumWidths(barWidths, barCount - geometry.stopCodeWindowSize - 1, geometry.stopCodeWindowSize, 1),
            p2.y);
        detectedSomething = ed != null;

        // try again at one bar farther, we might be off by one in rare cases!
        if (!detectedSomething) {
          ed = code.detectStop(barWidths, barCount - geometry.stopCodeWindowSize,
              code.sumWidths(barWidths, barCount - geometry.stopCodeWindowSize, geometry.stopCodeWindowSize, 1), p2.x,
              p2.y);
          detectedSomething = ed != null;
          if (detectedSomething)
            logger.debug("Was off by one in Edge.reScanStop");
        }

        if (detectedSomething) {
          if (enableDiagMarkup)
            diagSettings.addLine(Marker.Feature.STOP_RESCAN, p2, p1);
          logger.debug("stop rescan " + p2);

          // we fake the width here, because the scan might run diagonally.
          // this distorts the bar widths which causes them to fail the
          // detection test. we have, however, a rather good confidence
          // that this is actually a good scan during rescan, so that we
          // can "assume" the "correct" width here.
          // FIXME: no, we can't!
          if (!partner.testDetection(ed))
            logger.debug("ReScan rejected");
          yFuzz = p2.y + f;
        }
      }
    }
  }

  private EdgeDetection[] getExtremeHits() {
    int minY = Integer.MAX_VALUE;
    int maxY = Integer.MIN_VALUE;
    EdgeDetection minPoint = null;
    EdgeDetection maxPoint = null;
    for (final EdgeDetection hit : hits) {
      if (hit.y < minY) {
        minY = hit.y;
        minPoint = hit;
      }
      if (hit.y > maxY) {
        maxY = hit.y;
        maxPoint = hit;
      }
    }

    return new EdgeDetection[]{
        minPoint, maxPoint
    };
  }

  /**
   * @return
   */
  public boolean isBlack() {
    return isBlack;
  }

  public double getAngle() {
    return Math.atan(gradient);
  }

  Rectangle getConfidenceRectangle() {
    return confidenceRectangle;
  }
}