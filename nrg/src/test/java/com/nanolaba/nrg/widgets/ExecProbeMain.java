package com.nanolaba.nrg.widgets;

/**
 * Child process for {@link ExecWidgetTest}: a real command with behaviour the tests control.
 *
 * <p>{@code flood <stdout-bytes> <stderr-bytes>} writes that much to each stream — far more
 * than an OS pipe buffer holds, which is what a process blocks on when nobody reads.
 * {@code stdin} reports whether its standard input is at end of file.
 */
public final class ExecProbeMain {

    private ExecProbeMain() {
    }

    public static void main(String[] args) throws Exception {
        if ("flood".equals(args[0])) {
            flood(System.err, Integer.parseInt(args[2]), 'e');
            flood(System.out, Integer.parseInt(args[1]), 'o');
        } else if ("stdin".equals(args[0])) {
            System.out.print(System.in.read() == -1 ? "stdin-closed" : "stdin-open");
        }
    }

    private static void flood(java.io.PrintStream stream, int bytes, char fill) {
        byte[] chunk = new byte[8192];
        java.util.Arrays.fill(chunk, (byte) fill);
        for (int written = 0; written < bytes; written += chunk.length) {
            stream.write(chunk, 0, Math.min(chunk.length, bytes - written));
        }
        stream.flush();
    }
}
