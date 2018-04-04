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
package org.knime.dl.tensorflow.keras;

import java.io.IOException;
import java.net.URL;

import org.knime.core.data.filestore.FileStore;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLMissingExtensionException;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetwork;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetworkSpec;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonDefaultContext;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;
import org.knime.dl.python.util.DLPythonSourceCodeBuilder;
import org.knime.dl.python.util.DLPythonUtils;
import org.knime.dl.tensorflow.core.TFNetwork;
import org.knime.dl.tensorflow.core.TFNetworkSpec;
import org.knime.dl.tensorflow.core.convert.TFAbstractModelConverter;
import org.knime.dl.tensorflow.savedmodel.core.TFMetaGraphDef;
import org.knime.dl.tensorflow.savedmodel.core.TFSavedModel;
import org.knime.dl.tensorflow.savedmodel.core.TFSavedModelNetwork;
import org.knime.dl.tensorflow.savedmodel.core.TFSavedModelNetworkSpec;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class TFKerasModelConverter
		extends TFAbstractModelConverter<DLKerasTensorFlowNetwork, DLKerasTensorFlowNetworkSpec> {

	private static final String SAVE_TAG = "knime";

	private static final String SIGNATURE_KEY = "serve";

	public TFKerasModelConverter() {
		super(DLKerasTensorFlowNetwork.class, DLKerasTensorFlowNetworkSpec.class, TFSavedModelNetwork.class);
	}

	@Override
	public TFNetworkSpec convertSpecInternal(final DLKerasTensorFlowNetworkSpec spec) {
		// NB: We return null because we don't know the spec yet.
		// TODO document it in the API
		// TODO check if we can create the specs without starting python
		return null;
	}

	@Override
	public TFNetwork convertNetworkInternal(final DLKerasTensorFlowNetwork network, final FileStore fileStore) {
		try {
			final URL saveURL = fileStore.getFile().toURI().toURL();
			final String savePath = fileStore.getFile().getAbsolutePath();

			final DLPythonContext pythonContext = new DLPythonDefaultContext();

			// Save the keras model as a SavedModel using python
			final DLPythonNetworkHandle networkHandle = DLPythonNetworkLoaderRegistry.getInstance()
					.getNetworkLoader(network.getClass())
					.orElseThrow(() -> new DLMissingExtensionException(
							"Python back end '" + network.getClass().getCanonicalName()
									+ "' could not be found. Are you missing a KNIME Deep Learning extension?"))
					.load(network.getSource(), pythonContext, false);
			final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
					.a("import DLPythonNetwork") //
					.n("import keras.backend as K") //
					.n("from tensorflow import saved_model") //
					.n("model = DLPythonNetwork.get_network(").as(networkHandle.getIdentifier()).a(").model") //
					.n("print(model)") //
					.n("builder = saved_model.builder.SavedModelBuilder(").as(savePath).a(")") //
					.n("signature = saved_model.signature_def_utils.predict_signature_def(") //
					.n().t().a("inputs={").as("input").a(": model.input},") // TODO multiple inputs and outputs
					.n().t().a("outputs={").as("output").a(": model.output})") //
					.n("signature_def_map = { ").as(SIGNATURE_KEY).a(": signature }") //
					.n("builder.add_meta_graph_and_variables(K.get_session(), [").as(SAVE_TAG)
					.a("], signature_def_map=signature_def_map)") //
					.n("builder.save()");
			pythonContext.executeInKernel(b.toString());

			// Create a TFSavedModelNetwork
			final TFSavedModel savedModel = new TFSavedModel(saveURL);
			TFMetaGraphDef metaGraphDefs = savedModel.getMetaGraphDefs(new String[] { SAVE_TAG });
			TFSavedModelNetworkSpec specs = metaGraphDefs.createSpecs(SIGNATURE_KEY);

			return specs.create(saveURL);
		} catch (DLInvalidSourceException | DLInvalidEnvironmentException | DLMissingExtensionException
				| IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}