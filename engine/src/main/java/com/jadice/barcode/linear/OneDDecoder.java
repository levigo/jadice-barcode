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
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.jadice.barcode.AbstractDecoder;
import com.jadice.barcode.BaseSettings;
import com.jadice.barcode.BinaryDecoder;
import com.jadice.barcode.DiagnosticSettings;
import com.jadice.barcode.Marker;
import com.jadice.barcode.Options;
import com.jadice.barcode.Result;
import com.jadice.barcode.Marker.Feature;
import com.jadice.barcode.grid.BinaryGrid;

/**
 * Abstract implementation of a barcode detector for a linear (1D) code.
 * 
 */
public abstract class OneDDecoder extends AbstractDecoder implements BinaryDecoder {

  /**
   * The Node represents a node in a code tree.
   */
  protected static class Node {
    Node children[];
    int code = -1;
  }

  protected static class Symbol {
    public final int[] widths;
    public final int value;

    public Symbol(int[] widths, int value) {
      super();
      this.widths = widths;
      this.value = value;
    }

    @Override
    public String toString() {
      return value + " " + Arrays.toString(widths);
    }
  }

  protected static class EdgeDetection {
    public final float overprint;
    public final int x, y, width;

    public EdgeDetection(int x, int y, int width, float overprintEstimate) {
      super();
      this.x = x;
      this.y = y;
      this.width = width;
      this.overprint = overprintEstimate;
    }
  }

  protected static class CodeGeometry {
    public final int dataCodeWindowSize;
    public final int moduleWidthData;
    public final int moduleWidthQuiet;
    public final int moduleWidthStart;
    public final int moduleWidthStop;
    public final int startCodeWindowSize;
    public final int stopCodeWindowSize;
    public final boolean acceptInverse;

    public CodeGeometry(int startCodeWindowSize, int stopCodeWindowSize, int dataCodeWindowSize, int moduleWidthStart,
        int moduleWidthStop, int moduleWidthData, int moduleWidthQuiet, boolean acceptInverse) {
      this.startCodeWindowSize = startCodeWindowSize;
      this.stopCodeWindowSize = stopCodeWindowSize;
      this.dataCodeWindowSize = dataCodeWindowSize;
      this.moduleWidthStart = moduleWidthStart;
      this.moduleWidthStop = moduleWidthStop;
      this.moduleWidthData = moduleWidthData;
      this.moduleWidthQuiet = moduleWidthQuiet;
      this.acceptInverse = acceptInverse;
    }
  }

  protected static final int DETECTION_METHOD_AGGREGATE = 2;

  protected static final int DETECTION_METHOD_DIRECT = 1;

  /** The Log4J logger to use */
  protected static Logger logger = Logger.getLogger(OneDDecoder.class);

  protected static final boolean within(float value, float expected, int tolerancePercent) {
    return Math.abs(value - expected) < tolerancePercent / 100f;
  }

  protected static int[] splitCodeString(int codeString, int length) {
    int split[] = new int[length];

    int divident = 1;
    for (int i = 1; i < length; i++)
      divident *= 10;

    while (codeString / divident == 0 && divident > 1)
      divident /= 10;

    int p = 0;
    while (divident > 0) {
      split[p++] = codeString / divident % 10;
      divident /= 10;
    }

    return split;
  }

  /**
   * A collection of start-edges which where detected.
   */
  protected Collection<Edge> startEdges = new LinkedList<Edge>();

  /**
   * A collection of start-edges which where detected.
   */
  protected Collection<Edge> stopEdges = new LinkedList<Edge>();

  protected LinearCodeSettings linearCodeSettings;

  protected BaseSettings baseSettings;

  protected boolean isDiagMarkupEnabled;

  private DiagnosticSettings diagnosticOptions;

  @Override
  public void setOptions(Options options) {
    super.setOptions(options);
    baseSettings = options.getSettings(BaseSettings.class);
    linearCodeSettings = options.getSettings(LinearCodeSettings.class);

    diagnosticOptions = options.getSettings(DiagnosticSettings.class);
    isDiagMarkupEnabled = diagnosticOptions.isMarkupEnabled();
  }

