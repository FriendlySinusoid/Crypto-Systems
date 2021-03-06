package crypto;

/**
 *An implementation of AES in ecb mode
 *
 *ADSB: Final Project
 *@author Sam Felsenfeld, Bryan Li
 */
public class AES {

    private int[][][] state;

    private int[][][] expandedKey;

    private int round = 0;

    private int rounds = 11;

    public static final int DECRYPT_MODE = 0;

    public static final int ENCRYPT_MODE = 1;

    private int mode;

    public static void main(String args[]) {
        int[][] key = new int[][]{{5, 8, 13, 5}, {2, 7, 9, 0}, {9, 9, 2, 11}, {13, 8, 5, 13}};

        AES sys = new AES(key.clone(), "Hello world this is a test", AES.ENCRYPT_MODE);
        String s;
        s = sys.digest();
        System.out.println(s);
        sys = new AES(key.clone(), s, AES.DECRYPT_MODE);
        System.out.println(sys.digest());
    }

    /**
     *Constructor for an AES object
     *@param key a two dimensional 4 by 4 array representing the key
     *@param message the message to be encrypted
     *@param mode either AES.DECRYPT_MODE for decryption or AES.ENCRYPT_MODE for encryption
     */
    public AES(int[][] key, String message, int mode) {
        int blocks = message.length() / 16 + 1 - ((message.length() % 16 == 0) ? 1 : 0);
        state = new int[blocks][4][4];
        int index = 0;
        for (int i = 0; i < blocks; i++)
            for (int j = 0; j < 4; j++)
                for (int k = 0; k < 4; k++) {
                    state[i][j][k] = (index < message.length()) ? (int) message.charAt(index) : 0;//pad with nulls, might make cipher more vulnerable
                    index++;
                }
        for (int k = 0; k < state.length; k++) {
            int[][] tState = new int[4][4];
            for (int i = 0; i < 4; i++) {
                int[] col = new int[4];
                for (int j = 0; j < 4; j++)
                    tState[i][j] = state[k][j][i];
            }
            state[k] = tState;
        }

        //Transpose key matrix
        int[][] subkey = new int[4][4];
        for (int i = 0; i < 4; i++) {
            int[] col = new int[4];
            for (int j = 0; j < 4; j++)
                subkey[i][j] = key[j][i];
        }
        keySchedule(subkey);
        this.mode = mode;
    }

    /**
     *Create the expanded key
     *
     *This method follows the Rjindael key schedule to make the expanded key used in the addRoundKey step.
     *@param key the input key
     *
     */
    private void keySchedule(int[][] key) {

        expandedKey = new int[12][4][4];

        expandedKey[0] = key.clone();

        for (int iter = 1; iter <= rounds; iter++) {
            int[] t = expandedKey[iter - 1][3];
            int[][] block = new int[4][4];

            //key schedule core
            for (int i = 0; i < 4; i++)
                t[i] = Tables.sBox[t[(i + 1) % 4]];
            t[0] = t[0] ^ Tables.rCon[iter];
            for (int i = 0; i < 4; i++) {
                t[i] = t[i] ^ expandedKey[iter - 1][0][i];
            }
            block[0] = t.clone();
            //end of core
            for (int j = 1; j < 4; j++) {
                for (int i = 0; i < 4; i++) {
                    t[i] = t[i] ^ expandedKey[iter - 1][j - 1][i];
                }
                block[j] = t.clone();
            }
            expandedKey[iter] = block;
        }
    }

    /**
     * Add round key step
     *In this step, for each block of the current state, each byte of the round key is Xored with the corresponding bite of the block.
     *
     */
    private void addRoundKey() {
        for (int[][] block : state) {
            for (int i = 0; i < 4; i++)
                for (int j = 0; j < 4; j++)
                    block[i][j] = block[i][j] ^ expandedKey[round][i][j];
        }
    }

    /**
     * Inverse add round key step
     *In this step, for each block of the current state, each byte of the round key is Xored with the corresponding bite of the block.  This works
     * because the inverse of Xor is Xor.
     *
     */
    private void invKey() {
        for (int[][] block : state) {
            for (int i = 0; i < 4; i++)
                for (int j = 0; j < 4; j++)
                    block[i][j] = block[i][j] ^ expandedKey[rounds - round][i][j];
        }
    }

    /**
     * Substitute each byte in the state with the element at it's index in the s box.
     */
    private void substitute() {
        //substitution
        for (int[][] block : state)
            for (int[] row : block)
                for (int i = 0; i < 4; i++) {
                    row[i] = Tables.sBox[row[i]];
                }
    }

    /**
     * Substitute each byte in the state with the element at it's index in the inverse s box.  This serves to undo normal substitution
     */
    private void invSub() {
        //substitution
        for (int[][] block : state)
            for (int[] row : block)
                for (int i = 0; i < 4; i++) {
                    row[i] = Tables.invSBox[row[i]];
                }
    }

