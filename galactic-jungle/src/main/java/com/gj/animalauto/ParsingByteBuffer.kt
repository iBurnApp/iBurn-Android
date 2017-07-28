package com.gj.animalauto

import timber.log.Timber
import java.nio.ByteBuffer

/**
 * Created by dbro on 7/27/17.
 */
public class ParsingByteBuffer(val capacity: Int) : ParsingBuffer {

    private val buffer = ByteBuffer.allocate(capacity)

    override fun appendData(data: ByteArray, offset: Int, len: Int) {
        Timber.d("Will add $len bytes to buffer. Bytes remaining ${buffer.remaining()}")
        buffer.put(data, offset, len)
        Timber.d("Added $len bytes to buffer. Bytes remaining ${buffer.remaining()}")
    }

    override fun discardEarliestBytes(count: Int) {
        Timber.d("Will discard $count bytes. Bytes remaining ${buffer.remaining()}")

        val prevBufferPos = buffer.position()

        if (prevBufferPos <= count) {
            // We're clearing all the buffered data
            buffer.clear()
        } else {

            buffer.limit(buffer.position())
            buffer.position(count)
            buffer.compact()
        }
        Timber.d("Did discard $count bytes. Bytes remaining ${buffer.remaining()}")
    }

    override fun getUndiscardedBytes(): ByteArray {
        val remainingSize = buffer.position()
        val output = ByteArray(remainingSize)

        Timber.d("Will get $remainingSize undiscarded bytes. Bytes remaining ${buffer.remaining()}")

        val bufferPos = buffer.position()
        buffer.position(0)
        buffer.limit(bufferPos)
        buffer.get(output)
        buffer.limit(capacity)
        Timber.d("Got $remainingSize undiscarded bytes. Bytes remaining ${buffer.remaining()}")
        return output
    }
}

public interface ParsingBuffer {
    fun appendData(data: ByteArray, offset: Int, len: Int)
    fun getUndiscardedBytes(): ByteArray
    fun discardEarliestBytes(count: Int)
}