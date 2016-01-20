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

    int checkSum = 0;
    for (int i = 0; i < codes.length - 1; i++)
      checkSum += codes[i] * (i % 2 != 0 ? 3 : 1);

    if (checkSum % 10 == 0) {
      result.setChecksumVerificationOK(true);
      return true;
    }

    return false;
  }
}
