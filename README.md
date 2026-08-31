# A2W Automator v1.0

Automazione Android costruita sul flusso A2W mostrato negli screenshot.

## Excel richiesto
Due colonne:
- `ASSET`
- `VALORE DA ASSUMERE`

## Sequenza implementata
Home A2W → menu ☰ → Asset → Codice → ricerca → risultato esatto → Riepilogo → scheda INFO → matita → sostituzione Descrizione → floppy Salva → ritorno a Riepilogo → verifica codice + nuova descrizione → casetta 🏠 → asset successivo.

## Protezioni
- verifica codice asset prima/dopo la modifica;
- marca OK solo se dopo il salvataggio trova sia codice sia nuova descrizione;
- log locale;
- STOP manuale;
- preferisce elementi Accessibility e usa tap percentuali solo come fallback per icone senza etichetta.

## Prima prova
Usare 2-3 asset non critici. L'interfaccia A2W può non esporre alcune icone ad Accessibility: i fallback sono tarati sugli screenshot forniti, ma vanno validati sul telefono reale prima di elaborazioni massive.
