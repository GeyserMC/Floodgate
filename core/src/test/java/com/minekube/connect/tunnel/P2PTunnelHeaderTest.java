package com.minekube.connect.tunnel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class P2PTunnelHeaderTest {

    @Test
    void encodesMoxyCompatibleHeader() {
        byte[] got = P2PTunnelHeader.encode("abc");
        byte[] want = new byte[] {
                0, 20,
                '{', '"', 's', 'e', 's', 's', 'i', 'o', 'n', '_', 'i', 'd', '"', ':',
                '"', 'a', 'b', 'c', '"', '}'
        };

        assertArrayEquals(want, got);
    }

    @Test
    void escapesJsonStringCharacters() {
        byte[] got = P2PTunnelHeader.encode("a\\\"\n\t\rb");

        assertHeaderJson("{\"session_id\":\"a\\\\\\\"\\n\\t\\rb\"}", got);
    }

    @Test
    void escapesControlCharacters() {
        byte[] got = P2PTunnelHeader.encode("a\u0001b");

        assertHeaderJson("{\"session_id\":\"a\\u0001b\"}", got);
    }

    @Test
    void rejectsOversizedHeader() {
        assertThrows(IllegalArgumentException.class, () -> P2PTunnelHeader.encode(repeat('a', 0x10000)));
    }

    @Test
    void rejectsEmptySessionId() {
        assertThrows(IllegalArgumentException.class, () -> P2PTunnelHeader.encode(""));
    }

    @Test
    void rejectsNullSessionId() {
        assertThrows(IllegalArgumentException.class, () -> P2PTunnelHeader.encode(null));
    }

    private static void assertHeaderJson(String expectedJson, byte[] encoded) {
        int length = ByteBuffer.wrap(encoded, 0, 2).getShort() & 0xffff;
        byte[] json = expectedJson.getBytes(StandardCharsets.UTF_8);

        byte[] expected = ByteBuffer.allocate(2 + json.length)
                .putShort((short) json.length)
                .put(json)
                .array();
        assertArrayEquals(expected, encoded);
        if (length != json.length) {
            throw new AssertionError("header length = " + length + ", want " + json.length);
        }
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        for (int i = 0; i < count; i++) {
            chars[i] = c;
        }
        return new String(chars);
    }
}
