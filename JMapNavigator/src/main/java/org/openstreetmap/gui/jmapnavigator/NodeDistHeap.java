package org.openstreetmap.gui.jmapnavigator;



/**
 * Min heap for map nodes
 *
 * @author Jonas Grunert
 *
 */
@SuppressWarnings("javadoc")
public class NodeDistHeap {

	private final int maxCapacity;
	private final float[] valuesArray;
	// Combined gridIndex(63-32)+nodeIndex(31-0)
	private final long[] nodeGridIndexArray;
	private int size;
	private int sizeUsageMax;

	/**
	 * Initializes heap
	 */
	public NodeDistHeap(int maxCapacity) {
		this.maxCapacity = maxCapacity;
		valuesArray = new float[maxCapacity + 1];
		nodeGridIndexArray = new long[maxCapacity + 1];
		size = 0;
		sizeUsageMax = 0;
	}

	/**
	 * Resets and fills with nodes
	 */
	//    public void resetFill(int nodeCount) {
	//        System.out.println("Start reset NodeDistHeap");
	//        for(int i = 1; i <= nodeCount; i++) {
	//            indexArray[i] = i-1;
	//            nodeHeapIndices[i-1] = i;
	//        }
	//        Arrays.fill(valuesArray, Float.MAX_VALUE);
	//        size = nodeCount;
	//        sizeUsageMax = 0;
	//        System.out.println("Finished reset NodeDistHeap");
	//    }

	public void resetEmpty() {
		size = 0;
		sizeUsageMax = 0;
	}

	public void add(long nodeGridIndex, float value) {
		if (size >= maxCapacity) {
			throw new IllegalStateException("Heap capacity exceeded");
		}
		//        if(nodeGridHeapIndices.containsKey(nodeGridIndex)) {
		//            throw new IllegalStateException("Cant add same node twice");
		//        }

		// place element into heap at bottom
		size++;
		int indexInHeap = size;
		valuesArray[indexInHeap] = value;
		nodeGridIndexArray[indexInHeap] = nodeGridIndex;

		if (size > sizeUsageMax) {
			sizeUsageMax = size;
		}

		bubbleUp(this.size);
	}

	/**
	 * Decreases key if new key smaller than existing key Slow linear time complexity (n=heapsize) but isnt called very often.
	 */
	public boolean decreaseKeyIfSmaller(long nodeGridIndex, float newKey) {
		int heapIndex = findNode(nodeGridIndex);
		if (newKey < valuesArray[heapIndex]) {
			valuesArray[heapIndex] = newKey;
			bubbleUp(heapIndex);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Changes key, assumes that newKey<oldKey
	 */
	public void decreaseKey(int heapIndex, int newKey) {
		valuesArray[heapIndex] = newKey;
		bubbleUp(heapIndex);
	}


	/**
	 * Tries to find a node with given index
	 *
	 * @param nodeId
	 *            GridNode ID of node
	 * @return Index in heap or -1 if not found
	 */
	public int findNode(long nodeGridIndex) {
		for (int i = 1; i < size + 1; i++) {
			if (nodeGridIndexArray[i] == nodeGridIndex) {
				return i;
			}
		}
		return -1;
	}


	public boolean isEmpty() {
		return size == 0;
	}


	public float peekNodeValue() {
		if (this.isEmpty()) {
			throw new IllegalStateException();
		}

		return valuesArray[1];
	}


	public long peekNodeGridIndex() {
		//        if (this.isEmpty()) {
		//            throw new IllegalStateException();
		//        }

		return nodeGridIndexArray[1];
	}



	public long removeFirst() {
		assert !this.isEmpty();
		//        if (this.isEmpty()) {
		//          throw new IllegalStateException();
		//      }

		long nodeGridIndex = nodeGridIndexArray[1];

		// get rid of the last leaf/decrement
		valuesArray[1] = valuesArray[size];
		valuesArray[size] = -1;
		nodeGridIndexArray[1] = nodeGridIndexArray[size];
		//nodeGridIndexArray[size] = -1;
		size--;

		bubbleDown();

		return nodeGridIndex;
	}


	//    static long sum;
	//    static long invocs;

	protected void bubbleDown() {
		//        long beg = System.nanoTime() ;
		//        invocs++;

		int i = 1;

		// bubble down
		while (hasLeftChild(i)) {
			// which of my children is smaller?
			int smallerChild = leftIndex(i);

			// bubble with the smaller child, if I have a smaller child
			if (hasRightChild(i) && valuesArray[leftIndex(i)] > valuesArray[rightIndex(i)]) {
				smallerChild = rightIndex(i);
			}

			if (valuesArray[i] > valuesArray[smallerChild]) {
				swap(i, smallerChild);
			}
			else {
				// otherwise, get outta here!
				break;
			}

			// make sure to update loop counter/index of where last el is put
			i = smallerChild;
		}

		//        sum += System.nanoTime() - beg;
		//        if(sum > 300000000) {
		//            System.out.println("HEAP: " + (double)sum / invocs);
		//            throw new RuntimeException();
		//        }
	}


	protected void bubbleUp(int i) {
		while (hasParent(i) && (parent(i) > valuesArray[i])) {
			// parent/child are out of order; swap them
			swap(i, parentIndex(i));
			i = parentIndex(i);
		}
	}


	protected boolean hasParent(int i) {
		return i > 1;
	}


	protected int leftIndex(int i) {
		return i * 2;
	}


	protected int rightIndex(int i) {
		return i * 2 + 1;
	}


	protected boolean hasLeftChild(int i) {
		return leftIndex(i) <= size;
	}


	protected boolean hasRightChild(int i) {
		return rightIndex(i) <= size;
	}


	protected float parent(int i) {
		return valuesArray[parentIndex(i)];
	}


	protected int parentIndex(int i) {
		return i / 2;
	}

	public int getSize() {
		return size;
	}

	public int getSizeUsageMax() {
		return sizeUsageMax;
	}



	protected void swap(int index1, int index2) {
		float tmp1 = valuesArray[index1];
		valuesArray[index1] = valuesArray[index2];
		valuesArray[index2] = tmp1;

		long tmp2 = nodeGridIndexArray[index1];
		nodeGridIndexArray[index1] = nodeGridIndexArray[index2];
		nodeGridIndexArray[index2] = tmp2;
	}
}