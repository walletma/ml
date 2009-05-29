package edu.berkeley.compbio.ml;

/**
 * @author <a href="mailto:dev@davidsoergel.com">David Soergel</a>
 * @version $Id$
 */
public class BinaryCrossValidationResults extends CrossValidationResults
	{
	protected int numExamples;
	protected int tt;
	protected int tf;
	protected int ft;
	protected int ff;

	float trueTrueRate()
		{
		return (float) tt / (float) numExamples;
		}

	float falseTrueRate()
		{
		return (float) ft / (float) numExamples;
		}

	float trueFalseRate()
		{
		return (float) tf / (float) numExamples;
		}

	float falseFalseRate()
		{
		return (float) ff / (float) numExamples;
		}

	public float accuracy()
		{
		return (float) (tt + ff) / (float) numExamples;
		}

	public float accuracyGivenClassified()
		{
		// ** for now everything was classified
		return accuracy();
		}

	public float unknown()
		{
		// ** for now everything was classified
		return 0F;
		}

	public float classNormalizedSensitivity()
		{
		return ((float) tt / (float) (tt + tf) + (float) ff / (float) (ff + ft)) / 2f;
		}
	}