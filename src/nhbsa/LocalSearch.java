package nhbsa;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import wsc.InitialWSCPool;
import wsc.data.pool.Service;
import wsc.graph.ServiceGraph;
import wsc.problem.WSCEvaluation;
import wsc.problem.WSCGraph;
import wsc.problem.WSCIndividual;
import wsc.problem.WSCInitializer;

public class LocalSearch {

	// stochastic local search
	public List<WSCIndividual> swap(List<WSCIndividual> population, Random random, WSCGraph graGenerator,
			WSCEvaluation eval) {

		// swap

		int split = 0;
		for (WSCIndividual indi : population) {

			double rSLS = random.nextDouble();

			if (rSLS < WSCInitializer.Pls) {

				// obtain the split position of the individual
				split = indi.getSplitPosition();

				WSCIndividual indi_temp = new WSCIndividual();
				List<Integer> serQueue_temp = new ArrayList<Integer>(); // service Index arrayList

				// deep clone the services into a service queue for indi_temp
				for (Integer ser : indi.serQueue) {
					serQueue_temp.add(ser);

				}

				indi_temp.serQueue = serQueue_temp;

				if (split == 0) {
					System.out.println(split);
				}

				int swap_a = random.nextInt(split);// between 0 (inclusive) and split (exclusive)

				int swap_b = split + random.nextInt(WSCInitializer.dimension_size - split);// between split(inclusive)
																							// and
																							// itemsize
				// (exclusive)

				Integer item_a = indi_temp.serQueue.get(swap_a);
				Integer item_b = indi_temp.serQueue.get(swap_b);
				Integer item_temp = new Integer(item_a);

				indi_temp.serQueue.set(swap_a, item_b);
				indi_temp.serQueue.set(swap_b, item_temp);

				List<Service> serviceCandidates = new ArrayList<Service>();
				for (int n = 0; n < indi_temp.serQueue.size(); n++) {

					// deep clone may be not needed if no changes are applied to the pointed service
					serviceCandidates.add(WSCInitializer.Index2ServiceMap.get(indi_temp.serQueue.get(n)));
				}

				// set the service candidates according to the sampling
				InitialWSCPool.getServiceCandidates().clear();
				InitialWSCPool.setServiceCandidates(serviceCandidates);
				ServiceGraph update_graph = graGenerator.generateGraphBySerQueue();
				// adjust the bias according to the valid solution from the service queue

				List<Integer> usedSerQueue = new ArrayList<Integer>();
				// create a queue of services according to BreathFirstSearch algorithm

				List<Integer> usedQueue = graGenerator.usedQueueofLayers("startNode", update_graph, usedSerQueue);
				// set up the split index for the updated individual
				indi_temp.setSplitPosition(usedQueue.size());

				// add unused queue to form a complete a vector-based individual
				List<Integer> serQueue = graGenerator.completeSerQueueIndi(usedQueue);

				// set the serQueue to the updatedIndividual
				indi_temp.serQueue = serQueue;

				// evaluate updated updated_graph
				// eval.aggregationAttribute(indi_updated, updated_graph);
				eval.aggregationAttribute(indi_temp, update_graph);
				eval.calculateFitness(indi_temp);

				// replace it current individual if a better solution found by swap method
				if (indi_temp.fitness > indi.fitness) {
					population.set(population.indexOf(indi), indi_temp);
					WSCInitializer.noOfls++;
					System.out.println("No. of effective local search :" + WSCInitializer.noOfls + 1);
				}
			}

		}

		return population;
	}

}
