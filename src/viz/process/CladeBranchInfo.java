package viz.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CladeBranchInfo {
	int count = 0;
	int totalNrOfTrees = 0;
	//int [] overlapcount;
	float [] cumscore;
	List<Float> height= new ArrayList<Float>();
	
	private float score, oldLength = -1;
	private float dScore, dOldLength = -1;

	void initialise(List<Double> cladeHeightSetBottom, List<Double> cladeHeightSetTop, int totalNrOfTrees) {
		this.totalNrOfTrees = totalNrOfTrees;
		for (int i = 0; i < cladeHeightSetBottom.size(); i++) {
			height.add((float)(cladeHeightSetBottom.get(i)-cladeHeightSetTop.get(i)));
		}
		Collections.sort(height);
		
		count = height.size();
		float [] revcumscore = new float[count];
		cumscore = new float[count];
		cumscore[0] = 0;
		for (int i = 1; i < count; i++) {
			cumscore[i] = cumscore[i-1] + (height.get(i) - height.get(i-1)) * i;
		}
		revcumscore[count-1] = 0;
		for (int i = count - 2; i >= 0; i--) {
			revcumscore[i] = revcumscore[i+1] + (height.get(i+1) - height.get(i)) * (count - i - 1);
		}
		for (int i = 0; i < count; i++) {
			cumscore[i] += revcumscore[i];
		}
		
	}
	
//	float score(float bottom, float top) {
//		float score = score2(bottom, top);
//		if (score < 0) {
//			int h = 3;
//			h++;
//			score = score2(bottom, top);
//		}
//		return score;
//	}
	float score(float bottom, float top) {
		if (count == 0) {
			return 0;
		}
		float length = top - bottom;
		if (length == oldLength) {
			return score;
		}
		oldLength = length;
		if (length < -1e-8) {
			return Float.POSITIVE_INFINITY;
		}
		// contribution due to trees not containing this clade
		score = length * (totalNrOfTrees - count);
		int i = Collections.binarySearch(height, length);
		if (i >= 0) {
			score += cumscore[i]; 
			return score;
		}
		i = -1-i;
		if (i >= count) { 
			// length is larger than largest branch length
			score += cumscore[count - 1];
			score += (length - height.get(count - 1)) * count;
			return score;
		}
		if (i == 0) {
			score += cumscore[0];
			score += (height.get(0) - length) * count;
			return score;
		}
		score += cumscore[i-1];
		score += (length - height.get(i-1))/(height.get(i) - height.get(i-1)) * (cumscore[i] - cumscore[i-1]);
		return score;
	}

	float dScore(float bottom, float top) {
		float length = top - bottom;
		if (length == dOldLength) {
			return dScore;
		}
		dOldLength = length;
		int i = Collections.binarySearch(height, length);
		if (i < 0) {
			i = -1-i;
		}		
		if (i >= count) {
			dScore = count;
			return dScore;
		}
		if (i == 0) {
			dScore = -count;
			return dScore;
		}
		dScore += (cumscore[i] - cumscore[i-1])/(height.get(i) - height.get(i-1));
		return dScore;
	}
	
	public float getMaxLength() {
		return height.get(count - 1);
	}
	
//	void initialise(List<Double> cladeHeightSetBottom, List<Double> cladeHeightSetTop) {
//		for (int i = 0; i < cladeHeightSetBottom.size(); i++) {
//			insertBranch((float)(double)cladeHeightSetTop.get(i), (float)(double)cladeHeightSetBottom.get(i));
//		}
//		overlapcount = new int[height.size()];
//		cumscore = new float[height.size()];
//		overlapcount[0] = 1;
//		cumscore[0] = 0;
//		for (int i = 1; i < overlapcount.length; i++) {
//			if (height.get(i) >= 0) {
//				cumscore[i] = cumscore[i-1] + (height.get(i) - height.get(i-1)) * overlapcount[i-1];
//				overlapcount[i] = overlapcount[i-1] + 1;
//			} else {
//				height.set(i, -height.get(i));
//				cumscore[i] = cumscore[i-1] + (height.get(i) - height.get(i-1)) * overlapcount[i-1];
//				overlapcount[i] = overlapcount[i-1] - 1;
//			}
//		}
//	}
//
//	private void insertBranch(float bottom, float top) {
//		if (top <= bottom) {
//			// ignore zero length branches, since their 
//			// contribution is constant and does not change 
//			// when the summary tree changes.
//			return;
//		}
//		
//		int iBottom = Collections.binarySearch(height, bottom);
//		if (iBottom < 0) {
//			iBottom = -1-iBottom;
//		}
//		height.add(iBottom, Math.abs(bottom));
//		int iTop = Collections.binarySearch(height, top, floatComparator);
//		if (iTop < 0) {
//			iTop = -1-iTop;
//		}
//		if (iTop <= iBottom) {
//			iTop = iBottom+1;
//		}
//		
//		height.add(iTop, -top);
//		count++;
//	}
//	
//	float score(float bottom, float top) {
//		float score = score2(bottom, top);
//		if (score < 0) {
//			int h = 3;
//			h++;
//			score = score2(bottom, top);
//		}
//		return score;
//	}
//	float score2(float bottom, float top) {
//		if (height.size() == 0) {
//			return 0;
//		}
//		
//		int iBottom = Collections.binarySearch(height, bottom);
//		if (iBottom < 0) {
//			iBottom = -1-iBottom;
//		}
//		int iTop = Collections.binarySearch(height, top, floatComparator);
//		if (iTop < 0) {
//			iTop = -1-iTop;
//		}
//		
//		int max = height.size()-1;
//		if (iBottom > 0) {
//			if (iBottom > max) {
//				// branch outside region
//				float score = cumscore[max] + (top-bottom) * count; 
//				return score;
//			}
//			float score = cumscore[iBottom-1];
//			score += (bottom - height.get(iBottom-1)) * overlapcount[iBottom-1];  
//			if (iTop <= max) {
//				score += (top - bottom) * count;
//				score -= (height.get(iBottom) - bottom) * overlapcount[iBottom-1];
//				score -= cumscore[iTop-1] - cumscore[iBottom];
//				score -= (top - height.get(iTop-1)) * overlapcount[iTop-1];
//				score += (height.get(iTop) - top) * overlapcount[iTop-1];
//				score += cumscore[max] - cumscore[iTop];
//				return score;
//			} else { // iTop >= max
//				score += (height.get(max) - bottom) * count;
//				score -= (height.get(iBottom) - bottom) * overlapcount[iBottom];
//				score -= cumscore[max] - cumscore[iBottom];
//				score += (top - height.get(max)) * count;
//				return score;
//			}
//		} else { // iBottom == 0
//			if (iTop == 0) {
//				// branch outside region
//				float score = cumscore[max] + (top-bottom) * count;					
//				return score;
//			}
//			if (iTop <= max) {
//				float score = (height.get(0) - bottom) * count;
//				score += (top - height.get(0)) * count;
//				score -= cumscore[iTop-1] - cumscore[0];
//				score -= (top - height.get(iTop-1)) * overlapcount[iTop-1];
//				score += (height.get(iTop) - top) * overlapcount[iTop-1];
//				score += cumscore[max] - cumscore[iTop];
//				return score;
//			} else { // iTop >= max, branch overlaps complete interval
//				float score = (top - bottom) * count - cumscore[max];
//				return score;
//			}
//		}
//	}
}
