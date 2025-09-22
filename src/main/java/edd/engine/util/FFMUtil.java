package edd.engine.util;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;

public final class FFMUtil {
    private FFMUtil() {}

    /** Zeroed allocation for a single struct. */
    public static MemorySegment calloc(Arena a, MemoryLayout layout) {
        var seg = a.allocate(layout);
        seg.fill((byte) 0);
        return seg;
    }

    /** Build a sequence (array) layout of n elements. */
    public static SequenceLayout seq(MemoryLayout elem, long n) {
        return MemoryLayout.sequenceLayout(n, elem);
    }

    /** Zeroed allocation for an array using a sequence layout. */
    public static MemorySegment calloc(Arena a, SequenceLayout seq) {
        var seg = a.allocate(seq);
        seg.fill((byte) 0);
        return seg;
    }

    /** Convenience: zeroed allocation for n elements of elem layout. */
    public static MemorySegment callocArray(Arena a, MemoryLayout elem, int n) {
        return calloc(a, seq(elem, n));
    }

    /** Address-of slot for T** out params. */
    public static MemorySegment outPtr(Arena a) {
        return a.allocate(ValueLayout.ADDRESS);
    }

    /** Read back T* from an out slot. */
    public static MemorySegment readPtr(MemorySegment outSlot) {
        return outSlot.get(ValueLayout.ADDRESS, 0);
    }

    /** Uint32* out slot helpers. */
    public static MemorySegment outU32(Arena a) { return a.allocate(ValueLayout.JAVA_INT); }
    public static int readU32(MemorySegment slot) { return slot.get(ValueLayout.JAVA_INT, 0); }

    /**
     * Bounds-safe element slice for sequence arrays.
     * Uses element byteSize as stride (sequence elements are contiguous).
     */
    public static MemorySegment elem(MemorySegment array, SequenceLayout seq, int index) {
        long count = seq.elementCount();
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException(index + " (size " + count + ")");
        }
        long stride = seq.elementLayout().byteSize();
        long off = stride * index;
        return array.asSlice(off, stride);
    }

    /** Simple null checks to catch mistakes early. */
    public static void requireNonNullAddr(MemorySegment p, String what) {
        if (p == null || p.equals(MemorySegment.NULL)) {
            throw new IllegalArgumentException(what + " is NULL");
        }
    }
}
