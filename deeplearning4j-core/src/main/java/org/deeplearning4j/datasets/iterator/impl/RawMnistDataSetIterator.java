package org.deeplearning4j.datasets.iterator.impl;

import java.io.IOException;

import org.deeplearning4j.datasets.fetchers.MnistDataFetcher;
import org.deeplearning4j.datasets.iterator.BaseDatasetIterator;

public class RawMnistDataSetIterator extends BaseDatasetIterator {

	public RawMnistDataSetIterator(int batch, int numExamples) throws IOException {
		super(batch, numExamples,new MnistDataFetcher(false));

	}



}
