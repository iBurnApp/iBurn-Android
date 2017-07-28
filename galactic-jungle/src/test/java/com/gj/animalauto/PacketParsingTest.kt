package com.gj.animalauto

import com.gj.animalauto.message.GjMessageFactory
import com.gj.animalauto.message.GjMessageGps
import com.gj.animalauto.message.GjMessageStatusResponse
import com.gj.animalauto.message.internal.GjMessageConsole
import com.gj.animalauto.message.internal.GjMessageError
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Created by dbro on 7/28/17.
 */
class PacketParsingTest {

    @Before
    fun setup() {
        Timber.plant(TestTimberTree(startTime = System.currentTimeMillis()))
    }

    @Test
    @Throws(Exception::class)
    fun testBufferHelper() {
        var pktNum = 0
        val vehicleId = 1

        val statusData = GjMessageStatusResponse.createData(false, false, false, false, false)
        val packet1 = GjMessageStatusResponse(pktNum++.toByte(), vehicleId.toByte(), byteArrayOf(statusData))
        val packet1Buffer = packet1.toByteArray()

        val gpsData = GjMessageGps.createData(
                System.currentTimeMillis().toInt(),
                42.0,
                -122.4,
                30.0)
        val packet2 = GjMessageGps(pktNum++.toByte(), vehicleId.toByte(), gpsData.array())
        val packet2Buffer = packet2.toByteArray()

        val parsingBuffer = ParsingByteBuffer(packet1Buffer.size + packet2Buffer.size)

        // Add all but one-byte of packet1, thte parser should return no results and should not advance index
        parsingBuffer.appendData(packet1Buffer, 0, packet1Buffer.size - 1)

        val partialFirstPacket = parsingBuffer.getUndiscardedBytes()
        val noMsgResp = GjMessageFactory.parseAll(partialFirstPacket, partialFirstPacket.size)

        assertEquals(noMsgResp.lastParsedRawDataIndex, 0)
        assertEquals(noMsgResp.messages.size, 1) // One EOF message
        assertTrue(noMsgResp.messages.first() is GjMessageConsole) // The EOF message

        parsingBuffer.appendData(packet1Buffer, packet1Buffer.size - 1, 1)

        // Now all of packet1 should be in the hole
        val undiscardedFirstPacket = parsingBuffer.getUndiscardedBytes()
        assertArrayEquals(undiscardedFirstPacket, packet1Buffer)

        val oneMsgResp = GjMessageFactory.parseAll(undiscardedFirstPacket, undiscardedFirstPacket.size)

        assertEquals(oneMsgResp.lastParsedRawDataIndex, packet1Buffer.size - 1)
        assertEquals(oneMsgResp.messages.size, 1) // One status message
        assertTrue(oneMsgResp.messages.first() is GjMessageStatusResponse) // The status message

        // Add part of packet2

        parsingBuffer.appendData(packet2Buffer, 0, 2)
        val undiscardedFirstAndPartOfSecondPacket = parsingBuffer.getUndiscardedBytes()
        val twoMsgResp = GjMessageFactory.parseAll(undiscardedFirstAndPartOfSecondPacket, undiscardedFirstAndPartOfSecondPacket.size)

        assertEquals(twoMsgResp.lastParsedRawDataIndex, packet1Buffer.size - 1)
        assertEquals(twoMsgResp.messages.size, 2) // One status message, One error
        assertTrue(twoMsgResp.messages.first() is GjMessageStatusResponse) // The status message
        assertTrue(twoMsgResp.messages[1] is GjMessageError) // Preamble not found

        // Remove packet 1
        parsingBuffer.discardEarliestBytes(twoMsgResp.lastParsedRawDataIndex + 1)
        val firstTwoBytesPacket2 = parsingBuffer.getUndiscardedBytes()

        assertArrayEquals(firstTwoBytesPacket2, packet2Buffer.sliceArray(IntRange(0, 1)))

        // Add rest of packet 2
        parsingBuffer.appendData(packet2Buffer, 2, packet2Buffer.size - 2)

        val undiscardedPkt2 = parsingBuffer.getUndiscardedBytes()
        val oneMsg2 = GjMessageFactory.parseAll(undiscardedPkt2, undiscardedPkt2.size)

        assertEquals(oneMsg2.lastParsedRawDataIndex, packet2Buffer.size - 1)
        assertEquals(oneMsg2.messages.size, 1) // One gps message
        assertTrue(oneMsg2.messages.first() is GjMessageGps) // The gps message
    }
}