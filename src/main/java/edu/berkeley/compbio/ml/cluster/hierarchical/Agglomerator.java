package edu.berkeley.compbio.ml.cluster.hierarchical;

import com.davidsoergel.dsutils.collections.Symmetric2dBiMap;
import edu.berkeley.compbio.ml.cluster.Clusterable;

import java.util.HashSet;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public abstract class Agglomerator<T extends Clusterable<T>>
	{
	protected abstract void addCompositeVsNodeToDistanceMatrix(final HierarchicalCentroidCluster<T> origA,
	                                                           final HierarchicalCentroidCluster<T> origB,
	                                                           final HierarchicalCentroidCluster<T> composite,
	                                                           final HierarchicalCentroidCluster<T> otherNode,
	                                                           final Symmetric2dBiMap<HierarchicalCentroidCluster<T>, Double> theActiveNodeDistanceMatrix);

	private void addCompositeToDistanceMatrix(final HierarchicalCentroidCluster<T> a,
	                                          final HierarchicalCentroidCluster<T> b,
	                                          final HierarchicalCentroidCluster<T> composite,
	                                          final Symmetric2dBiMap<HierarchicalCentroidCluster<T>, Double> theActiveNodeDistanceMatrix)
		{
		// there was a mysterious concurrent-modification sort of problem; does this fix it?
		final HashSet<HierarchicalCentroidCluster<T>> activeKeys =
				new HashSet<HierarchicalCentroidCluster<T>>(theActiveNodeDistanceMatrix.getActiveKeys());


		//** serial test
		for (HierarchicalCentroidCluster<T> node : activeKeys)
			{
			addCompositeVsNodeToDistanceMatrix(a, b, composite, node, theActiveNodeDistanceMatrix);
			}
		/*	Parallel.forEach(activeKeys,
								 new Function<HierarchicalCentroidCluster<T>, Void>()
								 {
								 public Void apply(final HierarchicalCentroidCluster<T> node)
									 {
									 addCompositeVsNodeToDistanceMatrix(a, b, composite, node, theActiveNodeDistanceMatrix);
									 return null;
									 }
								 });*/
		}

	public HierarchicalCentroidCluster<T> joinNodes(final int id, final HierarchicalCentroidCluster<T> a,
	                                                final HierarchicalCentroidCluster<T> b,
	                                                final Symmetric2dBiMap<HierarchicalCentroidCluster<T>, Double> theActiveNodeDistanceMatrix)
		{
		// set the branch lengths

		Double distance = theActiveNodeDistanceMatrix.get(a, b) / 2.;
		a.setLength(distance);
		b.setLength(distance);

		// create a composite node

		final HierarchicalCentroidCluster<T> composite = new HierarchicalCentroidCluster<T>(id,
		                                                                                    null);  // don't bother storing explicit centroids for composite nodes


		a.setParent(composite);
		b.setParent(composite);

		composite.addAll(a);
		composite.addAll(b);

		// weight and weightedLabels.getItemCount() are maybe redundant; too bad
		composite.setWeight(a.getWeight() + b.getWeight());

		composite.doneLabelling();

		int numActive = theActiveNodeDistanceMatrix.numKeys();
		int numPairs = theActiveNodeDistanceMatrix.numPairs();

		addCompositeToDistanceMatrix(a, b, composite, theActiveNodeDistanceMatrix);


		if (numActive > 2)
			{
			assert theActiveNodeDistanceMatrix.numKeys() == numActive + 1;
			assert theActiveNodeDistanceMatrix.numPairs() == numPairs + numActive - 2;
			}
		// remove the two merged clusters from consideration

		int removedA = theActiveNodeDistanceMatrix.remove(a);
		int removedB = theActiveNodeDistanceMatrix.remove(b);

		assert removedA == numActive - 1;
		assert removedB == numActive - 2;

		if (numActive > 2)
			{
			assert theActiveNodeDistanceMatrix.numKeys() == numActive - 1;
			assert theActiveNodeDistanceMatrix.numPairs() == numPairs - (numActive - 1);
			}

		// add the composite node to the active list
		// no longer needed; automatic
		// theActiveNodeDistanceMatrix.add(composite);
		return composite;
		}
	}