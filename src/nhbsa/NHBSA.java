package nhbsa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;

import com.google.common.math.DoubleMath;

public class NHBSA {
	private int m_N; // population size
	private int m_L; // length of permutation
	int[][] m_pop = new int[m_N][m_L];// a population matrix
	double[][] m_node; // a node histogram matrix (NHM)
	private double m_bRatio;// a bias for NHM
	double Pls = 0.1; // probability of local search

	// settings for discount learning
	boolean isDiscount = false; // true for considering the learning rate, false for no
	boolean isFirstNHM = true; // true for the first NHM without any discount
	boolean isAdaptive = true;// false for default, no adaptive changes according to the entropy of the matrix
	double lrate = 0.5; // probability of learning rate
	double k = 1;
	double[][] m_node_archive;

	// a array for storing entropy
	private double[] entropyTemp;
	private List<String> entropy4Gen;
	private List<Double> discountRate4Gen;

	public NHBSA(int m_N, int m_L) {
		m_node = new double[m_N][m_L]; // initial a node histogram matrix (NHM)
		m_node_archive = new double[m_N][m_L]; // initial archive for storing a node histogram matrix (NHM)
		entropyTemp = new double[m_N]; // initial an entropy array for storing entropy for a matrix
		entropy4Gen = new ArrayList<String>(); // initial an entropy array for storing entropy for all matrix through
												// all
		// generations
		discountRate4Gen = new ArrayList<Double>(); // initial an discountRate arraylist for storing the changing
													// disount rate
	}

	public final void setDefaultPara() {
		m_bRatio = 0.0002;// defined by users
		m_bRatio = (m_N * m_bRatio) / m_L; // bias
	}

	public List<int[]> sampling4NHBSA(int sampleSize, Random random) {
		int i;// position i
		int j;// node j
		List<int[]> sampled_pop = new ArrayList<int[]>();

		setDefaultPara(); // set bias

		// add bias to all elements of NHM
		for (i = 0; i < m_N; i++) {
			for (j = 0; j < m_L; j++) {
				m_node[i][j] = m_bRatio;
			}
		}

		// add delta function to all elements of NHM
		for (i = 0; i < m_N; i++) {
			for (j = 0; j < m_L; j++) {
				double delta_sum = delta_sum_calcu(m_N, i, j, m_node);
				m_node[i][j] += delta_sum;
			}
		}

		// System.out.println("before discounted learning");
		// printNHM(m_node);
		// calculateEntropy(m_node);

		// check the discount rate is considered in the learning process
		if (isDiscount == true) {

			if (isFirstNHM == true) {
				isFirstNHM = false;
				copy2dArray(m_node_archive, m_node);
				discountRate4Gen.add(lrate);

			} else {
				// update m_node using m_node and m_node_archive
				double[][] m_node_updated;
				if (isAdaptive == false) {

					m_node_updated = discountedNHM(m_node_archive, m_node, lrate);

				} else {
					m_node_updated = adaptive_discountedNHM(m_node_archive, m_node, lrate);

				}

				// empty m_node_archive first, is it necessary？

				// store the data of new NHM
				copy2dArray(m_node_archive, m_node);

				// update m_node using m_node_updated
				copy2dArray(m_node, m_node_updated);
			}

		}

		// System.out.println("after discounted learning");
		// printNHM(m_node);
		calculateEntropy(m_node);

		// NHBSA/WO Sampling sampleSize numbers of individuals
		for (int no_sample = 0; no_sample < sampleSize; no_sample++) {

			int[] sampledIndi = new int[m_L]; // an individual sampled

			// generate a random position index permutation
			List<Integer> position_permutation = new ArrayList<Integer>();

			for (int m = 0; m < m_L; m++) {
				position_permutation.add(m);
			}

			Collections.shuffle(position_permutation, random);

			// generate a candidate node list
			int[] c_candidates = new int[m_L];
			for (int m = 0; m < m_L; m++) {
				c_candidates[m] = m;
			}

			// set the position counter
			int p_counter = 0;
			while (p_counter < m_L) { // postion_counter for the random
										// permutation

				int position_rand = position_permutation.get(p_counter);
				// initial numsToGenerate from the candidate node list
				double[] discreteProbabilities = new double[m_L - p_counter];

				// calculate probability and put them into proba[]
				double sum_proba = 0;
				for (int c : c_candidates) {
					sum_proba += m_node[position_rand][c];
				}
				int m = 0;

				for (int c : c_candidates) {
					discreteProbabilities[m] = m_node[position_rand][c] / sum_proba;
					m++;
				}

				// sample x of individual for c[r[p]]
				int[] node_x = sampling(c_candidates, discreteProbabilities, random);

				sampledIndi[position_rand] = node_x[0];

				// remove x from numsToGenerate
				c_candidates = ArrayUtils.removeElement(c_candidates, node_x[0]);

				// update the position counter P+1
				p_counter += 1;
			}

			// printIndi(sampledIndi);
			sampled_pop.add(sampledIndi);
		}
		return sampled_pop;
	}

