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

import com.jadice.barcode.AbstractDecoder.CodeString;
import com.jadice.barcode.checksum.ChecksumVerifier;

public class Code39ChecksumVerifier implements ChecksumVerifier {

  @Override
  public boolean verifyChecksum(CodeString result) {
    int[] codes = result.getCodes();
    if (codes.length > 2) {
      int sum = 0;
      for (int i = 1; i < codes.length - 2; i++) {
        Integer s = Code39Constants.INDEXBYSYMBOL.get((char) codes[i]);
        if (null == s)
          break;
        sum += s;
      }

      Integer cs = Code39Constants.INDEXBYSYMBOL.get((char) codes[codes.length - 2]);
      if (null != cs && (sum % 43) == cs.intValue()) {
        result.setChecksumVerificationOK(true);
        return true;
      }
    }
    return false;
  }

}
