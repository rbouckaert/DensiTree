package viz.ccd;

/**
 * Realization of the reimplemented, stripped-down {@link BitSet} for 65 to 128 bits.
 */
public class BitSet128 extends BitSet {
    /* Used to shift left or right for a partial word mask */
    private static final long WORD_MASK = 0xffffffffffffffffL;

    /**
     * The internal field corresponding to the serialField "bits".
     */
    private long word1, word2;

    public BitSet128() {
        super();
    }

    public BitSet128(BitSet128 other) {
        word1 = other.word1;
        word2 = other.word2;
    }

    /**
     * Sets the bit at the specified index to {@code true}.
     *
     * @param bitIndex a bit index
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.0
     */
    public void set(int bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);


        if (bitIndex < 64)
            word1 |= (1L << bitIndex); // Restores invariants
        else
            word2 |= (1L << (bitIndex - 64));
    }

    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to {@code true}.
     *
     * @param fromIndex index of the first bit to be set
     * @param toIndex   index after the last bit to be set
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                   or {@code toIndex} is negative, or {@code fromIndex} is
     *                                   larger than {@code toIndex}
     * @since 1.4
     */
    public void set(int fromIndex, int toIndex) {
        if (fromIndex < 64) {
            word1 = WORD_MASK << fromIndex;
            if (toIndex >= 64) {
                word2 = WORD_MASK >>> (-toIndex + 64);
            } else {
                word1 &= (WORD_MASK >>> -toIndex);
                word2 = 0;
            }
        } else {
            word1 = 0;
            word2 = WORD_MASK >>> (-toIndex + 64);
            word2 &= (WORD_MASK << (fromIndex - 64));
        }
    }

    /**
     * Performs a logical <b>OR</b> of this bit set with the bit set
     * argument. This bit set is modified so that a bit in it has the
     * value {@code true} if and only if it either already had the
     * value {@code true} or the corresponding bit in the bit set
     * argument has the value {@code true}.
     *
     * @param set a bit set
     */
    public void or(BitSet set) {
        if (this == set)
            return;

        word1 |= ((BitSet128) set).word1;
        word2 |= ((BitSet128) set).word2;
    }

    /**
     * Returns the index of the first bit that is set to {@code true}
     * that occurs on or after the specified starting index. If no such
     * bit exists then {@code -1} is returned.
     *
     * <p>To iterate over the {@code true} bits in a {@code BitSet},
     * use the following loop:
     *
     * <pre> {@code
     * for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
     *     // operate on index i here
     *     if (i == Integer.MAX_VALUE) {
     *         break; // or (i+1) would overflow
     *     }
     * }}</pre>
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the next set bit, or {@code -1} if there
     * is no such bit
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.4
     */
    public int nextSetBit(int fromIndex) {
        if (fromIndex >= 64) {
            long word = word2 & (WORD_MASK << (fromIndex - 64));
            if (word != 0)
                return (BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            return -1;
        }
        long word = word1 & (WORD_MASK << fromIndex);
        if (word != 0)
            return Long.numberOfTrailingZeros(word);
        word = word2;
        if (word != 0)
            return (BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
        return -1;
    }

    /**
     * Returns the index of the first bit that is set to {@code false}
     * that occurs on or after the specified starting index.
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the next clear bit
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.4
     */
    public int nextClearBit(int fromIndex) {
        if (fromIndex >= 64) {
            long word = ~word2 & (WORD_MASK << (fromIndex - 64));
            if (word != 0)
                return (BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            return -1;
        }
        long word = ~word1 & (WORD_MASK << fromIndex);
        if (word != 0)
            return Long.numberOfTrailingZeros(word);
        word = ~word2;
        if (word != 0)
            return (BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
        return -1;
    }

    /**
     * Cloning this {@code BitSet} produces a new {@code BitSet}
     * that is equal to it.
     * The clone of the bit set is another bit set that has exactly the
     * same bits set to {@code true} as this bit set.
     *
     * @return a clone of this bit set
     * @see #size()
     */
    public Object clone() {
        BitSet128 result = (BitSet128) new BitSet128();
        result.word1 = word1;
        result.word2 = word2;
        return result;
    }

    /**
     * Performs a logical <b>AND</b> of this target bit set with the
     * argument bit set. This bit set is modified so that each bit in it
     * has the value {@code true} if and only if it both initially
     * had the value {@code true} and the corresponding bit in the
     * bit set argument also had the value {@code true}.
     *
     * @param set a bit set
     */
    public void and(BitSet set) {
        word1 &= ((BitSet128) set).word1;
        word2 &= ((BitSet128) set).word2;
    }

    /**
     * Clears all of the bits in this {@code BitSet} whose corresponding
     * bit is set in the specified {@code BitSet}.
     *
     * @param set the {@code BitSet} with which to mask this
     *            {@code BitSet}
     * @since 1.2
     */
    public void andNot(BitSet set) {
        // Perform logical (a & !b) on words in common
        word1 &= ~((BitSet128) set).word1;
        word2 &= ~((BitSet128) set).word2;

    }

    /**
     * Performs a logical <b>XOR</b> of this bit set with the bit set
     * argument. This bit set is modified so that a bit in it has the
     * value {@code true} if and only if one of the following
     * statements holds:
     * <ul>
     * <li>The bit initially has the value {@code true}, and the
     *     corresponding bit in the argument has the value {@code false}.
     * <li>The bit initially has the value {@code false}, and the
     *     corresponding bit in the argument has the value {@code true}.
     * </ul>
     *
     * @param set a bit set
     */
    public void xor(BitSet set) {
        word1 ^= ((BitSet128) set).word1;
        word2 ^= ((BitSet128) set).word2;
    }

    /**
     * Returns true if the specified {@code BitSet} has any bits set to
     * {@code true} that are also set to {@code true} in this {@code BitSet}.
     *
     * @param set {@code BitSet} to intersect with
     * @return boolean indicating whether this {@code BitSet} intersects
     * the specified {@code BitSet}
     * @since 1.4
     */
    public boolean intersects(BitSet set) {
        if ((word1 & ((BitSet128) set).word1) != 0)
            return true;
        if ((word2 & ((BitSet128) set).word2) != 0)
            return true;
        return false;
    }

    /**
     * Returns true if this {@code BitSet} contains no bits that are set
     * to {@code true}.
     *
     * @return boolean indicating whether this {@code BitSet} is empty
     * @since 1.4
     */
    public boolean isEmpty() {
        return word1 == 0 && word2 == 0;
    }

    /**
     * Returns the number of bits set to {@code true} in this {@code BitSet}.
     *
     * @return the number of bits set to {@code true} in this {@code BitSet}
     * @since 1.4
     */
    public int cardinality() {
        return Long.bitCount(word1) + Long.bitCount(word2);
    }

    /**
     * Returns the number of bits of space actually in use by this
     * {@code BitSet} to represent bit values.
     * The maximum element in the set is the size - 1st element.
     *
     * @return the number of bits currently in this bit set
     */
    public int size() {
        return 2 * BITS_PER_WORD;
    }

    /**
     * Returns the "logical size" of this {@code BitSet}: the index of
     * the highest set bit in the {@code BitSet} plus one. Returns zero
     * if the {@code BitSet} contains no set bits.
     *
     * @return the logical size of this {@code BitSet}
     * @since 1.2
     */
    public int length() {
        if (word2 == 0) {
            if (word1 == 0) {
                return 0;
            } else {
                return BITS_PER_WORD - Long.numberOfLeadingZeros(word1);
            }
        } else {
            return BITS_PER_WORD * 2 - Long.numberOfLeadingZeros(word2);
        }
    }

    /**
     * Sets all of the bits in this BitSet to {@code false}.
     *
     * @since 1.4
     */
    public void clear() {
        word1 = 0;
        word2 = 0;
    }

    @Override
    public int hashCode() {
        long h = 1234;
        h ^= word1 + word2 * 2;
        return (int) ((h >> 32) ^ h);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BitSet128))
            return false;
        if (this == obj)
            return true;
        return ((BitSet128)obj).word1 == word1 && ((BitSet128)obj).word2 == word2;
    }

    @Override
    public String toString() {

        final int MAX_INITIAL_CAPACITY = Integer.MAX_VALUE - 8;
        int numBits = cardinality();
        // Avoid overflow in the case of a humongous numBits
        int initialCapacity = (numBits <= (MAX_INITIAL_CAPACITY - 2) / 6) ?
                6 * numBits + 2 : MAX_INITIAL_CAPACITY;
        StringBuilder b = new StringBuilder(initialCapacity);
        b.append('{');

        int i = nextSetBit(0);
        if (i != -1) {
            b.append(i);
            while (true) {
                if (++i < 0) break;
                if ((i = nextSetBit(i)) < 0) break;
                int endOfRun = nextClearBit(i);
                do {
                    b.append(", ").append(i);
                }
                while (++i != endOfRun);
            }
        }

        b.append('}');
        return b.toString();
    }

    @Override
    public boolean contains(BitSet other) {
        BitSet128 otherset = ((BitSet128) other);
        return ((word1 & otherset.word1) == otherset.word1)
                && ((word2 & otherset.word2) == otherset.word2);
    }

    @Override
    public boolean disjoint(BitSet other) {
        BitSet128 otherset = ((BitSet128) other);
        return ((word1 & otherset.word1) == 0)
                && ((word2 & otherset.word2) == 0);
    }

    @Override
    public int lastSetBit() {
    	if (word2 != 0)
    		return 2 * BITS_PER_WORD - Long.numberOfLeadingZeros(word2) - 1;
        return BITS_PER_WORD - Long.numberOfLeadingZeros(word1) - 1;
    }
}
