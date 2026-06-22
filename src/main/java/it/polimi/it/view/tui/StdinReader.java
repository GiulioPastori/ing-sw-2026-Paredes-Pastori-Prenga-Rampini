package it.polimi.it.view.tui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Dedicated stdin reader backed by a blocking queue.
 *
 * <p>A single daemon thread reads stdin lines through the regular
 * {@link BufferedReader#readLine()} call. This blocking call works reliably
 * across systems and IDEs. Each line is then inserted into a
 * {@link BlockingQueue}.
 *
 * <p>The input methods in {@link TuiView} call {@link #readLine(long, TimeUnit)}
 * to poll the queue with a timeout. This guarantees that:
 * <ul>
 *   <li>stdin is read by <em>one thread only</em>, avoiding input races;</li>
 *   <li>inputExecutor tasks do not block on non-interruptible system calls;</li>
 *   <li>the timeout lets a task wake up and check whether it was interrupted
 *       or the game has ended.</li>
 * </ul>
 */
public class StdinReader {

    private static final Logger LOG = Logger.getLogger(StdinReader.class.getName());

    /** Lines read from stdin and not yet consumed. */
    private final BlockingQueue<String> lines = new LinkedBlockingQueue<>();

    /** Singleton: one reader for the whole application. */
    private static final StdinReader INSTANCE = new StdinReader();

    public static StdinReader getInstance() { return INSTANCE; }

    private StdinReader() {
        Thread t = new Thread(this::readLoop, "stdin-reader");
        t.setDaemon(true);
        t.start();
    }

    // Daemon read loop.

    private void readLoop() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.put(line);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.warning("stdin-reader stopped: " + e.getMessage());
        }
    }

    // Public API.

    /**
     * Polls the next line from the queue, waiting at most the given timeout.
     *
     * @return the line read from stdin, or {@code null} if the timeout expires
     * @throws InterruptedException if the thread is interrupted
     */
    public String readLine(long timeout, TimeUnit unit) throws InterruptedException {
        return lines.poll(timeout, unit);
    }

    /**
     * Takes the next line from the queue, blocking indefinitely.
     *
     * @return the line read from stdin, never {@code null}
     * @throws InterruptedException if the thread is interrupted
     */
    public String readLine() throws InterruptedException {
        return lines.take();
    }
}
