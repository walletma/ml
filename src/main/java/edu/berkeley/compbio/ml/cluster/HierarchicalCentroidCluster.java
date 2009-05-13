/*
 * Copyright (c) 2001-2008 David Soergel
 * 418 Richmond St., El Cerrito, CA  94530
 * dev@davidsoergel.com
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the author nor the names of any contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.berkeley.compbio.ml.cluster;

import com.davidsoergel.dsutils.collections.WeightedSet;
import edu.berkeley.compbio.phyloutils.BasicPhylogenyNode;
import org.apache.commons.lang.NotImplementedException;

import java.util.Formatter;
import java.util.Iterator;


/**
 * A tree node representing a hierarchical cluster.  Fakes multiple inheritance by providing a facade to the contained
 * Cluster.
 *
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class HierarchicalCentroidCluster<T extends Clusterable<T>> extends BasicPhylogenyNode<CentroidCluster<T>>
		implements CentroidCluster<T>
	{
// --------------------------- CONSTRUCTORS ---------------------------

	public HierarchicalCentroidCluster(int id, Clusterable<T> sample)
		{
		super(new BasicCentroidCluster(id, sample));
		setWeight(1.);
		}

// ------------------------ CANONICAL METHODS ------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HierarchicalCentroidCluster<T> clone()
		{
		return (HierarchicalCentroidCluster<T>) super.clone();
		}

	/**
	 * {@inheritDoc}
	 */
	/*	public void setN(int i)
	   {
	   getValue().setN(i);
	   }*/

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString()
		{
		Formatter f = new Formatter();
		f.format("l=%.2f w=%.2f %s", length, weight, value);//%[Cluster %d] n=%d sd=%.2f", id, n, getStdDev());

		return f.out().toString();
		}

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface CentroidCluster ---------------------


	/**
	 * {@inheritDoc}
	 */
	public void addToSumOfSquareDistances(double v)
		{
		throw new NotImplementedException();
		}

	/**
	 * {@inheritDoc}
	 */
	public T getCentroid()
		{
		return getValue().getCentroid();
		}

	/**
	 * {@inheritDoc}
	 */
	public double getStdDev()
		{
		throw new NotImplementedException();
		}

	/**
	 * {@inheritDoc}
	 */
	public void setCentroid(T centroid)
		{
		getValue().setCentroid(centroid);
		}

	/**
	 * {@inheritDoc}
	 */
	public void setSumOfSquareDistances(double i)
		{
		throw new NotImplementedException();
		}

// --------------------- Interface Cluster ---------------------


	/**
	 * {@inheritDoc}
	 */
	public boolean add(T point)
		{
		throw new NotImplementedException();
		}

	/**
	 * {@inheritDoc}
	 */
	public boolean addAll(Cluster<T> point)
		{
		return getValue().addAll(point);
		//throw new NotImplementedException();
		}

	/**
	 * Recomputes the probabilities of labels, based on the actual labels observed in the contained samples.  This must be
	 * called explicitly to avoid unnecessary recomputation on every sample addition.
	 */
	public WeightedSet<String> getDerivedLabelProbabilities()
		{
		return getValue().getDerivedLabelProbabilities();
//		throw new NotImplementedException();
		}

	/**
	 * {@inheritDoc}
	 */
	public int getId()
		{
		return getValue().getId();
		}

	/**
	 * {@inheritDoc}
	 */
	public int getN()
		{
		return getValue().getN();
		}

	/**
	 * Gets the probabilities of mutually exclusive String labels.
	 *
	 * @return a Multinomial giving the probabilities of mutually exclusive String labels.
	 */
	public WeightedSet<String> getWeightedLabels()
		{
		return getValue().getWeightedLabels();
		//	throw new NotImplementedException();
		}

	/**
	 * {@inheritDoc}
	 */
	public boolean remove(T point)
		{
		throw new NotImplementedException();
		}

	/**
	 * {@inheritDoc}
	 */
	public boolean removeAll(Cluster<T> point)
		{
		throw new NotImplementedException();
		}

	/**
	 * Sets the probabilities of String labels.  The labels need not be mututally exclusive, so the weights need not sum to
	 * one.
	 *
	 * @param derivedLabelProbabilities a WeightedSet giving the probabilities of mutually exclusive String labels.
	 */
	public void setDerivedLabelProbabilities(WeightedSet<String> derivedLabelProbabilities)
		{
		throw new NotImplementedException();
		}

	/**
	 * {@inheritDoc}
	 */
	public void setId(int id)
		{
		getValue().setId(id);
		}

	/**
	 * Copy the local label weights into the derived label weights.
	 */
	public void updateDerivedWeightedLabelsFromLocal()
		{
		getValue().updateDerivedWeightedLabelsFromLocal();
		}

// --------------------- Interface LengthWeightHierarchyNode ---------------------


	/**
	 * Gets the probabilities of mutually exclusive String labels.
	 *
	 * @return a Multinomial giving the probabilities of mutually exclusive String labels.
	 */
	/*public WeightedSet<String> addWeightedLabels(WeightedSet<String> l)
		{
		throw new NotImplementedException();
		}*/
	public void toNewick(StringBuffer sb, String prefix, String tab, int minClusterSize, double minLabelProb)
		{
		// (children)name:length

		if (prefix != null)
			{
			sb.append(prefix);
			}

		if (!children.isEmpty())
			{
			prefix = prefix == null ? null : prefix + tab;
			sb.append("(");
			Iterator<BasicPhylogenyNode<CentroidCluster<T>>> i = children.iterator();
			while (i.hasNext())
				{
				final BasicPhylogenyNode<CentroidCluster<T>> child = i.next();
				if (child.getValue().getN() >= minClusterSize)
					{
					child.toNewick(sb, prefix, tab, minClusterSize, minLabelProb);
					if (i.hasNext())
						{
						sb.append(",");
						}
					}
				}
			sb.append(")");
			}

		sb.append(getN());

		WeightedSet<String> labels = getDerivedLabelProbabilities();

		for (String label : labels.keysInDecreasingWeightOrder())
			{
			double labelProb = labels.getNormalized(label);
			if (labelProb < minLabelProb)
				{
				break;
				}
			sb.append("_").append(label).append("=").append(String.format("%.2f", labelProb));
			}

		if (bootstrap != 0)
			{
			sb.append(":").append(bootstrap);
			}
		}
	}
