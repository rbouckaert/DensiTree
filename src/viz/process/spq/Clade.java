package viz.process.spq;

import java.util.BitSet;

public class Clade {

	BitSet bits;
	String[] taxa;
	int count = 0;
	double credibility = 0.0;

	public Clade(BitSet bits, String[] taxa) {
		this.bits = bits;
		this.taxa = taxa;
	}

	int getSize() {
		return bits.cardinality();
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public void incrementCount() {
		this.count += 1;
	}

	public double getCredibility() {
		return credibility;
	}

	public void setCredibility(double credibility) {
		this.credibility = credibility;
	}

	public int[] getItems() {
		int[] items = new int[bits.cardinality()];
		int k = 0;
		for (int i = 0; i < bits.length(); i++) {
			if (bits.get(i)) {
				items[k++] = i;
			}
		}
		return items;
	}

	public int firstItem() {
		return bits.nextSetBit(0);
	}

	public boolean isSubSetOf(BitSet other) {
		if (other.cardinality() >= bits.cardinality()) {
			BitSet and = (BitSet) bits.clone();
			and.and(other);
			if (and.cardinality() == bits.cardinality()) {
				return true;
			}
		}
		return false;
	}

	public boolean isSuperSetOf(BitSet other) {
		if (other.cardinality() <= bits.cardinality()) {
			BitSet and = (BitSet) bits.clone();
			and.and(other);
			if (and.cardinality() == other.cardinality()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}

		final Clade clade = (Clade) other;

		return !((bits != null) ? !bits.equals(clade.bits) : (clade.bits != null));
	}

	@Override
	public int hashCode() {
		return (bits != null ? bits.hashCode() : 0);
	}

	@Override
	public String toString() {
		if (taxa != null) {
			// List<String> taxa = tree.getTaxonset().asStringList();
			StringBuilder b = new StringBuilder();
			b.append("{");
			for (int i = 0; i < taxa.length; i++) {
				if (bits.get(i)) {
					b.append(taxa[i]).append(",");
				}
			}
			b.deleteCharAt(b.length() - 1);
			b.append('}');
			return b.toString();
		}
		return "clade " + bits.toString();
	}
}
