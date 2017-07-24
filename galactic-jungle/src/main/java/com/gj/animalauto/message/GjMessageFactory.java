package com.gj.animalauto.message;

import android.util.Log;

import com.gj.animalauto.message.internal.GjMessageConsole;
import com.gj.animalauto.message.internal.GjMessageError;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Created by liorsaar on 2015-04-21
 */

public class GjMessageFactory {
    protected final static String TAG = GjMessageFactory.class.getSimpleName();

    public static void test() {
        byte[] buf;
        String hex;
        String s;

        GjMessageText textMessage = new GjMessageText("HI");
        buf = textMessage.toByteArray();
        hex = textMessage.toHexString();
        s = textMessage.toString();

    }

    public static ByteBuffer create1() {
        ByteBuffer bb = ByteBuffer.allocate(4096);
        bb.put((byte) 0x01);
        bb.put((byte) 0x02);
        bb.put((byte) 0xFF);
        bb.put((byte) 0x04);
        bb.put((byte) 0xFF);
        bb.put((byte) 0x55);
        bb.put(new GjMessageText("1234567890").toByteArray());
        bb.put(new GjMessageText("abcdefghijklmnopqrstuvwxysABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxysABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789").toByteArray());
        bb.put(new GjMessageStatusResponse((byte) 0x1F).toByteArray());

        bb.flip(); // IMPORTANT !!!
        return bb;
    }

    public static ByteBuffer create2() {
        ByteBuffer bb = ByteBuffer.allocate(4096);
        for (int i = 0; bb.remaining() > 100; i++) {
            bb.put(new GjMessageText(i + "-abcdefghijklmnopqrstuvwxysABCDEFGHIJKLMNOPQRSTUVWXYZ").toByteArray());
        }

        bb.flip(); // IMPORTANT !!!
        return bb;
    }

    public static ByteBuffer create3() {
        ByteBuffer bb = ByteBuffer.allocate(50);
        bb.put(new GjMessageText("123456").toByteArray());

        bb.flip(); // IMPORTANT !!!
        return bb;
    }


    public static void testParser4() {
        ByteBuffer bb = create4();
        List<GjMessage> list = parseAll(bb).messages;
        for (GjMessage message : list) {
            Log.e(TAG, message.toString());
        }
        return;
    }

