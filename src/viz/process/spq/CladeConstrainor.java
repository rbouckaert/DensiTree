package viz.process.spq;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import viz.Node;

/**
 * This class represents the process to find possible sequences (represented by
 * a {@link PQTree}) that comply to as many as possible {@link Clade clades}.
 */
public class CladeConstrainor extends Constrainor {

	private Clade[] clades;
	private BitSet cladesCovered;
	//private String[] taxa;
	private int numberOfLeaves;

	public CladeConstrainor(Node [] trees) {
		super();
		this.trees = trees;
		//this.taxa = trees[0].getTaxaNames();
		this.numberOfLeaves = Util.getLeafNodeCount(trees[0]);
	}

	public CladeConstrainor(Clade[] clades) {
		super();
		this.clades = clades;
		this.cladesCovered = new BitSet(clades.length);
		this.numberOfLeaves = clades[0].bits.length();
	}

	public void run() {
		// 1. extract clades
		initClades();

		// 2. create star tree
		createBinaryStartTree();

		// 3. constrain tree with clade after clade
		constrainWithClades();
		
		// 4. optimise ...
		optimise();
	}

	private void optimise() {
		// determine PQVertex node heights
		Map<BitSet, PQVertex> vertexMap = new LinkedHashMap<BitSet, PQVertex>();
		populateMap(tree.getRoot(), vertexMap);
		
		for (Node inputTree : trees) {
			getHeights(inputTree, vertexMap);
		}
		
		// sort children of nodes in tree by branch length
		sort(tree.getRoot());
	}
			
	private void populateMap(PQVertex node, Map<BitSet, PQVertex> vertexMap) {
		vertexMap.put(node.getClade(), node);		
		for (PQVertex child : node.getChildren()) {
				populateMap(child, vertexMap);
		}		
	}

	private BitSet getHeights(Node node, Map<BitSet, PQVertex> vertexMap) {
		BitSet bits = new BitSet(numberOfLeaves);
		PQVertex vertex = vertexMap.get(bits);
		if (vertex != null) {
			vertex.addHeight(node.m_fPosY);
		}

		if (node.isLeaf()) {
			int index = node.getNr(); // mapTaxonIDToNr.get(node.getID());
			bits.set(index);
		} else {
			BitSet left = getHeights(node.m_left, vertexMap);
			BitSet right = getHeights(node.m_right, vertexMap);
			bits.or(left);
			bits.or(right);
		}
		return bits;
	}

	private void sort(PQVertex node) { 
		List<PQVertex> nodes = node.getChildren();
		
		Collections.sort(nodes, (x,y) -> {
			return x.getLength() < y.getLength() ? 1 : -1;
		});
		
		for (PQVertex child : nodes) {
			sort(child);
		}
	}

	private void initClades() {
		if (clades == null) {
			extractClades();
			sortClades();

//			int k = 0;
//			for (Clade clade : clades) {
//				System.out.println(k++ + " " + clade.getCount() + " " + clade);
//			}
		}
	}

	private void extractClades() {
		Map<BitSet, Map<BitSet, Clade>> conditionalCladeMap = new HashMap<BitSet, Map<BitSet, Clade>>();
		Map<BitSet, Clade> cladeMap = new LinkedHashMap<BitSet, Clade>();

		Iterator<Node> iterator = Arrays.stream(trees).iterator();
		Node currentBeastTree;
		while (iterator.hasNext()) {
			currentBeastTree = iterator.next();
			addClades(currentBeastTree, cladeMap, conditionalCladeMap);
		}

		clades = cladeMap.values().toArray(new Clade[] {});
	}

	private BitSet addClades(Node node, Map<BitSet, Clade> cladeMap,
			Map<BitSet, Map<BitSet, Clade>> conditionalCladeMap) {
		BitSet bits = new BitSet(numberOfLeaves);

		if (node.isLeaf()) {
			int index = node.getNr(); // mapTaxonIDToNr.get(node.getID());
			bits.set(index);
		} else {
			BitSet left = addClades(node.m_left, cladeMap, conditionalCladeMap);
			BitSet right = addClades(node.m_right, cladeMap, conditionalCladeMap);
			bits.or(left);
			bits.or(right);

			addClade(bits, left, right, cladeMap, conditionalCladeMap);
		}

		return bits;
	}

	private void addClade(BitSet bits, BitSet left, BitSet right, Map<BitSet, Clade> cladeMap,
			Map<BitSet, Map<BitSet, Clade>> conditionalCladeMap) {
		Clade clade = cladeMap.get(bits);

		if (clade == null) {
			clade = new Clade(bits, taxa);
			cladeMap.put(bits, clade);
		}
		clade.incrementCount();

		Map<BitSet, Clade> clades = conditionalCladeMap.get(bits);
		if (clades == null) {
			clades = new HashMap<>();
			conditionalCladeMap.put(bits, clades);
		}

		clade = clades.get(left);
		if (clade == null) {
			clade = new Clade(left, taxa);
			clades.put(left, clade);
		}
		clade.incrementCount();

		clade = clades.get(right);
		if (clade == null) {
			clade = new Clade(right, taxa);
			clades.put(right, clade);
		}
		clade.incrementCount();
	}

	private void sortClades() {
		Arrays.sort(clades, (Clade o1, Clade o2) -> {
			if (o1.getCount() < o2.getCount()) {
				return 1;
			}
			if (o1.getCount() > o2.getCount()) {
				return -1;
			}
			if (o1.getSize() < o2.getSize()) {
				return -1;
			}
			if (o1.getSize() > o2.getSize()) {
				return 1;
			}
			return 0;
		});
	}

	private void createBinaryStartTree() {
		tree = PQTree.createStarTree(numberOfLeaves);
	}

	boolean[] fittedClade;
	
	public boolean fitted(int cladeNr) {
		if (fittedClade == null) {
			throw new RuntimeException("fittedClade is not initialised yet: call run() before calling fitted()");
		}
		return fittedClade[cladeNr];
	}
	
	public Clade getClade(int cladeNr) {
		if (clades == null) {
			throw new RuntimeException("Clades are not initialised yet: call run() before calling getClade()");
		}
		return clades[cladeNr];
	}
	
	public int getCladeCount() {
		if (clades == null) {
			throw new RuntimeException("Clades are not initialised yet: call run() before calling getCladeCount()");
		}
		return clades.length;
	}
	
	private void constrainWithClades() {
		int fitted = 0;
		fittedClade = new boolean[clades.length];

		for (int i = 0; i < clades.length; i++) {
			boolean fittedIn = tree.constrainByClade(clades[i].bits);
			fittedClade[i] = fittedIn;
			fitted += fittedIn ? 1 : 0;
		}

//		System.out.println(fitted + " of " + clades.length + " clades fitted.");
//		for (int i = 0; i < fittedClade.length; i++) {
//			String result = fittedClade[i] ? "+" : "-";
//			System.out.println(result + " clade " + clades[i]);
//			System.out.println(result + " " + clades[i].bits);
//		}
	}

	public Clade[] getClades() {
		return this.clades;
	}

	public BitSet getCladesCoveredIndicator() {
		return cladesCovered;
	}
}