  /**
   * @param aggregateScan
   * @param barWidths
   * @param barCount
   */
  protected void aggregateScan(int[] aggregateScan, int[] barWidths, int barCount) {
    int totalWidth = 0;
    for (int i = 0; i < barCount; i++)
      totalWidth += barWidths[i];

    if (totalWidth > aggregateScan.length) {
      logger.warn("totalWidth > aggregateScan.length (should not happen)");
      return;
    }

    boolean black = false;
    int aggregateIndex = 0;
    int sourcePos = 0;
    for (int i = 0; i < barCount; i++) {
      int width = barWidths[i];
      sourcePos += width;
      int endAggregateIndex = sourcePos * aggregateScan.length / totalWidth;
      if (black)
        for (int j = aggregateIndex; j < endAggregateIndex; j++)
          aggregateScan[j]++;
      aggregateIndex = endAggregateIndex;
      black = !black;
    }
  }

  /**
   * Condense all given code strings into one result string.
   * 
   * @param codeStrings
   * @return
   */
  protected CodeString consolidateCodeStrings(List<CodeString> codeStrings) {
    // dump results
    logger.debug("Consolidating the following code strings:");
    if (logger.isDebugEnabled())
      for (CodeString string : codeStrings)
        logger.debug("Result: " + decodeCodeString(string) + " confidence=" + string.getConfidence() + " method="
            + string.getDetectionMethod());

    int maxCodeStringLength = 0;
    for (CodeString string : codeStrings) {
      int length = string.getCodes().length;
      if (length > maxCodeStringLength)
        maxCodeStringLength = length;
    }

    // FIXME: this is inefficient as hell!
    HashMap<Integer, Integer> codeHistogram = new HashMap<Integer, Integer>();
    int resultString[] = new int[maxCodeStringLength];
    for (int i = 0; i < maxCodeStringLength; i++) {
      codeHistogram.clear();
      // accumulate characters
      for (CodeString string : codeStrings)
        if (string.getCodes().length > i) {
          Integer key = new Integer(string.getCodes()[i]);
          Integer count = codeHistogram.get(key);
          if (null == count)
            count = new Integer(1);
          else
            count = new Integer(count.intValue() + 1);
          codeHistogram.put(key, count);
        }

      // find max histogram entry
      int maxEntry = -1;
      int maxCount = 0;
      for (Integer key : codeHistogram.keySet()) {
        int value = codeHistogram.get(key).intValue();
        if (value > maxCount) {
          maxCount = value;
          maxEntry = key.intValue();
        }
      }
      resultString[i] = maxEntry;
    }

    if (codeStrings.size() == 0)
      return null;

    // prepare result
    CodeString result = new CodeString(resultString);
    result.setConfidence(codeStrings.get(0).getConfidence());
    result.setChecksumVerificationOK(codeStrings.get(0).isChecksumVerificationOK());

    return result;
  }

  /**
   * Decode the given CodeString into the final decoded result string.
   * 
   * @param string
   * @return
   */
  protected abstract String decodeCodeString(CodeString string);

  /*
   * @see com.levigo.barcode.AbstractCode#detect(com.levigo.barcode.ImageDataSource,
   * com.levigo.barcode.DetectionOptions)
   */
  @Override
  public Collection<Result> detect(BinaryGrid image) {
    List<Result> results = new ArrayList<Result>();

    startEdges.clear();
    stopEdges.clear();
    startEdges.addAll(scanForEdges(image, results));

    postprocessEdges(image, results);

    detectCodes(image, results);

    return results;
  }

  /**
   * Detect and decode codes from edges.
   * 
   * @param results
   * @param options TODO
   * @param result TODO
   * 
   * @return
   */
  protected void detectCodes(BinaryGrid image, List<Result> results) {
    for (Edge edge : startEdges) {
      Result result = processEdgePair(edge, image, results);
      if (null != result && result.isValid() && result.getCodeString().length() > 0)
        results.add(result);
    }
  }