    public static ByteBuffer create4() {
        StringBuffer sb = new StringBuffer();
        sb.append("ff 55 aa 3f 02 04 10 40 f9 75 1e ae 07 a6 16 3a ");
        sb.append("3f 3c b7 f3 09 00 00 f8 ff 55 aa 40 01 04 10 58 ");
        sb.append("f5 75 1e 05 04 a6 16 c5 40 3c b7 91 15 00 00 96 ");
        sb.append("ff 55 aa 41 02 04 10 28 fd 75 1e ad 07 a6 16 36 ");
        sb.append("3f 3c b7 45 0a 00 00 34 ff 55 aa 42 01 04 10 40 ");
        sb.append("f9 75 1e 0c 04 a6 16 c8 40 3c b7 91 15 00 00 8e ");
        sb.append("ff 55 aa 42 02 01 01 00 44 ff 55 aa 43 02 04 10 ");
        sb.append("10 01 76 1e a6 07 a6 16 5a 3f 3c b7 d3 09 00 00 ");
        sb.append("cd ff 55 aa 44 01 04 10 28 fd 75 1e 12 04 a6 16 ");
        sb.append("cd 40 3c b7 7c 15 00 00 72 ff 55 aa 45 02 04 10 ");
        sb.append("f8 04 76 1e 9f 07 a6 16 6d 3f 3c b7 45 0a 00 00 ");
        sb.append("39 ff 55 aa 46 01 04 10 10 01 76 1e 1e 04 a6 16 ");
        sb.append("d6 40 3c b7 76 15 00 00 70 ff 55 aa 47 02 04 10 ");
        sb.append("e0 08 76 1e a1 07 a6 16 7c 3f 3c b7 7c 0a 00 00 ");
        sb.append("6f ff 55 aa 48 01 04 10 f8 04 76 1e 30 04 a6 16 ");
        sb.append("e1 40 3c b7 86 15 00 00 8a ff 55 aa 49 02 04 10 ");
        sb.append("c8 0c 76 1e a0 07 a6 16 86 3f 3c b7 99 0a 00 00 ");
        sb.append("83 ff 55 aa 4a 01 04 10 e0 08 76 1e 3f 04 a6 16 ");
        sb.append("e4 40 3c b7 7c 15 00 00 80 ff 55 aa 4b 02 04 10 ");
        sb.append("b0 10 76 1e a7 07 a6 16 a1 3f 3c b7 d2 0a 00 00 ");
        sb.append("cc ff 55 aa 4c 01 04 10 c8 0c 76 1e 4d 04 a6 16 ");
        sb.append("e6 40 3c b7 85 15 00 00 87 ff 55 aa 4c 02 01 01 ");
        sb.append("00 4e ff 55 aa 4d 02 04 10 98 14 76 1e b4 07 a6 ");
        sb.append("16 d0 3f 3c b7 3e 0b 00 00 63 ff 55 aa 4e 01 04 ");
        sb.append("10 b0 10 76 1e 5c 04 a6 16 d6 40 3c b7 56 15 00 ");
        sb.append("00 45 ff 55 aa 4f 02 04 10 80 18 76 1e bb 07 a6 ");
        sb.append("16 dc 3f 3c b7 ed 0a 00 00 12 ff 55 aa 50 01 04 ");
        sb.append("10 98 14 76 1e 6b 04 a6 16 c6 40 3c b7 5b 15 00 ");
        sb.append("00 37 ff 55 aa 51 02 04 10 68 1c 76 1e c4 07 a6 ");
        sb.append("16 fe 3f 3c b7 b6 0a 00 00 f4 ff 55 aa 52 01 04 ");
        sb.append("10 68 1c 76 1e 84 04 a6 16 b2 40 3c b7 65 15 00 ");
        sb.append("00 20 ff 55 aa 53 02 04 10 50 20 76 1e ca 07 a6 ");
        sb.append("16 14 40 3c b7 61 0a 00 00 aa ff 55 aa 54 01 04 ");
        sb.append("10 50 20 76 1e 8b 04 a6 16 ab 40 3c b7 9c 15 00 ");
        sb.append("00 45 ff 55 aa 55 02 04 10 20 28 76 1e c9 07 a6 ");
        sb.append("16 41 40 3c b7 0e 0a 00 00 5d ff 55 aa 56 01 04 ");
        sb.append("10 38 24 76 1e 98 04 a6 16 a7 40 3c b7 81 15 00 ");
        sb.append("00 21 ff 55 aa 56 02 01 01 00 58 ff 55 aa 57 02 ");
        sb.append("04 10 08 2c 76 1e c4 07 a6 16 54 40 3c b7 61 0a ");
        sb.append("00 00 ac ff 55 aa 58 01 04 10 20 28 76 1e a9 04 ");
        sb.append("a6 16 a5 40 3c b7 8c 15 00 00 29 ff 55 aa 59 02 ");
        sb.append("04 10 f0 2f 76 1e c4 07 a6 16 68 40 3c b7 44 0a ");
        sb.append("00 00 90 ff 55 aa 5a 01 04 10 08 2c 76 1e ba 04 ");
        sb.append("a6 16 a4 40 3c b7 6b 15 00 00 06 ff 55 aa 5b 02 ");
        sb.append("04 10 d8 33 76 1e c7 07 a6 16 82 40 3c b7 5f 0a ");
        sb.append("00 00 b6 ff 55 aa 5c 01 04 10 f0 2f 76 1e c6 04 ");
        sb.append("a6 16 a3 40 3c b7 86 15 00 00 19 ff 55 aa 5d 02 ");
        sb.append("04 10 c0 37 76 1e cd 07 a6 16 99 40 3c b7 eb 0a ");
        sb.append("00 00 4d ff 55 aa 5e 01 04 10 d8 33 76 1e cd 04 ");
        sb.append("a6 16 a3 40 3c b7 a1 15 00 00 29 ff 55 aa 5f 02 ");
        sb.append("04 10 a8 3b 76 1e da 07 a6 16 b1 40 3c b7 23 0b ");
        sb.append("00 00 99 ff 55 aa 60 01 04 10 c0 37 76 1e d4 04 ");
        sb.append("a6 16 9c 40 3c b7 8c 15 00 00 02 ff 55 aa 60 02 ");
        sb.append("01 01 00 62 ff 55 aa 61 02 04 10 90 3f 76 1e d9 ");
        sb.append("07 a6 16 be 40 3c b7 7f 0a 00 00 ee ff 55 aa 62 ");
        sb.append("01 04 10 a8 3b 76 1e dd 04 a6 16 8a 40 3c b7 96 ");
        sb.append("15 00 00 f1 ff 55 aa 63 02 04 10 78 43 76 1e d8 ");
        sb.append("07 a6 16 ca 40 3c b7 07 0b 00 00 70 ff 55 aa 64 ");
        sb.append("01 04 10 90 3f 76 1e dd 04 a6 16 7c 40 3c b7 8b ");
        sb.append("15 00 00 c6 ff 55 aa 65 02 04 10 60 47 76 1e da ");
        sb.append("07 a6 16 da 40 3c b7 ed 0a 00 00 55 ff 55 aa 66 ");
        sb.append("01 04 10 78 43 76 1e de 04 a6 16 70 40 3c b7 86 ");
        sb.append("15 00 00 a4 ff 55 aa 66 01 06 13 54 68 65 20 71 ");
        sb.append("75 69 63 6b 20 62 72 6f 77 6e 20 66 6f 78 91 ff ");
        sb.append("55 aa 67 02 04 10 48 4b 76 1e db 07 a6 16 e8 40 ");
        sb.append("3c b7 08 0b 00 00 6e ff 55 aa 68 01 04 10 60 47 ");
        sb.append("76 1e de 04 a6 16 66 40 3c b7 81 15 00 00 83 ff ");
        sb.append("55 aa 69 02 04 10 30 4f 76 1e db 07 a6 16 f0 40 ");
        sb.append("3c b7 98 0a 00 00 f3 ff 55 aa 6a 01 04 10 48 4b ");
        sb.append("76 1e e2 04 a6 16 5d 40 3c b7 81 15 00 00 6c ff ");
        sb.append("55 aa 6a 02 01 01 00 6c ff 55 aa 6b 02 04 10 18 ");
        sb.append("53 76 1e e0 07 a6 16 fd 40 3c b7 61 0a 00 00 bc ");
        sb.append("ff 55 aa 6c 01 04 10 30 4f 76 1e ea 04 a6 16 5e ");
        sb.append("40 3c b7 8c 15 00 00 6e ff 55 aa 6d 02 04 10 00 ");
        sb.append("57 76 1e e7 07 a6 16 10 41 3c b7 2a 0a 00 00 8e ");
        sb.append("ff 55 aa 6e 01 04 10 18 53 76 1e f0 04 a6 16 64 ");
        sb.append("40 3c b7 91 15 00 00 6d ff 55 aa 6f 02 04 10 e8 ");
        sb.append("5a 76 1e f0 07 a6 16 23 41 3c b7 26 0a 00 00 93 ");
        sb.append("ff 55 aa 70 01 04 10 00 57 76 1e f8 04 a6 16 6e ");
        sb.append("40 3c b7 9c 15 00 00 78 ff 55 aa 71 02 04 10 d0 ");
        sb.append("5e 76 1e f1 07 a6 16 31 41 3c b7 0a 0a 00 00 74 ");
        sb.append("ff 55 aa 72 01 04 10 e8 5a 76 1e 04 05 a6 16 76 ");
        sb.append("40 3c b7 71 15 00 00 4f ff 55 aa 73 02 04 10 b8 ");
        sb.append("62 76 1e ea 07 a6 16 55 41 3c b7 98 0a 00 00 0d ");
        sb.append("ff 55 aa 74 01 04 10 d0 5e 76 1e 0e 05 a6 16 80 ");
        sb.append("40 3c b7 91 15 00 00 71 ff 55 aa 74 02 01 01 00 ");
        sb.append("76 ff 55 aa 75 02 04 10 a0 66 76 1e d3 07 a6 16 ");
        sb.append("64 41 3c b7 64 0a 00 00 bf ff 55 aa 76 01 04 10 ");
        sb.append("b8 62 76 1e 1c 05 a6 16 93 40 3c b7 67 15 00 00 ");
        sb.append("56 ff 55 aa 77 02 04 10 88 6a 76 1e c2 07 a6 16 ");
        sb.append("7c 41 3c b7 23 0b 00 00 74 ff 55 aa 78 01 04 10 ");
        sb.append("88 6a 76 1e 26 05 a6 16 a8 40 3c b7 70 15 00 00 ");
        sb.append("58 ff 55 aa 79 02 04 10 70 6e 76 1e b1 07 a6 16 ");
        sb.append("9f 41 3c b7 07 0b 00 00 58 ff 55 aa 7a 01 04 10 ");
        sb.append("70 6e 76 1e 28 05 a6 16 ac 40 3c b7 86 15 00 00 62 ");
        return fromString(sb.toString());
    }

