package wsc.problem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import java.util.List;

import chart.LineChart;
import nhbsa.LocalSearch;
import nhbsa.NHBSA;
import wsc.InitialWSCPool;
import wsc.data.pool.Service;
import wsc.graph.ServiceGraph;

public class WSCProblem {
	public static void main(String[] args) throws IOException {

		if (args.length != 5) {
			System.out.println("missing arguments!");
			return;
		}

		WSCInitializer init = new WSCInitializer(args[0], args[1], args[2], args[3], Long.valueOf(args[4]));
		WSCGraph graGenerator = new WSCGraph();
		WSCEvaluation eval = new WSCEvaluation();

		WSCProblem p = new WSCProblem();
		p.NHBSASolver(graGenerator, eval);

	}

	// The main entry to NHBSASolver.
	public void NHBSASolver(WSCGraph graGenerator, WSCEvaluation eval) {

		WSCInitializer.startTime = System.currentTimeMillis();
		WSCInitializer.initTime.add(System.currentTimeMillis() - WSCInitializer.initialisationStartTime);
		WSCInitializer.initialization = (long) 0;

		List<WSCIndividual> population = new ArrayList<WSCIndividual>();
		// entry to NHBSA
		NHBSA nhbsa = new NHBSA(WSCInitializer.dimension_size, WSCInitializer.dimension_size);

		// random initalize one population solutions
		while (population.size() < WSCInitializer.population_size) {
			WSCIndividual individual = new WSCIndividual();
			List<Integer> usedSerQueue = new ArrayList<Integer>();

			// graph-based representation
			ServiceGraph graph = graGenerator.generateGraph();

			// create a queue of services according to breath first search
			List<Integer> usedQueue = graGenerator.usedQueueofLayers("startNode", graph, usedSerQueue);
			// set the position of the split position of the queue
			individual.setSplitPosition(usedQueue.size()); // index from 0 to (splitposition-1)

			// add unused queue to form a complete a vector-based individual
			List<Integer> serQueue = graGenerator.completeSerQueueIndi(usedQueue);
			// Set serQueue to individual(do I need deep clone ?)
			individual.serQueue.addAll(serQueue);

			eval.aggregationAttribute(individual, graph);
			eval.calculateFitness(individual);
			population.add(individual);
			WSCInitializer.evalCounter++;
			System.out.print("; evalCounter:"+WSCInitializer.evalCounter);;
			BestIndiSoFar4EvalStep(population);
		}

		// entry to learn the matrix and sampling individuals
		int iteration = 0;
		while (iteration < WSCInitializer.MAX_NUM_ITERATIONS) {
			System.out.println("NHM " + iteration);

			// add a local search
			// LocalSearch ls = new LocalSearch();
			// ls.swap(population, WSCInitializer.random, graGenerator, eval);

			// sort the individuals according to the fitness
			Collections.sort(population);

			// entry to NHBSA

			// NHBSA nhbsa = new NHBSA(WSCInitializer.dimension_size,
			// WSCInitializer.dimension_size);
			// select first half population into matrix
			int[][] m_generation = new int[WSCInitializer.dimension_size][WSCInitializer.dimension_size];
			for (int m = 0; m < WSCInitializer.dimension_size; m++) {
				for (int n = 0; n < WSCInitializer.dimension_size; n++) {
					m_generation[m][n] = population.get(m).serQueue.get(n);
				}
			}

			nhbsa.setM_pop(m_generation);
			nhbsa.setM_L(WSCInitializer.dimension_size);
			nhbsa.setM_N(WSCInitializer.dimension_size);

			List<int[]> pop_updated = nhbsa.sampling4NHBSA(WSCInitializer.dimension_size, WSCInitializer.random);

			// update the population
			population.clear();

			for (int m = 0; m < pop_updated.size(); m++) {
				int[] id_updated = pop_updated.get(m);
				WSCIndividual indi_updated = new WSCIndividual();
				List<Service> serviceCandidates = new ArrayList<Service>();
				for (int n = 0; n < id_updated.length; n++) {

					// deep clone may be not needed if no changes are applied to the pointed service
					serviceCandidates.add(WSCInitializer.Index2ServiceMap.get(id_updated[n]));
				}

				// set the service candidates according to the sampling
				InitialWSCPool.getServiceCandidates().clear();
				InitialWSCPool.setServiceCandidates(serviceCandidates);
				ServiceGraph update_graph = graGenerator.generateGraphBySerQueue();
				// adjust the bias according to the valid solution from the service queue

				List<Integer> usedSerQueue = new ArrayList();
				// create a queue of services according to breathfirstsearch algorithm

				List<Integer> usedQueue = graGenerator.usedQueueofLayers("startNode", update_graph, usedSerQueue);
				// set up the split index for the updated individual
				indi_updated.setSplitPosition(usedQueue.size());

				// add unused queue to form a complete a vector-based individual
				List<Integer> serQueue = graGenerator.completeSerQueueIndi(usedQueue);

				// set the serQueue to the updatedIndividual
				indi_updated.serQueue = serQueue;

				// evaluate updated updated_graph
				// eval.aggregationAttribute(indi_updated, updated_graph);
				eval.aggregationAttribute(indi_updated, update_graph);
				eval.calculateFitness(indi_updated);
				population.add(indi_updated);

				WSCInitializer.evalCounter++;
				System.out.print("; evalCounter:"+WSCInitializer.evalCounter);;
				BestIndiSoFar4EvalStep(population);

			}

			iteration += 1;
		}
		// writeLogs(nhbsa);

	}

