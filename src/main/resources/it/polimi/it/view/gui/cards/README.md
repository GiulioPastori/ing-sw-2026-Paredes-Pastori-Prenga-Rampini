## Immagini delle carte

Le immagini PNG delle carte vanno messe in questa cartella usando l'ID
numerico della carta come nome file:

    cards/<id>.png

Esempi:
- `cards/1001.png` → personaggio (Era I)
- `cards/101.png`  → edificio (Era I)
- `cards/2001.png` → carta evento (Era I)

Convenzione degli ID (vedi `it.polimi.it.cards.Card`):
- **Personaggi:** 1001–1999  (Era I: 1001-1099, II: 1100-1199, III: 1200-1299)
- **Eventi:**     2001–2099  (Era I: 2001-2019, II: 2020-2039, III: 2040-2059, finals: 2090+)
- **Edifici:**    101–399    (Era I: 101-106, II: 201-207, III: 301-308)

Se un'immagine non è presente, la GUI mostra un placeholder testuale
(tipo carta + era + costo/punti per gli edifici): vedi
`it.polimi.it.view.gui.widgets.CardCellFactory`.

Le immagini vengono caricate **da classpath** (`getResourceAsStream`) e
mantenute in cache da `CardImageCache`. Dimensione consigliata: ~90×120 px,
sfondo trasparente (la cella ha già il colore di sfondo).
