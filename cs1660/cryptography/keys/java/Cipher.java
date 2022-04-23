import java.lang.Integer;

public class Cipher {
    private static int rounds = 64;
    private static int delta = 0x9E3779B9;

    public static ByteArrayWrapper encrypt(int k, ByteArrayWrapper mw) {
        byte[] m = mw.getByteArray();
        k &= (1 << 24) - 1;
        int v0 = m[3] & 0xFF |
                (m[2] & 0xFF) << 8 |
                (m[1] & 0xFF) << 16 |
                (m[0] & 0xFF) << 24;
        int v1 = m[7] & 0xFF |
                (m[6] & 0xFF) << 8 |
                (m[5] & 0xFF) << 16 |
                (m[4] & 0xFF) << 24;
        int sum = 0;
        int[] key = {k, k, k, k};
        for (int i = 0; i < rounds; i++) {
            int v1shift5 = Integer.rotateRight((v1 >> 5) << 5, 5);
            v0 += (((v1 << 4) ^ v1shift5) + v1) ^ (sum + key[sum&3]);
            sum += delta;
            int v0shift5 = Integer.rotateRight((v0 >> 5) << 5, 5);
            int sumShift11 = Integer.rotateRight((sum >> 11) << 11, 11);
            v1 += (((v0 << 4) ^ v0shift5) + v0) ^ (sum + key[sumShift11&3]);
        }
        byte[] ret = {
            (byte)(v0 >> 24),
            (byte)(v0 >> 16),
            (byte)(v0 >> 8),
            (byte)v0,
            (byte)(v1 >> 24),
            (byte)(v1 >> 16),
            (byte)(v1 >> 8),
            (byte)v1,
        };
        return new ByteArrayWrapper(ret);
    }

    public static ByteArrayWrapper decrypt(int k, ByteArrayWrapper cw) {
        byte[] c = cw.getByteArray();
        k &= (1 << 24) - 1;
        int v0 = c[3] & 0xFF |
                (c[2] & 0xFF) << 8 |
                (c[1] & 0xFF) << 16 |
                (c[0] & 0xFF) << 24;
        int v1 = c[7] & 0xFF |
                (c[6] & 0xFF) << 8 |
                (c[5] & 0xFF) << 16 |
                (c[4] & 0xFF) << 24;
        int sum = (int)((long)delta * (long)rounds);
        int[] key = {k, k, k, k};
        for (int i = 0; i < rounds; i++) {
            int v0shift5 = Integer.rotateRight((v0 >> 5) << 5, 5);
            int sumShift11 = Integer.rotateRight((sum >> 11) << 11, 11);
            v1 -= (((v0 << 4) ^ v0shift5) + v0) ^ (sum + key[sumShift11&3]);
            sum -= delta;
            int v1shift5 = Integer.rotateRight((v1 >> 5) << 5, 5);
            v0 -= (((v1 << 4) ^ v1shift5) + v1) ^ (sum + key[sum&3]);
        }
        byte[] ret = {
            (byte)(v0 >> 24),
            (byte)(v0 >> 16),
            (byte)(v0 >> 8),
            (byte)v0,
            (byte)(v1 >> 24),
            (byte)(v1 >> 16),
            (byte)(v1 >> 8),
            (byte)v1,
        };
        return new ByteArrayWrapper(ret);
    }

    public static ByteArrayWrapper doubleEncrypt(int k1, int k2, ByteArrayWrapper m) {
        return encrypt(k2, encrypt(k1, m));
    }

    public static ByteArrayWrapper doubleDecrypt(int k1, int k2, ByteArrayWrapper c) {
        return decrypt(k1, decrypt(k2, c));
    }
}

