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
package com.jadice.barcode.twod.dmtx;

import java.util.Arrays;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.jadice.barcode.AbstractDecodeTest;
import com.jadice.barcode.Options;

@RunWith(Parameterized.class)
public class DmtxTestSimple extends AbstractDecodeTest {
  private static final String BLURB_14 = "http://www.libdmtx.org";
  private static final String BLURB_13 = "30Q324343430794<OQQ";
  private static final String BLURB_12 = "123456789012345678901234567890123456789012345678901234567890";
  private static final String BLURB_11 = "123456";
  private static final String BLURB_10 = "abcdefghijklmABCDEFGHIJKLM0123456789";
  private static final String BLURB_9 = "abcdefghijklmnopqrsABCDEFGHIJKLMNOPQRS";
  private static final String BLURB_8 = "bathroom mirror casts confusing looks from me to me";
  private static final String BLURB_6 = "and when i squinted the WORLD SEEMED ROSE TINTED";
  private static final String BLURB_5 = "AND WHEN I SQUINTED THE WORLD SEEMED ROSE TINTED";
  private static final String BLURB_4 = "as the raindrops penetrate the silence all around";
  private static final String BLURB_1 = "libdmtx is a shared library for Linux that can be used to read (scan & decode) and write (encode & print) 2D Data Matrix barcode symbols.  It is released under the LGPL and can be used and distributed freely under these terms.\n"
      + "Data Matrix barcodes are two-dimensional symbols that hold a dense pattern of data with built-in error correction.  The Data Matrix symbology (sometimes referred to as DataMatrix) was invented and released into the public domain by RVSI Acuity CiMatrix.  Wikipedia has a good article on the symbology and its characteristics.";
  private static final String BLURB_2 = "This test case contains newline charactes,\n"
      + "including in the middle and at the end of\n" + "the message.\n";
  private static final String BLURB_3 = "between subtle shading and the absence of light";
  private static final String BLURB_7 = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. "
      + "Vivamus vitae diam ac ante tristique faucibus. In ac velit. "
      + "Pellentesque enim ligula, accumsan vitae, convallis non, molestie quis, neque. "
      + "Nullam in lacus. Praesent convallis. Suspendisse ut est non augue sollicitudin semper. "
      + "Donec dui arcu, blandit ac, fermentum nec, tempor non, nibh. "
      + "Nunc ut purus vel lorem dignissim consequat. Donec felis. "
      + "Suspendisse vel dolor ac lacus ullamcorper nonummy. In hac habitasse platea dictumst. "
      + "Ut dolor nunc, tincidunt id, cursus a, vehicula adipiscing, urna. "
      + "Integer eget enim eu nunc elementum sollicitudin. Etiam sodales condimentum nulla. "
      + "Nam blandit molestie felis. Ut faucibus sollicitudin sem.";

