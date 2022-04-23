import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        ArrayList<Pair> pairs = parsePairs();
        if (pairs.isEmpty()) {
            System.err.println("need at least one plaintext/ciphertext pair");
            System.exit(1);
        }

        // TODO: Implement your attack here
		Pair firstPair = pairs.get(0);
		ByteArrayWrapper plaintext = firstPair.plaintext;
		ByteArrayWrapper ciphertext = firstPair.ciphertext;
		Cipher cipher = new Cipher();
        //printKeyPair(0, 0);
		HashMap<ByteArrayWrapper, Integer> encryptedSet = new HashMap();
		int encryptionKey, decryptionKey;
		for (encryptionKey = 0; encryptionKey < 16777216; encryptionKey++) {
			//encrypt plaintext with key
			ByteArrayWrapper midtext = cipher.encrypt(encryptionKey, plaintext);
			encryptedSet.put(midtext, encryptionKey);
		}

		for (decryptionKey = 0; decryptionKey < 16777216; decryptionKey++) {
			ByteArrayWrapper decryptedText = cipher.decrypt(decryptionKey, ciphertext); 
			if (encryptedSet.containsKey(decryptedText)) {
				encryptionKey = encryptedSet.get(decryptedText);
				Boolean isCorrect = true;
				Pair curPair;
				for (int i = 1; i < pairs.size(); i++) {
						curPair = pairs.get(i);
						if (!cipher.encrypt(encryptionKey, curPair.plaintext).equals(cipher.decrypt(decryptionKey, curPair.ciphertext))) {	
							isCorrect = false;
							break;
						}	
				}
				if (!isCorrect) {
					continue;
				}
				printKeyPair(encryptionKey, decryptionKey);
			}
		}
}

    private static void printKeyPair(int k1, int k2) {
        System.out.println("Recovered key pair: (" + ("000000" +
            Integer.toHexString(k1)).substring(Integer.toHexString(k1).length())
            + ", " + ("000000" +
            Integer.toHexString(k2)).substring(Integer.toHexString(k2).length())
            + ")");
    }

    private static ArrayList<Pair> parsePairs() {
        ArrayList<Pair> pairs = new ArrayList<Pair>();

        try{
            BufferedReader br =
                          new BufferedReader(new InputStreamReader(System.in));

            String input;

            while((input=br.readLine())!=null){
                String[] parts = input.split("\\s+");
                if (parts.length == 0) {
                    continue;
                } else if (parts.length != 2) {
                    System.err.println("invalid syntax: "
                            + "expected lines of the form <plaintext> <ciphertext>");
                    System.exit(1);
                }

                byte[] plaintext = null;
                byte[] ciphertext = null;

                try {
                    plaintext = hexStringToByteArray(parts[0]);
                } catch (IllegalArgumentException e) {
                    System.err.println("could not parse plaintext as hex: " + parts[0]);
                    System.exit(1);
                }

                try {
                    ciphertext = hexStringToByteArray(parts[1]);
                } catch (IllegalArgumentException e) {
                    System.err.println("could not parse ciphertext as hex: " + parts[1]);
                    System.exit(1);
                }


                if (plaintext.length != 8) {
                    System.err.println("plaintext must be 8 bytes (got " + plaintext.length + ")");
                    System.exit(1);
                }
                if (ciphertext.length != 8) {
                    System.err.println("ciphertext must be 8 bytes");
                    System.exit(1);
                }

                Pair pair = new Pair(new ByteArrayWrapper(Arrays.copyOf(plaintext,
                                plaintext.length)), new ByteArrayWrapper(Arrays.copyOf(ciphertext,
                                ciphertext.length)));

                pairs.add(pair);
            }

            br.close();

        } catch(IOException io){
            io.printStackTrace();
        }

        return pairs;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static class Pair {
        private ByteArrayWrapper plaintext;
        private ByteArrayWrapper ciphertext;

        public Pair(ByteArrayWrapper plaintext, ByteArrayWrapper ciphertext) {
            this.plaintext = plaintext;
            this.ciphertext = ciphertext;
        }

        public ByteArrayWrapper getPlaintext() {
            return plaintext;
        }

        public ByteArrayWrapper getCiphertext() {
            return ciphertext;
        }

    }
}

