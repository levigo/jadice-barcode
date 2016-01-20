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
package com.jadice.barcode.linear.c39;

import com.jadice.barcode.Settings;
import com.jadice.barcode.checksum.Checksum;
import com.jadice.barcode.checksum.ChecksumVerifier;


/**
 * A set of settings which apply to Code 39 bar codes only.
 */
public class Code39Settings implements Settings, Checksum {
  /**
   * Whether to strip surrounding asterisks from decoding results.
   */
  private boolean stripSurroundingAsterisks = false;

  /**
   * Whether to enable the optional Code 39 checksum verification.
   */
  private boolean enableChecksumVerification = true;

  /**
   * The checksumverifier for Code39
   */
  private ChecksumVerifier checksumVerifier = null;

  /**
   * Whether to enabled Code 39 full ASCII decoding.
   */
  private boolean enableFullASCII = true;

  /**
   * Set whether to strip surrounding asterisks.
   * 
   * @return
   */
  public boolean isStripSurroundingAsterisks() {
    return stripSurroundingAsterisks;
  }

  /**
   * Set whether to strip surrounding askerisks from the detected codes. In Code 39 the symbol for
   * an asterisk (*) doubles as the start and stop code. As valid codes are therefore required to
   * start and end with asterisks, they are included in all raw detections.
   * 
   * If this property is set to <code>false</code> those asterisks will be included in the decoded
   * result. Otherwise all surrounding asterisks will be stripped.
   * 
   * Default: <code>false</code>
   * 
   * @param stripSurroundingAsterisks
   */
  public void setStripSurroundingAsterisks(boolean stripSurroundingAsterisks) {
    this.stripSurroundingAsterisks = stripSurroundingAsterisks;
  }

  /**
   * Return whether to enable the optional Code 39 checksum verification.
   * 
   * @return
   */
  @Override
  public boolean isEnableChecksumVerification() {
    return enableChecksumVerification;
  }

  /**
   * Set whether to enable the optional Code 39 checksum verification. Code 39 checksums are always
   * in-band, i.e. part of the detected code.
   * 
   * If the input is known to not use checksums, this property should be set to <code>false</code>,
   * as codes with valid checksums are preferred in ambiguous situations with low-quality input
   * images. It is therefore possible that a false positive with an erroneously valid checksum masks
   * the actual correct detection.
   * 
   * Default: <code>true</code>
   * 
   * @param enableChecksumVerification
   */
  @Override
  public void setEnableChecksumVerification(boolean enableChecksumVerification) {
    this.enableChecksumVerification = enableChecksumVerification;
  }

  /**
   * Return whether to enable Code 39 Full ASCII decoding.
   * 
   * @return
   */
  public boolean isEnableFullASCII() {
    return enableFullASCII;
  }

  /**
   * Set whether to enable Code 39 Full ASCII decoding. If the input is known not to use Full ASCII
   * encoding, this should be set to <code>false</code>, since the ASCII decoding may otherwise
   * mangle some results.
   * 
   * @param enableFullASCII
   */
  public void setEnableFullASCII(boolean enableFullASCII) {
    this.enableFullASCII = enableFullASCII;
  }

  @Override
  public ChecksumVerifier getChecksumVerifier() {
    if (checksumVerifier == null) {
      checksumVerifier = new Code39ChecksumVerifier();
    }
    return checksumVerifier;
  }

  @Override
  public void setChecksumVerifier(ChecksumVerifier checksumVerifier) {
    this.checksumVerifier = checksumVerifier;
  }
}
