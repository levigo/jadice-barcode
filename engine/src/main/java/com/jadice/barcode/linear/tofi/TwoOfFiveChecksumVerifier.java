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
package com.jadice.barcode.linear.tofi;

import com.jadice.barcode.AbstractDecoder.CodeString;
import com.jadice.barcode.checksum.ChecksumVerifier;

/**
 * A default implementation for a 2 of 5 checksum verifier.
 */
public class TwoOfFiveChecksumVerifier implements ChecksumVerifier {

  @Override
  public boolean verifyChecksum(CodeString result) {
    final int[] codes = result.getCodes();

    int calculatedChecksum = calculateChecksum(codes);
    int checksum = codes[codes.length - 1];

    // calculated checksum matches appended checksum
    if (calculatedChecksum == checksum) {
      result.setChecksumVerificationOK(true);
      return true;
    }

    return false;
  }

  private int calculateChecksum(int[] codes) {
    int checksum = 0;
    for (int i = 0; i < codes.length - 1; i++)
      checksum += codes[i] * (i % 2 != 0 ? 3 : 1);
    return checksum % 10;
  }
}