	// check the bestIndi found every evalStep
	private void BestIndiSoFar4EvalStep(List<WSCIndividual> population) {
		if (WSCInitializer.evalCounter % WSCInitializer.evalStep == 0) {
			System.out.println("===EVALUATION===NO." + WSCInitializer.evalCounter / 200);
			
			WSCInitializer.time.add(System.currentTimeMillis() - WSCInitializer.startTime);
			WSCInitializer.initTime.add(WSCInitializer.initialization);
			WSCInitializer.startTime = System.currentTimeMillis();

			// sort the individuals according to the fitness
			Collections.sort(population);

			if (WSCInitializer.evalCounter == WSCInitializer.evalStep) {
				WSCInitializer.bestFitnessSoFar4EvalTimes.add(population.get(0));
			} else {
				if (WSCInitializer.bestFitnessSoFar4EvalTimes.get(
						WSCInitializer.bestFitnessSoFar4EvalTimes.size() - 1).fitness < population.get(0).fitness) {
					WSCInitializer.bestFitnessSoFar4EvalTimes.add(population.get(0));
				} else {
					WSCInitializer.bestFitnessSoFar4EvalTimes.add(WSCInitializer.bestFitnessSoFar4EvalTimes
							.get(WSCInitializer.bestFitnessSoFar4EvalTimes.size() - 1));
				}
			}

			if (WSCInitializer.evalCounter == WSCInitializer.evalMax) {
				writeLogs();
				System.exit(0);
			}

		}
	}

	public void writeLogs() {
		try {
			FileWriter writer = new FileWriter(new File(WSCInitializer.logName));
			for (int i = 0; i < WSCInitializer.bestFitnessSoFar4EvalTimes.size(); i++) {
				writer.append(String.format("%d %d %d %f\n", i, WSCInitializer.initTime.get(i),
						WSCInitializer.time.get(i), WSCInitializer.bestFitnessSoFar4EvalTimes.get(i).fitness));
			}
			writer.append(WSCInitializer.bestFitnessSoFar4EvalTimes
					.get(WSCInitializer.bestFitnessSoFar4EvalTimes.size() - 1).getStrRepresentation());
			writer.append("\n");

			// print out the entropy for obeservation
			for (int i = 0; i < NHBSA.entropy4Gen.size(); i++) {
				writer.append(String.format("%d %s\n", i, NHBSA.entropy4Gen.get(i)));
			}
			//
			// LineChart lc = new LineChart();
			// lc.createLineChart(NHBSA.entropy4Gen, NHBSA.discountRate4Gen);

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeLogs(NHBSA nhbsa) {
		try {
			FileWriter writer = new FileWriter(new File(WSCInitializer.logName));
			for (int i = 0; i < WSCInitializer.bestFitnessSoFar4EvalTimes.size(); i++) {
				writer.append(String.format("%d %d %d %f\n", i, WSCInitializer.initTime.get(i),
						WSCInitializer.time.get(i), WSCInitializer.bestFitnessSoFar4EvalTimes.get(i).fitness));
			}
			writer.append(WSCInitializer.bestFitnessSoFar4EvalTimes
					.get(WSCInitializer.bestFitnessSoFar4EvalTimes.size() - 1).getStrRepresentation());
			writer.append("\n");

			// print out the entropy for obeservation
			for (int i = 0; i < nhbsa.getEntropy4Gen().size(); i++) {
				writer.append(String.format("%d %s\n", i, nhbsa.getEntropy4Gen().get(i)));
			}

			LineChart lc = new LineChart();
			lc.createLineChart(nhbsa.getEntropy4Gen(), nhbsa.getDiscountRate4Gen());

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
