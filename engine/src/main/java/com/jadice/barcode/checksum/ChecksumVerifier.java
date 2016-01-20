package com.jadice.barcode.checksum;

import com.jadice.barcode.AbstractDecoder.CodeString;

/**
 * Implementations of ChecksumVerifier are used to verify the integrity of recognized barcodes.
 */
public interface ChecksumVerifier {

  public boolean verifyChecksum(CodeString result);

}
