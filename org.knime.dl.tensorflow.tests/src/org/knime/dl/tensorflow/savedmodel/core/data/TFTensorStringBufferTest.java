/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.dl.tensorflow.savedmodel.core.data;

import static org.junit.Assert.*;
import static org.knime.dl.tensorflow.testing.TFTestUtil.*;

import java.util.Arrays;

import org.junit.Test;
import org.knime.dl.core.DLDefaultFixedTensorShape;
import org.tensorflow.Tensor;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class TFTensorStringBufferTest {

	@Test
	public void testGetCapacity() throws Exception {
		try (TFTensorStringBuffer buffer = new TFTensorStringBuffer(new long[] { 10l })) {
			assertEquals(10l, buffer.getCapacity());
		}
	}

	@Test
	public void testZeroPad() throws Exception {
		try (TFTensorStringBuffer buffer = new TFTensorStringBuffer(new long[] { 10l })) {
			String value = "knime";
			fillBufferWithValue(buffer, value);
			assertBufferFilledWithValue(buffer, value);
			buffer.reset();
			buffer.zeroPad(buffer.getCapacity());
			assertBufferFilledWithValue(buffer, "");
		}
	}

	@Test
	public void testPutRead() throws Exception {
		try (TFTensorStringBuffer buffer = new TFTensorStringBuffer(new long[] { 10l })) {
			String value = "knime";
			fillBufferWithValue(buffer, value);
			assertBufferFilledWithValue(buffer, value);
		}
	}

	@Test
	public void testPutAll() throws Exception {
		try (TFTensorStringBuffer buffer = new TFTensorStringBuffer(new long[] { 10l })) {
			String value = "knime";
			String[] values = new String[10];
			Arrays.fill(values, value);
			buffer.putAll(values);
			assertBufferFilledWithValue(buffer, value);
		}
	}

	@Test
	public void testSize() throws Exception {
		try (TFTensorStringBuffer buffer = new TFTensorStringBuffer(new long[] { 10l })) {
			assertEquals(0, buffer.size());
			buffer.put("knime");
			assertEquals(1, buffer.size());
			buffer.put("knime");
			assertEquals(2, buffer.size());
		}
	}

	@Test
	public void testWriteReadTensor() throws Exception {
		try (TFTensorStringBuffer buffer = new TFTensorStringBuffer(new long[] { 10l })) {
			String value = "knime";
			fillBufferWithValue(buffer, value);
			try (Tensor<String> tensor = buffer.readIntoTensor(10l, new DLDefaultFixedTensorShape(new long[] { 1l }))) {
				buffer.reset();
				String empty = "";
				fillBufferWithValue(buffer, empty);
				assertBufferFilledWithValue(buffer, empty);
				buffer.writeFromTensor(tensor);
			}
			buffer.resetRead();
			assertBufferFilledWithValue(buffer, value);
		}
	}
}
