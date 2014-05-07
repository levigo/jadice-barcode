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

import java.util.Collection;

import com.jadice.barcode.grid.BinaryGrid;


/**
 * Implementations of {@link Decoder} may derive from this abstract class in order to simplify their
 * life. At this time, the base class simply manages the decoder's {@link Options}, but more
 * amenities may be added in the future.
 */
public abstract class AbstractDecoder implements Decoder {
  protected static final float clamp(float v, float min, float max) {
    if (v < min)
      v = min;
    else if (v > max)
      v = max;
    return v;
  }

  protected Options options;

  /**
   * A code string represents a raw decoded code string from a retection run. It carries a
   * confidence value which represents the confidence with which the code string is correct.
   * Typically, several code string instances are generated during the scan of a single barcode.
   * During post-processing the code strings used for the result are judged by their confidence
   * values.
   */
  protected class CodeString {
    /** The internal representation of the code */
    private int codes[];

    /** The code array truncated to it's real length */
    private int truncatedCodes[];

    /** Current write offset */
    private int offset = 0;

    /** The code's confidence value */
    private int confidence;

    /** The method by which the string was detected. This is code-dependant */
    private int detectionMethod;

    /** whether the checksum calculate OK */
    private boolean checksumVerificationOK;

    public CodeString() {
      codes = new int[32];
    }

    public CodeString(int code[]) {
      codes = code;
      offset = code.length;
    }

    /**
     * Add a code character to the string.
     * 
     * @param code
     */
    public void add(int code) {
      // need to extend the array?
      if (offset >= codes.length) {
        final int newCodes[] = new int[codes.length * 2];
        System.arraycopy(codes, 0, newCodes, 0, codes.length);
        codes = newCodes;
      }
      codes[offset++] = code;
      truncatedCodes = null;
    }

    public int size() {
      return offset;
    }

    /**
     * Set a code character in the string.
     * 
     * @param code
     */
    public void set(int offset, int code) {
      // need to extend the array?
      if (offset >= codes.length) {
        final int newCodes[] = new int[Math.max(offset, codes.length * 2)];
        System.arraycopy(codes, 0, newCodes, 0, codes.length);
        codes = newCodes;
      }
      codes[offset] = code;
      truncatedCodes = null;
    }

    /**
     * Return an int array of codes.
     * 
     * @return
     */
    public int[] getCodes() {
      if (null == truncatedCodes) {
        truncatedCodes = new int[offset];
        System.arraycopy(codes, 0, truncatedCodes, 0, offset);
      }
      return truncatedCodes;
    }

    /**
     * Get the code's confidence value
     * 
     * @return
     */
    public int getConfidence() {
      return confidence;
    }

    /**
     * Get the method by which the string was detected
     * 
     * @return
     */
    public int getDetectionMethod() {
      return detectionMethod;
    }

    /**
     * Set the code's confidence value
     * 
     * @param i
     */
    public void setConfidence(int i) {
      confidence = i;
    }

    /**
     * Set the method by which the string was detected
     * 
     * @param i
     */
    public void setDetectionMethod(int i) {
      detectionMethod = i;
    }

    /**
     * @return
     */
    public boolean isChecksumVerificationOK() {
      return checksumVerificationOK;
    }

    /**
     * @param b
     */
    public void setChecksumVerificationOK(boolean b) {
      checksumVerificationOK = b;
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      final StringBuffer sb = new StringBuffer();
      for (int i = 0; i < offset; i++)
        sb.append(codes[i]).append(' ');
      return sb.toString();
    }
  }

  public abstract Collection<Result> detect(BinaryGrid image);

  @Override
  public void setOptions(Options options) {
    this.options = options;
  }
}
