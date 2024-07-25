package viz.ccd;

/**
 * Stripped down version of {@link java.util.BitSet} adapted for speedup;
 * safety checks on sizes removed and special child classes for small bitsets used.
 *
 * <p>
 * For up to 256 bits, child classes are used; for more bits the general framework with an array of longs is used.
 *
 * @author Remco Bouckaert, Jonathan Klawitter
 */
public class BitSet implements Cloneable {

    /**
     * The internal field corresponding to the serialField "bits".
     */
    private long[] words;

    protected BitSet() {
    }

    /** not to be used for production code **/
    @Deprecated
    public static BitSet newBitSetForTesting(int nbits) {
        return new BitSet(nbits);
    }

    public static BitSet newBitSet(int nbits) {
        // if (true) return new BitSet(nbits);
        if (nbits <= 64) {
            return new BitSet64();
        } else if (nbits <= 128) {
            return new BitSet128();
        } else if (nbits <= 192) {
            return new BitSet192();
        } else if (nbits <= 256) {
            return new BitSet256();
        } else {
            return new BitSet(nbits);
        }
    }

    public static BitSet newBitSet(BitSet other) {
        if (other instanceof BitSet128 set) {
            return new BitSet128(set);
        }
        if (other instanceof BitSet192 set) {
            return new BitSet192(set);
        }
        if (other instanceof BitSet64 set) {
            return new BitSet64(set);
        }
        if (other instanceof BitSet256 set) {
            return new BitSet256(set);
        }
        BitSet b = new BitSet(other.length());
        b.or(other);
        return b;
    }

    private BitSet(int nbits) {
        // nbits can't be negative; size 0 is OK
        if (nbits < 0)
            throw new NegativeArraySizeException("nbits < 0: " + nbits);

        words = new long[wordIndex(nbits - 1) + 1];
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

        int wordIndex = wordIndex(bitIndex);

        words[wordIndex] |= (1L << bitIndex); // Restores invariants
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
        if (fromIndex == toIndex)
            return;

        // Increase capacity if necessary
        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex = wordIndex(toIndex - 1);

        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            words[startWordIndex] |= (firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            words[startWordIndex] |= firstWordMask;

            // Handle intermediate words, if any
            for (int i = startWordIndex + 1; i < endWordIndex; i++)
                words[i] = WORD_MASK;

            // Handle last word (restores invariants)
            words[endWordIndex] |= lastWordMask;
        }
    }

    /**
     * Returns the value of the bit with the specified index. The value
     * is {@code true} if the bit with the index {@code bitIndex}
     * is currently set in this {@code BitSet}; otherwise, the result
     * is {@code false}.
     *
     * @param bitIndex the bit index
     * @return the value of the bit with the specified index
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    public boolean get(int bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = wordIndex(bitIndex);
        return ((words[wordIndex] & (1L << bitIndex)) != 0);
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

        // Perform logical OR on words in common
        for (int i = 0; i < words.length; i++)
            words[i] |= set.words[i];
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
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        int u = wordIndex(fromIndex);
        if (u >= words.length)
            return -1;

        long word = words[u] & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == words.length)
                return -1;
            word = words[u];
        }
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
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        int u = wordIndex(fromIndex);
        if (u >= words.length)
            return fromIndex;

        long word = ~words[u] & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == words.length)
                return words.length * BITS_PER_WORD;
            word = ~words[u];
        }
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
        BitSet result = new BitSet(this.size());
        for (int i = 0; i < this.words.length; i++) {
            result.words[i] = this.words[i];
        }
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
        if (this == set)
            return;

        // Perform logical AND on words in common
        for (int i = 0; i < words.length; i++)
            words[i] &= set.words[i];
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
        for (int i = words.length - 1; i >= 0; i--)
            words[i] &= ~set.words[i];

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
        // Perform logical XOR on words in common
        for (int i = 0; i < words.length; i++)
            words[i] ^= set.words[i];
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
        for (int i = words.length - 1; i >= 0; i--)
            if ((words[i] & set.words[i]) != 0)
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
        for (long word : words) {
            if (word != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the number of bits set to {@code true} in this {@code BitSet}.
     *
     * @return the number of bits set to {@code true} in this {@code BitSet}
     * @since 1.4
     */
    public int cardinality() {
        int sum = 0;
        for (int i = 0; i < words.length; i++)
            sum += Long.bitCount(words[i]);
        return sum;
    }

    /**
     * Returns the number of bits of space actually in use by this
     * {@code BitSet} to represent bit values.
     * The maximum element in the set is the size - 1st element.
     *
     * @return the number of bits currently in this bit set
     */
    public int size() {
        return words.length * BITS_PER_WORD;
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
        if (words.length == 0)
            return 0;

        return BITS_PER_WORD * (words.length - 1) +
                (BITS_PER_WORD - Long.numberOfLeadingZeros(words[words.length - 1]));
    }

    /**
     * Sets all of the bits in this BitSet to {@code false}.
     *
     * @since 1.4
     */
    public void clear() {
        for (int i = 0; i < words.length; i++) {
            words[i] = 0;
        }
    }

    /*
     * BitSets are packed into arrays of "words."  Currently, a word is
     * a long, which consists of 64 bits, requiring 6 address bits.
     * The choice of word size is determined purely by performance concerns.
     */
    protected static final int ADDRESS_BITS_PER_WORD = 6;
    protected static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    protected static final int BIT_INDEX_MASK = BITS_PER_WORD - 1;

    /* Used to shift left or right for a partial word mask */
    protected static final long WORD_MASK = 0xffffffffffffffffL;

    /**
     * Given a bit index, return word index containing it.
     */
    private static int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }

    @Override
    public int hashCode() {
        long h = 1234;
        for (int i = words.length; --i >= 0; )
            h ^= words[i] * (i + 1);

        return (int) ((h >> 32) ^ h);
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

    /**
     * Returns whether this BitSet contains the other
     *
     * @param other bitset (assumed to have same length)
     * @return whether this BitSet contains the other
     */
    public boolean contains(BitSet other) {
        BitSet copy = (BitSet) this.clone();
        copy.and(other);
        return copy.equals(other);
    }

    /**
     * Returns whether this BitSet contains the other
     *
     * @param other bitset (assumed to have same length)
     * @return whether this BitSet contains the other
     */
    public boolean disjoint(BitSet other) {
        BitSet copy = (BitSet) this.clone();
        copy.and(other);
        return copy.isEmpty();
    }

    /**
     * Compares this object against the specified object.
     * The result is true if and only if the argument is not null and is a BitSet object
     * that has exactly the same set of bits set to true as this bit set.
     * That is, for every nonnegative int index k,
     * ((BitSet)obj).get(k) == this.get(k)
     * must be true. The current sizes of the two bit sets are not compared.
     *
     * @param obj object compared to
     * @return true if the objects are the same; false otherwise
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof BitSet set))
            return false;
        if (this == obj)
            return true;

        if (words.length != set.words.length)
            return false;

        // Check words in use by both BitSets
        for (int i = 0; i < words.length; i++)
            if (words[i] != set.words[i])
                return false;

        return true;
    }
}