  /**
   * Perform basic detection of start and stop codes along a scan line. The scan line is supposed to
   * start at an x of zero.
   * 
   * @param barWidths
   * @param results
   * @param results TODO
   * @param normalPosition the position normal to the scan/read axis
   * @param direction
   * 
   * @return
   */
  protected boolean detectEdges(int y, int[] barWidths, int barCount, List<Result> results) {
    CodeGeometry geometry = getCodeGeometry();

    int step = geometry.acceptInverse ? 1 : 2;

    // whether we detected at least one start/stop
    boolean detectedSomething = false;

    int totalWidthInWindow = sumWidths(barWidths, 1, geometry.startCodeWindowSize, 1);

    // current position along scan axis
    int x = barWidths[0];
    int offset = 1; // start at first actual bar (index 0 is quiet zone)
    while (offset < barCount - geometry.startCodeWindowSize - 1) {
      int expectedQuietZoneWidth = (totalWidthInWindow * geometry.moduleWidthQuiet
          * linearCodeSettings.getQuietZoneTolerance() / geometry.moduleWidthStart / 100);

      /*
       * check quiet zone. for the very first bar, we always assume a valid quiet zone. this means
       * that the "paper" is thought to extend beyond the left border.
       */
      final int quietZoneWidth = barWidths[offset - 1];
      if (quietZoneWidth >= expectedQuietZoneWidth || offset == 1) {
        EdgeDetection ed = detectStart(barWidths, offset, barCount, x, y);
        if (null != ed) {
          detectedSomething = true;
          registerStartStopDetection(geometry.moduleWidthStart, true, (offset & 1) != 0, quietZoneWidth, ed);
          if (logger.isDebugEnabled())
            logger.debug("Detected start at " + x + "/" + y);
          if (isDiagMarkupEnabled)
            diagnosticOptions.addLine(Marker.Feature.START, x, y, x + totalWidthInWindow, y);
        }
      }

      for (int i = 0; i < step; i++) {
        totalWidthInWindow -= barWidths[offset];
        totalWidthInWindow += barWidths[offset + geometry.startCodeWindowSize];
        x += barWidths[offset];
        offset++;
      }
    }

    totalWidthInWindow = sumWidths(barWidths, 1, geometry.stopCodeWindowSize, 1);

    // current position along scan axis
    x = barWidths[0];
    offset = 1; // start at first actual bar (index 0 is quiet zone)
    while (offset < barCount - geometry.stopCodeWindowSize) {
      // detect quiet zone at end
      int expectedQuietZoneWidth = (totalWidthInWindow * geometry.moduleWidthQuiet
          * linearCodeSettings.getQuietZoneTolerance() / geometry.moduleWidthStop / 100);

      int quietZoneBar = offset + geometry.stopCodeWindowSize;
      if (quietZoneBar < barCount) {
        final int quietZoneWidth = barWidths[quietZoneBar];
        if ((quietZoneWidth >= expectedQuietZoneWidth || quietZoneBar == barCount - 1)) {
          EdgeDetection ed = detectStop(barWidths, offset, barCount, x, y);
          if (ed != null) {
            detectedSomething = true;
            registerStartStopDetection(geometry.moduleWidthStop, false, (quietZoneBar & 1) == 0, quietZoneWidth, ed);
            if (logger.isDebugEnabled())
              logger.debug("Detected stop at " + x + "/" + y);
            if (isDiagMarkupEnabled)
              diagnosticOptions.addLine(Marker.Feature.STOP, x, y, x + totalWidthInWindow, y);
          }
        }
      }

      for (int i = 0; i < step; i++) {
        totalWidthInWindow -= barWidths[offset];
        totalWidthInWindow += barWidths[offset + geometry.stopCodeWindowSize];
        x += barWidths[offset];
        offset++;
      }
    }

    return detectedSomething;
  }

  /**
   * Detect whether a code starts at the given scan offset. The x any y coordinates are of informal
   * value only.
   * 
   * @param barWidths
   * @param offset
   * @param x
   * @param y
   * @return an overprint estimate (&gt;0) for a match, -1 otherwise
   */
  protected abstract EdgeDetection detectStart(int[] barWidths, int offset, int barCount, int x, int y);

