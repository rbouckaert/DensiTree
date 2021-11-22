package viz.process;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import org.apache.commons.math.FunctionEvaluationException;
//import org.apache.commons.math.analysis.DifferentiableMultivariateRealFunction;
//import org.apache.commons.math.analysis.MultivariateRealFunction;
//import org.apache.commons.math.analysis.MultivariateVectorialFunction;

import viz.DensiTree;
import viz.Node;

public class BranchScorer {// implements DifferentiableMultivariateRealFunction {

	Map<Integer, CladeBranchInfo> m_cladeBranchInfo;
	DensiTree m_dt;
	double nonCladeScore = -1;
	Node [] nodes;
	
	public BranchScorer(DensiTree dt, Node [] nodes) {
		this.nodes = nodes;
		m_cladeBranchInfo = new HashMap<Integer, CladeBranchInfo>();
		m_dt = dt;
	}
	
	double score(float [] heights) {
		double cladeScore = 0;
		Set<Integer> clades = new HashSet<Integer>();
		// count contributions of clades in root canal tree
		// and initialise CladeBranchInfo structures
		for (Node node: nodes) {
			int iClade = node.m_iClade;
			clades.add(iClade);
			if (!node.isRoot()) {
				int iNode = node.getNr();
				CladeBranchInfo info = null;
				if (!m_cladeBranchInfo.containsKey(node.m_iClade)) {
					info = new CladeBranchInfo();
					info.initialise(m_dt.treeData.m_cladeHeightSetBottom.get(iClade), m_dt.treeData.m_cladeHeightSetTop.get(iClade), m_dt.treeData.m_trees.length);
					m_cladeBranchInfo.put(iClade, info);
				} else {
					info = m_cladeBranchInfo.get(iClade);
				}
				cladeScore += info.score(heights[node.getParent().getNr()], heights[iNode]);
			} else {
				if (!m_cladeBranchInfo.containsKey(iClade)) {
					m_cladeBranchInfo.put(iClade, new CladeBranchInfo());
				}
			}
		}
		
		if (nonCladeScore < 0) {
			// count contributions of all clades not in root canal tree 
			nonCladeScore = 0;
			for (int i = 0; i < m_dt.treeData.m_cladeHeightSetBottom.size(); i++) {
				if (!clades.contains(i)) {
					List<Double> bottom = m_dt.treeData.m_cladeHeightSetBottom.get(i);
					List<Double> top = m_dt.treeData.m_cladeHeightSetTop.get(i);
					double sum = 0;
					for (int j = 0; j < bottom.size(); j++) {
						sum += bottom.get(j) - top.get(j);
					}
					nonCladeScore += sum;
				}
			}
		}
		return cladeScore + nonCladeScore;
	}

	float [] heights;
	
//	@Override
//	public double value(double[] point) throws FunctionEvaluationException, IllegalArgumentException {
//		if (heights == null) {
//			heights = new float[point.length];
//		}
//		for (int i = 0; i < point.length; i++) {
//			heights[i] = (float) point[i];
//		}
//		return score(heights);
//	}
//
//	@Override
//	public MultivariateRealFunction partialDerivative(int k) {
//		MultivariateRealFunction f = new MultivariateRealFunction() {
//			
//			@Override
//			public double value(double[] point) throws FunctionEvaluationException, IllegalArgumentException {
//				double dScore = 0;
//				for (int i = 0; i < nodes.length; i++) {
//					Node node = nodes[i];
//					int iClade = node.m_iClade;
//					CladeBranchInfo info = m_cladeBranchInfo.get(iClade);
//					dScore += info.dScore(0f, (float) point[i]);
//				}
//				return dScore;
//			}
//		};
//			
//		return f;
//	}
//
//	@Override
//	public MultivariateVectorialFunction gradient() {
//		// TODO Auto-generated method stub
//		return null;
//	}

}
