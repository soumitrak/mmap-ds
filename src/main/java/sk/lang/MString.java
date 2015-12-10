package sk.lang;

import sk.mmap.AllocObject;
import sk.mmap.Allocator;
import sk.mmap.MObject;

import java.nio.CharBuffer;

public class MString implements AllocObject, MObject, CharSequence, Comparable<MString> {
    private final Allocator _allocator;
    private final long _allocHandle;
    private final CharBuffer _buffer;

    public MString(Allocator allocator, long handle) {
        _allocator = allocator;
        _allocHandle = handle;
        _buffer = allocator.getCharBuffer(_allocHandle).asReadOnlyBuffer();
    }

    public void delete() {
        _allocator.free(_allocHandle);
    }

    public int length() {
        return _buffer.length();
    }

    public char charAt(int index) {
        return _buffer.charAt(index);
    }

    // Copied openjdk code.
    public int compareTo(MString anotherString) {
        int len1 = length();
        int len2 = anotherString.length();
        int n = Math.min(len1, len2);
        char v1[] = _buffer.array();
        char v2[] = anotherString._buffer.array();
        int i = 0;
        int j = 0;

        if (i == j) {
            int k = i;
            int lim = n + i;
            while (k < lim) {
                char c1 = v1[k];
                char c2 = v2[k];
                if (c1 != c2) {
                    return c1 - c2;
                }
                k++;
            }
        } else {
            while (n-- != 0) {
                char c1 = v1[i++];
                char c2 = v2[j++];
                if (c1 != c2) {
                    return c1 - c2;
                }
            }
        }
        return len1 - len2;
    }

    public CharSequence subSequence(int start, int end) {
        return this.new SubSequence(start, end);
    }

    public class SubSequence implements CharSequence, AllocObject {

        private final int _start;
        private final int _end;

        SubSequence(int start, int end) {
            _start = start;
            _end = end;
        }

        public int length() {
            return _end - _start;
        }

        public char charAt(int index) {
            return MString.this.charAt(_start + index);
        }

        public CharSequence subSequence(int start, int end) {
            return MString.this.new SubSequence(_start + start, _end - end);
        }

        public void delete() {
            // No OP.
            // Buffer is owned by parent class.
        }
    }

    public MString print() {
        for (int i = 0; i < _buffer.length(); i++) {
            System.out.print(_buffer.get(i));
        }
        System.out.println();

        return this;
    }
}