  /**
   * Detect whether a code ends at the given scan offset. The x any y coordinates are of informal
   * value only.
   * 
   * @param barWidths
   * @param offset
   * @param x
   * @param y
   * @return an overprint estimate (&gt;0) for a match, -1 otherwise
   */
  protected abstract EdgeDetection detectStop(int[] barWidths, int offset, int barCount, int x, int y);

  protected abstract CodeGeometry getCodeGeometry();

  protected GeneralPath getDetectionShape(Edge edge) {
    GeneralPath gp = new GeneralPath();
    gp.moveTo((float) edge.getLine().getP1().getX(), (float) edge.getLine().getP1().getY());
    gp.lineTo((float) edge.getPartner().getLine().getP1().getX(), (float) edge.getPartner().getLine().getP1().getY());
    gp.lineTo((float) edge.getPartner().getLine().getP2().getX(), (float) edge.getPartner().getLine().getP2().getY());
    gp.lineTo((float) edge.getLine().getP2().getX(), (float) edge.getLine().getP2().getY());
    gp.closePath();
    return gp;
  }

  /**
   * Post-process the detected edges.
   * 
   * @param results
   * @param results TODO
   */
  protected void postprocessEdges(BinaryGrid image, List<Result> results) {
    // first, let the edges process their hits.
    // they return whether they think they are sane (i.e. have enough points)
    for (Iterator<Edge> i = startEdges.iterator(); i.hasNext();) {
      final Edge e = i.next();
      if (!e.pruneHits()) {
        i.remove();
        if (isDiagMarkupEnabled)
          diagnosticOptions.add(e.getConfidenceRectangle(), Marker.Feature.INVALID_EDGE);
      }
    }
    for (Iterator<Edge> i = stopEdges.iterator(); i.hasNext();) {
      final Edge e = i.next();
      if (!e.pruneHits()) {
        i.remove();
        if (isDiagMarkupEnabled)
          diagnosticOptions.add(e.getConfidenceRectangle(), Marker.Feature.INVALID_EDGE);
      }
    }

    if (isDiagMarkupEnabled) {
      for (Edge edge : startEdges)
        diagnosticOptions.add(edge.getLine(), Marker.Feature.START_EDGE);
      for (Edge edge : stopEdges)
        diagnosticOptions.add(edge.getLine(), Marker.Feature.STOP_EDGE);
    }

    // now try to associate each edge with a partner
    for (Iterator<Edge> i = startEdges.iterator(); i.hasNext();) {
      Edge edge = i.next();
      Edge bestPartner = null;
      int bestPartnerConfidence = Integer.MIN_VALUE;
      for (Edge possiblePartner : stopEdges) {
        int confidence = edge.getPartnerConfidence(possiblePartner);
        if (confidence > bestPartnerConfidence) {
          bestPartner = possiblePartner;
          bestPartnerConfidence = confidence;
        }
      }

      if (bestPartner == null) {
        logger.debug("Did not find a partner for " + edge);
        if (isDiagMarkupEnabled)
          diagnosticOptions.add(edge.getConfidenceRectangle(), Marker.Feature.SINGLETON_EDGE);
        i.remove();
      } else {
        // try it the other way around: is this edge the best partner
        // for the suspected partner?
        int bestPartnerConfidence2 = bestPartnerConfidence;
        Edge bestPartnerForPartner = null;
        for (Edge possiblePartner : startEdges)
          // we already know about this combination and we don't care about
          // edges which are already taken.
          if (possiblePartner != edge && possiblePartner.getPartner() == null) {
            int confidence = possiblePartner.getPartnerConfidence(bestPartner);
            if (confidence > bestPartnerConfidence2) {
              bestPartnerForPartner = possiblePartner;
              bestPartnerConfidence2 = confidence;
            }
          }

        if (bestPartnerForPartner != null && bestPartnerForPartner != edge) {
          // nope, this bride is already taken!
          logger.debug("Found partner " + bestPartner + " for " + edge + ", but " + bestPartnerForPartner
              + " is a better match for the first.");
          if (isDiagMarkupEnabled)
            diagnosticOptions.add(edge.getConfidenceRectangle(), Marker.Feature.SINGLETON_EDGE);
          i.remove();
        } else {
          // ok, fine, let them live happily ever after
          logger.debug("Edge Pair: \n" + edge + "\n" + bestPartner);
          edge.setPartner(bestPartner);
          bestPartner.setPartner(edge);
          stopEdges.remove(bestPartner);

          if (isDiagMarkupEnabled)
            diagnosticOptions.addLine(Marker.Feature.EDGE_ASSOCIATION, edge.getConfidenceRectangle().getCenterX(),
                edge.getConfidenceRectangle().getCenterY(), bestPartner.getConfidenceRectangle().getCenterX(),
                bestPartner.getConfidenceRectangle().getCenterY());
        }
      }
    }

    // ideally, the stopEdges is now empty
    if (logger.isDebugEnabled())
      for (Edge edge : stopEdges) {
        logger.debug("Did not find a partner for " + edge);
        if (isDiagMarkupEnabled)
          diagnosticOptions.add(edge.getConfidenceRectangle(), Marker.Feature.SINGLETON_EDGE);
      }

    stopEdges.clear();

    // now re-scan the edges and maximize their coverage
    for (Iterator<Edge> i = startEdges.iterator(); i.hasNext();) {
      final Edge e = i.next();
      if (!e.reScan(this, image, options, results)) {
        i.remove();
        if (isDiagMarkupEnabled) {
          diagnosticOptions.add(e.getConfidenceRectangle(), Marker.Feature.INVALID_EDGE);
          diagnosticOptions.add(e.getPartner().getConfidenceRectangle(), Marker.Feature.INVALID_EDGE);
        }
      }
    }

    if (isDiagMarkupEnabled)
      for (Edge edge : startEdges) {
        diagnosticOptions.add(edge.getConfidenceRectangle(), Marker.Feature.EDGE);
        diagnosticOptions.add(edge.getPartner().getConfidenceRectangle(), Marker.Feature.EDGE);
      }
  }

