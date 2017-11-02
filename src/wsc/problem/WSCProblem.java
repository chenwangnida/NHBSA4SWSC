package wsc.problem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;

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

		// random initalize one population solutions
		while (population.size() < init.pop_size) {
			WSCIndividual indi = new WSCIndividual();
			ServiceGraph graph = generateGraph(init);

			// indi.items = itemList;
			// evaluate fitness and punishment for TotalWeight
			// indi.evaluateIndi(indi, totalWeight);
			population.add(indi);
		}

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

}
