package edu.berkeley.compbio.ml.cluster.hierarchical;

import com.davidsoergel.dsutils.collections.Symmetric2dBiMap;
import com.davidsoergel.dsutils.concurrent.Parallel;
import com.davidsoergel.stats.DissimilarityMeasure;
import com.google.common.base.Function;
import edu.berkeley.compbio.ml.cluster.CentroidCluster;
import edu.berkeley.compbio.ml.cluster.ClusterMove;
import edu.berkeley.compbio.ml.cluster.Clusterable;
import edu.berkeley.compbio.ml.cluster.ClusterableIterator;
import edu.berkeley.compbio.ml.cluster.NoGoodClusterException;
import edu.berkeley.compbio.ml.cluster.PointClusterFilter;
import edu.berkeley.compbio.ml.cluster.ProhibitionModel;
import edu.berkeley.compbio.phyloutils.LengthWeightHierarchyNode;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agglomeratively cluster points in an online manner.  Clusters are joined when their distance is within the provided
 * threshold.  During the online process, there may be many active clusters that are not related to each other-- their
 * pairwise distances are known to be greater than the threshold, and there is no root.
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class OnlineAgglomerativeClustering<T extends Clusterable<T>>
		extends OnlineHierarchicalClusteringMethod<T>
	{
	private static final Logger logger = Logger.getLogger(OnlineAgglomerativeClustering.class);
	protected final Symmetric2dBiMap<HierarchicalCentroidCluster<T>, Double> theActiveNodeDistanceMatrix =
			new Symmetric2dBiMap<HierarchicalCentroidCluster<T>, Double>();
	private HierarchicalCentroidCluster<T> theRoot;
	private final Map<T, HierarchicalCentroidCluster<T>> sampleToLeafClusterMap =
			new HashMap<T, HierarchicalCentroidCluster<T>>();
	private final AtomicInteger idCount = new AtomicInteger(0);
	HierarchicalCentroidCluster<T> saveNode;
	double threshold;
	Agglomerator<T> agglomerator;

	public OnlineAgglomerativeClustering(final DissimilarityMeasure<T> dm, final Set<String> potentialTrainingBins,
	                                     final Map<String, Set<String>> predictLabelSets,
	                                     final ProhibitionModel<T> prohibitionModel, final Set<String> testLabels,
	                                     double threshold, Agglomerator agg)
		{
		super(dm, potentialTrainingBins, predictLabelSets, prohibitionModel, testLabels);
		this.threshold = threshold;
		this.agglomerator = agg;
		}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ClusterMove<T, HierarchicalCentroidCluster<T>> bestClusterMove(final T p) throws NoGoodClusterException
		{
		final ClusterMove result = new ClusterMove();
		result.bestDistance = Double.POSITIVE_INFINITY;

		final HierarchicalCentroidCluster<T> c = sampleToLeafClusterMap.get(p);
		if (c != null)
			{
			// this sample was part of the initial clustering, so of course the best cluster is the one representing just the sample itself
			result.bestCluster = c;
			result.bestDistance = 0;
			return result;
			}

		PointClusterFilter<T> clusterFilter = prohibitionModel == null ? null : prohibitionModel.getFilter(p);

		for (final CentroidCluster<T> theCluster : getClusters())
			{
			if (clusterFilter != null && clusterFilter.isProhibited(theCluster))
				{
				// ignore this cluster
				}
			else
				{
				final double distance = measure.distanceFromTo(p, theCluster.getCentroid());
				if (distance < result.bestDistance)
					{
					result.bestCluster = theCluster;
					result.bestDistance = distance;
					}
				}
			}
		if (result.bestCluster == null)
			{
			throw new NoGoodClusterException("No cluster found for point: " + p);
			}
		return result;
		}

	protected synchronized void trainWithKnownTrainingLabels(final ClusterableIterator<T> samples)
		{
		// careful, synchronization is very tricksy here

		Parallel.forEach(samples, new Function<T, Void>()
		{
		public Void apply(final T sample)
			{
			final HierarchicalCentroidCluster<T> c =
					new HierarchicalCentroidCluster<T>(idCount.getAndIncrement(), sample);
			c.doneLabelling();

			// these is synchronized on the cluster list
			// the cluster list is never used in thes implementation; onle the active list in the distance matrix matters
			//addCluster(c);

			//final List<HierarchicalCentroidCluster<T>> allClusters = getClusters();
			// includes all leaves and internal nodes so far.
			// In particular, it includes new leaves from other threads that have not yet appeared in the distance matrix.

			// this is synchronized on the distance matrix, so we probably have to wait for another thread to finish its updates
			final Set<HierarchicalCentroidCluster<T>> activeClusters =
					theActiveNodeDistanceMatrix.getActiveKeys();//getClusters();

			// then things are purely local for a while
			final int ahc = c.hashCode();
			// store up the results in this thread before copying them to the synchronized store at the end
			final Map<HierarchicalCentroidCluster<T>, Double> distancesToC =
					new HashMap<HierarchicalCentroidCluster<T>, Double>(getClusters().size());


			for (HierarchicalCentroidCluster<T> b : activeClusters)
				{
				/*if (b.getParent() != null)
					{
					// this cluster has already been agglomerated, so it's no longer available
					}
				else */
				if (ahc <= b.hashCode() && !c.equals(b))
					{
					final Double d = measure.distanceFromTo(c.getPayload().getCentroid(), b.getPayload().getCentroid());

					// concurrency bottleneck
					//theActiveNodeDistanceMatrix.put(a, b, d);

					distancesToC.put(b, d);
					int numPairs = theActiveNodeDistanceMatrix.numPairs();

					if (numPairs % 10000 == 0)
						{
						int numKeys = theActiveNodeDistanceMatrix.getActiveKeys().size();
						logger.info("Online agglomerative clustering: " + numKeys + " active nodes, " + numPairs
						            + " pair distances");
						}
					}
				}

			synchronized (theActiveNodeDistanceMatrix)
				{
				// we computed distances to all the active clusters previously known,
				// but some of them may have disappeared in the meantime.
				// Just drop them if so, even if the distance was short; too bad, that's the cost of the online setting.

				final Set<HierarchicalCentroidCluster<T>> remainingActiveKeys =
						new HashSet<HierarchicalCentroidCluster<T>>(theActiveNodeDistanceMatrix.getActiveKeys());

				distancesToC.keySet().retainAll(remainingActiveKeys);
				for (Map.Entry<HierarchicalCentroidCluster<T>, Double> entry : distancesToC.entrySet())
					{
					final HierarchicalCentroidCluster<T> b = entry.getKey();
					final Double d = entry.getValue();
					theActiveNodeDistanceMatrix.put(c, b, d);

					remainingActiveKeys.remove(b);
					}

				// also there may be new active nodes for which we didn't compute distances before; that's why we copied the active list before
				// catch up and do them now; too bad about the synchronization
				for (HierarchicalCentroidCluster<T> b : remainingActiveKeys)
					{
					final Double d = measure.distanceFromTo(c.getPayload().getCentroid(), b.getPayload().getCentroid());
					theActiveNodeDistanceMatrix.put(c, b, d);
					}

				// the new node may have various effects, depending on the subclass
				// it may induce multiple joins, e.g. by acting as a bridge in a single-linkage situation

				while (theActiveNodeDistanceMatrix.getSmallestValue() <= threshold)
					{
					final HierarchicalCentroidCluster<T> a = theActiveNodeDistanceMatrix.getKey1WithSmallestValue();
					final HierarchicalCentroidCluster<T> b = theActiveNodeDistanceMatrix.getKey2WithSmallestValue();
					final HierarchicalCentroidCluster<T> composite =
							agglomerator.joinNodes(idCount.getAndIncrement(), a, b, theActiveNodeDistanceMatrix);
					addCluster(composite);
					theRoot = composite;  // this will actually be true on the last iteration
					}
				}

			return null;
			}
		}

		);

		normalizeClusterLabelProbabilities();
		}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LengthWeightHierarchyNode<CentroidCluster<T>, ? extends LengthWeightHierarchyNode> getTree()
		{
		return theRoot;
		}


	protected abstract void computeDistance(HierarchicalCentroidCluster<T> origA, HierarchicalCentroidCluster<T> origB,
	                                        HierarchicalCentroidCluster<T> composite,
	                                        HierarchicalCentroidCluster<T> otherNode);
	}