    /**
     * Shift each row in each block by an amount equal to it's row - 1 (the second row down would be shifted 1 to the left)
     */
    private void shift() {
        //transpose
        for (int k = 0; k < state.length; k++) {
            int[][] tState = new int[4][4];
            for (int i = 0; i < 4; i++) {
                int[] col = new int[4];
                for (int j = 0; j < 4; j++)
                    tState[i][j] = state[k][j][i];
            }
            state[k] = tState;
        }

        //shift
        for (int[][] block : state) {
            for (int row = 1; row < 4; row++) {
                int[] newRow = new int[4];
                for (int i = 0; i < 4; i++) {
                    newRow[i] = block[row][(row + i) % 4];
                }
                block[row] = newRow;
            }
        }

        //transpose
        for (int k = 0; k < state.length; k++) {
            int[][] tState = new int[4][4];
            for (int i = 0; i < 4; i++) {
                int[] col = new int[4];
                for (int j = 0; j < 4; j++)
                    tState[i][j] = state[k][j][i];
            }
            state[k] = tState;
        }
    }

    /**
     * Inverse of the shift method, does the same thing but shifts right
     */
    private void invShift() {
        for (int k = 0; k < state.length; k++) {
            int[][] tState = new int[4][4];
            for (int i = 0; i < 4; i++) {
                int[] col = new int[4];
                for (int j = 0; j < 4; j++)
                    tState[i][j] = state[k][j][i];
            }
            state[k] = tState;
        }
        for (int[][] block : state) {
            for (int row = 1; row < 4; row++) {
                int[] newRow = new int[4];
                for (int i = 0; i < 4; i++) {
                    int pos = (i - row) % 4;
                    if (pos < 0)
                        pos += 4;
                    newRow[i] = block[row][pos];
                }
                block[row] = newRow;
            }
        }

        for (int k = 0; k < state.length; k++) {
            int[][] tState = new int[4][4];
            for (int i = 0; i < 4; i++) {
                int[] col = new int[4];
                for (int j = 0; j < 4; j++)
                    tState[i][j] = state[k][j][i];
            }
            state[k] = tState;
        }
    }

    /**
     * One round for encryption, this is called 11 times for a 128 bit key (which is what we use)
     */
    private void round() {
        substitute();
        shift();
        mixColumns();
        addRoundKey();
    }

    /**
     * One round for decryption, this does everything in the encryption round but in reverse order, note that the order shift and substitute occur in
     * does not matter.
     */
    private void invRound() {
        invKey();
        invMixColumns();
        invSub();
        invShift();
    }


    /**
     * Reverse the mix columns step
     */
    public void invMixColumns() {
        for (int n = 0; n < state.length; n++) {
            int[][] arr = state[n];
            int[][] temp = new int[4][4];
            for (int i = 0; i < 4; i++) {
                System.arraycopy(arr[i], 0, temp[i], 0, 4);
            }
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    arr[i][j] = invColHelper(temp, Tables.invgalois, i, j);
                }
            }
        }
    }

    private int invColHelper(int[][] arr, int[][] igalois, int i, int j){
        int colsum = 0;
        for (int k = 0; k < 4; k++) {
            int a = igalois[i][k];
            int b = arr[k][j];
            colsum ^= invColCalc(a, b);
        }
        return colsum;
    }

    private int invColCalc(int a, int b) //Helper method for invMcHelper
    {
        if (a == 9) {
            return Tables.mc9[b / 16][b % 16];
        } else if (a == 0xb) {
            return Tables.mc11[b / 16][b % 16];
        } else if (a == 0xd) {
            return Tables.mc13[b / 16][b % 16];
        } else if (a == 0xe) {
            return Tables.mc14[b / 16][b % 16];
        }
        return 0;
    }

    /**
     * Mix columns step
     */
    private void mixColumns() {
        for (int n = 0; n < state.length; n++) {
            int[][] arr = state[n];
            int[][] temp = new int[4][4];
            for (int i = 0; i < 4; i++) {
                System.arraycopy(arr[i], 0, temp[i], 0, 4);
            }
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    arr[i][j] = colHelper(temp, Tables.galois, i, j);
                }
            }
        }
    }

    private int colHelper(int[][] arr, int[][] g, int i, int j) {
        int colsum = 0;
        for (int k = 0; k < 4; k++) {
            int a = g[i][k];
            int b = arr[k][j];
            colsum ^= colCalc(a, b);
        }
        return colsum;
    }

    private int colCalc(int a, int b) //Helper method for mcHelper
    {
        if (a == 1) {
            return b;
        } else if (a == 2) {
            return Tables.mc2[b / 16][b % 16];
        } else if (a == 3) {
            return Tables.mc3[b / 16][b % 16];
        }
        return 0;
    }

    /**
     * This method actually does all the processing, calling round the appropiate number of times
     * and outputting the result.
     * @return the result of the encryption or decryption
     */
    public String digest() {
        for (round = 0; round <= rounds; round++) {
            if (this.mode == AES.ENCRYPT_MODE) {
                round();
            } else {
                invRound();
            }
        }
        for (int k = 0; k < state.length; k++) {
            int[][] tState = new int[4][4];
            for (int i = 0; i < 4; i++) {
                int[] col = new int[4];
                for (int j = 0; j < 4; j++)
                    tState[i][j] = state[k][j][i];
            }
            state[k] = tState;
        }

        StringBuilder sb = new StringBuilder();
        for (int[][] block : state)
            for (int[] row : block)
                for (int c : row)
                    sb.append((char) c);

        return sb.toString();
    }


}