  /**
   * @param edge
   * @param results
   * @param results TODO
   * @param options TODO
   * @return
   */
  protected Result processEdgePair(Edge edge, BinaryGrid image, List<Result> results) {
    Line2D startEdgeLine = edge.getLine();
    Line2D stopEdgeLine = edge.getPartner().getLine();

    // interpolate and scan along the edges
    int dyStart = (int) (startEdgeLine.getY2() - startEdgeLine.getY1());
    int dyStop = (int) (stopEdgeLine.getY2() - stopEdgeLine.getY1());
    int dyMajor = Math.max(dyStart, dyStop);

    // we do a simple interpolation, no fancy bresenham stuff here.
    // this is not performance critical.
    int dxStart = (int) (startEdgeLine.getX2() - startEdgeLine.getX1());
    int dxStop = (int) (stopEdgeLine.getX2() - stopEdgeLine.getX1());
    int pStartX = (int) startEdgeLine.getP1().getX();
    int pStartY = (int) startEdgeLine.getP1().getY();
    int pStopX = (int) stopEdgeLine.getP1().getX();
    int pStopY = (int) stopEdgeLine.getP1().getY();

    // some more preparation
    boolean isBlackCode = edge.isBlack();
    int barWidths[] = new int[image.getWidth()];
    int maxScanLength = (int) Math.max(
        stopEdgeLine.getP1().getX() - startEdgeLine.getP1().getX()
            + Math.abs(stopEdgeLine.getP1().getY() - startEdgeLine.getP1().getY()), stopEdgeLine.getP2().getX()
            - startEdgeLine.getP2().getX() + Math.abs(stopEdgeLine.getP2().getY() - startEdgeLine.getP2().getY()));

    if (maxScanLength < 0) {
      logger.warn("Should not happen: max scan length < 0");
      return null;
    }

    int aggregateScan[] = new int[maxScanLength * 2];

    // interpolate along the edges
    List<CodeString> codeStrings = new LinkedList<CodeString>();
    Point start = new Point();
    Point stop = new Point();

    /*
     * This is where the decode starts with respect to the scanned bars. For black codes we start at
     * the second bar, since we start the scan right 'inside' the first bar, so the scan will
     * generate a zero-pixel white bar at the start, because white bars are at even offsets, by
     * definition. For white codes we start at the first bar.
     */
    final int offset = isBlackCode ? 1 : 0;

    for (int t = 0; t < dyMajor; t++) {
      start.x = pStartX + dxStart * t / dyMajor;
      start.y = pStartY + dyStart * t / dyMajor;
      stop.x = pStopX + dxStop * t / dyMajor;
      stop.y = pStopY + dyStop * t / dyMajor;

      // position start.x right at the start of the first bar
      while (isBlackCode == image.samplePixel(start.x, start.y) && start.x >= 0)
        start.x--;
      start.x++;

      // position stop.x right at the end of the last bar
      while (isBlackCode == image.samplePixel(stop.x, stop.y) && stop.x < image.getWidth())
        stop.x++;
      stop.x--;

      if (logger.isDebugEnabled())
        logger.debug("Scanning " + start + "..." + stop);

      // scan the bars
      if (isDiagMarkupEnabled)
        diagnosticOptions.addLine((Feature.DETECTION_SCAN), start, stop);

      int barCount = scanBars(image, barWidths, start, stop);
      aggregateScan(aggregateScan, barWidths, barCount);

      // if this is a black code, scanBars will have generated a zero-width
      // first bar, so we must start at the second one.
      CodeString codeString = decodeBarsToCodeString(barWidths, offset, barCount,
          (edge.getOverprintEstimate() + edge.getPartner().getOverprintEstimate()) / 2);
      codeString.setDetectionMethod(DETECTION_METHOD_DIRECT);
      codeStrings.add(codeString);
      if (logger.isDebugEnabled())
        logger.debug("Method 1 detection: " + decodeCodeString(codeString) + " confidence="
            + codeString.getConfidence() + (codeString.isChecksumVerificationOK() ? " OK" : ""));
    }

    // now process the aggregate scan
    // calculate min/max of aggregate scan
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    for (int element : aggregateScan) {
      if (min > element)
        min = element;
      if (max < element)
        max = element;
    }

    // detect bar widths based on aggregate scan
    for (int threshold = min; threshold <= max; threshold++) {
      int barCount = scanBarsFromAggregateScan(barWidths, aggregateScan, threshold);
      CodeString codeString = decodeBarsToCodeString(barWidths, offset, barCount, edge.getOverprintEstimate());
      codeString.setDetectionMethod(DETECTION_METHOD_AGGREGATE);
      codeStrings.add(codeString);
      if (logger.isDebugEnabled())
        logger.debug("Method 2 detection: " + decodeCodeString(codeString) + " confidence="
            + codeString.getConfidence() + (codeString.isChecksumVerificationOK() ? " OK" : ""));
    }

    // find max confidence
    int maxConfidence = 0;
    for (CodeString string : codeStrings)
      if (string.getConfidence() > maxConfidence)
        maxConfidence = string.getConfidence();

    // dump all but max confidence hits
    for (Iterator<CodeString> i = codeStrings.iterator(); i.hasNext();) {
      CodeString string = i.next();
      if (string.getConfidence() < maxConfidence)
        i.remove();
    }

    // condense remaining code strings
    CodeString consolidatedCodeString = consolidateCodeStrings(codeStrings);
    if (null == consolidatedCodeString)
      return null;

    String codeString = decodeCodeString(consolidatedCodeString);

    if (isDiagMarkupEnabled) {
      GeneralPath p = new GeneralPath();
      int x0 = (int) edge.getLine().getP1().getX();
      int y0 = (int) edge.getLine().getP1().getY();
      p.moveTo(x0, y0);
      for (int i = 0; i < aggregateScan.length; i++)
        p.lineTo(x0 + (i / 2), y0 - aggregateScan[i]);
      diagnosticOptions.add(p, Marker.Feature.METHOD2);
    }

    // come up with the detection shape
    GeneralPath gp = getDetectionShape(edge);

    return new Result(getSymbology(), gp, codeString, true, consolidatedCodeString.isChecksumVerificationOK(),
        edge.getAngle());
  }

