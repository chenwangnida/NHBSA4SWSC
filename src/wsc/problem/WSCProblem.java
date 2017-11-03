package wsc.problem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;

import nhbsa.NHBSA;
import wsc.graph.GraphUtils;
import wsc.graph.ServiceEdge;
import wsc.graph.ServiceGraph;

public class WSCProblem {
	public static void main(String[] args) throws IOException {

		if (args.length != 5) {
			System.out.println("missing arguments!");
			return;
		}

		WSCInitializer init = new WSCInitializer(args[0], args[1], args[2], args[3], Long.valueOf(args[4]));

		WSCProblem p = new WSCProblem();
		p.NHBSASolver(init);

	}

	// The main entry to NHBSASolver.
	public void NHBSASolver(WSCInitializer init) {
		List<WSCIndividual> population = new ArrayList<WSCIndividual>();
		WSCEvaluation eval = new WSCEvaluation();

		// random initalize one population solutions
		while (population.size() < WSCInitializer.population_size) {
			WSCIndividual individual = new WSCIndividual();

			// we need to modify the codes to generate an array of queue
			ServiceGraph graph = generateGraph(init);
			eval.aggregationAttribute(individual, graph);
			eval.calculateFitness(individual);
			population.add(individual);
		}

		int iteration = 0;
		while (iteration < WSCInitializer.MAX_NUM_ITERATIONS) {
			long startTime = System.currentTimeMillis();
			System.out.println("ITERATION " + iteration);
			// add a local search
			// need to be done later

			// sort the individuals according to the fitness
			Collections.sort(population);

			// update best individual so far
			if (iteration == 0) {
				WSCInitializer.bestFitnessSoFar.add(population.get(0));
			} else {
				if (WSCInitializer.bestFitnessSoFar.get(iteration - 1).fitness < population.get(0).fitness) {
					WSCInitializer.bestFitnessSoFar.add(population.get(0));
				} else {
					WSCInitializer.bestFitnessSoFar.add(WSCInitializer.bestFitnessSoFar.get(iteration - 1));
				}
			}

			// entry to NHBSA

			NHBSA nhbsa = new NHBSA(WSCInitializer.dimension_size, WSCInitializer.dimension_size);
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

				for (int n = 0; n < id_updated.length; n++) {
					// do I need to clone ??????????????????????
					// ???????????????
					indi_updated.serQueue.add(id_updated[n]);

				}
				// generate $updated_graph$ from updatedIndividual
				// ?? need to be completed

				// evaluate updated updated_graph
				// eval.aggregationAttribute(indi_updated, updated_graph);
				eval.calculateFitness(indi_updated);
				population.add(indi_updated);
			}

			WSCInitializer.initTime.add(WSCInitializer.initialization);
			WSCInitializer.time.add(System.currentTimeMillis() - startTime);
			WSCInitializer.initialization = (long) 0;

			iteration += 1;
		}
		writeLogs();

	}

	/**
	 * generate graph that remove all dangle nodes
	 *
	 * @return graph
	 */

	public ServiceGraph generateGraph(WSCInitializer init) {

		ServiceGraph graph = new ServiceGraph(ServiceEdge.class);

		WSCInitializer.initialWSCPool.createGraphService(WSCInitializer.taskInput, WSCInitializer.taskOutput, graph);

		while (true) {
			List<String> dangleVerticeList = dangleVerticeList(graph);
			if (dangleVerticeList.size() == 0) {
				break;
			}
			removeCurrentdangle(graph, dangleVerticeList);
		}
		graph.removeEdge("startNode", "endNode");
		// System.out.println("original DAG:"+graph.toString());
		optimiseGraph(graph);
		// System.out.println("optimised DAG:"+graph.toString());

		return graph;
	}

	private static List<String> dangleVerticeList(DirectedGraph<String, ServiceEdge> directedGraph) {
		Set<String> allVertice = directedGraph.vertexSet();

		List<String> dangleVerticeList = new ArrayList<String>();
		for (String v : allVertice) {
			int relatedOutDegree = directedGraph.outDegreeOf(v);

			if (relatedOutDegree == 0 && !v.equals("endNode")) {
				dangleVerticeList.add(v);

			}
		}
		return dangleVerticeList;
	}

	private static void removeCurrentdangle(DirectedGraph<String, ServiceEdge> directedGraph,
			List<String> dangleVerticeList) {
		// Iterator the endTangle
		for (String danglevertice : dangleVerticeList) {

			Set<ServiceEdge> relatedEdge = directedGraph.incomingEdgesOf(danglevertice);
			Set<String> potentialTangleVerticeList = new HashSet<String>();

			for (ServiceEdge edge : relatedEdge) {
				String potentialTangleVertice = directedGraph.getEdgeSource(edge);
				// System.out.println("potentialTangleVertice:" +
				// potentialTangleVertice);
				potentialTangleVerticeList.add(potentialTangleVertice);
			}

			directedGraph.removeVertex(danglevertice);
		}
	}

	/**
	 * remove the edge between the node and it direct successors that also consumed
	 * by successors of direct successors
	 * 
	 * @param serviceGraph
	 */
	private void optimiseGraph(ServiceGraph graph) {
		for (String vertice : graph.vertexSet()) {
			if (graph.outDegreeOf(vertice) > 1) {
				List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();

				outgoingEdges.addAll(graph.outgoingEdgesOf(vertice));

				for (ServiceEdge outgoingedge : outgoingEdges) {
					if (graph.getEdgeTarget(outgoingedge).equals("endNode")) {
						// Remove the output node from the children list
						outgoingEdges.remove(outgoingedge);
						break;
					}

				}

				if (outgoingEdges.size() > 1) {
					// save the direct successors
					Set<String> directSuccesors = new HashSet<String>();
					Set<String> allTargets = new HashSet<String>();

					outgoingEdges.forEach(outgoingedge -> directSuccesors.add(graph.getEdgeTarget(outgoingedge)));

					for (String succesor : directSuccesors) {
						Set<String> targets = GraphUtils.getOutgoingVertices(graph, succesor);
						allTargets.addAll(targets);
					}

					for (String succesor : directSuccesors) {
						if (allTargets.contains(succesor)) {
							graph.removeEdge(vertice, succesor);
						}
					}
				}
			}
		}
	}

	public void writeLogs() {
		try {
			FileWriter writer = new FileWriter(new File(WSCInitializer.logName));
			for (int i = 0; i < WSCInitializer.bestFitnessSoFar.size(); i++) {
				writer.append(String.format("%d %d %d %f\n", i, WSCInitializer.initTime.get(i),
						WSCInitializer.time.get(i), WSCInitializer.bestFitnessSoFar.get(i).fitness));
			}
			writer.append(WSCInitializer.bestFitnessSoFar.get(WSCInitializer.bestFitnessSoFar.size() - 1)
					.getStrRepresentation());
			writer.append("\n");
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
