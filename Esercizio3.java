import java.io.*;
import java.util.*;

public class Esercizio3 {

    static List<Ticket> leggiTicket(String nomeFile, List<String> logErrori) {
        List<Ticket> ticketLetti = new ArrayList<>();

        // try-with-resources: il file viene chiuso da solo alla fine
        try (BufferedReader lettore = new BufferedReader(new FileReader(nomeFile))) {
            String riga;
            int numeroRiga = 0;
            while ((riga = lettore.readLine()) != null) {
                numeroRiga++;
                if (numeroRiga == 1 || riga.isBlank()) {
                    continue; // salto l'intestazione e le righe vuote
                }

                String[] campi = riga.split(",");
                if (campi.length != 4) {
                    logErrori.add("Riga " + numeroRiga + ": campo mancante -> " + riga);
                    continue;
                }

                try {
                    // valueOf fallisce se il testo non è esattamente CRITICO/ALTO/MEDIO/BASSO (anche "medio" minuscolo)
                    Livello livello = Livello.valueOf(campi[2].trim());
                    long timestamp = Long.parseLong(campi[3].trim());
                    ticketLetti.add(new Ticket(campi[0].trim(), campi[1].trim(), livello, timestamp));
                } catch (IllegalArgumentException e) {
                    // NumberFormatException (timestamp errato) è un tipo di IllegalArgumentException,
                    // quindi questo catch prende entrambi gli errori
                    logErrori.add("Riga " + numeroRiga + ": livello o timestamp non valido -> " + riga);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("ERRORE: file '" + nomeFile + "' non trovato. Mettilo nella cartella da cui lanci il programma.");
        } catch (IOException e) {
            System.out.println("ERRORE durante la lettura del file: " + e.getMessage());
        }
        return ticketLetti;
    }

    // Bonus: allarme se ci sono più di 2 ticket critici in attesa
    static void controllaAllarme(PriorityQueue<Ticket> criticiInAttesa) {
        if (criticiInAttesa.size() > 2) {
            System.out.println("!!! ALLARME: " + criticiInAttesa.size() + " ticket CRITICI in attesa !!!");
        }
    }

    public static void main(String[] args) {
        List<String> logErrori = new ArrayList<>();
        List<Ticket> ticketLetti = leggiTicket("ticket.csv", logErrori);

        System.out.println("Ticket letti: " + ticketLetti.size() + " | righe scartate: " + logErrori.size());

        // Salvo le righe malformate in un file di log a parte
        try (PrintWriter scrittoreLog = new PrintWriter(new FileWriter("log_errori.txt"))) {
            for (String errore : logErrori) {
                scrittoreLog.println(errore);
            }
        } catch (IOException e) {
            System.out.println("ERRORE scrittura log: " + e.getMessage());
        }

        // Metto tutti i ticket nella coda di priorità (usa il compareTo scritto sopra)
        PriorityQueue<Ticket> coda = new PriorityQueue<>(ticketLetti);

        // Bonus: seconda coda con i soli CRITICI non ancora lavorati
        PriorityQueue<Ticket> criticiInAttesa = new PriorityQueue<>();
        for (Ticket t : ticketLetti) {
            if (t.livello == Livello.CRITICO) {
                criticiInAttesa.add(t);
            }
        }
        controllaAllarme(criticiInAttesa);

        int tempoCumulativo = 0;
        int urgentiConsecutivi = 0; // ticket CRITICI/ALTI di fila senza pausa
        int[] conteggioPerLivello = new int[Livello.values().length];
        List<String> righeReport = new ArrayList<>();
        int contatoreOrdine = 0;

        // poll() estrae e rimuove il ticket con la priorità più alta
        while (!coda.isEmpty()) {
            Ticket ticket = coda.poll();
            contatoreOrdine++;
            System.out.println("Lavorazione ticket " + ticket.id + " (" + ticket.livello + ")...");

            tempoCumulativo += ticket.livello.minuti;
            conteggioPerLivello[ticket.livello.ordinal()]++; // ordinal() = posizione nell'enum (0,1,2,3)
            righeReport.add(contatoreOrdine + ". " + ticket.id + " | " + ticket.livello + " | "
                    + ticket.descrizione + " | completato al minuto " + tempoCumulativo);

            if (ticket.livello == Livello.CRITICO) {
                criticiInAttesa.remove(ticket); // non è più in attesa
                controllaAllarme(criticiInAttesa);
            }

            // Pausa obbligatoria dopo 5 ticket urgenti consecutivi
            if (ticket.livello == Livello.CRITICO || ticket.livello == Livello.ALTO) {
                urgentiConsecutivi++;
                if (urgentiConsecutivi == 5 && !coda.isEmpty()) {
                    tempoCumulativo += 10;
                    righeReport.add("   --- PAUSA di 10 minuti (tempo: " + tempoCumulativo + ") ---");
                    urgentiConsecutivi = 0;
                }
            } else {
                urgentiConsecutivi = 0; // un ticket normale interrompe la serie
            }
        }

        // Scrittura del report (try-with-resources chiude il file da solo)
        try (PrintWriter scrittore = new PrintWriter(new FileWriter("report_lavorazione.txt"))) {
            scrittore.println("ORDINE DI LAVORAZIONE");
            for (String riga : righeReport) {
                scrittore.println(riga);
            }
            scrittore.println();
            scrittore.println("RIEPILOGO");
            scrittore.println("Totale ticket: " + ticketLetti.size());
            for (Livello livello : Livello.values()) {
                scrittore.println("  " + livello + ": " + conteggioPerLivello[livello.ordinal()]);
            }
            scrittore.println("Tempo totale stimato: " + tempoCumulativo + " minuti");
            System.out.println("\nReport scritto in report_lavorazione.txt (tempo totale: " + tempoCumulativo + " min)");
        } catch (IOException e) {
            System.out.println("ERRORE scrittura report: " + e.getMessage());
        }
    }
}

// enum = elenco di valori fissi. L'ORDINE conta: CRITICO è il primo, quindi il "più piccolo".
// Dato che la PriorityQueue estrae prima il "più piccolo", i CRITICI escono per primi.
enum Livello {
    CRITICO(15), ALTO(30), MEDIO(60), BASSO(120);

    final int minuti; // tempo stimato di lavorazione

    Livello(int minuti) {
        this.minuti = minuti;
    }
}

class Ticket implements Comparable<Ticket> {
    String id;
    String descrizione;
    Livello livello;
    long timestampArrivo;

    Ticket(String id, String descrizione, Livello livello, long timestampArrivo) {
        this.id = id;
        this.descrizione = descrizione;
        this.livello = livello;
        this.timestampArrivo = timestampArrivo;
    }

    // Punto centrale dell'esercizio: definisce chi passa prima
    @Override
    public int compareTo(Ticket altro) {
        // 1) prima il livello (CRITICO < ALTO < MEDIO < BASSO)
        int confrontoLivello = this.livello.compareTo(altro.livello);
        if (confrontoLivello != 0) {
            return confrontoLivello;
        }
        // 2) a parità di livello vince il più vecchio (timestamp più piccolo) = FIFO
        return Long.compare(this.timestampArrivo, altro.timestampArrivo);
    }
}
