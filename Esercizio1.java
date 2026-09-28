import java.util.*;

public class Esercizio1 {

    public static void main(String[] args) {
        String[] pacchetti = {
                "temp=23.5;umid=61;ts=1732000000;batt_low=false",  // valido
                "temp=19.0;umid=45;ts=1732000060;batt_low=true",   // valido
                "temp=xx.xx;umid=50;ts=1732000120;batt_low=false", // temperatura corrotta
                "umid=70;ts=1732000180",                           // campi mancanti
                "temp=25.5;umid=150;ts=1732000240;batt_low=false", // umidità fuori range
                "",                                                // vuoto
                "temp=21.0;batt_low=boh",                          // booleano corrotto
                "temp=30.2;umid=20;ts=1732000300;batt_low=false",  // valido
                "spazzatura senza uguale"                          // formato sbagliato
        };

        List<LetturaSensore> lettureValide = new ArrayList<>();
        int pacchettiScartati = 0;

        for (String pacchetto : pacchetti) {
            System.out.println("\nPacchetto: \"" + pacchetto + "\"");
            try {
                Optional<LetturaSensore> risultato = LetturaSensore.parsePacchetto(pacchetto);
                if (risultato.isPresent()) {
                    lettureValide.add(risultato.get());
                    System.out.println("  OK -> " + risultato.get());
                } else {
                    pacchettiScartati++;
                    System.out.println("  Scartato (nessun dato utile)");
                }
            } catch (LetturaInvalidaException e) {
                pacchettiScartati++;
                System.out.println("  Scartato: " + e.getMessage());
            }
        }

        // Media delle temperature
        double somma = 0;
        int conteggio = 0;
        for (LetturaSensore l : lettureValide) {
            if (l.temperatura != null) {
                somma += l.temperatura; // unboxing automatico Double -> double
                conteggio++;
            }
        }

        System.out.println("\n===== REPORT =====");
        System.out.println("Letture valide: " + lettureValide.size());
        System.out.println("Pacchetti scartati: " + pacchettiScartati);
        System.out.println("Campi corrotti: " + LetturaSensore.campiCorrotti);
        if (conteggio > 0) {
            System.out.println("Media temperature: " + (somma / conteggio) + " (su " + conteggio + " letture)");
        } else {
            System.out.println("Media temperature: nessun dato");
        }


        System.out.println("\n===== TEST INTEGER CACHE =====");
        Integer piccolo1 = Integer.valueOf(100), piccolo2 = Integer.valueOf(100);
        Integer grande1 = Integer.valueOf(1000), grande2 = Integer.valueOf(1000);
        System.out.println("100 == 100    (sbagliato): " + LetturaSensore.confrontaBatteriaSbagliato(piccolo1, piccolo2)); // true
        System.out.println("1000 == 1000  (sbagliato): " + LetturaSensore.confrontaBatteriaSbagliato(grande1, grande2));   // false!
        System.out.println("100 equals 100   (corretto): " + LetturaSensore.confrontaBatteria(piccolo1, piccolo2));         // true
        System.out.println("1000 equals 1000 (corretto): " + LetturaSensore.confrontaBatteria(grande1, grande2));           // true


        // ---- ordinamento per temperatura decrescente ----
        System.out.println("\n===== BONUS: ORDINE PER TEMPERATURA DECRESCENTE =====");
        lettureValide.sort((a, b) -> {
            if (a.temperatura == null && b.temperatura == null) return 0;
            if (a.temperatura == null) return 1;  // i null vanno in fondo
            if (b.temperatura == null) return -1;
            return Double.compare(b.temperatura, a.temperatura); // b prima di a = decrescente
        });
        for (LetturaSensore l : lettureValide) {
            System.out.println("  " + l);
        }

        // Integer.compare e Boolean.compare funzionano allo stesso modo: negativo, 0 o positivo
        System.out.println("Integer.compare(5, 10) = " + Integer.compare(5, 10));       // negativo
        System.out.println("Boolean.compare(true, false) = " + Boolean.compare(true, false)); // positivo
    }
}

// Eccezione personalizzata: estende IllegalArgumentException come richiesto
class LetturaInvalidaException extends IllegalArgumentException {
    public LetturaInvalidaException(String messaggio) {
        super(messaggio);
    }
}

class LetturaSensore {
    Double temperatura;
    Integer umiditaPercentuale;
    Long timestampUnix;
    Boolean batteriaScarica;

    static int campiCorrotti = 0; // conta i campi corrotti trovati

    // Trasforma la stringa in una LetturaSensore
    static Optional<LetturaSensore> parsePacchetto(String raw) {
        if (raw == null || raw.isBlank()) {
            System.out.println("[LOG] Pacchetto vuoto");
            return Optional.empty();
        }

        LetturaSensore lettura = new LetturaSensore();
        int campiLetti = 0;

        // Il pacchetto "temp=23.5;umid=61" si spezza prima sui ';' e poi sui '='
        for (String coppia : raw.split(";")) {
            String[] parti = coppia.split("=");
            if (parti.length != 2) {
                System.out.println("[LOG] Campo malformato: '" + coppia + "'");
                campiCorrotti++;
                continue; // passa al campo successivo
            }
            String chiave = parti[0].trim();
            String valore = parti[1].trim();

            try {
                switch (chiave) {
                    case "temp":
                        // Double.valueOf restituisce direttamente un Double (wrapper)
                        lettura.temperatura = Double.valueOf(valore);
                        campiLetti++;
                        break;
                    case "umid":
                        Integer umidita = Integer.valueOf(valore);
                        if (umidita < 0 || umidita > 100) {
                            throw new LetturaInvalidaException("Umidità fuori range: " + umidita);
                        }
                        lettura.umiditaPercentuale = umidita;
                        campiLetti++;
                        break;
                    case "ts":
                        lettura.timestampUnix = Long.valueOf(valore);
                        campiLetti++;
                        break;
                    case "batt_low":
                        if (valore.equalsIgnoreCase("true") || valore.equalsIgnoreCase("false")) {
                            lettura.batteriaScarica = Boolean.valueOf(valore);
                            campiLetti++;
                        } else {
                            throw new NumberFormatException("booleano non valido: " + valore);
                        }
                        break;
                    default:
                        System.out.println("[LOG] Campo sconosciuto: '" + chiave + "'");
                }
            } catch (NumberFormatException e) {
                System.out.println("[LOG] Campo corrotto '" + coppia + "' -> " + e.getMessage());
                campiCorrotti++;
            }
        }

        if (campiLetti == 0) {
            return Optional.empty(); // nessun dato utilizzabile
        }
        return Optional.of(lettura);
    }

    // VERSIONE SBAGLIATA: == su oggetti confronta i "riferimenti" (l'indirizzo in memoria), non i valori
    static boolean confrontaBatteriaSbagliato(Integer a, Integer b) {
        return a == b;
    }

    // VERSIONE CORRETTA: Objects.equals confronta i valori (e gestisce anche i null)
    static boolean confrontaBatteria(Integer a, Integer b) {
        return Objects.equals(a, b);
    }

    @Override
    public String toString() {
        return "temp=" + temperatura + ", umid=" + umiditaPercentuale
                + ", ts=" + timestampUnix + ", battScarica=" + batteriaScarica;
    }
}
