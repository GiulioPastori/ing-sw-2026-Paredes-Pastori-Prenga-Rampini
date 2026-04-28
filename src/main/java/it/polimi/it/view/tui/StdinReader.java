package it.polimi.it.view.tui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Lettore stdin dedicato con coda bloccante.
 *
 * <p>Un unico thread daemon legge righe da stdin con il normale
 * {@link BufferedReader#readLine()} (bloccante, funziona su tutti i sistemi
 * e in tutti gli IDE). Ogni riga letta viene inserita in una
 * {@link BlockingQueue}.
 *
 * <p>I metodi di input della {@link TuiView} chiamano {@link #readLine(long, TimeUnit)}
 * che preleva dalla coda con timeout. In questo modo:
 * <ul>
 *   <li>stdin viene letto da <em>un solo thread</em> → nessuna race condition</li>
 *   <li>i task dell'inputExecutor non bloccano su syscall non interrompibili</li>
 *   <li>il timeout permette al task di svegliarsi e controllare se è stato
 *       interrotto o se la partita è finita</li>
 * </ul>
 */
public class StdinReader {

    private static final Logger LOG = Logger.getLogger(StdinReader.class.getName());

    /** Coda delle righe lette da stdin, non ancora consumate. */
    private final BlockingQueue<String> lines = new LinkedBlockingQueue<>();

    /** Singleton: un solo lettore per tutta l'applicazione. */
    private static final StdinReader INSTANCE = new StdinReader();

    public static StdinReader getInstance() { return INSTANCE; }

    private StdinReader() {
        Thread t = new Thread(this::readLoop, "stdin-reader");
        t.setDaemon(true);
        t.start();
    }

    // ── Loop di lettura (thread daemon) ───────────────────────────────────────

    private void readLoop() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.put(line);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.warning("stdin-reader terminato: " + e.getMessage());
        }
    }

    // ── API pubblica ──────────────────────────────────────────────────────────

    /**
     * Preleva la prossima riga dalla coda, bloccando per al massimo
     * {@code timeout} {@code unit}.
     *
     * @return la riga letta, oppure {@code null} se il timeout è scaduto
     * @throws InterruptedException se il thread viene interrotto
     */
    public String readLine(long timeout, TimeUnit unit) throws InterruptedException {
        return lines.poll(timeout, unit);
    }

    /**
     * Preleva la prossima riga dalla coda, bloccando indefinitamente.
     *
     * @return la riga letta (mai null)
     * @throws InterruptedException se il thread viene interrotto
     */
    public String readLine() throws InterruptedException {
        return lines.take();
    }
}
