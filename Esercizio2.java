import java.util.*;

public class Esercizio2 {

    static List<RichiestaHttp> ultimeErrori500(List<RichiestaHttp> storico, int n) {
        List<RichiestaHttp> risultato = new ArrayList<>();
        for (int i = storico.size() - 1; i >= 0 && risultato.size() < n; i--) {
            RichiestaHttp richiesta = storico.get(i);
            if (richiesta.statusCode >= 500) {
                risultato.add(richiesta);
            }
        }
        return risultato; // dalla più recente alla più vecchia
    }

    static long percentile90(TreeSet<Long> tempi) {
        int posizione = (int) Math.ceil(tempi.size() * 0.9); // es. su 40 valori -> il 36°
        Long corrente = tempi.first();                        // parto dal più piccolo
        for (int i = 1; i < posizione; i++) {
            corrente = tempi.higher(corrente);                // higher = il valore subito più grande
        }
        return corrente;
    }

    public static void main(String[] args) {
        Random casuale = new Random(42);
        String[] elencoIp = {"10.0.0.1", "10.0.0.2", "10.0.0.3", "10.0.0.4", "10.0.0.5", "10.0.0.6"};
        String[] elencoPath = {"/home", "/login", "/api/utenti", "/prodotti", "/carrello"};
        int[] elencoStatus = {200, 200, 200, 200, 301, 403, 404, 500, 503}; // più 200 che errori

        ArrayList<RichiestaHttp> storico = new ArrayList<>();      // 1) storico completo
        LinkedList<RichiestaHttp> finestra = new LinkedList<>();   // 2) ultime 10
        HashSet<String> ipSospetti = new HashSet<>();              // 3) IP con errori
        TreeSet<Long> tempiUnici = new TreeSet<>();                // 4) tempi distinti ordinati

        for (int i = 0; i < 35; i++) {
            RichiestaHttp richiesta = new RichiestaHttp(
                    elencoIp[casuale.nextInt(elencoIp.length)],
                    elencoPath[casuale.nextInt(elencoPath.length)],
                    elencoStatus[casuale.nextInt(elencoStatus.length)],
                    20 + casuale.nextInt(50) * 10L,   // tempi tra 20 e 510 ms (con ripetizioni)
                    1732000000L + i * 10L);

            storico.add(richiesta);

            finestra.addLast(richiesta);
            if (finestra.size() > 10) {
                finestra.removeFirst();
            }

            if (richiesta.statusCode >= 400) {
                ipSospetti.add(richiesta.ip);
            }

            tempiUnici.add(richiesta.tempoRispostaMs);
        }

        System.out.println("Richieste totali: " + storico.size());

        System.out.println("\nUltime 3 richieste con errore 5xx:");
        for (RichiestaHttp r : ultimeErrori500(storico, 3)) {
            System.out.println("  " + r);
        }

        System.out.println("\nFinestra scorrevole (ultime " + finestra.size() + " richieste):");
        for (RichiestaHttp r : finestra) {
            System.out.println("  " + r);
        }

        System.out.println("\nIP sospetti (errori 4xx/5xx): " + ipSospetti);

        HashSet<String> sospettiInFinestra = new HashSet<>();
        for (RichiestaHttp r : finestra) {
            if (ipSospetti.contains(r.ip)) {   // contains su HashSet è O(1) in media
                sospettiInFinestra.add(r.ip);
            }
        }

        long p90 = percentile90(tempiUnici);
        System.out.println("\nTempi di risposta distinti (ordinati): " + tempiUnici);
        System.out.println("Tempi oltre il 90° percentile: " + tempiUnici.tailSet(p90, false)); // tailSet = sottoinsieme
        System.out.println("Tempo più alto <= 300 ms: " + tempiUnici.floor(300L));               // floor = valore <= 300

        System.out.println("\n===== RIEPILOGO =====");
        System.out.println(sospettiInFinestra.size() + " IP sospetti (su " + ipSospetti.size()
                + " totali) compaiono tra le ultime " + finestra.size() + " richieste in finestra; "
                + "il tempo di risposta al 90° percentile è di " + p90 + " ms");
    }
}

class RichiestaHttp {
    String ip;
    String path;
    int statusCode;
    long tempoRispostaMs;
    long timestamp;

    RichiestaHttp(String ip, String path, int statusCode, long tempoRispostaMs, long timestamp) {
        this.ip = ip;
        this.path = path;
        this.statusCode = statusCode;
        this.tempoRispostaMs = tempoRispostaMs;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return ip + " " + path + " -> " + statusCode + " (" + tempoRispostaMs + " ms)";
    }
}