  // @formatter:off
	private static Object PARAMS[][] = { //
			{ "compare_confirmed/barcode_004_a.png", BLURB_11 }, //
			{ "compare_confirmed/barcode_014_c.png", "A1B2C3D4E5F6G7H8I9J0K1L2" }, //
			{ "compare_confirmed/barcode_060_e.png", "AB" }, //
			{ "compare_siemens/siemens_000_8.png", BLURB_9 }, //
			{ "compare_siemens/siemens_000_a.png", BLURB_9 }, //
			{ "compare_siemens/siemens_000_c.png", BLURB_9 }, //
			{ "compare_siemens/siemens_000_f.png", BLURB_9 }, //
			{ "compare_siemens/siemens_000_t.png", BLURB_9 }, //
			{ "compare_siemens/siemens_001_8.png", BLURB_10 }, //
			{ "compare_siemens/siemens_001_a.png", BLURB_10 }, //
			{ "compare_siemens/siemens_001_c.png", BLURB_10 }, //
			{ "compare_siemens/siemens_001_f.png", BLURB_10 }, //
			{ "compare_siemens/siemens_001_t.png", BLURB_10 }, //
			{ "compare_siemens/siemens_002_8.png", BLURB_1 }, // 
			{ "compare_siemens/siemens_002_a.png", BLURB_1 }, // 
			{ "compare_siemens/siemens_002_c.png", BLURB_1 }, //
			{ "compare_siemens/siemens_002_f.png", BLURB_1 }, //
			{ "compare_siemens/siemens_002_t.png", BLURB_1 }, //
			{ "compare_siemens/siemens_003_8.png", BLURB_2 }, //
			{ "compare_siemens/siemens_003_a.png", BLURB_2 }, //
			{ "compare_siemens/siemens_003_c.png", BLURB_2 }, //
			{ "compare_siemens/siemens_003_f.png", BLURB_2 }, //
			{ "compare_siemens/siemens_003_t.png", BLURB_2 }, //
			{ "compare_siemens/siemens_004_8.png", BLURB_11 }, //
			{ "compare_siemens/siemens_004_a.png", BLURB_11 }, //
			{ "compare_siemens/siemens_004_c.png", BLURB_11 }, //
			{ "compare_siemens/siemens_004_f.png", BLURB_11 }, //
			{ "compare_siemens/siemens_004_t.png", BLURB_11 }, //
			{ "compare_siemens/siemens_005_8.png", BLURB_12 }, //
			{ "compare_siemens/siemens_005_a.png", BLURB_12 }, //
			{ "compare_siemens/siemens_005_c.png", BLURB_12 }, //
			{ "compare_siemens/siemens_005_f.png", BLURB_12 }, //
			{ "compare_siemens/siemens_005_t.png", BLURB_12 }, //
			{ "compare_siemens/siemens_006_8.png", BLURB_13 }, //
			{ "compare_siemens/siemens_006_a.png", BLURB_13 }, //
			{ "compare_siemens/siemens_006_c.png", BLURB_13 }, //
			{ "compare_siemens/siemens_006_f.png", BLURB_13 }, //
			{ "compare_siemens/siemens_006_t.png", BLURB_13 }, //
			{ "compare_siemens/siemens_007_8.png", BLURB_3 }, //
			{ "compare_siemens/siemens_007_a.png", BLURB_3 }, //
			{ "compare_siemens/siemens_007_c.png", BLURB_3 }, //
			{ "compare_siemens/siemens_007_f.png", BLURB_3 }, //
			{ "compare_siemens/siemens_007_t.png", BLURB_3 }, //
			{ "compare_siemens/siemens_008_8.png", BLURB_4 }, //
			{ "compare_siemens/siemens_008_a.png", BLURB_4 }, //
			{ "compare_siemens/siemens_008_c.png", BLURB_4 }, //
			{ "compare_siemens/siemens_008_f.png", BLURB_4 }, //
			{ "compare_siemens/siemens_008_t.png", BLURB_4 }, //
			{ "compare_siemens/siemens_009_8.png", BLURB_8 }, //
			{ "compare_siemens/siemens_009_a.png", BLURB_8 }, //
			{ "compare_siemens/siemens_009_c.png", BLURB_8 }, //
			{ "compare_siemens/siemens_009_f.png", BLURB_8 }, //
			{ "compare_siemens/siemens_009_t.png", BLURB_8 }, //
			{ "compare_siemens/siemens_010_8.png", BLURB_5 }, //
			{ "compare_siemens/siemens_010_a.png", BLURB_5 }, //
			{ "compare_siemens/siemens_010_c.png", BLURB_5 }, //
			{ "compare_siemens/siemens_010_f.png", BLURB_5 }, //
			{ "compare_siemens/siemens_010_t.png", BLURB_5 }, //
			{ "compare_siemens/siemens_011_8.png", BLURB_14 }, //
			{ "compare_siemens/siemens_011_a.png", BLURB_14 }, //
			{ "compare_siemens/siemens_011_c.png", BLURB_14 }, //
			{ "compare_siemens/siemens_011_f.png", BLURB_14 }, //
			{ "compare_siemens/siemens_011_t.png", BLURB_14 }, //
			{ "compare_siemens/siemens_012_8.png", BLURB_6 }, //
			{ "compare_siemens/siemens_012_a.png", BLURB_6 }, //
			{ "compare_siemens/siemens_012_c.png", BLURB_6 }, //
			{ "compare_siemens/siemens_012_f.png", BLURB_6 }, //
			{ "compare_siemens/siemens_012_t.png", BLURB_6 }, //
			{ "compare_siemens/siemens_013_8.png", BLURB_7 }, //
			{ "compare_siemens/siemens_013_a.png", BLURB_7 }, //
			{ "compare_siemens/siemens_013_c.png", BLURB_7 }, //
			{ "compare_siemens/siemens_013_f.png", BLURB_7 }, //
			{ "compare_siemens/siemens_013_t.png", BLURB_7 }, //
			
// The tests marked with NO_LICENSE can currently not be included, because
// the distribution license for those images could not be obtained.
// They can, however, be obtained by downloading the LIBDMTX source code from
// http://www.libdmtx.org/
//
// NO_LICENSE { "images_public/crea_bad.png", "Jusan Network Srl" }, //
// NO_LICENSE { "images_public/crea_converted.png", "Example Data Matrix" }, //
// NO_LICENSE { "images_public/crea_converted2.png", "Example Data Matrix" }, //
// NO_LICENSE { "images_public/crea_grayscaled.png", "Example Data Matrix" }, //
// NO_LICENSE { "images_public/crea_grayscaled2.png", "Example Data Matrix" }, //
// NO_LICENSE { "images_public/crea_multi-symbol1_rejected.png", BLURB_8 }, //
// NO_LICENSE { "images_public/crea_multi-symbol2_rejected.png", BLURB_8 }, //
// NO_LICENSE { "images_public/crea_multi-symbol3.png", "Deutsche Post: sample print of new generation digital franking meter. Void." }, //
// NO_LICENSE { "images_public/crea_original.jpg", "Example Data Matrix" }, //
// NO_LICENSE { "images_public/crea_original2.jpg", "Example Data Matrix" }, //
// NO_LICENSE { "images_public/crea_rejected.png", null }, //
// NO_LICENSE { "images_public/crea_too_big.png", "Jusan Network Srl" }, //
			// FIXME very slow
			// { "images_public/crea_too_little.png", null }, //
// NO_LICENSE { "images_public/crea_too_long_segfault.png", "now we're testing the library with a phrase longer than normal" }, //
// NO_LICENSE { "images_public/laughton_ABABABe_C40.png", "ABABABe" }, //
// NO_LICENSE { "images_public/laughton_ABCABCABCe_C40.png", "ABCABCABCe" }, //
// NO_LICENSE { "images_public/laughton_ABe_C40.png", "ABe" }, //
// NO_LICENSE { "images_public/laughton_ABeABCABCABCA_C40.png", "ABeABCABCABCA" }, //
// NO_LICENSE { "images_public/laughton_big_mail_bright.png", "04242310301" }, //
// NO_LICENSE { "images_public/laughton_big_mail_dark.png", "04242310301" }, //
// NO_LICENSE { "images_public/laughton_color_vs_grey.png", "No, Minnesota is not \"one of the rectangle states in the middle.\"" }, //
// NO_LICENSE { "images_public/laughton_d255_c40.png", "\u00ff" }, //
// NO_LICENSE { "images_public/laughton_dotpeen_clean.png", "dot peen" }, //
// NO_LICENSE { "images_public/laughton_dotpeen_crazy.png", "dot peen" }, //
// NO_LICENSE { "images_public/laughton_giant_multi.png", 
//			  "README for libdmtx version 0.3.0 - October 15, 2006" +
//			  "-----------------------------------------------------------------" +
//			  "libdmtx version 0.3.0 doesnt have all the features I was hoping to " +
//			  "add before the next release, but its at a point where I need to break " +
//			  "the library for a while in order to rip out and replace the main region " +
//			  "detection algorithm.Before breaking the CVS version, though, Im " +
//			  "releasing v0.3 so everyone can continue using the latest code with all " +
//			  "the restructuring, bug fixes, and enhancements that have happened since " +
//			  "v0.2.Revisiting the disclaimers from earlier releases, you might pick up " +
//			  "on a more optimistic tone:   " +
//			  "* It shouldnt crash very often anymore (please let me know)   " +
//			  "* It definitely shouldnt hang your system   " +
//			  "* The API will still change, but not as frequently" +
//			  "Im already working on the next release, so hopefully it wont take as " +
//			  "long to get it out the door.  And a special thanks to everyone who has " +
//			  "contributed questions, ideas, bug reports, and patches since the last release." }, //
// NO_LICENSE { "images_public/laughton_large_multi.png", 
//			    "I've noticed that the USPS has been increasingly printing Data Matrix " +
//			    "barcodes on envelopes as part of the normal delivery process.  When I see " +
//			    "stuff like this it drives me crazy not knowing what it says.  " +
//			    "Turns out it says \"2669394000020000CI\".  Snore.  And yes, that's my thumb." }, //
// NO_LICENSE { "images_public/laughton_mosaic-blue.png", "BLU LAYER" }, //
// NO_LICENSE { "images_public/laughton_mosaic-green.png", "GRN LAYER" }, //
// NO_LICENSE { "images_public/laughton_mosaic-red.png", "RED LAYER" }, //
// NO_LICENSE { "images_public/laughton_small_multi.png", "This is a test of multi region Data Matrix barcodes." }, //
// NO_LICENSE { "images_public/laughton_test_fax01.png", "GNNNNXSM1T700" }, //
// NO_LICENSE { "images_public/laughton_test_image05_error.png", "123456" }, //
// NO_LICENSE { "images_public/laughton_test_image06_error.png", "Maggie" }, //
// NO_LICENSE { "images_public/potter_Ez1.png", "X" }, //
// NO_LICENSE { "images_public/potter_HandHolding1.png", "X" }, //
// NO_LICENSE { "images_public/potter_HandHolding2.png", "X" }, //
//			{ "images_public/sportcam_LNG06_1009_763r.jpg", "foo" }, //
			// FIXME: way too slow
			// { "images_public/sportcam_LNG06_1057c.jpg", "1057" }, //
			// { "images_public/sportcam_LNG06_1059r2.jpg", "1059" }, //
// NO_LICENSE { "images_public/sportcam_LNG06_1329x1.jpg", "1329" }, //
			// FIXME: way too slow
			// { "images_public/sportcam_LNG06_1332r.jpg", "7732" }, //
			// { "images_public/sportcam_LNG06_1333c.jpg", "1333" }, //
			// { "images_public/sportcam_LNG06_1a.jpg", "1" }, //
			// { "images_public/sportcam_LNG06_1e.jpg", "1" }, //
			// { "images_public/sportcam_LNG06_1r1.jpg", "1" }, //
			// { "images_public/sportcam_LNG06_2c.jpg", "2" }, //
// NO_LICENSE { "images_public/zummo_24x24.png", "ABCDEF" }, //
			// { "images_public/zummo_bc1.png", "1 00000000000           139000000000                        010201T07074" }, //
// NO_LICENSE { "images_public/zummo_bc3.png", "1 00000000000           139000000000                        010201T07074" }, //
// NO_LICENSE { "images_public/zummo_bc3.png", "1 00000000000           139000000000                        010201T07074" }, //
// NO_LICENSE { "images_public/zummo_bc4.png", "1 00000000000           139000000000                        010201T07074" }, //
// NO_LICENSE { "images_public/zummo_bc5.png", "1 00000000000           139000000000                        010201T07074" }, //
			{ "rotate_test/test_image01.png", BLURB_3 }, //
			{ "rotate_test/test_image02.png", BLURB_4 }, //
			{ "rotate_test/test_image03.png", "bathroom mirror casts confusing looks from me to me" }, //
			{ "rotate_test/test_image04.png", "AND WHEN I SQUINTED THE WORLD SEEMED ROSE TINTED" }, //
			{ "rotate_test/test_image05.png", "123456" }, //
			{ "rotate_test/test_image06.png", "Maggie" }, //
			{ "rotate_test/test_image07.png", "Base256 Data Matrix Barcode" }, //
			{ "rotate_test/test_image08.png", null }, //
			// { "rotate_test/test_image09.png", null }, //
			{ "rotate_test/test_image10.png", "20060712141442342817000000" }, //
			{ "rotate_test/test_image11.png", "2669394000020000CI" }, //
			{ "rotate_test/test_image12.png", "9411300724000003" }, //
			{ "rotate_test/test_image13.png", BLURB_14 }, //
			// { "rotate_test/test_image14.png", null }, //
			// { "rotate_test/test_image15.png", null }, //
			{ "rotate_test/test_image16.png", "QRST1234" }, //
			{ "rotate_test/test_image17.png", "Base256" }, //
			{ "rotate_test/test_image18.png", "What do you call an angle after it gets in a car crash?" }, //
	};
	// @formatter:on

  @Parameters
  public static List<Object[]> parameters() {
    return Arrays.asList(PARAMS);
  }

  public DmtxTestSimple(String imageName, String expectedResult) {
    super(imageName, expectedResult);
  }

  @Override
  protected void initOptions(Options options) {
    options.getOptions(DatamatrixSettings.class).setMinExtent(8);
  }

  @Override
  protected Datamatrix createSymbology() {
    return new Datamatrix();
  }
}