    public static ByteBuffer createBadChecksum() {
        StringBuffer sb = new StringBuffer();
        sb.append("ff 55 aa 35 00 01 01 00 35 "); //ok
        sb.append("ff 55 aa 36 00 04 12 80 30 78 1d 6a 3d 3c b7 15 06 a6 16 91 23 00 00 b2 "); // error
        sb.append("ff 55 aa 36 00 01 01 00 36 "); // ok
        sb.append("ff 55 aa 36 00 04 12 80 30 78 1d 6a 3d 3c b7 15 06 a6 16 91 23 00 00 b2 "); // error
        sb.append("ff 55 aa 37 00 01 01 00 07 "); // error
        sb.append("ff 55 aa 36 00 04 10 80 30 78 1d 6a 3d 3c b7 15 06 a6 16 91 23 00 00 b4 "); // error
        sb.append("ff 55 aa 38 00 01 01 00 38 "); //ok
        return fromString(sb.toString());
    }

    public static ByteBuffer createGps() {
        StringBuffer sb = new StringBuffer();
        sb.append("ff 55 aa 36 01 04 10 80 30 78 1d 15 06 a6 16 6a 3d 3c b7 91 23 00 00 b3 ");
        return fromString(sb.toString());
    }

    public static void testStream() {

        ByteBuffer bb = create1();

        ByteBuffer cc = ByteBuffer.allocate(400);
        cc.rewind();
        while (bb.remaining() > 0) {
            for (int i = 0; i < 7 && bb.remaining() > 0; i++) {
                byte b = bb.get();
                cc.put(b);
            }
            cc.limit(cc.position());
            cc.rewind();
            List<GjMessage> list = parseAll(cc).messages;
            cc.compact();
            for (GjMessage message : list) {
                Log.e(TAG, message.toString());
            }
        }
    }

