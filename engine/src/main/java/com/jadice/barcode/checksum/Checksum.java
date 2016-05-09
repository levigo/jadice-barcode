package com.jadice.barcode.checksum;

/**
 * Implementations of Checksum provide methods for checksum calculation.
 */
public interface Checksum {

  public boolean isEnableChecksumVerification();

  public void setEnableChecksumVerification(boolean enableChecksumVerification);

  public ChecksumVerifier getChecksumVerifier();

  public void setChecksumVerifier(ChecksumVerifier checksumVerifier);
}
