package com.jadice.barcode.twod.dmtx;

import com.jadice.barcode.DiagnosticSettings;
import com.jadice.barcode.Options;
import com.jadice.barcode.Marker.Feature;
import com.jadice.barcode.grid.Grid;

public class ScanGrid implements ScanStrategy {
  public enum Range {
    Good, Bad, End
  }

  /* set once */
  private int minExtent; /* Smallest cross size used in scan */
  private final int maxExtent; /* Size of bounding grid region (2^N - 1) */
  private final int xOffset; /* Offset to obtain image X coordinate */
  private final int yOffset; /* Offset to obtain image Y coordinate */
  private final int xMin; /* Minimum X in image coordinate system */
  private final int xMax; /* Maximum X in image coordinate system */
  private final int yMin; /* Minimum Y in image coordinate system */
  private final int yMax; /* Maximum Y in image coordinate system */

  /* reset for each level */
  @SuppressWarnings("unused")
  private int total; /* Total number of crosses at this size */
  private int extent; /* Length/width of cross in pixels */
  private int jumpSize; /* Distance in pixels between cross centers */
  private int pixelTotal; /* Total pixel count within an individual cross path */
  private int startPos; /* X and Y coordinate of first cross center in pattern */

  /* reset for each cross */
  private int pixelCount; /* Progress (pixel count) within current cross pattern */
  private int xCenter; /* X center of current cross pattern */
  private int yCenter; /* Y center of current cross pattern */

  private final boolean createMarkup;
  private final DiagnosticSettings diagnostics;

  /**
   * \brief Initialize scan grid pattern \param dec \return Initialized grid
   * 
   * @param options
   */
  ScanGrid(Grid grid, Options options) {
    createMarkup = options.getOptions(DiagnosticSettings.class).isMarkupEnabled();
    diagnostics = options.getOptions(DiagnosticSettings.class);

    int scale = 1;
    int smallestFeature = 1 / scale;

    xMin = 0;
    xMax = grid.getWidth() - 1;
    yMin = 0;
    yMax = grid.getHeight() - 1;

    /* Values that get set once */
    int xExtent = xMax - xMin;
    int yExtent = yMax - yMin;
    int maxExtent = Math.max(xExtent, yExtent);
    assert maxExtent > 1;

    int minExtent = 1;
    int extent;
    for (extent = 1; extent < maxExtent; extent = (extent + 1) * 2 - 1)
      if (extent <= smallestFeature)
        minExtent = extent;
    this.minExtent = minExtent;
    this.maxExtent = extent;

    xOffset = (xMin + xMax - this.maxExtent) / 2;
    yOffset = (yMin + yMax - this.maxExtent) / 2;

    /* Values that get reset for every level */
    total = 1;
    this.extent = this.maxExtent;

    setDerivedFields();
  }

  /**
   * \brief Extract current grid position in pixel coordinates and return whether location is good,
   * bad, or end \param grid \return Pixel location
   */
  private Range getGridCoordinates(PixelLocation p) {
    /*
     * Initially pixelCount may fall beyond acceptable limits. Update grid state before testing
     * coordinates
     */

    /* Jump to next cross pattern horizontally if current column is done */
    if (pixelCount >= pixelTotal) {
      pixelCount = 0;
      xCenter += jumpSize;
    }

    /* Jump to next cross pattern vertically if current row is done */
    if (xCenter > maxExtent) {
      xCenter = startPos;
      yCenter += jumpSize;
    }

    /* Increment level when vertical step goes too far */
    if (yCenter > maxExtent) {
      total *= 4;
      this.extent = extent / 2;
      setDerivedFields();
    }

    if (extent == 0 || extent < minExtent) {
      p.x = p.y = -1;
      return Range.End;
    }

    int count = pixelCount;

    assert count < pixelTotal;

    final PixelLocation loc = new PixelLocation();
    if (count == pixelTotal - 1) {
      /* center pixel */
      loc.x = xCenter;
      loc.y = yCenter;
    } else {
      int half = pixelTotal / 2;
      int quarter = half / 2;

      /* horizontal portion */
      if (count < half) {
        loc.x = xCenter + (count < quarter ? count - quarter : half - count);
        loc.y = yCenter;
      }
      /* vertical portion */
      else {
        count -= half;
        loc.x = xCenter;
        loc.y = yCenter + (count < quarter ? count - quarter : half - count);
      }
    }

    loc.x += xOffset;
    loc.y += yOffset;

    p.x = loc.x;
    p.y = loc.y;

    if (loc.x < xMin || loc.x > xMax || loc.y < yMin || loc.y > yMax)
      return Range.Bad;

    return Range.Good;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.levigo.barcode.twod.dmtx.ScanStrategy#popGridLocation(com.levigo.barcode.twod.dmtx.
   * PixelLocation)
   */
  @Override
  public boolean getNextScanLocation(PixelLocation p) {
    Range locStatus;
    do {
      locStatus = getGridCoordinates(p);

      /* Always leave grid pointing at next available location */
      pixelCount++;
    } while (locStatus == Range.Bad);

    if (createMarkup)
      diagnostics.addTransientPoint(Feature.INITIAL_SCAN, p.x, yMax - p.y);

    return locStatus != Range.End;
  }

  /**
   * \brief Update derived fields based on current state \param grid \return void
   */
  private void setDerivedFields() {
    jumpSize = extent + 1;
    pixelTotal = 2 * extent - 1;
    startPos = extent / 2;
    pixelCount = 0;
    xCenter = yCenter = startPos;
  }

  public int getMinExtent() {
    return minExtent;
  }

  public void setMinExtent(int minExtent) {
    this.minExtent = minExtent;
  }
}