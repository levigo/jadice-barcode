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
package com.jadice.barcode.grid;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;

/**
 * @author jh
 */
public abstract class AbstractImageDataSource implements BinaryGrid {

	protected Dimension size = new Dimension(0, 0);
	private final int histogramSamplingInterval = 10;

	/**
	 * Sample pixel at coordinate and return whether the pixel can be considered
	 * black.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public abstract boolean samplePixel(int x, int y);

	/**
	 * Get the normalized intensity value (0-255) for the given pixel.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public abstract int getPixelValue(int x, int y);

	public int[] getHistogram() {
		int histogram[] = new int[256];

		for (int y = 0; y < size.height; y += histogramSamplingInterval)
			for (int x = 0; x < size.width; x += histogramSamplingInterval) {
				int value = getPixelValue(x, y);
				if (value < 0)
					value = 0;
				if (value > 255)
					value = 255;
				histogram[value]++;
			}

		return histogram;
	}

	/**
	 * @return
	 */
	public Dimension getSize() {
		return size;
	}

	/**
	 * @return
	 */
	protected void setSize(Dimension s) {
		size = s;
	}

	public AffineTransform getInverseTransform() {
		return new AffineTransform();
	}
}