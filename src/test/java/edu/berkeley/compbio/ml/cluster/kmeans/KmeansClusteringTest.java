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


package edu.berkeley.compbio.ml.cluster.kmeans;

import com.davidsoergel.dsutils.math.MathUtils;
import org.apache.log4j.Logger;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author lorax
 * @version 1.0
 */
public class KmeansClusteringTest
	{
	// ------------------------------ FIELDS ------------------------------

	private static final Logger logger = Logger.getLogger(KmeansClusteringTest.class);


	// -------------------------- OTHER METHODS --------------------------

	@BeforeSuite
	public void setUp()
		{
		MathUtils.initApproximateLog(-12, +12, 3, 100000);
		}

	@Test
	public void testSimilarPointsClusterTogether() throws CloneNotSupportedException, IOException
		{
		// BAD Test is commented out!
		/*
			  ClusterableIterator ci;

			  ci = new MockClusterableIterator().init();

			  KmeansClustering<ClusterableDoubleArray> oc = new KmeansClustering<ClusterableDoubleArray>(ci, 5, EuclideanDistance.getInstance());

			  oc.run(ci, 7);

			  //	batchUpdateAndPrint(oc);
			  //	batchUpdateAndPrint(oc);

			  List<Cluster<ClusterableDoubleArray>> theClusters = oc.getClusters();

			  for (Cluster<ClusterableDoubleArray> c : theClusters)
				  {
				  logger.debug(c);

				  }

			  oc.writeAssignmentsAsTextToStream(System.err);

			  assert true; // this test doesn't assert anything,but looks good
  */
		}
	}
