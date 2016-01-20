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
