package com.jadice.barcode.linear.ean;

import com.jadice.barcode.AbstractDecoder.CodeString;
import com.jadice.barcode.checksum.ChecksumVerifier;

public class EANChecksumVerifier implements ChecksumVerifier {

  @Override
  public boolean verifyChecksum(CodeString result) {
    result.setChecksumVerificationOK(false);
    int[] codes = result.getCodes();
    if (codes.length == 14) {
      int sum = 0;
      for (int i = 1; i < 14; i++) {
        int code = codes[i];
        if (code < 0 || code > 9)
          return false;
        sum += code * ((i & 1) == 0 ? 3 : 1);
      }

      if ((sum % 10) == 0){
        result.setChecksumVerificationOK(true);
        return true;
      }
    }
    return false;
  }

}
