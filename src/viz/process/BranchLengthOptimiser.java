package viz.process;


import java.io.PrintStream;


//import org.apache.commons.math.FunctionEvaluationException;
//import org.apache.commons.math.optimization.DifferentiableMultivariateRealOptimizer;
//import org.apache.commons.math.optimization.GoalType;
//import org.apache.commons.math.optimization.MultivariateRealOptimizer;
//import org.apache.commons.math.optimization.OptimizationException;
//import org.apache.commons.math.optimization.RealPointValuePair;
//import org.apache.commons.math.optimization.VectorialPointValuePair;
//import org.apache.commons.math.optimization.direct.PowellOptimizer;
//import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;


import viz.DensiTree;
import viz.Node;

public class BranchLengthOptimiser {
	DensiTree m_dt;
	BranchScorer scorer;

	final static int MAX_ATTEMPTS = 500;
	final static float RANGE = 100;

	public BranchLengthOptimiser(DensiTree dt) {
		m_dt = dt;
	}

	
	
	
	public void optimiseScore(Node tree) {
		long start = System.currentTimeMillis();

		// initialise CladeBranchInfo data structures		
		float [] heights = new float[m_dt.m_sLabels.size() * 2 - 1];
		Node [] nodes = new Node[m_dt.m_sLabels.size() * 2 - 1];
		collectNodes(tree, nodes, heights, tree.m_fPosY);
		scorer = new BranchScorer(m_dt, nodes);
		double startscore = scorer.score(heights);
		
		initialiseTree(heights, nodes);
		
		long start2 = System.currentTimeMillis();
		

//		PowellOptimizer optimizer = new PowellOptimizer(); 
//		double [] startvalue = new double[heights.length];
//		for (int i = 0; i < heights.length; i++) {
//			startvalue[i] = heights[i];
//		}
//		try {
//			RealPointValuePair optimum = optimizer.optimize(scorer,
//					GoalType.MINIMIZE,
//			        startvalue);
//			for (int i = 0; i < heights.length; i++) {
//				heights[i] = (float)optimum.getPoint()[i];
//			}
//			for (int i = 0; i < heights.length; i++) {
//				Node node = nodes[i];
//				if (!node.isRoot()) {
//					node.m_fLength = heights[i] - heights[node.getParent().getNr()];
//				}
//			}
//		} catch (OptimizationException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (FunctionEvaluationException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (IllegalArgumentException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		optimiseTree(heights, nodes, scorer);
		
		for (int i = 0; i < nodes.length - 1; i++) {
			nodes[i].m_fLength = heights[i] - heights[nodes[i].getParent().getNr()];
		}

		long end = System.currentTimeMillis();
		System.err.println("\n\n\n" + (end-start)/1000.0 + " seconds optimising " + (start2-start)/1000.0 + " seconds initialising");
		double endscore = scorer.score(heights);
		System.err.println("Start score: " + startscore + " End score: " + endscore + "\n\n\n");
		if (m_dt.m_sOptFile != null) {
			try {
				PrintStream out = new PrintStream(m_dt.m_sOptFile);
				out.println(tree.toString(m_dt.m_sLabels, false));
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.exit(0);
		}
	}
	
	void optimiseTree(float[] heights, Node[] nodes, BranchScorer scorer) {
		boolean bProgress = true;
		for (int i = 0; i < MAX_ATTEMPTS && bProgress; i++) {
			bProgress = false;
			// optimise internal nodes by finding the best Uniform operation on a grid
			// for each node individually
			for (int k = nodes.length/2+1; k < nodes.length; k++) {
				Node node = nodes[k];
				int iCladeLeft = node.m_left.m_iClade;
				int iCladeRight = node.m_right.m_iClade;

				float leftHeight = heights[node.m_left.m_iLabel];
				float rightHeight = heights[node.m_right.m_iLabel];
				float minHeight = Math.min(leftHeight, rightHeight);
				float maxHeight;
				if (node.isRoot()) {
					CladeBranchInfo infoLeft = scorer.m_cladeBranchInfo.get(iCladeLeft);
					CladeBranchInfo infoRight = scorer.m_cladeBranchInfo.get(iCladeRight);
					maxHeight = Math.min(leftHeight - infoLeft.getMaxLength(), rightHeight - infoRight.getMaxLength());
				} else {
					maxHeight = heights[node.getParent().getNr()];
				}
				
				float bestHeight = heights[node.getNr()];				
				
				heights[k] = bestHeight; 
				double bestScore = scorer.score(heights);
						
				for (int j = 1; j < RANGE; j++) {
					float height = j*(maxHeight - minHeight)/RANGE + minHeight;
					heights[k] = height;
					double score = scorer.score(heights);
					if (score < bestScore) {
						bProgress = true;
						bestScore = score;
						bestHeight = height;
					}
				}
				
				heights[k] = bestHeight;
			}
			System.err.print(".");
		}
	}

	private void initialiseTree(float[] heights, Node[] nodes) {
		// do pre-optimisation; position each node optimally, without considering parents
		for (int k = m_dt.m_sLabels.size(); k < nodes.length; k++) {
			Node node = nodes[k];
			CladeBranchInfo infoLeft = scorer.m_cladeBranchInfo.get(node.m_left.m_iClade);
			CladeBranchInfo infoRight = scorer.m_cladeBranchInfo.get(node.m_right.m_iClade);
			
			float leftHeight = heights[node.m_left.m_iLabel];
			float rightHeight = heights[node.m_right.m_iLabel];
			float minHeight = Math.min(leftHeight, rightHeight);
			float maxHeight = Math.min(leftHeight - infoLeft.getMaxLength(), rightHeight - infoRight.getMaxLength());
			
			float bestHeight = Math.min(minHeight, heights[node.m_iLabel]);

			float bestScore = infoLeft.score(bestHeight, leftHeight) +
					infoRight.score(bestHeight, rightHeight);
					
			for (int j = 2; j < RANGE; j++) {
				float height = j*(maxHeight - minHeight)/RANGE + minHeight;
				float score = infoLeft.score(height, leftHeight) +
						infoRight.score(height, rightHeight);
				if (score < bestScore) {
					bestScore = score;
					bestHeight = height;
				}
			}
			
			heights[k] = bestHeight;
			if (!node.isRoot()) {
				node.m_fLength = bestHeight - heights[node.getParent().m_iLabel];
			}
			node.m_left.m_fLength = leftHeight - bestHeight;
			node.m_right.m_fLength = rightHeight - bestHeight;
			
		}		
	}

	private void collectNodes(Node node, Node[] nodes, float [] heights, float height) {
		nodes[node.m_iLabel] = node;
		heights[node.m_iLabel] = height;
		if (!node.isLeaf()) {
			collectNodes(node.m_left, nodes, heights, height + node.m_left.m_fLength);
			collectNodes(node.m_right, nodes, heights, height + node.m_right.m_fLength);
		}
	}
	

} // class BranchLengthOptimiser 

//38.52506983048988