  /**
   * Decode a scan of bars into a CodeString. During the decoding a confidence value is calculated
   * which indicates the confidence regarding the correctness of the decoded code string.
   * 
   * @param barWidths
   * @param offset TODO
   * @param barCount
   * @param overprintEstimate TODO
   * @return CodeString
   */
  protected abstract CodeString decodeBarsToCodeString(int[] barWidths, int offset, int barCount,
      float overprintEstimate);

  /**
   * Register the detection of a start/stop pattern for later processing.
   * 
   * @param edgeDetection
   * 
   * @param direction
   * @param axisPosition
   * @param normalPosition
   * @param totalWidthInWindow
   * @param b
   */
  protected void registerStartStopDetection(int moduleWidth, boolean isStartCode, boolean isBlack, int quietZoneWidth,
      EdgeDetection edgeDetection) {
    boolean foundMatchingEdge = false;

    // select start/stop edges
    Collection<Edge> edgeCollection = isStartCode ? startEdges : stopEdges;

    for (Edge edge : edgeCollection)
      if (edge.testDetection(edgeDetection)) {
        foundMatchingEdge = true;
        break;
      }

    if (!foundMatchingEdge)
      edgeCollection.add(new Edge(options, moduleWidth, isStartCode, isBlack, quietZoneWidth, edgeDetection));
  }

