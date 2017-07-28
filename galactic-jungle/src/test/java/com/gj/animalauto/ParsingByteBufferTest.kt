package com.gj.animalauto

import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import timber.log.Timber


/**
 * Example local unit test, which will execute on the development machine (host).

 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ParsingByteBufferTest {

    @Before
    fun setup() {
        Timber.plant(TestTimberTree(startTime = System.currentTimeMillis()))
    }

    @Test
    fun testParsingByteBuffer() {

        val data = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05)

        val bufferHelper = ParsingByteBuffer(data.size)

        bufferHelper.appendData(data, 0, 2)

        val twoBytes = bufferHelper.getUndiscardedBytes()

        assertArrayEquals(twoBytes, byteArrayOf(0x00, 0x01))

        bufferHelper.discardEarliestBytes(1)

        val oneByte = bufferHelper.getUndiscardedBytes()

        assertArrayEquals(oneByte, byteArrayOf(0x01))

        bufferHelper.appendData(data, 2, 4)

        val fiveBytes = bufferHelper.getUndiscardedBytes()

        assertArrayEquals(fiveBytes, byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05))

        bufferHelper.appendData(data, 0, 1)

        val sixBytes = bufferHelper.getUndiscardedBytes()

        assertArrayEquals(sixBytes, byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x00))

    }
}