	private void calculateEntropy(double[][] m_node) {

		int p_counter = 0;
		ArrayUtils.nullToEmpty(entropyTemp);

		while (p_counter < m_N) { // postion_counter for the random
									// permutation
			// initial numsToGenerate from the candidate node list
			double[] discreteProbabilities = new double[m_N];

			// calculate probability and put them into proba[]
			double sum_proba = 0;

			for (int m = 0; m < m_L; m++) {
				sum_proba += m_node[p_counter][m];
			}
			for (int m = 0; m < m_L; m++) {
				discreteProbabilities[m] = m_node[p_counter][m] / sum_proba;
			}

			entropyTemp[p_counter] = entropy(discreteProbabilities);

			p_counter++;
		}

		// System.out.print("Mean Entropy:" + Arrays.stream(entropyTemp).average());
		// System.out.print("Best Entropy:" + Arrays.stream(entropyTemp).max());
		// System.out.println("Worst Entropy:" + Arrays.stream(entropyTemp).min());

		entropy4Gen.add(Arrays.stream(entropyTemp).average().getAsDouble() + " "
				+ Arrays.stream(entropyTemp).max().getAsDouble() + " "
				+ Arrays.stream(entropyTemp).min().getAsDouble());
	}

	private double entropy(double[] discreteProbabilities) {
		double entropy_indi = 0;

		for (double p : discreteProbabilities) {
			entropy_indi += -(p * DoubleMath.log2(p));
		}

		return entropy_indi;
	}

	private void copy2dArray(double[][] targetArray, double[][] sourceArray) {
		for (int m = 0; m < sourceArray.length; m++)
			targetArray[m] = sourceArray[m].clone();
	}

	private double[][] adaptive_discountedNHM(double[][] m_node_archive, double[][] m_node, double lrate) {

		double[][] m_node_updated = new double[m_N][m_L];

		String[] s = entropy4Gen.get(entropy4Gen.size() - 1).split("\\s+");
		double lrate_update = updateLRate(Double.parseDouble(s[0]), lrate);
		System.out.println(lrate_update);

		for (int indi_pos = 0; indi_pos < m_N; indi_pos++) {
			for (int index = 0; index < m_L; index++) {
				m_node_updated[indi_pos][index] = m_node_archive[indi_pos][index] * (1 - lrate_update)
						+ m_node[indi_pos][index] * lrate_update;
			}
		}

		// printNHM(m_node_updated);

		discountRate4Gen.add(lrate_update);
		return m_node_updated;
	}

	private double[][] discountedNHM(double[][] m_node_archive, double[][] m_node, double lrate) {

		double[][] m_node_updated = new double[m_N][m_L];

		for (int indi_pos = 0; indi_pos < m_N; indi_pos++) {
			for (int index = 0; index < m_L; index++) {
				m_node_updated[indi_pos][index] = m_node_archive[indi_pos][index] * lrate
						+ m_node[indi_pos][index] * (1-lrate);
			}
		}

		// printNHM(m_node_updated);
		return m_node_updated;
	}

	// adaptively calculate the discount rate according to the value of entropy
	private double updateLRate(double meanEntropy, double lrate) {
//		return lrate * (1 - 1 / (1 + Math.exp(meanEntropy * k)));
//		return  (1 - 1 / (1 + Math.exp(meanEntropy * k)));
		return  1 / (1 + Math.exp(meanEntropy * k));


	}

	// sampling function for sampling one individual requiring
	// re-normalization of the remaining after each sampling
	public int[] sampling(int[] numsToGenerate, double[] discreteProbabilities, Random random) {

		// sample node x with probability
		EnumeratedIntegerDistribution distribution = new EnumeratedIntegerDistribution(numsToGenerate,
				discreteProbabilities);

		distribution.reseedRandomGenerator(random.nextInt());

		return distribution.sample(1);
	}

	private double delta_sum_calcu(int m_N, int i, int j, double[][] m_node) {
		double delta_sum = 0;
		for (int k = 1; k <= m_N; k++) {

			if (m_pop[k - 1][i] == j) {
				delta_sum += 1;
			} else {
				delta_sum += 0;
			}

		}
		return delta_sum;
	}

	public int getM_N() {
		return m_N;
	}

	public void setM_N(int m_N) {
		this.m_N = m_N;
	}

	public int getM_L() {
		return m_L;
	}

	public void setM_L(int m_L) {
		this.m_L = m_L;
	}

	public List<String> getEntropy4Gen() {
		return entropy4Gen;
	}

	public void setEntropy4Gen(List<String> entropy4Gen) {
		this.entropy4Gen = entropy4Gen;
	}

	public List<Double> getDiscountRate4Gen() {
		return discountRate4Gen;
	}

	public void setDiscountRate4Gen(List<Double> discountRate4Gen) {
		this.discountRate4Gen = discountRate4Gen;
	}

	public void printNHM(int[][] m_node) {
		System.out.println("");
		for (int i = 0; i < m_N; i++) {
			System.out.print("[");
			for (int j = 0; j < m_L; j++) {
				System.out.print(m_node[i][j] + " ");
			}
			System.out.println("]");
		}

	}

	public void printNHM(double[][] m_node) {
		System.out.println("");
		for (int i = 0; i < m_N; i++) {
			System.out.print("[");
			for (int j = 0; j < m_L; j++) {
				System.out.print(m_node[i][j] + " ");
			}
			System.out.println("]");
		}

	}

	public void printIndi(int[] indi) {
		System.out.println("");
		System.out.print("[");
		for (int i = 0; i < m_N; i++) {
			System.out.print(indi[i] + " ");
		}
		System.out.println("]");
	}

	public int[][] getM_pop() {
		return m_pop;
	}

	public void setM_pop(int[][] m_pop) {
		this.m_pop = m_pop;
	}

}