  protected int scanBarsFromAggregateScan(int[] barWidths, int[] aggregateScan, int threshold) {
    int barCount = 0;
    boolean black = false;
    int currentWidth = 0;
    for (int element : aggregateScan) {
      if ((element > threshold) != black) {
        barWidths[barCount] = currentWidth;
        currentWidth = 0;
        barCount++;
        black = !black;
      }

      currentWidth++;
    }
    barWidths[barCount++] = currentWidth;
    return barCount;
  }

  /**
   * Scan for edge detection
   * 
   * @param image
   * @param results
   * @param results TODO
   * @param options
   * @param direction
   */
  protected Collection<Edge> scanForEdges(BinaryGrid image, List<Result> results) {
    Collection<Edge> detectedEdges = new LinkedList<Edge>();

    int y = 0; // horizontal or vertical coordinate!
    int height = image.getHeight();
    Point from = new Point(0, 0);
    Point to = new Point(image.getWidth() - 1, 0);

    // set up bars
    int barWidths[] = new int[image.getWidth() + 2];

    int fineScanForwardTo = -1;
    do {
      // update scan vector
      from.y = to.y = y;

      if (isDiagMarkupEnabled)
        diagnosticOptions.addLine((fineScanForwardTo > y ? Feature.SCAN : Feature.INITIAL_SCAN), from, to);

      // scan the bars
      int barCount = scanBars(image, barWidths, from, to);

      // try to detect start and stop markers
      if (detectEdges(y, barWidths, barCount, results)) {
        if (fineScanForwardTo < y) {
          // if we just found something, we fine-scan the area between
          // the previous and the next regular scan interval:
          // fine-scan backwards
          int previousY = Math.max(0, y - linearCodeSettings.getScanInterval());
          for (int y1 = y - 1; y1 > previousY; y1--) {
            from.y = to.y = y1;

            if (isDiagMarkupEnabled)
              diagnosticOptions.addLine(Feature.SCAN, from, to);

            barCount = scanBars(image, barWidths, from, to);
            detectEdges(y1, barWidths, barCount, results);
          }
        }

        // fine-scan forward
        fineScanForwardTo = Math.min(y + linearCodeSettings.getScanInterval(), height);
      }

      if (fineScanForwardTo > y)
        y += 1;
      else
        y += linearCodeSettings.getScanInterval();
    } while (y < height);

    return detectedEdges;
  }

