package nhbsa;

//does it always square matrix learned ??

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;

public class NHBSA {
	private int m_N; // population size
	private int m_L; // length of permutation
	int[][] m_pop = new int[m_N][m_L];// a population matrix
	double[][] m_node; // a node histogram matrix (NHM)
	private double m_bRatio;// a bias for NHM
    double Pls=0.1; //probability of local search


	public NHBSA(int m_N, int m_L) {
		m_node = new double[m_N][m_L]; // a node histogram matrix (NHM)

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
		// printNHM(m_node);

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