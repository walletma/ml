package edu.berkeley.compbio.ml.cluster.stats;

import com.davidsoergel.dsutils.tree.DepthFirstTreeIterator;
import com.davidsoergel.dsutils.tree.TreeException;
import edu.berkeley.compbio.ml.cluster.CentroidCluster;
import edu.berkeley.compbio.ml.cluster.ClusterList;
import edu.berkeley.compbio.ml.cluster.Clusterable;
import edu.berkeley.compbio.ml.cluster.ClusteringStats;
import edu.berkeley.compbio.ml.cluster.SimpleClusterList;
import edu.berkeley.compbio.ml.cluster.hierarchical.HierarchicalCentroidCluster;
import edu.berkeley.compbio.phyloutils.LengthWeightHierarchyNode;
import edu.berkeley.compbio.phyloutils.PhylogenyNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class HierarchicalClusteringStats
	{
	final HierarchicalCentroidCluster theClustering;

	public HierarchicalClusteringStats(final HierarchicalCentroidCluster theClustering)
		{
		this.theClustering = theClustering;
		}

	public Map<Double, ClusteringStats> statsByLevel(Collection<Double> thresholds) throws TreeException
		{
		final Map<Double, ClusteringStats> result = new HashMap<Double, ClusteringStats>();
		Map<Double, ClusterList> clusterSets = selectOTUs(theClustering, thresholds);
		for (Map.Entry<Double, ClusterList> entry : clusterSets.entrySet())
			{
			Double threshold = entry.getKey();
			ClusterList clusters = entry.getValue();
			ClusteringStats stats = new ClusteringStats(clusters);
			result.put(threshold, stats);
			}
		return result;
		}

	private static Map<Double, ClusterList> selectOTUs(final HierarchicalCentroidCluster tree,
	                                                   Collection<Double> thresholds) throws TreeException
		{
		final DepthFirstTreeIterator<CentroidCluster, PhylogenyNode<CentroidCluster>> it = tree.depthFirstIterator();

		final Map<Double, ClusterList> results = new HashMap<Double, ClusterList>();

		int i = 0;
		for (final Double threshold : thresholds)
			{
			Set<Clusterable> result = new HashSet<Clusterable>();
			final double halfThreshold = threshold / 2.0;
			//int otuCount = 0;
			while (it.hasNext())
				{
				final PhylogenyNode<CentroidCluster> node = it.next();
				final Collection<? extends LengthWeightHierarchyNode> children = node.getChildren();
				//assert children.isEmpty() || children.size() == 2;

				// take advantage of the fact that we know that this is a binary tree, and the two branch lengths below each node are equal
				// the exception is the root node, which may have many children at great distance, but that will fail the skip test here anyway

				if (children.isEmpty() || children.iterator().next().getLength() < halfThreshold)
					{
					//otuCount++;
					final CentroidCluster payload = node.getPayload();
					final Clusterable cluster = payload.getCentroid();
					result.add(cluster);
					it.skipAllDescendants(node);
					}
				}
			results.put(threshold, new SimpleClusterList(result));
			i++;
			}
		return results;
		}
	}
