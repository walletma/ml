package edu.berkeley.compbio.ml.cluster.svm;

import com.davidsoergel.dsutils.CollectionIteratorFactory;
import com.davidsoergel.dsutils.GenericFactory;
import com.davidsoergel.dsutils.GenericFactoryException;
import com.davidsoergel.dsutils.collections.HashWeightedSet;
import com.google.common.base.Function;
import com.google.common.base.Nullable;
import com.google.common.collect.MapMaker;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.jlibsvm.binary.BinaryClassificationSVM;
import edu.berkeley.compbio.jlibsvm.multi.MultiClassModel;
import edu.berkeley.compbio.jlibsvm.multi.MultiClassProblem;
import edu.berkeley.compbio.jlibsvm.multi.MultiClassProblemImpl;
import edu.berkeley.compbio.jlibsvm.multi.MultiClassificationSVM;
import edu.berkeley.compbio.jlibsvm.multi.VotingResult;
import edu.berkeley.compbio.jlibsvm.scaler.NoopScalingModel;
import edu.berkeley.compbio.ml.cluster.BatchCluster;
import edu.berkeley.compbio.ml.cluster.ClusterException;
import edu.berkeley.compbio.ml.cluster.ClusterMove;
import edu.berkeley.compbio.ml.cluster.Clusterable;
import edu.berkeley.compbio.ml.cluster.NoGoodClusterException;
import edu.berkeley.compbio.ml.cluster.SupervisedOnlineClusteringMethod;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MultiClassificationSVMAdapter<T extends Clusterable<T>>
		extends SupervisedOnlineClusteringMethod<T, BatchCluster<T>>
	{
	private static final Logger logger = Logger.getLogger(MultiClassificationSVMAdapter.class);

	private MultiClassificationSVM<BatchCluster<T>, T> svm;
	private MultiClassModel<BatchCluster<T>, T> model;


	private BinaryClassificationSVM<BatchCluster<T>, T> binarySvm;

	//public MultiClassificationSVMAdapter(@NotNull ImmutableSvmParameter<BatchCluster<T>, T> param)
	//	{
	//	super(null);
	//	this.param = param;
	//	}

	public MultiClassificationSVMAdapter(Set<String> potentialTrainingBins, Set<String> predictLabels,
	                                     Set<String> leaveOneOutLabels, Set<String> testLabels,
	                                     @NotNull ImmutableSvmParameter<BatchCluster<T>, T> param)
		{
		super(null, potentialTrainingBins, predictLabels, leaveOneOutLabels, testLabels);
		this.param = param;
		}

	public void setBinarySvm(BinaryClassificationSVM<BatchCluster<T>, T> binarySvm)
		{
		this.binarySvm = binarySvm;
		}


	ImmutableSvmParameter<BatchCluster<T>, T> param;


	public void train(CollectionIteratorFactory<T> trainingCollectionIteratorFactory)
			throws IOException, ClusterException
		{
		Iterator<T> trainingIterator = trainingCollectionIteratorFactory.next();

		// cache the training set into an example map
		//  (too bad the svm training requires all examples in memory)

		//Multimap<String, T> examples = new HashMultimap<String, T>();
		Map<T, BatchCluster<T>> examples = new HashMap<T, BatchCluster<T>>();
		Map<T, Integer> exampleIds = new HashMap<T, Integer>();

		int c = 0;
		while (trainingIterator.hasNext())
			{
			T sample = trainingIterator.next();
			String label = sample.getWeightedLabels().getDominantKeyInSet(potentialTrainingBins);

			BatchCluster<T> cluster = theClusterMap.get(label);
			cluster.add(sample);

			examples.put(sample, cluster);
			exampleIds.put(sample, c);
			c++;

			if (c % 1000 == 0)
				{
				logger.debug("Prepared " + c + " training samples");
				}
			}
		logger.debug("Prepared " + c + " training samples");

		svm = new MultiClassificationSVM<BatchCluster<T>, T>(binarySvm);
		MultiClassProblem<BatchCluster<T>, T> problem =
				new MultiClassProblemImpl<BatchCluster<T>, T>(BatchCluster.class, new BatchClusterLabelInverter<T>(),
				                                              examples, exampleIds, new NoopScalingModel<T>());
		//svm.setupQMatrix(problem);
		logger.debug("Performing multiclass training");
		model = svm.train(problem, param);

		if (leaveOneOutLabels != null)
			{
			leaveOneOutModels =
					new MapMaker().makeComputingMap(new Function<String, MultiClassModel<BatchCluster<T>, T>>()
					{
					public MultiClassModel<BatchCluster<T>, T> apply(@Nullable String disallowedLabel)
						{
						Set<BatchCluster<T>> disallowedClusters = new HashSet<BatchCluster<T>>();

						for (BatchCluster<T> cluster : model.getLabels())
							{
							if (cluster.getWeightedLabels().getDominantKeyInSet(leaveOneOutLabels)
									.equals(disallowedLabel))
								{
								disallowedClusters.add(cluster);
								}
							}
						return new MultiClassModel<BatchCluster<T>, T>(model, disallowedClusters);
						}
					});
			}
		}


	Map<String, BatchCluster<T>> theClusterMap;

	public void initializeWithRealData(Iterator<T> trainingIterator, int initSamples,
	                                   GenericFactory<T> prototypeFactory)
			throws GenericFactoryException, ClusterException
		{		// do nothing with the iterator or any of that
		assert initSamples == 0;

		// by analogy with BayesianClustering, take this opportunity to initialize the clusters

		theClusterMap = new HashMap<String, BatchCluster<T>>(potentialTrainingBins.size());
		int i = 0;
		for (String label : potentialTrainingBins)
			{
			BatchCluster<T> cluster = theClusterMap.get(label);

			if (cluster == null)
				{
				cluster = new BatchCluster<T>(i++);
				theClusterMap.put(label, cluster);

				// ** consider how best to store the test labels
				HashWeightedSet<String> derivedLabelProbabilities = new HashWeightedSet<String>();
				derivedLabelProbabilities.add(label, 1.);
				derivedLabelProbabilities.incrementItems();
				cluster.setDerivedLabelProbabilities(derivedLabelProbabilities);
				}
			}
		theClusters = theClusterMap.values();
		}

	Map<String, MultiClassModel<BatchCluster<T>, T>> leaveOneOutModels;

	public ClusterMove<T, BatchCluster<T>> bestClusterMove(T p) throws NoGoodClusterException
		{
		MultiClassModel<BatchCluster<T>, T> leaveOneOutModel = model;
		if (leaveOneOutLabels != null)
			{
			try
				{
				String disallowedLabel = p.getWeightedLabels().getDominantKeyInSet(leaveOneOutLabels);
				leaveOneOutModel = leaveOneOutModels.get(disallowedLabel);
				}
			catch (NoSuchElementException e)
				{
				// OK, just use the full model then
				//leaveOneOutModel = model;
				}
			}


		VotingResult<BatchCluster<T>> r = leaveOneOutModel.predictLabelWithQuality(p);
		ClusterMove<T, BatchCluster<T>> result = new ClusterMove<T, BatchCluster<T>>();
		result.bestCluster = r.getBestLabel();

		result.voteProportion = r.getBestVoteProportion();
		result.secondBestVoteProportion = r.getSecondBestVoteProportion();

		result.bestDistance = r.getBestProbability();
		result.secondBestDistance = r.getSecondBestProbability();


		//**  just drop these for now
		/*
		r.getBestOneVsAllProbability();
		r.getSecondBestOneVsAllProbability();

		r.getBestOneClassProbability();
		r.getSecondBestOneClassProbability();
		*/


		if (result.bestCluster == null)
			{
			throw new NoGoodClusterException();
			}

		// no other fields of ClusterMove are populated :(
		return result;
		}
	}