  /**
   * Calculate the sum of the supplied bar widths.
   * 
   * @param barWidths
   * @param offset where to start
   * @param length the length of the window
   * @param stride the summation stride (1=every bar and space, 2=only bars/spaces, 4=every second
   *          bar/space etc.)
   * @return
   */
  protected int sumWidths(int[] barWidths, int offset, int length, int stride) {
    int sum = 0;
    for (int i = 0; i < length; i++, offset += stride)
      sum += barWidths[offset];
    return sum;
  }

  /**
   * Perform a interpolating scan for bars between the given points and return the number of scanned
   * bars.
   * 
   * @param barWidths
   * @param p1
   * @param p2
   * @return number of scanned bars
   */
  int scanBars(BinaryGrid image, int barWidths[], Point p1, Point p2) {
    int dx = p2.x - p1.x;
    int dy = p2.y - p1.y;

    // we always start with a white bar!
    boolean isBlack = false;
    int currentBarWidth = 0;
    int scannedBars = 0;

    if (dx > dy) {
      int epsilon = dy / 2;
      int stepY = dy >= 0 ? 1 : -1;

      dy = Math.abs(dy);
      dx = Math.abs(dx);
      for (int x = p1.x, y = p1.y; x < p2.x; x++) {
        boolean currentPixelIsBlack = image.samplePixel(x, y);
        if (isBlack != currentPixelIsBlack) {
          isBlack = currentPixelIsBlack;
          barWidths[scannedBars] = currentBarWidth;
          currentBarWidth = 0;
          scannedBars++;
        }

        currentBarWidth++;

        if (epsilon >= dx) {
          // step
          y += stepY;

          currentPixelIsBlack = image.samplePixel(x, y);
          if (isBlack != currentPixelIsBlack) {
            isBlack = currentPixelIsBlack;
            barWidths[scannedBars] = currentBarWidth;
            currentBarWidth = 0;
            scannedBars++;
          }

          currentBarWidth++;

          epsilon -= dx;
        }

        epsilon += dy;
      }
    } else {
      int epsilon = dx / 2;
      int stepX = dx >= 0 ? 1 : -1;
      for (int y = p1.y, x = p1.x; y < p2.y; y++) {
        boolean currentPixelIsBlack = image.samplePixel(x, y);
        if (isBlack != currentPixelIsBlack) {
          isBlack = currentPixelIsBlack;
          barWidths[scannedBars] = currentBarWidth;
          currentBarWidth = 0;
          scannedBars++;
        }

        currentBarWidth++;

        if (epsilon >= dy) {
          // step
          x += stepX;

          currentPixelIsBlack = image.samplePixel(x, y);
          if (isBlack != currentPixelIsBlack) {
            isBlack = currentPixelIsBlack;
            barWidths[scannedBars] = currentBarWidth;
            currentBarWidth = 0;
            scannedBars++;
          }

          currentBarWidth++;

          epsilon -= dy;
        }

        epsilon += dx;
      }
    }

    // check the very last pixel
    if (isBlack != image.samplePixel(p2.x, p2.y)) {
      barWidths[scannedBars] = currentBarWidth;
      currentBarWidth = 0;
      scannedBars++;
    }

    // register last bar
    barWidths[scannedBars] = currentBarWidth + 1;
    scannedBars++;

    return scannedBars;
  }

  protected final boolean barRatioOk(int a, int b, int ua, int ub, boolean includeOverprintTolerance, int barCountFactor) {
    float pixelsPerUnit = (float) (a + b) / (ua + ub);
    float expectedA = ((float) ua / ub) * b;
    final float tolerance = barCountFactor
        * pixelsPerUnit
        * (linearCodeSettings.getBarWidthTolerance() + (includeOverprintTolerance
            ? baseSettings.getOverprintTolerance()
            : 0)) / 100f;
    final boolean ok = Math.abs(a - expectedA) < tolerance;

    if (!ok && logger.isDebugEnabled())
      logger.debug(a + "/" + b + " not within " + ua + "/" + ub + " +-" + linearCodeSettings.getBarWidthTolerance()
          + "%");

    return ok;
  }
}