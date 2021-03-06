/*-
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package org.deeplearning4j.nn.conf.layers;

import lombok.*;
import org.deeplearning4j.exception.DL4JInvalidConfigException;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.ParamInitializer;
import org.deeplearning4j.nn.conf.CacheMode;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.memory.LayerMemoryReport;
import org.deeplearning4j.nn.conf.memory.MemoryReport;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.util.LayerValidation;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**Dense layer: fully connected feed forward layer trainable by backprop.
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DenseLayer extends FeedForwardLayer {

    private DenseLayer(Builder builder) {
        super(builder);
    }

    @Override
    public Layer instantiate(NeuralNetConfiguration conf, Collection<IterationListener> iterationListeners,
                    int layerIndex, INDArray layerParamsView, boolean initializeParams) {
        LayerValidation.assertNInNOutSet("DenseLayer", getLayerName(), layerIndex, getNIn(), getNOut());

        org.deeplearning4j.nn.layers.feedforward.dense.DenseLayer ret =
                        new org.deeplearning4j.nn.layers.feedforward.dense.DenseLayer(conf);
        ret.setListeners(iterationListeners);
        ret.setIndex(layerIndex);
        ret.setParamsViewArray(layerParamsView);
        Map<String, INDArray> paramTable = initializer().init(conf, layerParamsView, initializeParams);
        ret.setParamTable(paramTable);
        ret.setConf(conf);
        return ret;
    }

    @Override
    public ParamInitializer initializer() {
        return DefaultParamInitializer.getInstance();
    }

    @Override
    public LayerMemoryReport getMemoryReport(InputType inputType) {
        InputType outputType = getOutputType(-1, inputType);

        int numParams = initializer().numParams(this);
        int updaterStateSize = (int)getIUpdater().stateSize(numParams);

        int trainSizeFixed = 0;
        int trainSizeVariable = 0;
        if(getDropOut() > 0){
            if(false) {
                //TODO drop connect
                //Dup the weights... note that this does NOT depend on the minibatch size...
                trainSizeVariable += 0; //TODO
            } else {
                //Assume we dup the input
                trainSizeVariable += inputType.arrayElementsPerExample();
            }
        }

        //Also, during backprop: we do a preOut call -> gives us activations size equal to the output size
        // which is modified in-place by activation function backprop
        // then we have 'epsilonNext' which is equivalent to input size
        trainSizeVariable += outputType.arrayElementsPerExample();

        return new LayerMemoryReport.Builder(layerName, DenseLayer.class, inputType, outputType)
                .standardMemory(numParams, updaterStateSize)
                .workingMemory(0, 0, trainSizeFixed, trainSizeVariable)     //No additional memory (beyond activations) for inference
                .cacheMemory(MemoryReport.CACHE_MODE_ALL_ZEROS, MemoryReport.CACHE_MODE_ALL_ZEROS) //No caching in DenseLayer
                .build();
    }

    @AllArgsConstructor
    public static class Builder extends FeedForwardLayer.Builder<Builder> {

        @Override
        @SuppressWarnings("unchecked")
        public DenseLayer build() {
            return new DenseLayer(this);
        }
    }

}
