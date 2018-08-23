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
package com.jadice.barcode.linear.c128;

import com.jadice.barcode.Settings;
import com.jadice.barcode.checksum.Checksum;
import com.jadice.barcode.checksum.ChecksumVerifier;

public class Code128Settings implements Settings, Checksum {

  /**
   * Wether checksum verification is active.
   */
  private boolean enableChecksumVerification = true;

  /**
   * The checksumverifier for Code128.
   */
  private ChecksumVerifier checksumVerifier = null;

  @Override
  public boolean isEnableChecksumVerification() {
    return enableChecksumVerification;
  }

  @Override
  public void setEnableChecksumVerification(boolean enableChecksumVerification) {
    this.enableChecksumVerification = enableChecksumVerification;
  }

  @Override
  public ChecksumVerifier getChecksumVerifier() {
    if (checksumVerifier == null) {
      checksumVerifier = new Code128ChecksumVerifier();
    }
    return checksumVerifier;
  }

  @Override
  public void setChecksumVerifier(ChecksumVerifier checksumVerifier) {
    this.checksumVerifier = checksumVerifier;
  }
}
