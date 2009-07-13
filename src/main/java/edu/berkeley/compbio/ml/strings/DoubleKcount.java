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

package edu.berkeley.compbio.ml.strings;

import com.davidsoergel.dsutils.DSArrayUtils;
import com.davidsoergel.stats.DoubleArrayContainer;

import java.util.Arrays;

/**
 * Represents the counts of a set of given patterns appearing in a string.  Typically the patterns counted are simply
 * all subsequences up to a length K.  How the symbols are interpreted and counted is up to the implementation and the
 * associated KcountScanner, so it's easy for instance to make optimized scanners for certain kinds of sequences, or to
 * use compressed alphabets, and so forth.
 * <p/>
 * Kcounts may be related through a "parent" link.  The interpretation depends on the implementation, but a typical
 * usage would be for the "parent" Kcount to contain less-specific information, i.e. fewer counts of more general
 * patterns, perhaps generated by aggregating the specific counts from the child.
 * <p/>
 * ("Parent" may not be the best name for this... perhaps "generalized" or some such?)
 *
 * @author David Soergel
 * @version $Id$
 */
public abstract class DoubleKcount<T extends DoubleKcount> extends Kcount<T> implements DoubleArrayContainer
	{
// ------------------------------ FIELDS ------------------------------

	protected final double[] counts;

	protected Double dataSum = null;


// --------------------------- CONSTRUCTORS ---------------------------

	/**
	 * Creates a new Kcount instance.
	 */
	public DoubleKcount(double[] counts)
		{
		super();
		this.counts = counts;
		}

// ------------------------ CANONICAL METHODS ------------------------

	/**
	 * Clones this object.  Should behave like {@link Object#clone()} except that it returns an appropriate type and so
	 * requires no cast.  Also, we insist that is method be implemented in inheriting classes, so it does not throw
	 * CloneNotSupportedException.
	 *
	 * @return a clone of this instance.
	 * @see Object#clone
	 * @see Cloneable
	 */
	@Override
	public abstract T clone();


// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface AdditiveClusterable ---------------------


	public synchronized void multiplyBy(final double v)
		{
		DSArrayUtils.multiplyBy(counts, v);
		dataSum = null;
		}

// --------------------- Interface Clusterable ---------------------

	/**
	 * Tests whether the given pattern counts are equal to those stored here.  Differs from equals() in that
	 * implementations of this interface may contain additional state which make them not strictly equal; here we're only
	 * interested in whether they're equal as far as this interface is concerned.
	 *
	 * @param other The Kcount to compare against
	 * @return True if they are equivalent, false otherwise
	 */
	public synchronized boolean equalValue(final T other)
		{
		return Arrays.equals(counts, other.counts);
		}

// --------------------- Interface DoubleArrayContainer ---------------------

	public double getArraySum() //synchronized
		{
		if (dataSum == null)
			{
			synchronized (this)
				{
				if (dataSum == null)
					{
					dataSum = DSArrayUtils.sum(counts);
					}
				}
			}
		return dataSum;
		}

// --------------------- Interface SequenceSpectrum ---------------------

	/**
	 * Tests whether the given sequence statistics are equivalent to this one.  Differs from equals() in that
	 * implementations of this interface may contain additional state which make them not strictly equal; here we're only
	 * interested in whether they're equal as far as this interface is concerned.  Equivalent to {@link
	 * #equalValue(DoubleKcount)} in this case.
	 *
	 * @param spectrum the SequenceSpectrum
	 * @return the boolean
	 */
	public boolean spectrumEquals(final SequenceSpectrum spectrum)
		{
		return equalValue((T) spectrum);
		}

// -------------------------- OTHER METHODS --------------------------

	public synchronized double[] getLevelOneArray()
		{
		DoubleKcount<T> a = this;
		DoubleKcount<T> b = a.getParent();
		if (b == null)
			{
			return null;
			}

		DoubleKcount c = b.getParent();
		while (c != null)
			{
			a = b;
			b = c;
			c = b.getParent();
			}
		return a.getArray();
		}

	/**
	 * Returns an array of the counts.  The mapping of patterns to array indices is implementation-dependent.  A typical
	 * implementation will order all possible patterns up to length K in lexical order according to the symbol alphabet.
	 *
	 * @return The array of counts
	 */
	public double[] getArray()
		{
		return counts;
		}
	}
