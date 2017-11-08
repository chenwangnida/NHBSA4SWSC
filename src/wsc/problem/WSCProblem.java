package wsc.problem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.GraphIterator;

import nhbsa.LocalSearch;
import nhbsa.NHBSA;
import wsc.InitialWSCPool;
import wsc.data.pool.Service;
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
		WSCGraph graGenerator = new WSCGraph();
		WSCEvaluation eval = new WSCEvaluation();

		WSCProblem p = new WSCProblem();
		p.NHBSASolver(graGenerator, eval);

	}

	// The main entry to NHBSASolver.
	public void NHBSASolver(WSCGraph graGenerator, WSCEvaluation eval) {
		List<WSCIndividual> population = new ArrayList<WSCIndividual>();

		// random initalize one population solutions
		while (population.size() < WSCInitializer.population_size) {
			WSCIndividual individual = new WSCIndividual();
			List<Integer> usedSerQueue = new ArrayList();

			// graph-based representation
			ServiceGraph graph = graGenerator.generateGraph();

			// create a queue of services according to breathfirstsearch
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
		}

		// entry to learn the matrix and sampling individuals
		int iteration = 0;
		while (iteration < WSCInitializer.MAX_NUM_ITERATIONS) {
			long startTime = System.currentTimeMillis();
			System.out.println("ITERATION " + iteration);

			// add a local search
			LocalSearch ls = new LocalSearch();
			ls.swap(population, WSCInitializer.random, graGenerator, eval);

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
			}

			WSCInitializer.initTime.add(WSCInitializer.initialization);
			WSCInitializer.time.add(System.currentTimeMillis() - startTime);
			WSCInitializer.initialization = (long) 0;

			iteration += 1;
		}
		writeLogs();

	}

	// private List<Integer> usedQueueofLayers(String sourceVertice, ServiceGraph
	// graph, List<Integer> usedSerQueue) {
	//
	// GraphIterator<String, ServiceEdge> iterator = new
	// BreadthFirstIterator<String, ServiceEdge>(graph,
	// sourceVertice);
	// while (iterator.hasNext()) {
	// String serviceId = iterator.next();
	// if ((!serviceId.equals("startNode")) && (!serviceId.equals("endNode"))) {
	// usedSerQueue.add(WSCInitializer.serviceIndexBiMap.inverse().get(serviceId));
	// }
	// }
	//
	// return usedSerQueue;
	// }
	//
	// private List<Integer> completeSerQueueIndi(List<Integer> usedQueue) {
	// for (Service ser : WSCInitializer.initialWSCPool.getServiceSequence()) {
	// if (!usedQueue.contains(ser.serviceIndex)) {
	// usedQueue.add(ser.serviceIndex);
	// }
	// }
	// if (usedQueue.size() !=
	// WSCInitializer.initialWSCPool.getServiceSequence().size()) {
	//
	// System.err.println("the size of individual is not correcct");
	// return null;
	// }
	//
	// return usedQueue;
	// }

	// // remove the serviceId
	// private Set<Integer> removingDangle4UsedQueue(ServiceGraph graph,
	// Set<Service> usedSerQueue) {
	// Set<Integer> usedSerIdQueue = new HashSet<Integer>();
	// Set<String> vertices = graph.vertexSet();
	//
	// for (Iterator<Service> iterator = usedSerQueue.iterator();
	// iterator.hasNext();) {
	// Service s = iterator.next();
	// if (!(vertices.contains(s.serviceID))) {
	// // Remove the current element from the iterator and the list.
	// iterator.remove();
	// }
	// }
	//
	// // transfer service set into integer set
	// usedSerQueue.forEach(ser -> usedSerIdQueue.add(ser.serviceIndex));
	// return usedSerIdQueue;
	// }

	// /**
	// * generate graph that remove all dangle nodes
	// *
	// * @return graph
	// */
	//
	// public ServiceGraph generateGraph() {
	//
	// ServiceGraph graph = new ServiceGraph(ServiceEdge.class);
	//
	// WSCInitializer.initialWSCPool.createGraphService(WSCInitializer.taskInput,
	// WSCInitializer.taskOutput, graph);
	//
	// while (true) {
	// List<String> dangleVerticeList = dangleVerticeList(graph);
	// if (dangleVerticeList.size() == 0) {
	// break;
	// }
	// removeCurrentdangle(graph, dangleVerticeList);
	// }
	// graph.removeEdge("startNode", "endNode");
	// // System.out.println("original DAG:"+graph.toString());
	// optimiseGraph(graph);
	// // System.out.println("optimised DAG:"+graph.toString());
	//
	// return graph;
	// }
	//
	// public ServiceGraph generateGraphBySerQueue() {
	//
	// ServiceGraph graph = new ServiceGraph(ServiceEdge.class);
	//
	// WSCInitializer.initialWSCPool.createGraphServiceBySerQueue(WSCInitializer.taskInput,
	// WSCInitializer.taskOutput,
	// graph);
	//
	// while (true) {
	// List<String> dangleVerticeList = dangleVerticeList(graph);
	// if (dangleVerticeList.size() == 0) {
	// break;
	// }
	// removeCurrentdangle(graph, dangleVerticeList);
	// }
	// graph.removeEdge("startNode", "endNode");
	// // System.out.println("original DAG:"+graph.toString());
	// optimiseGraph(graph);
	// // System.out.println("optimised DAG:"+graph.toString());
	//
	// return graph;
	// }
	//
	// private static List<String> dangleVerticeList(DirectedGraph<String,
	// ServiceEdge> directedGraph) {
	// Set<String> allVertice = directedGraph.vertexSet();
	//
	// List<String> dangleVerticeList = new ArrayList<String>();
	// for (String v : allVertice) {
	// int relatedOutDegree = directedGraph.outDegreeOf(v);
	//
	// if (relatedOutDegree == 0 && !v.equals("endNode")) {
	// dangleVerticeList.add(v);
	//
	// }
	// }
	// return dangleVerticeList;
	// }
	//
	// private static void removeCurrentdangle(DirectedGraph<String, ServiceEdge>
	// directedGraph,
	// List<String> dangleVerticeList) {
	// // Iterator the endTangle
	// for (String danglevertice : dangleVerticeList) {
	//
	// Set<ServiceEdge> relatedEdge = directedGraph.incomingEdgesOf(danglevertice);
	// Set<String> potentialTangleVerticeList = new HashSet<String>();
	//
	// for (ServiceEdge edge : relatedEdge) {
	// String potentialTangleVertice = directedGraph.getEdgeSource(edge);
	// // System.out.println("potentialTangleVertice:" +
	// // potentialTangleVertice);
	// potentialTangleVerticeList.add(potentialTangleVertice);
	// }
	//
	// directedGraph.removeVertex(danglevertice);
	// }
	// }
	//
	// /**
	// * remove the edge between the node and it direct successors that also
	// consumed
	// * by successors of direct successors
	// *
	// * @param serviceGraph
	// */
	// private void optimiseGraph(ServiceGraph graph) {
	// for (String vertice : graph.vertexSet()) {
	// if (graph.outDegreeOf(vertice) > 1) {
	// List<ServiceEdge> outgoingEdges = new ArrayList<ServiceEdge>();
	//
	// outgoingEdges.addAll(graph.outgoingEdgesOf(vertice));
	//
	// for (ServiceEdge outgoingedge : outgoingEdges) {
	// if (graph.getEdgeTarget(outgoingedge).equals("endNode")) {
	// // Remove the output node from the children list
	// outgoingEdges.remove(outgoingedge);
	// break;
	// }
	//
	// }
	//
	// if (outgoingEdges.size() > 1) {
	// // save the direct successors
	// Set<String> directSuccesors = new HashSet<String>();
	// Set<String> allTargets = new HashSet<String>();
	//
	// outgoingEdges.forEach(outgoingedge ->
	// directSuccesors.add(graph.getEdgeTarget(outgoingedge)));
	//
	// for (String succesor : directSuccesors) {
	// Set<String> targets = GraphUtils.getOutgoingVertices(graph, succesor);
	// allTargets.addAll(targets);
	// }
	//
	// for (String succesor : directSuccesors) {
	// if (allTargets.contains(succesor)) {
	// graph.removeEdge(vertice, succesor);
	// }
	// }
	// }
	// }
	// }
	// }

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
