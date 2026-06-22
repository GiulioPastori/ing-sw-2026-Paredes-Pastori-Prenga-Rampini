package it.polimi.it.view.tui;

import java.io.IOException;
import java.io.InputStream;

/**
 * Stdin reader that can be interrupted through {@link Thread#interrupt()}.
 *
 * <p>{@link java.util.Scanner#nextLine()} and {@link java.io.BufferedReader#readLine()}
 * block inside a non-interruptible system call: even after
 * {@code Thread.interrupt()} or {@code ExecutorService.shutdownNow()}, the
 * thread remains blocked until the user presses Enter.
 *
 * <p>This helper reads byte by byte and checks {@link InputStream#available()}
 * before blocking, with short sleeps between checks. This makes the thread
 * observe {@code Thread.interrupt()} regularly and wake within
 * {@value #POLL_MS} ms.
 *
 * <p>{@link TuiView} uses it to make input methods interruptible, allowing the
 * {@code ClientController} to cancel pending input when the turn changes.
 */
public class InterruptibleInput {

    /** Polling interval in milliseconds; balances responsiveness and CPU use. */
    private static final int POLL_MS = 100;

    private final InputStream in;

    public InterruptibleInput(InputStream in) {
        this.in = in;
    }

    /**
     * Reads a stdin line, interruptible through {@link Thread#interrupt()}.
     *
     * @return the line without the trailing newline, or {@code null} on EOF
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws IOException          on I/O errors
     */
    public String readLine() throws InterruptedException, IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException("Input interrupted");
            }
            int available = in.available();
            if (available > 0) {
                int b = in.read();
                if (b == -1) return sb.isEmpty() ? null : sb.toString();
                char c = (char) b;
                if (c == '\n') return sb.toString().stripTrailing(); // removes \r on Windows
                sb.append(c);
            } else {
                Thread.sleep(POLL_MS);
            }
        }
    }
}
