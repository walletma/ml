package edu.berkeley.compbio.ml.cluster.svm;

import com.davidsoergel.dsutils.CollectionIteratorFactory;
import com.davidsoergel.dsutils.GenericFactory;
import com.davidsoergel.dsutils.GenericFactoryException;
import com.davidsoergel.dsutils.collections.Symmetric2dBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import edu.berkeley.compbio.ml.cluster.BatchCluster;
import edu.berkeley.compbio.ml.cluster.Cluster;
import edu.berkeley.compbio.ml.cluster.ClusterException;
import edu.berkeley.compbio.ml.cluster.ClusterMove;
import edu.berkeley.compbio.ml.cluster.Clusterable;
import edu.berkeley.compbio.ml.cluster.NoGoodClusterException;
import edu.berkeley.compbio.ml.cluster.SupervisedOnlineClusteringMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class MulticlassSVM<T extends Clusterable<T>> extends SupervisedOnlineClusteringMethod<T, BatchCluster<T>>
	{
	Symmetric2dBiMap<BatchCluster<T>, BinarySVM<T, BatchCluster<T>>> allVsAllClassifiers;
	Map<BatchCluster<T>, BinarySVM<T, BatchCluster<T>>> oneVsAllClassifiers;

	Map<String, BatchCluster<T>> theClusterMap;

	public void initializeWithRealData(Iterator<T> trainingIterator, int initSamples,
	                                   GenericFactory<T> prototypeFactory)
			throws GenericFactoryException, ClusterException
		{
		// do nothing with the iterator or any of that
		assert initSamples == 0;

		// by analogy with BayesianClustering, take this opportunity to initialize the clusters

		theClusterMap = new HashMap<String, BatchCluster<T>>(mutuallyExclusiveLabels.size());
		int i = 0;
		for (String label : mutuallyExclusiveLabels)
			{
			BatchCluster<T> cluster = theClusterMap.get(label);

			if (cluster == null)
				{
				cluster = new BatchCluster<T>(i++);
				theClusterMap.put(label, cluster);
				}
			}
		}


	/**
	 * Return a ClusterMove object describing the best way to reassign the given point to a new cluster.
	 *
	 * @param p
	 * @return
	 */
	public ClusterMove bestClusterMove(T p) throws NoGoodClusterException
		{
		// phase 1: classify by all vs. all voting

		final Multiset<BatchCluster<T>> votes = new HashMultiset<BatchCluster<T>>();

		for (BinarySVM<T, BatchCluster<T>> svm : allVsAllClassifiers.values())
			{
			votes.add(svm.classify(p));
			}

		BatchCluster<T> winner;// = votes.getDominantKey();

		// PERF

		List<BatchCluster<T>> clustersInVoteOrder = new ArrayList<BatchCluster<T>>(votes.elementSet());

		Collections.sort(clustersInVoteOrder, new Comparator<Cluster<T>>()
		{
		public int compare(Cluster<T> o1, Cluster<T> o2)
			{
			int v1 = votes.count(o1);
			int v2 = votes.count(o2);
			return v1 < v2 ? -1 : v1 == v2 ? 0 : 1;
			}
		});

		winner = clustersInVoteOrder.get(0);


		// phase 2: reject classification by one vs. all

		if (!oneVsAllClassifiers.get(winner).classify(p).equals(winner))
			{
			throw new NoGoodClusterException("Winning bin rejected by one-vs-all filter");
			}

		// if the top hit is rejected, should we try the second hit, etc.?
		// that's why we bothered sorting tho whore list above

		ClusterMove<T, BatchCluster<T>> result = new ClusterMove<T, BatchCluster<T>>();
		result.bestCluster = winner;
		return result;
		}

	/**
	 * consider each of the incoming data points exactly once.
	 */
	public void train(CollectionIteratorFactory<T> trainingCollectionIteratorFactory)
			throws IOException, ClusterException
		{
		// throw out any existing classifiers

		allVsAllClassifiers = new Symmetric2dBiMap<BatchCluster<T>, BinarySVM<T, BatchCluster<T>>>();
		oneVsAllClassifiers =
				new HashMap<BatchCluster<T>, BinarySVM<T, BatchCluster<T>>>(mutuallyExclusiveLabels.size());

		Iterator<T> trainingIterator = trainingCollectionIteratorFactory.next();

		// separate the training set into label-specific sets, caching all the while
		// (too bad the svm training requires all examples in memory)

		Multimap<String, T> examples = new HashMultimap<String, T>();

		while (trainingIterator.hasNext())
			{
			T sample = trainingIterator.next();
			String label = sample.getWeightedLabels().getDominantKeyInSet(mutuallyExclusiveLabels);
			examples.put(label, sample);
			}

		// create and train all vs all classifiers

		for (BatchCluster<T> cluster1 : theClusters)
			{
			for (BatchCluster<T> cluster2 : theClusters)
				{
				if (cluster2.getId() > cluster1.getId())// avoid redundant pairs
					{
					BinarySVM<T, BatchCluster<T>> svm = new BinarySVM<T, BatchCluster<T>>(cluster1, cluster2);
					allVsAllClassifiers.put(cluster1, cluster2, svm);
					svm.train(cluster1.getPoints(), cluster2.getPoints());
					}
				}
			}

		// create and train one vs all classifiers

		BatchCluster<T> notCluster = new BatchCluster<T>(-1);
		for (Cluster<T> cluster1 : theClusters)
			{
			notCluster.addAll(cluster1);
			}

		for (BatchCluster<T> cluster1 : theClusters)
			{
			notCluster.removeAll(cluster1);

			BinarySVM<T, BatchCluster<T>> svm = new BinarySVM<T, BatchCluster<T>>(cluster1, notCluster);
			oneVsAllClassifiers.put(cluster1, svm);
			svm.train(cluster1.getPoints(), notCluster.getPoints());

			notCluster.addAll(cluster1);
			}

		// we can throw out the examples now
		for (BatchCluster<T> cluster1 : theClusters)
			{
			cluster1.forgetExamples();
			}
		}


	/**
	 * Sets a list of labels to be used for classification.  For a supervised method, this must be called before training.
	 *
	 * @param mutuallyExclusiveLabels
	 */
	public void setLabels(Set<String> mutuallyExclusiveLabels)
		{
		super.setLabels(mutuallyExclusiveLabels);
		}
	}
