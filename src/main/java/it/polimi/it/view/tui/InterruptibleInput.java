package it.polimi.it.view.tui;

import java.io.IOException;
import java.io.InputStream;

/**
 * Lettura da stdin interrompibile via {@link Thread#interrupt()}.
 *
 * <p>{@link java.util.Scanner#nextLine()} e {@link java.io.BufferedReader#readLine()}
 * bloccano il thread in una syscall non interrompibile: anche chiamando
 * {@code Thread.interrupt()} o {@code ExecutorService.shutdownNow()}, il thread
 * rimane bloccato finché l'utente non preme Invio.
 *
 * <p>Questo helper legge byte per byte usando {@link InputStream#available()}
 * per verificare se ci sono dati prima di bloccarsi, con brevi sleep tra un
 * controllo e l'altro. In questo modo {@code Thread.interrupt()} viene
 * controllato regolarmente e il thread si sveglia entro {@value #POLL_MS} ms.
 *
 * <p>Usato da {@link TuiView} per rendere tutti i metodi di input interrompibili,
 * così il {@code ClientController} può cancellare un task di input in attesa
 * quando il turno cambia.
 */
public class InterruptibleInput {

    /** Intervallo di polling in ms — bilanciamento tra reattività e CPU. */
    private static final int POLL_MS = 100;

    private final InputStream in;

    public InterruptibleInput(InputStream in) {
        this.in = in;
    }

    /**
     * Legge una riga da stdin, interrompibile tramite {@link Thread#interrupt()}.
     *
     * @return la riga letta (senza newline), oppure {@code null} se EOF
     * @throws InterruptedException se il thread viene interrotto durante l'attesa
     * @throws IOException          in caso di errore di I/O
     */
    public String readLine() throws InterruptedException, IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (Thread.interrupted()) {
                throw new InterruptedException("Input interrotto");
            }
            int available = in.available();
            if (available > 0) {
                int b = in.read();
                if (b == -1) return sb.isEmpty() ? null : sb.toString();
                char c = (char) b;
                if (c == '\n') return sb.toString().stripTrailing(); // rimuove \r su Windows
                sb.append(c);
            } else {
                Thread.sleep(POLL_MS);
            }
        }
    }
}