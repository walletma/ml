/*
 * Copyright (c) 2006-2013  David Soergel  <dev@davidsoergel.com>
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package edu.berkeley.compbio.ml.cluster.kohonen;

import com.davidsoergel.stats.DissimilarityMeasure;
import edu.berkeley.compbio.ml.cluster.AdditiveClusterable;
import edu.berkeley.compbio.ml.cluster.ClusterMove;
import edu.berkeley.compbio.ml.cluster.NoGoodClusterException;
import org.apache.log4j.Logger;

import java.util.Iterator;


/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */

public class NearCurrentPositionSearchStrategy<T extends AdditiveClusterable<T>> extends KohonenSOM2DSearchStrategy<T>
	{
// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(NearCurrentPositionSearchStrategy.class);

	/*	private void setSearchRadius(int i)
		 {
		 searchRadius = i;
		 }
 */ final KohonenSOM2DSearchStrategy<T> fallbackStrategy = new CoarseGridSearchStrategy<T>();


// -------------------------- OTHER METHODS --------------------------

	/**
	 * Copied from KmeansClustering
	 *
	 * @param p
	 * @return
	 */
	@Override
	public ClusterMove<T, KohonenSOMCell<T>> bestClusterMove(final T p) throws NoGoodClusterException
		{
		final ClusterMove<T, KohonenSOMCell<T>> result = new ClusterMove<T, KohonenSOMCell<T>>();

		final String id = p.getId();
		result.oldCluster = som.getAssignment(id);

		if (result.oldCluster == null)
			{
			return fallbackStrategy.bestClusterMove(p);
			}

		if (logger.isTraceEnabled())
			{
			logger.trace("Choosing best cluster for " + p + " (previous = " + result.oldCluster + ")");
			}

		for (Iterator<KohonenSOM2D<T>.WeightedCell> i =
				som.getWeightedMask((int) getSearchRadius()).iterator(result.oldCluster); i.hasNext();)
			{
			final KohonenSOMCell<T> c = i.next().theCell;
			final double d = measure.distanceFromTo(p, c.getCentroid());//c.distanceToCentroid(p);
			if (d < result.bestDistance)
				{
				result.secondBestDistance = result.bestDistance;
				result.bestDistance = d;
				result.bestCluster = c;
				}
			else if (d < result.secondBestDistance)
				{
				result.secondBestDistance = d;
				}
			}

		if (logger.isTraceEnabled())
			{
			logger.trace("Chose " + result.bestCluster);
			}
		if (result.bestCluster == null)
			{
			//logger.error("Can't classify: " + p);
			throw new NoGoodClusterException("No cluster found for " + p + ": " + result);
			}
		return result;
		}

	public double getSearchRadius()
		{
		return 8;
		}

	@Override
	public void setDistanceMeasure(final DissimilarityMeasure<T> dissimilarityMeasure)
		{
		super.setDistanceMeasure(dissimilarityMeasure);
		fallbackStrategy.setDistanceMeasure(dissimilarityMeasure);
		}

	//** @Property
	//private final int gridSpacing = 4;


	//private int searchRadius;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSOM(final KohonenSOM2D<T> som)
		{
		super.setSOM(som);

		fallbackStrategy.setSOM(som);
		//** @Property
		//setSearchRadius(8);
		}
	}
