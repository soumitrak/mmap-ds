package sk.lang;

import org.testng.Assert;
import org.testng.annotations.Test;
import sk.mmap.Mallocator;
import sk.util.TestUtils;

import java.util.Random;

public class MStringBuilderTest {

    private MString buildMString(MStringBuilder builder, Mallocator mallocator, int length) {
        Random random = new Random();
        int limit = 'z' - '0' + 1;
        for (int i = 0; i < length; i++) {
            char c = (char) ('0' + random.nextInt(limit));
            builder.append(c);
        }
        return builder.toMString();
    }

    private void testMString(int length, int count) {
        Mallocator mallocator = new Mallocator(TestUtils.getMappedByteBufferProvider("mmap.dat"));
        MStringBuilder builder = new MStringBuilder(mallocator, 2);

        for (int i = 0; i < count; i++) {
            builder.clear();
            buildMString(builder, mallocator, length).print().delete();
        }

        builder.delete();
        System.out.println("Done testing MString.");

        mallocator.debug();
    }

    @Test
    public void testMString() {
        testMString(16, 1);
    }

    @Test
    public void testMStringCmp() {
        Mallocator mallocator = new Mallocator(TestUtils.getMappedByteBufferProvider("mmap.dat"));
        MStringBuilder builder = new MStringBuilder(mallocator, 2);

        Assert.assertEquals(builder.clear().append('a').toMString().compareTo(builder.clear().append('a').toMString()), 0);
        Assert.assertEquals(builder.clear().append('a').toMString().compareTo(builder.clear().append("b").toMString()), -1);
        Assert.assertEquals(builder.clear().append("c").toMString().compareTo(builder.clear().append('a').toMString()), 2);
        Assert.assertEquals(builder.clear().append("afdfdfdfgggfgfg").toMString().compareTo(builder.clear().append("afdfdfdfdf").toMString()), 3);
    }
}