    public static void testChecksumError() {
        ByteBuffer bb = createBadChecksum();
        List<GjMessage> list = parseAll(bb).messages;
        return;
    }

    public static void testLongString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0 ; i < 13; i++) {
            sb.append("0123456789");
        }
        sb.append("abcdefghijklmnopqrstuv");
        ByteBuffer bb = ByteBuffer.allocate(4096);
        bb.put(new GjMessageText(sb.toString()).toByteArray());
        bb.rewind(); // IMPORTANT !!!

        List<GjMessage> list = parseAll(bb).messages;
    }

    public static void testStream2() {

        ByteBuffer bb = ByteBuffer.allocate(4096);
        bb.put((byte) 0x01);
        bb.put((byte) 0x02);
        bb.put((byte) 0xFF);
        bb.put((byte) 0x04);
        bb.put((byte) 0xFF);
        bb.put((byte) 0x55);
        bb.put(new GjMessageText("123456").toByteArray());
        bb.put(new GjMessageText("abcdefghijklmnopqrstuvwxysABCDEFGHIJKLMNOPQRSTUVWXYZ").toByteArray());
        bb.put(new GjMessageStatusResponse((byte) 0x1F).toByteArray());

        bb.limit(bb.position() - 5);  // IMPORTANT !!!
        bb.rewind(); // IMPORTANT !!!

        List<GjMessage> list = parseAll(bb).messages;
        for (GjMessage message : list) {
            Log.e(TAG, message.toString());
        }

        bb.limit(bb.limit() + 5);
        list = parseAll(bb).messages;
        for (GjMessage m : list) {
            Log.e(TAG, m.toString());
        }
    }

    public static GjMessageParseResponse parseAll(ByteBuffer bb) {
        List<GjMessage> list = new ArrayList<>();
        int lastParsedByteIndex = 0;
        while (bb.remaining() > 0) {
            int savePosition = bb.position();
            try {
                GjMessage message = GjMessage.create(bb);
                lastParsedByteIndex = bb.position() - 1;
                list.add(message);
            } catch (EOFException e) {
                // rewind to pre-eof position
                bb.position(savePosition);
                list.add(new GjMessageConsole("EOF reached. Remaining " + bb.remaining()));
                Timber.w("EOF reached");
                break;
            } catch (GjMessage.ChecksumException e) {
                // output an error
                list.add(new GjMessageError(e.getMessage()));
                // skip this message, try to recover the next one
                Timber.w("ChecksumException");
                continue;
            } catch (GjMessage.PreambleNotFoundException e) {
                // bb.position(savePosition);   TODO make sure this is not needed
                Timber.w( "PreambleNotFoundException");
                list.add(new GjMessageError(e.getMessage()));
                break;
            } catch (GjMessage.ParserException e) {
                // some parameter value is off, continue
                Timber.e( "ParserException");
                list.add(new GjMessageError(e.getMessage()));
                continue;
            }
        }
        return new GjMessageParseResponse(list, bb, lastParsedByteIndex);
    }

    public static GjMessageParseResponse parseAll(byte[] bytes, int len) {
        ByteBuffer bb = ByteBuffer.allocate(len);
        bb.put(bytes, 0, len);
        bb.rewind();
        return parseAll(bb);
    }

    public static ByteBuffer fromString(String string) {
        String[] byteStrings = string.split(" ");
        ByteBuffer bb = ByteBuffer.allocate(byteStrings.length);
        for (String byteString : byteStrings) {
            byte b = Integer.decode("0x" + byteString).byteValue();
            bb.put(b);
        }
        bb.rewind();
        return bb;
    }

    public static class GjMessageParseResponse {
        public final List<GjMessage> messages;
        public final ByteBuffer rawData;
        public final int lastParsedRawDataIndex;

        public GjMessageParseResponse(List<GjMessage> messages, ByteBuffer rawData, int lastParsedRawDataIndex) {
            this.messages = messages;
            this.rawData = rawData;
            this.lastParsedRawDataIndex = lastParsedRawDataIndex;
        }
    }
}
