# Aventura — Audit de performance

*Version 1 : 30 septembre 2026. **Version 2 : 6 octobre 2026**, mise à jour sur la branche `Refactoring`
(commit `d3a2a47`, tag `v2-3-0`). Périmètre : `src/main` (moteur, modèle, maths, vues), pur Java, rendu CPU.*

> **Ce qui change dans la version 2.** Depuis la version 1, le moteur a gagné les ombres des spots (shadow map en
> perspective) et des lumières ponctuelles (cube map à 6 faces), le clipping du plan proche, les ombres douces
> (PCF 3 × 3, désormais activées par défaut dans `UrbanScape`) et les lumières visibles (halo, soleil, lens flare,
> rayons de lumière). Le chemin par pixel du moteur, lui, n'a pas changé : **tous les constats de la version 1
> restent valables**. La version 2 ajoute le **coût mesuré des nouvelles fonctions**, de nouvelles propositions
> (N1 à N7) et l'**état d'avancement** de chaque proposition : voir le [§10](#10-mise-à-jour-du-6-octobre-2026--nouvelles-fonctions-et-état-davancement).
> Les §0 à §9 sont ceux de la version 1, complétés par des encadrés « **v2** » là où la situation a changé.

L'objectif est d'accélérer le moteur **sans le rendre illisible** : chaque proposition indique son gain (mesuré
quand c'était possible), son coût et son effet sur la lisibilité. Les identifiants (A1, D3, R5…) sont repris dans
la feuille de route du §8.

## Contenu

0. [Synthèse](#0-synthèse)
1. [Méthode et mesures](#1-méthode-et-mesures)
2. [Algorithmique (programmation)](#2-algorithmique-programmation)
3. [Conception (design)](#3-conception-design)
4. [Techniques de rendu : éliminer le travail en amont](#4-techniques-de-rendu--éliminer-le-travail-en-amont)
5. [Les trois dimensions : mémoire, CPU, E/S](#5-les-trois-dimensions--mémoire-cpu-es)
6. [Multithreading](#6-multithreading)
7. [Tracer et instrumentation](#7-tracer-et-instrumentation)
8. [Feuille de route proposée](#8-feuille-de-route-proposée)
9. [Ce qu'il vaut mieux ne pas faire](#9-ce-quil-vaut-mieux-ne-pas-faire)
10. [Mise à jour du 6 octobre 2026 : nouvelles fonctions et état d'avancement](#10-mise-à-jour-du-6-octobre-2026--nouvelles-fonctions-et-état-davancement)

---

## 0. Synthèse

**Le moteur est limité par le traitement des pixels, pas par la géométrie.** Dans `UrbanScape` (8 912 triangles,
1280 × 720), **96 %** du temps CPU est passé dans `TriangleRasterizer.rasterizeScanLine()` et les consumers
qu'il appelle. La transformation des sommets représente environ **1 %**. Le travail doit donc porter sur le
chemin par pixel, sur le nombre de pixels traités, puis sur la répartition de ce travail entre les cœurs.

Constats principaux :

1. **Environ 145 Mo alloués par image**, presque tout dans la boucle par pixel (`java.awt.Color`, `Vector3`,
   `Vector4`, `int[]` créé par `BufferedImage.setRGB()`). Le principe « zéro allocation par pixel » de DESIGN §5
   n'est vrai que pour l'objet `Fragment`.
2. **Z-buffer, shadow map et textures sont stockés en colonnes (`[x][y]`) mais parcourus en lignes** : chaque
   pixel d'une ligne tombe dans un tableau différent, ce qui provoque des défauts de cache.
3. **Les compteurs statiques des constructeurs de `Vector3` et `Vector4`** (`count()`, liés à `Tracer.object`)
   sont écrits à chaque allocation. Ils coûtent peu en mono-thread, mais **ils empêchent tout gain en
   multithread** : dans le prototype, 2 threads étaient *plus lents* qu'un seul tant qu'ils étaient là.
4. **Les ombres doublent le temps d'une image** : la shadow map (2000 × 2000 dans UrbanScape) est réallouée et
   recalculée à chaque image, alors qu'avec une lumière et une scène fixes elle ne change pas.
5. **Environ 46 % des triangles envoyés au rasterizer ne couvrent aucun centre de pixel**, et **1,18 million de
   pixels sont ombrés pour 0,92 million de pixels à l'écran** : il existe une marge côté élimination en amont.
6. Le **8 à 10 % d'utilisation CPU** que tu observes correspond exactement à **un seul cœur logique occupé**
   (1/12 ≈ 8 % sur une machine à 12 threads). Le multithreading est donc le levier le plus important, **mais
   seulement une fois les points 1 à 3 corrigés**.

Gains mesurés avec des prototypes (sur une copie jetable, pas dans ton dépôt) :

| Étape (cumulative) | Sans ombres | Avec ombres |
|---|---:|---:|
| Référence | 114 ms | 267 ms |
| + Z-buffer et shadow map en `float[]` 1D, ligne par ligne (D7) | 109 ms | 239 ms |
| + écriture directe dans le `int[]` de l'image (D6) | 98 ms | 229 ms |
| + `Matrix4 × Vector4` déroulé, couleur de lumière calculée une fois (A4, A2) | **84 ms** (−26 %) | 214 ms |
| + shadow map conservée d'une image à l'autre (scène statique, R9) | — | **≈ 137 ms** (−49 %) |
| Multithread : 2 threads, 8 bandes horizontales (§6), sans les compteurs statiques | **≈ 53 ms** contre ≈ 95 ms en mono-thread avec le même code | non testé |

Ces mesures ont été prises sur une machine à **2 cœurs** (Xeon 2,8 GHz, Java 21). Les valeurs absolues seront
différentes sur ton portable ; ce sont les tendances qui comptent. Sur 8 à 12 threads, un gain de **×3 à ×5** sur
l'étape pixels est réaliste après les phases 1 et 2.

> **v2 — synthèse au 6 octobre.** Les nouvelles fonctions ont **déplacé le coût vers les ombres** : avec PCF 3 × 3
> (défaut d'UrbanScape), une image coûte environ **3,4 fois** une image sans ombres (contre 2 fois en ombres dures).
> Une lumière ponctuelle ou un spot avec ombres ajoute chacun environ 60 % au temps de l'image. Les mêmes
> 4 gains rapides (D1, D6, D7, A4) donnent aujourd'hui **−13 à −19 % avec ombres** et divisent par 2 le coût des
> lumières visibles. Les nouvelles propositions les plus rentables sont le **gradient du PCF par triangle** (N1),
> l'**élimination, la réutilisation et le cache des passes d'ombre** (N4) et le **calcul unique de la direction et
> de la distance des lumières** (N3). Détails au [§10](#10-mise-à-jour-du-6-octobre-2026--nouvelles-fonctions-et-état-davancement).

---

## 1. Méthode et mesures

### Protocole

- Un benchmark sans interface (`PerfBench`, laissé dans `Claude outputs/perf-audit/`) rend la scène `UrbanScape`
  dans une `ImageView` pendant 40 à 80 images le long du vol d'hélicoptère. La moyenne est prise sur les
  dernières images, une fois le JIT chaud (les premières images prennent 700 à 1 000 ms).
- Deux configurations : `INTERPOLATE` + spéculaire, avec et sans ombres (shadow map 2000 × 2000).
- **v2** : `PerfBench2` (même dossier) ajoute les modes `hard`, `pcf`, `point`, `spot`, `sunview` et `sunglow`
  (§10.2).
- Profilage avec **Java Flight Recorder** (`-XX:StartFlightRecording`), échantillonnage CPU et allocations.
- Le benchmark calcule une **somme de contrôle de l'image** : le prototype multithread produit une image
  identique au bit près à la version mono-thread.

### Où va le temps

Sans ombres (1 169 échantillons) :

| Zone (temps inclusif) | Part |
|---|---:|
| `rasterizeScanLine()` et tout ce qu'il appelle (étape pixels) | 96 % |
| `ShadingConsumer.consume()` (éclairage par pixel) | 70 % |
| `ZBuffer.test()`, surtout des défauts de cache dans `MapView.get()` | 16 % |
| `BufferedImage.setRGB()` | 12 % |
| `ElementTransform` (sommets et normales) | ≈ 1 % |

Avec ombres (2 371 échantillons) : `generateShadowMap()` **30 %**, `shadowFactorAt()` **24 %** (dont la majeure
partie dans `Vector4.times(Matrix4)`, qui passe par `get(i)` et `set(i)` avec un `switch` et un `try`/`catch` pour
chaque coefficient), `setRGB()` 8 %.

### Allocations et GC

- Environ **11,7 Go alloués sur 80 images, soit ≈ 145 Mo par image**. Principaux sites d'allocation :
  `ColorTools.multColor()` (via `Light.getLightColorAtPoint()`, une nouvelle `Color` par pixel et par lumière)
  et le `float[]` interne de `Color`, 52 % à eux deux ; `DirectColorModel.getDataElements()` (un `int[]` par
  appel de `setRGB()`), 19 % ; `Vector4.V3()`, 17 %.
- Environ **1,5 GC jeune par image**, pour 4 ms de pause par image sans ombres et 9 ms avec. Les pauses restent
  modestes : le vrai coût vient des allocations elles-mêmes et de la pollution des caches qu'elles provoquent.

### Statistiques du pipeline, par image

- 8 912 triangles : 4 110 éliminés en back-face, 212 hors du frustum, **4 590 rasterisés**.
- Sur les triangles rasterisés, environ **46 % ne produisent aucun pixel** (compteurs `RasterizerStats` : 160 693
  triangles avec au moins une ligne, 86 413 avec au moins un pixel).
- 1,18 million de pixels ombrés et 236 000 rejetés par le Z-test, pour 921 600 pixels à l'écran (dont une partie
  de ciel). Il y a donc un surcoût d'ombrage dû à la superposition (overdraw).

### Pourquoi 8 à 10 % de CPU

Le rendu tourne sur un seul thread : le thread `main` dans `UrbanScape`, l'**EDT** Swing dans `MovingCamera` et
`FractalLandscape_MouseMoving`. Un thread qui travaille sans arrêt occupe un cœur logique. Sur une machine à
12 threads logiques, cela affiche 8,3 %. Le moteur est déjà à 100 % de ce qu'un seul thread peut donner.

---

## 2. Algorithmique (programmation)

Légende : **Gain** mesuré (m) ou estimé (e) · **Effort** S / M / L · **Lisibilité** + (meilleure), = (neutre), − (plus
technique).

### A1. Interpolation par pixel sur des objets → pas incrémentaux sur des `float`

*`TriangleRasterizer.rasterizeScanLine()`, l. 309-340*

- Pour chaque pixel : `Tools.interpolate(Vector4…)` crée **4 objets**, dont un `new Vector4()` qui est
  immédiatement écrasé. S'y ajoutent `.times(wPixel)`, `lerpV3()` (3 objets), un autre `.times()`, puis la même
  chose pour les coordonnées de texture. Au total, **jusqu'à 14 allocations par pixel**. L'escape analysis du JIT
  n'en supprime qu'une partie (JFR le confirme).
- `z = 1 / interpolate(1/z1, 1/z2, g)` recalcule `1/z1` et `1/z2` pour chaque pixel, alors que `z1` et `z2`
  viennent eux-mêmes d'une inversion.
- `xScreen()` et `yScreen()` refont la division `x/w` à chaque appel : 8 divisions par ligne et 6 ou plus par
  triangle, pour des valeurs qui ne changent pas.

**Proposition.** C'est la méthode classique des rasterizers logiciels, et elle est aussi très pédagogique :

- calculer **une fois par sommet** `xs`, `ys`, `invW = 1/w`, et les attributs pré-divisés `attr × invW` ;
- sur une ligne, calculer le **pas** de chaque grandeur (`d(invW)/dx`, `d(attr×invW)/dx`), puis avancer par
  **additions** : `invW += dInvW; u += du; …`. Les variables locales sont des `float` ;
- remplir `Fragment` avec des `set(float…)`, comme c'est déjà prévu.

Le code reste lisible si le calcul des pas est regroupé dans une petite classe `EdgeInterpolant` ou
`ScanlineSetup` avec un commentaire de 5 lignes (analogue GPU : les « plane equations » des attributs).
**Gain** e : important (supprime la majorité des allocations et divisions par pixel) · **Effort** M · **Lisibilité** =.

### A2. Éclairage par pixel : calculer une seule fois ce qui ne dépend pas de la lumière

*`ShadingConsumer.consume()` l. 79-101, `Lighting.accumulateContribution()` l. 212-240*

- `material.baseColorAt(fragment)` est appelé **1 + N fois par pixel** (ambiante, puis chaque lumière). Avec un
  `TexturedMaterial`, la texture est donc **échantillonnée et filtrée plusieurs fois** et une `Color` est créée à
  chaque fois (+ `ColorTools.multColors()`).
- `fragment.getNormal().normalize()` est refait pour chaque lumière, **et modifie le fragment en place** (voir D5).
- `viewerDirection` (2 allocations) est calculé même quand le spéculaire est désactivé.
- `light.getLightColorAtPoint()` crée une `Color` par pixel via `ColorTools.multColor()`, alors que pour une
  lumière directionnelle ou ambiante le résultat est **constant sur toute l'image**.
- L'ordre dans `consume()` : `shadowFactorAt()` (projection et lecture de la shadow map) est appelé **avant** le
  test `dotNL <= 0` de `accumulateContribution()`. Pour tous les pixels qui tournent le dos à la lumière, on
  consulte la shadow map pour rien (voir R10).

**Proposition.** Dans `consume()` : couleur de base une fois, normale normalisée une fois, direction de vue
seulement si le spéculaire est actif, `dotNL` avant l'ombre. Couleurs des lumières pré-multipliées **une fois
par image** (`Lighting.prepareFrame()`) en trois `float`. Accumuler en `float` et ne produire qu'un `int` RGB à la
fin (voir D2).
**Gain** m (partiel, couleur de lumière) : fait partie de la ligne 84 ms du tableau · **Effort** S · **Lisibilité** +.

### A3. Z-buffer : état caché et double adressage

*`ZBuffer.test()` / `update()`, l. 107-135*

`test()` mémorise `lastX`, `lastY`, `lastBx`, `lastBy` pour que `update()` évite de recalculer deux additions. Le
gain est nul, mais cet état partagé rend la classe **impossible à utiliser depuis plusieurs threads**.
**Proposition** : `test()` et `update()` sans état. L'accès 1D ligne par ligne est traité en D7.
**Gain** ≈ 0 en mono-thread, prérequis du §6 · **Effort** S · **Lisibilité** +.

### A4. Produit matrice × vecteur

*`Matrix4.times(Vector4)` l. 530 → `Vector4.times(Matrix4)` l. 615*

La boucle double utilise `get(i)` et `set(i, …)` (un `switch` et une exception vérifiée). C'est **24 % du temps
avec ombres**, parce que `shadowFactorAt()` projette chaque pixel dans l'espace de la lumière.
**Proposition** : 16 multiplications-additions écrites explicitement (4 lignes lisibles), et `Matrix4` stockée
dans un `float[16]` ou 16 champs plutôt qu'un `float[][]` (5 objets). Un `transform(Vector4 in, Vector4 out)`
sans allocation peut servir dans les boucles critiques.
**Gain** m (inclus dans la ligne 84 ms) · **Effort** S · **Lisibilité** + (le code déroulé est plus lisible que
la boucle actuelle).

### A5. Détails par triangle

- `sortByScreenY()` : `Arrays.sort` avec un `Comparator` créé par lambda et des `Float` en boîte, pour trier
  **3 éléments**. Trois comparaisons et échanges suffisent. **Effort** S.
- 3 `RasterVertex`, un tableau, un `Material` et un consumer sont alloués **par triangle** : environ 4 600 par
  image, peu coûteux, mais à revoir dans le cadre de D3.
- `ElementTransform.computeNormalMatrix()` teste `M3 · M3ᵀ` **par égalité exacte de float** avec l'identité. Une
  rotation réelle échoue presque toujours à ce test, si bien qu'une inversion 4 × 4 est calculée pour chaque
  élément à chaque image. Il faut comparer avec une tolérance et mettre le résultat en cache avec la
  transformation (D8). **Gain** faible (≈ 1 %).

### A6. Textures

*`Texture`, l. 70-207*

- Stockage `int[width][height]` (en colonnes) : un échantillon bilinéaire lit deux tableaux différents.
- `getInterpolatedColor()` renvoie une nouvelle `Color`, et `TexturedMaterial` en crée une seconde pour la
  teinte.
- Le chargement appelle `img.getRGB(w, h)` **pixel par pixel**, soit 2,2 millions d'appels pour la texture
  1706 × 1279 de `MovingCamera`. Ce n'est pas un coût par image, mais le démarrage est lent.
- **`Texture` contient un `RGBAccumulator` partagé** : deux threads qui échantillonnent la même texture se
  marchent dessus (voir §6).

**Proposition** : `int[]` stocké ligne par ligne, lecture en masse avec `img.getRGB(0, 0, w, h, buf, 0, w)`, et un
échantillonnage qui renvoie un `int` ARGB ou écrit trois `float` dans un accumulateur **fourni par l'appelant**.
**Effort** S · **Lisibilité** =.

### A7. Ombrage FLAT constant et vrai mode Gouraud

- En `FLAT` sans spéculaire, sans ombres et sans texture, la couleur est **la même pour tout le triangle** : on peut
  la calculer une fois et remplir le triangle.
- `INTERPOLATE` est décrit comme « Gouraud » dans le code, mais c'est en réalité du **Phong shading** (normale
  interpolée, éclairage calculé à chaque pixel). Un vrai mode `GOURAUD` (éclairage aux 3 sommets, couleurs
  interpolées) coûterait **une fraction** du prix. Il est aussi très intéressant sur le plan pédagogique :
  comparer Gouraud et Phong sur la même scène est un classique des cours de synthèse d'images.
**Gain** e : fort pour ces modes · **Effort** M · **Lisibilité** +.

---

## 3. Conception (design)

### D1. Compteurs statiques dans les constructeurs de vecteurs — **priorité 1**

*`Vector3.count()` l. 182, `Vector4.count()` l. 200*

Chaque `new Vector3` et `new Vector4` incrémente `nb_vectors` et `nb_to_display`, deux champs statiques publics.
Cela a trois conséquences :

- **un effet de bord global dans une classe mathématique** ;
- une **course de données** dès qu'il y a plusieurs threads (les compteurs deviennent faux) ;
- surtout, la **ligne de cache de ces champs fait des allers-retours entre les cœurs** à chaque allocation.
  Mesuré : avec 2 threads, **118 à 150 ms contre 91 ms avec 1 thread**. Après avoir conditionné le compteur à
  `Tracer.object` : **53 ms**.

**Proposition** : supprimer ces compteurs. Si le diagnostic est utile, le mettre derrière `if (Tracer.object)` ou
utiliser un `LongAdder`. C'est exactement le cas « Tracer devenu pénalisant » que tu soupçonnais, à ceci près que ce
n'est pas `Tracer` lui-même qui coûte, mais cette instrumentation qui s'exécute même quand la trace est
désactivée. **Effort** S · **Lisibilité** +.

> **v2 — toujours présent, et plus coûteux.** Les nouvelles fonctions créent encore plus de vecteurs par pixel
> (PCF : 2 projections supplémentaires, des produits vectoriels et des normalisations ; lumières ponctuelles et
> spots : un `new Vector3` à chaque calcul de direction ou de distance). Chacune de ces allocations écrit dans ces
> deux compteurs partagés. C'est toujours le **prérequis n° 1 du multithreading**.

### D2. `java.awt.Color` comme type de couleur interne

`Color` est immuable (chaque opération alloue), `getRGBColorComponents(null)` alloue un `float[]`, et le
constructeur `Color(float, float, float)` vérifie ses bornes à chaque appel. C'est **plus de la moitié des
allocations** mesurées. Cela lie aussi le modèle à AWT.
**Proposition** : dans le pipeline, les couleurs circulent en trois `float` (accumulateur) ou en `int` RGB compacté.
`Color` reste le type de l'**API publique** (`Element.setColor()`, lumières), converti **une fois** à l'entrée
(préparation de l'image ou du matériau). **Effort** M · **Lisibilité** = (les signatures publiques ne changent pas).

> **v2 — D2 est découpé en deux étapes.**
>
> **D2a — gain rapide (phase 1).** Deux changements locaux qui ne touchent aucune API publique :
> - la **couleur des lumières dont l'intensité ne dépend pas du point** (ambiante, directionnelle) est calculée une
>   seule fois au lieu d'un `ColorTools.multColor()` par pixel et par lumière. Dans la version finale, cette
>   préparation se fait au début de l'image (`Lighting.prepareFrame()`) plutôt que dans un cache paresseux, pour
>   respecter « Refresh, don't cache » et rester sûre en multithread ;
> - `RGBAccumulator.toRGB()` produit directement un `int` RGB, écrit par un nouveau
>   `GUIView.drawPixel(x, y, int rgb)` dans le `int[]` de l'image (complète D6). La conversion arrondit comme
>   `new Color(float, float, float)`, d'où une image identique au bit près.
>
> **Mesuré** (§10.3) : les allocations passent de **134 à 21 Mo par image**, il y a presque 3 fois moins de GC, et
> une image sans ombres coûte **≈ 30 % de moins** que l'image avec les seuls 4 gains rapides.
> **Effort** S · **Lisibilité** =.
>
> **D2b — refactoring (phase 2).** `Material`, `Texture` et `Light` fournissent leurs composantes en `float` (dans un
> accumulateur fourni par l'appelant) au lieu de renvoyer une `Color`. C'est nécessaire pour les **textures** (un
> échantillon bilinéaire produit aujourd'hui une ou deux `Color`) et pour les **lumières ponctuelles et spots**, dont
> la couleur change à chaque pixel avec l'atténuation, ce qu'aucun cache ne peut éviter. **Effort** M.

### D3. Matériaux et consumers créés pour chaque triangle

*`RenderEngine.rasterizeShadedTriangle()` l. 580-583, `rasterizeUnlitTriangle()` l. 524-527*

Le coût est modéré, mais la conception pose problème pour le multithreading : un consumer contient un accumulateur
modifiable et doit appartenir à un seul thread. **Proposition** : consumers **réutilisés, un par thread**
(`consumer.setMaterial(m)`), et matériau résolu **par élément** (ou mis en cache par combinaison élément ×
texture × couleur).

### D4. L'état du pipeline est stocké dans le modèle de la scène

`Vertex.prj_position`, `wld_position`, `wld_normal`, `prj_normal` et `Triangle.wld_normal` sont **écrits par chaque
passe**. La passe d'ombre écrase par exemple `prj_position` avec les coordonnées dans l'espace de la lumière
avant que la passe principale ne les réécrive. Conséquences :

- les passes doivent être strictement séquentielles (impossible de calculer deux shadow maps en parallèle, ni une
  shadow map pendant la passe principale) ;
- chaque `Vertex` porte 7 références vers des objets séparés (cache peu favorable) ;
- le même objet représente à la fois « la scène » et « le résultat d'une passe », ce qui est déroutant quand on
  découvre le code.

**Proposition** : chaque passe possède ses **tampons de sortie des sommets** (des `float[]` par élément, par
exemple `xs, ys, invW, …`). C'est l'analogue direct de la sortie du *vertex shader* sur GPU. `Vertex` redevient une
simple description de la scène. **Effort** L · **Lisibilité** + (sépare modèle et pipeline). C'est la condition pour
paralléliser la géométrie et les ombres.

### D5. API mathématique à moitié immuable

`plus()`, `minus()` et `times()` renvoient de nouveaux objets, mais **`Vector3.normalize()` modifie l'objet et se
renvoie lui-même**. Conséquences actuelles : `Lighting` normalise en place la normale du `Fragment`, et
`shadowFactorAt(worldPosition, normal)` normalise en place **la normale de l'appelant**. C'est à la fois un piège
de lisibilité et un risque de course de données.
**Proposition** : une convention explicite, `normalize()` qui renvoie un nouveau vecteur et `normalizeInPlace()`
(ou `…Equals()` comme `timesEquals()`) pour les rares chemins critiques. **Effort** S à M · **Lisibilité** +.

### D6. Framebuffer : `setRGB()` et une nouvelle image par image

*`ImageView.drawPixel()` l. 200, `initBack()` l. 106*

`BufferedImage.setRGB()` passe par le `ColorModel` et alloue un `int[]` à chaque appel (19 % des allocations).
`initBack()` alloue en plus une nouvelle image de 3,7 Mo par image (BACKLOG §6).
**Proposition** : récupérer une fois le `int[]` sous-jacent
(`((DataBufferInt) img.getRaster().getDataBuffer()).getData()`), écrire directement dedans, effacer avec
`Arrays.fill`, et garder un pool de 2 ou 3 images (triple buffering) comme prévu dans le backlog. Ajouter
`GUIView.drawPixel(int x, int y, int rgb)` à côté de la version `Color`.
**Gain** m : −11 ms · **Effort** S · **Lisibilité** =.

> **v2.** La nouvelle méthode `ImageView.addPixel()` (lumières visibles) évite bien l'allocation de `Color`, mais
> elle fait un `getRGB()` puis un `setRGB()` par pixel. Dans une vue tournée vers le soleil avec les rayons de
> lumière, ces deux appels représentent **41 % du temps de l'effet**, qui touche presque tout l'écran. Écrire dans le
> `int[]` corrige les deux méthodes à la fois (voir N6 au §10).

### D7. `MapView` en `float[][]` par colonnes → `FloatMap` en `float[]` 1D par lignes

*`MapView` l. 36, utilisé par `ZBuffer` et les shadow maps*

Le rasterizer parcourt les pixels ligne par ligne (x varie) alors que le stockage est `map[x][y]` : chaque pixel
accède à un tableau Java différent. Le `clear()` du Z-buffer parcourt aussi 1 281 petits tableaux.
**Proposition** : c'est l'occasion de réaliser **BACKLOG §2** (classe `FloatMap` hors du paquet `view`) avec un
`float[width * height]` et un index `y * width + x`, un `clear` par `Arrays.fill`, et l'échantillonnage bilinéaire
factorisé. `MapView` devient un adaptateur d'affichage.
**Gain** m : −5 ms sans ombres, −28 ms avec · **Effort** S à M · **Lisibilité** +.

### D8. Tout est recalculé à chaque image

`world.worldProject()`, matrices normales, `initShadowing()` et `generateShadowMap()` (avec une nouvelle `ZBuffer`
de 2001 × 2001 à chaque image), couleurs des lumières… sont recalculés même quand rien n'a bougé.

DESIGN §12 impose la règle « *Refresh, don't cache* », qui répond à de vrais bugs de cache périmé. **Ne pas la
contourner avec des caches improvisés.** Proposition : des **compteurs de version** explicites (`Element`,
`Light`, `World` incrémentent un numéro à chaque modification ; un résultat dérivé mémorise la version à partir de
laquelle il a été calculé). La règle devient « rafraîchir si la version a changé ».
**Gain** m : shadow map conservée, 214 → ≈ 137 ms · **Effort** M · **Lisibilité** = si c'est fait de façon uniforme.

### D9. Géométrie et rasterisation entrelacées → deux étages

Aujourd'hui, `render(Element)` transforme un élément puis rasterise aussitôt ses triangles, de façon récursive.
Plusieurs techniques des §4 et §6 (tri avant → arrière, Z-prepass, répartition par tuiles, multithreading) ont
besoin d'un pipeline **en deux étages** :

1. **Étage géométrie** : transformation, élimination (frustum, back-face, petits triangles), clipping, puis
   production d'une **liste de triangles prêts** (coordonnées écran, `1/w`, attributs, matériau) ;
2. **Étage rasterisation** : parcours de cette liste (séquentiel, par bandes ou par tuiles).

C'est le découpage « vertex processing → primitive assembly → rasterization » d'un GPU, ce qui en fait un
**gain pédagogique** autant qu'un gain de performance. **Effort** M à L · **Lisibilité** +.

### D10. Rendu sur l'EDT dans deux démos

`MovingCamera` et `FractalLandscape_MouseMoving` appellent `renderer.render()` depuis des `Timer` Swing et des
écouteurs souris, donc **sur le thread de l'interface**. Pendant un rendu de 100 à 300 ms, la fenêtre ne répond pas
et les événements s'accumulent. `UrbanScape` fait déjà le rendu dans sa propre boucle.
**Proposition** : une boucle de rendu dédiée (ou un `SwingWorker`) qui ne rend que le **dernier** état de caméra
demandé. **Effort** S · **Lisibilité** +.

> **v2.** `SceneViewer` (code de test) montre le bon modèle : un thread de rendu dédié qui lit le dernier état
> demandé. `MovingCamera` et `FractalLandscape_MouseMoving` rendent toujours sur l'EDT : il suffit de reprendre ce
> modèle.

---

## 4. Techniques de rendu : éliminer le travail en amont

L'idée est d'écarter le plus tôt possible ce qui ne sera pas visible. Du moins coûteux au plus coûteux :
**élément → triangle → bloc de pixels → pixel**. Aventura ne fait aujourd'hui que le back-face culling (et
seulement pour les éléments `isClosed`) et un test de frustum par triangle.

### R1. Frustum culling hiérarchique par `Element` (volumes englobants)

Calculer au `build()` une **sphère englobante** (ou une AABB) par élément, qui couvre aussi ses sous-éléments. À
chaque image, la tester contre les 6 plans du frustum. On peut extraire ces plans directement de la matrice
`P · V` (méthode de Gribb et Hartmann, une dizaine de lignes).

- **Entièrement dehors** : on saute la transformation des sommets, tous les triangles et tous les sous-éléments.
- **Entièrement dedans** : on saute le test de frustum et le clipping de chacun de ses triangles.

Dans UrbanScape (toute la ville est à l'écran), le gain est faible. Il est **important** dès que la caméra ne
voit qu'une partie de la scène (zoom dans `FractalLandscape`, `MovingCamera`). La même méthode s'applique à la
passe d'ombre, dans l'espace de la lumière. **Effort** M · **Lisibilité** +.

### R2. Test de frustum par outcodes, et clipping du plan proche (correction et performance)

*`Triangle.isInViewFrustum()` l. 344, `Vertex.isInViewFrustum()` l. 203*

Le test actuel (« au moins un sommet dans le frustum ») est **faux dans les deux sens** :

- il **rejette** un grand triangle dont les 3 sommets sont hors de l'écran mais qui le traverse (un sol vu de près)
  → trous ;
- il **accepte** un triangle dont un sommet est derrière la caméra (`w < 0`). La division par `w` produit alors
  des coordonnées écran absurdes (le commentaire de `ScreenLineRenderer` le reconnaît : « no clipping »).

De plus, il fait 3 divisions par sommet, **refaites pour chaque triangle** qui partage ce sommet (environ 6 fois).

**Proposition** : un **outcode** de 6 bits par sommet, calculé **une fois** en espace clip et sans division
(`x < -w`, `x > w`, …) :

- `oc1 & oc2 & oc3 != 0` → entièrement d'un même côté d'un plan : **rejet** ;
- `oc1 | oc2 | oc3 == 0` → entièrement dedans : **pas de clipping** ;
- sinon, clipper **uniquement contre le plan proche** (Sutherland-Hodgman, 0 à 2 triangles en sortie). Les autres
  bords sont déjà gérés par le rasterizer, qui limite lignes et colonnes à l'écran (principe de la « guard band »).

**Effort** M · **Lisibilité** + (un algorithme classique, bien documenté).

> **v2 — partiellement fait.** `NearPlaneClipper` (octobre 2026) clippe désormais les triangles contre le plan proche
> en perspective, dans la passe principale et dans les shadow maps des spots et des lumières ponctuelles : les
> triangles derrière la caméra ne produisent plus de coordonnées absurdes. Restent à faire : le **test de frustum
> par outcodes** (`Triangle.isInViewFrustum()` est inchangé, donc les grands triangles qui traversent l'écran avec
> leurs 3 sommets dehors sont toujours rejetés), et un **chemin rapide** dans le clipper, qui alloue aujourd'hui
> 5 à 7 objets pour chaque triangle, même entièrement devant le plan proche (voir N5 au §10).

### R3. Back-face culling dans l'espace écran

*`RenderEngine.isBackFace()` l. 606*

Il suffit de calculer le **signe de l'aire du triangle projeté** (produit vectoriel 2D des arêtes en coordonnées
écran). C'est le test des GPU :

- exact, et **indépendant du type de projection** (plus de `switch FRUSTUM / ORTHOGRAPHIC`) ;
- aucune normale à transformer (aujourd'hui, `transformNormal()` et `projectNormal()` ne sont faits que pour ce
  test en mode `INTERPOLATE`) ;
- une aire nulle élimine au passage les triangles **dégénérés**.

Pour les éléments ouverts, le drapeau `Triangle.rectoVerso`, déjà présent mais inutilisé (BACKLOG §5), dirait
« ne pas éliminer ».
⚠ **Prérequis** : ce test repose sur **l'ordre des sommets (winding)**. Il faut d'abord corriger l'enroulement de
`Sphere` (BACKLOG §5) et vérifier les autres formes avec `ElementContractChecker`. **Effort** S (plus la correction
de `Sphere`) · **Lisibilité** +.

### R4. Petits triangles

46 % des triangles rasterisés ne couvrent aucun centre de pixel. Or chacun paie aujourd'hui le tri, 3
`RasterVertex`, un `Material`, un consumer et la préparation des lignes.
**Proposition** : dès que les coordonnées écran sont connues, tester si la boîte englobante contient au moins un
centre de pixel (`ceil(minX) < ceil(maxX)` et de même en y). Si ce n'est pas le cas, on rejette le triangle avant
toute préparation. Plus tard, en option : un **niveau de détail (LOD)** qui réduit le nombre de segments des
sphères et cylindres lointains. **Effort** S · **Lisibilité** =.

### R5. Ordre avant → arrière

Le Z-test a lieu **avant** l'ombrage (bon point de conception). Si les éléments proches sont dessinés en premier,
les pixels cachés derrière eux sont rejetés **avant** d'être ombrés. Il suffit de trier les ~900 éléments par
distance de leur sphère englobante à la caméra (coût négligeable). Avec 1,18 million de pixels ombrés pour au
plus 0,92 million visibles, on peut gagner **jusqu'à 20 à 30 % de l'ombrage**. **Effort** S (après R1) ·
**Lisibilité** =.

### R6. Z-prepass ou « visibility buffer »

C'est la version radicale de R5 : **chaque pixel visible n'est ombré qu'une seule fois**.

1. Passe 1 : rasterisation **profondeur seule** (le `DepthOnlyConsumer` existe déjà) de toute la scène.
   Variante : écrire aussi l'**identifiant du triangle** dans un tampon `int[]`.
2. Passe 2 : ombrer uniquement les pixels dont la profondeur est égale à celle du Z-buffer. Avec la variante,
   on parcourt simplement l'écran et on ombre chaque pixel à partir de son triangle.

C'est rentable quand l'ombrage coûte cher (ombres, plusieurs lumières, textures), ce qui est le cas ici. La
variante « visibility buffer » est en plus **parfaitement parallélisable** (chaque pixel est indépendant). Analogue
GPU : deferred shading. **Effort** M à L · **Lisibilité** = à + (deux passes clairement nommées).

### R7. Occlusion culling (Hi-Z)

On tient un **Z-buffer grossier** (profondeur maximale par tuile de 8 × 8 pixels). Avant de traiter un élément, on
compare sa boîte à l'écran et sa profondeur minimale au Hi-Z : si tout ce qu'il couvre est déjà plus proche, on
saute l'élément entier. UrbanScape s'y prête parfaitement (immeubles cachés derrière d'autres immeubles). Avancé,
à faire après R1 et R5. **Effort** L.

### R8. Et le stencil ?

Le **stencil buffer** est un masque entier par pixel qui **limite l'endroit où l'on dessine** : miroirs, portails,
ombres par volumes (shadow volumes), contours, décalques. Il **ne détermine pas la visibilité** à lui seul. Pour
de la géométrie opaque, les outils d'élimination précoce sont ceux de R1 à R7. Dans Aventura, le stencil n'est
donc pas un levier de performance. Il deviendrait utile pour une fonctionnalité comme les shadow volumes ou le
dessin de contours en `MONOCHROME`.
Son cousin, le **scissor test** (restreindre le dessin à un rectangle), est en revanche exactement ce dont le
multithreading par tuiles a besoin (§6).

### R9. Passe d'ombre

- **Conserver la shadow map** tant que la lumière et la scène ne changent pas (D8). Avec `SHADOWING_BOX_WORLD`,
  elle ne dépend pas de la caméra. Mesuré : **214 → ≈ 137 ms**.
- **Réutiliser le `ZBuffer`** au lieu d'en allouer un par image (16 Mo pour 2000 × 2000).
- Éliminer par élément dans l'espace de la lumière (R1), et **éliminer les faces avant** (ne dessiner que les faces
  arrière). C'est une technique connue qui divise le nombre de triangles et réduit l'acné d'ombre.
- **Résolution** : 2000² = 4 millions de texels rasterisés par image. `SHADOWING_BOX_VIEWFRUSTUM`, une boîte plus
  serrée, donne la même netteté avec moins de texels.
- Note de qualité : `shadowFactorAt()` interpole bilinéairement les **profondeurs**, ce qui n'est pas du PCF (qui
  interpole les **résultats des comparaisons**, BACKLOG §3).

> **v2.** Le PCF est maintenant implémenté correctement (pondération des comparaisons, `ShadowFilter.PCF_3X3`).
> L'enjeu de cette section est **multiplié** : il y a maintenant jusqu'à trois types de shadow maps (orthographique,
> perspective, cube map à 6 faces), et chaque `PointLight` peut coûter 6 passes d'ombre par image. Aucune des
> propositions ci-dessus n'est encore faite. Voir N4 au §10.

### R10. Sortir tôt de l'éclairage

Tester `dotNL <= 0` **avant** `shadowFactorAt()` : un pixel qui tourne le dos à la lumière n'est pas éclairé par
elle, qu'il soit dans l'ombre ou non. On économise une projection et une lecture bilinéaire de la shadow map pour
environ la moitié des pixels. **Effort** S · **Lisibilité** +.

> **v2 — mesuré, et estimation corrigée.** Le prototype (test de `dotNL` puis de l'intensité de la lumière avant
> `shadowFactorAt()`, image identique au bit près) ne donne **pas de gain mesurable sur UrbanScape** : le back-face
> culling a déjà retiré les faces tournées vers l'arrière, et le soleil éclaire la plupart des faces visibles.
> « La moitié des pixels » était trop optimiste pour cette scène. R10 reste juste et peu coûteux à écrire, mais son
> gain dépend de la scène : il devient important pour un **spot étroit** ou une **lumière ponctuelle de faible
> portée**, dont la plupart des pixels sont hors du cône ou hors de portée (voir N2 au §10).

---

## 5. Les trois dimensions : mémoire, CPU, E/S

### Mémoire

| Sujet | Constat | Action |
|---|---|---|
| Taux d'allocation | ≈ 145 Mo par image, 1,5 GC par image | A1, A2, D2, D6 |
| Disposition des données | `float[][]` et `int[][]` en colonnes, parcourus en lignes | D7, A6 |
| Grosses allocations par image | shadow map 16 Mo, image 3,7 Mo | R9, D6 |
| Enchaînement de pointeurs | `Vertex` → 7 objets `Vector` ; `Matrix4` → 5 objets | D4, A4 |
| En-têtes d'objets | un `Vector4` pèse 32 octets pour 16 octets de données | à accepter dans le modèle ; éviter dans les boucles critiques |

Options JVM : fixer `-Xms` = `-Xmx` évite l'agrandissement du tas au démarrage, et `-XX:+UseParallelGC` favorise
le débit. C'est du réglage d'appoint : **la vraie solution est d'allouer moins**. L'escape analysis du JIT aide,
mais ne suffit pas à rendre ces allocations gratuites (JFR l'a montré).

### CPU

- **Un seul cœur utilisé** (§1). Le levier principal est le §6.
- Points chauds : ombrage (A2, R10), Z-test (D7), écriture des pixels (D6), mathématiques des ombres (A4),
  génération de la shadow map (R9).
- **Préchauffage du JIT** : les 2 ou 3 premières images sont 3 à 7 fois plus lentes. C'est normal, mais il faut en
  tenir compte dans toute mesure.
- Le calcul du FPS dans `RenderEngine.render()` utilise `currentTimeMillis()`. `System.nanoTime()` est plus
  précis, et un **découpage par phase** serait plus utile (§7).

### E/S

**Il n'y a pas d'entrée/sortie dans la boucle de rendu**, ce n'est donc pas un goulot d'étranglement. Quelques
points secondaires :

- `Tracer.stats` affiche une ligne à chaque image (`System.out` est synchronisé, et la console Windows est lente).
  C'est négligeable, mais ce serait mieux sous forme de moyenne affichée toutes les N images.
- Chargement des textures : lecture pixel par pixel (A6) et chemins relatifs au répertoire courant (roadmap :
  chargement depuis le classpath). Cela ralentit le démarrage, pas le rendu.
- Trace vers un fichier : `Tracer.output()` écrit ligne par ligne dans un `FileOutputStream` sans tampon. Un
  `BufferedWriter` suffit.
- Enregistrement d'images (`ImageView.saveImage()`) : correct. Pour une future séquence d'images (BACKLOG §6),
  écrire les fichiers **dans un thread séparé** pour ne pas bloquer le rendu.

---

## 6. Multithreading

### Pourquoi ne pas rasteriser « un triangle par thread »

Sur un GPU, des milliers de triangles sont rasterisés en parallèle, mais **l'écriture dans le Z-buffer et le
framebuffer passe par des unités matérielles (ROP) qui garantissent l'ordre et l'atomicité**. Sur CPU, deux
threads qui rasterisent deux triangles qui se chevauchent se disputent les mêmes pixels. Il faudrait alors :

- un verrou ou une opération atomique **par pixel** (bien trop cher) ;
- et accepter une image **non déterministe** (à profondeur égale, le gagnant dépend du hasard), ce qui casserait
  les tests.

### La solution classique : découper l'écran

Chaque thread **possède une zone de l'écran** (sa part du Z-buffer et du framebuffer) et y traite **tous les
triangles qui la touchent, dans l'ordre d'origine**. Cette organisation s'appelle *sort-middle*. Elle apporte :

- **aucune synchronisation par pixel** ;
- une image **identique au bit près** à la version mono-thread (vérifié par somme de contrôle dans le
  prototype), donc les tests existants restent valables.

Deux niveaux :

1. **Bandes de lignes** (le plus simple avec un rasterizer par lignes). Il suffit de passer un intervalle
   `[rowMin, rowMax)` à `TriangleRasterizer`, qui limite déjà les lignes à l'écran. C'est ce qu'a fait le
   prototype : **environ 40 lignes de code**.
2. **Tuiles** (par exemple 64 × 64) avec une étape de **répartition (binning)** : la boîte englobante de chaque
   triangle détermine la liste des tuiles concernées. Les données d'une tuile tiennent dans le cache L2, la
   répartition de la charge est meilleure, et c'est le modèle des GPU mobiles. À faire après D9.

### Répartir la charge

Le prototype le montre clairement :

| Configuration (même code, 2 cœurs) | Temps par image |
|---|---:|
| 1 thread | ≈ 95 ms |
| 2 threads, **2 bandes** | ≈ 100 ms (aucun gain : la bande du haut, c'est surtout du ciel) |
| 2 threads, **8 bandes** | **≈ 53 ms** (×1,8) |
| 2 threads, 8 bandes, **avec les compteurs statiques de D1** | 118 à 150 ms (plus lent qu'un seul thread) |

Règle : **plus de bandes ou de tuiles que de threads** (4 à 8 fois plus), distribuées **dynamiquement** par la
file du pool, de sorte qu'un thread qui a fini prend la zone suivante.

### Contrôler le nombre de threads (ton idée de Factory)

- **Un pool de taille fixe, créé une seule fois** (pas à chaque image), qui appartient au `RenderEngine` et dont
  la taille se configure dans `RenderContext` (`setRenderThreads(n)`). Valeur par défaut raisonnable :
  `Runtime.getRuntime().availableProcessors() - 1`, pour laisser un cœur à l'interface. Ajouter une méthode
  `close()` ou `shutdown()`.
- Une **`ThreadFactory`** pour nommer les threads (`aventura-raster-3`, lisible dans un profileur) et en faire des
  démons.
- Une **factory de workers** (`RasterWorker`) : chaque worker possède **son** `TriangleRasterizer`, **son**
  `Fragment`, **ses** consumers et **ses** statistiques, fusionnées à la fin de l'image. C'est là que le pattern
  Factory prend tout son sens : on fabrique un contexte d'exécution par thread, pas les threads eux-mêmes.
- `ExecutorService.invokeAll(tâches)` sert de **barrière** en fin d'image : simple et lisible. `ForkJoinPool`
  n'apporte rien de plus ici.
- À éviter : les **threads virtuels** (ils sont faits pour les E/S bloquantes, pas pour le calcul) et les
  `parallelStream()` dans le moteur (moins de contrôle et moins pédagogique).

### Prérequis : l'état partagé repéré dans le code

| État partagé et modifiable | Où | Correction |
|---|---|---|
| Compteurs statiques des vecteurs | `Vector3`, `Vector4` | D1 |
| `lastX/lastY/lastBx/lastBy` | `ZBuffer` | A3 |
| `RGBAccumulator` interne | `Texture` | A6 : accumulateur fourni par l'appelant |
| Compteurs et `Fragment` | `TriangleRasterizer` | une instance par worker |
| `RGBAccumulator` | `ShadingConsumer` | un consumer par worker (D3) |
| `RasterizerStats` | `RenderEngine` | statistiques par worker, fusionnées |
| `normalize()` en place sur des vecteurs partagés | `Lighting`, `ShadowingLight` | D5 |
| `prj_position` et autres dans `Vertex` | passes géométrie et ombre | barrière entre les étages (D9), puis D4 |

⚠ **Faux partage (false sharing)** : les objets d'un worker doivent être **créés dans le thread du worker** (pour
qu'ils ne soient pas côte à côte en mémoire avec ceux des autres workers), et les compteurs doivent être des
variables locales ajoutées au total en fin de tâche.

### Autres parallélisations possibles, par ordre de rentabilité

1. **Étape pixels par bandes ou tuiles**, décrite plus haut : environ 95 % du temps.
2. **Ombrage d'un visibility buffer** (R6) : chaque pixel est indépendant.
3. **Shadow maps** : une tâche par lumière (après D4), et chaque shadow map elle-même par bandes dans l'espace
   de la lumière.
4. **Étage géométrie** : un élément par tâche. Gain faible ici (≈ 1 % du temps), mais utile pour les scènes très
   détaillées.
5. **Recouvrement entre images** : rendre l'image N+1 pendant que l'interface affiche l'image N (avec le pool
   d'images de D6).

> **v2.** Deux nouvelles sources de parallélisme faciles :
> - les **6 faces de la cube map** d'une `PointLight` sont 6 rendus indépendants, de même que les shadow maps de
>   plusieurs lumières. Il faut d'abord que chaque passe ait ses propres sorties de sommets (D4), car toutes
>   écrivent aujourd'hui dans `Vertex.prj_position` ;
> - le **post-traitement des lumières visibles** (`LightGlowRenderer` : halo, rayons de lumière) travaille pixel
>   par pixel sur l'image terminée, et se découpe donc en bandes sans aucune difficulté.

Sur une machine à 8 à 12 threads logiques, la loi d'Amdahl donne un maximum d'environ ×6 à ×8 pour 95 % de code
parallèle. En pratique, la bande passante mémoire, l'hyperthreading et le déséquilibre de charge ramènent cela à
**×3 à ×5**. Tu verras alors le moteur utiliser 60 à 90 % du CPU au lieu de 8 %.

---

## 7. Tracer et instrumentation

- **`Tracer` lui-même ne coûte rien de mesurable** : les tests `if (Tracer.info)` sont des lectures de champs
  statiques bien prédites, et aucune fonction de `Tracer` n'apparaît dans les profils. Les rendre `static final`
  permettrait au compilateur de les supprimer, mais on perdrait la configuration à l'exécution. **Ce n'est pas
  nécessaire.**
- **Le vrai coût lié à la trace est D1** (les compteurs de `Vector3` et `Vector4`, qui s'exécutent même quand
  `Tracer.object` est désactivé).
- À ajouter plutôt : un **chronométrage par phase** dans `RenderEngine` (effacement, ombres, géométrie,
  rasterisation, présentation) avec `System.nanoTime()`, exposé dans `renderStats()` avec une moyenne glissante.
  Tu auras ainsi en permanence, sans profileur, la réponse à la question « où part le temps ? ».
- Faire du **benchmark** un outil du projet : `PerfBench` (dans `src/test/java/…/demo`, en tant que programme et
  non test unitaire), avec la somme de contrôle de l'image comme test de non-régression visuelle pour chaque
  optimisation.

---

## 8. Feuille de route proposée

### Phase 0 — Mesurer (½ journée)

- `PerfBench` et somme de contrôle de l'image dans le dépôt ; chronométrage par phase (§7).
- Mesurer la référence sur **ton** portable (sans ombres et avec ombres, 1 thread).

### Phase 1 — Gains rapides, risque faible, lisibilité égale ou meilleure

| # | Action | Gain attendu |
|---|---|---|
| D1 | Supprimer les compteurs statiques de `Vector3` et `Vector4` | prérequis du multithreading |
| D7 | `FloatMap` en `float[]` 1D ligne par ligne (réalise BACKLOG §2) | −5 à −28 ms (m) |
| D6 | Écriture directe dans le `int[]` de l'image, pool d'images (BACKLOG §6) | −11 ms (m) |
| A4 | `Matrix4` déroulée, `float[16]` | inclus dans −14 ms (m) |
| A2, R10 | Éclairage : valeurs communes calculées une fois, `dotNL` avant l'ombre | fort avec ombres ou textures (e) |
| R9 | Réutiliser le Z-buffer des ombres | faible à moyen |
| R4 | Rejeter tôt les petits triangles | moyen (e) |
| A6 | Textures : `int[]` par lignes, chargement en masse, sans état partagé | moyen avec textures (e) |
| A3, A5 | `ZBuffer` sans état, tri de 3 sommets | faible |
| D10 | Plus de rendu sur l'EDT dans les démos | réactivité |

### Phase 2 — Restructurer le pipeline (conditions pour la suite)

- **D9** : étage géométrie → liste de triangles prêts → étage rasterisation.
- **R2** : outcodes et clipping du plan proche (corrige aussi les triangles derrière la caméra).
- **R3** : back-face culling dans l'espace écran (après la correction de l'enroulement de `Sphere`).
- **R1**, **R5** : volumes englobants, élimination par élément, tri avant → arrière.
- **A1** : interpolation incrémentale sur des `float`.
- **D2** : couleurs internes en `float` ou `int`. **D8** : compteurs de version (conservation de la shadow map).

### Phase 3 — Multithreading

- Pool de threads, factory de workers, bandes (4 à 8 fois plus que de threads), statistiques fusionnées.
- Puis tuiles avec binning, et shadow maps en parallèle (avec **D4**).

### Phase 4 — Optionnel et avancé (à choisir selon l'intérêt pédagogique)

- **R6** Z-prepass ou visibility buffer · **R7** occlusion Hi-Z · **A7** vrai mode Gouraud · LOD · mipmapping.

Chaque étape se valide avec : image identique (somme de contrôle), puis gain mesuré. Si une étape n'apporte pas de
gain mesurable, on ne la garde pas.

---

## 9. Ce qu'il vaut mieux ne pas faire

Pour rester dans l'esprit d'un projet pédagogique :

- **Vector API** (`jdk.incubator.vector`, SIMD) : encore en incubation, verbeux, et le gain est incertain sur ce
  code.
- `Unsafe`, mémoire hors du tas, calcul en virgule fixe, déroulage manuel généralisé.
- Passer **toute** la scène en structures de tableaux (`float[]` partout). Il vaut mieux garder les objets
  (`Element`, `Vertex`, `Triangle`) comme **description de la scène**, et n'utiliser des tableaux de primitives que
  dans les structures traversées à chaque pixel (Z-buffer, framebuffer, textures, sorties de passes).
- Des caches improvisés qui contournent la règle « Refresh, don't cache » : utiliser des versions (D8).

Principe directeur : **optimiser les chemins de données, pas les concepts**. `Fragment`, `FragmentConsumer`,
`Material`, `TriangleRasterizer` et les deux contextes gardent leur rôle. Ce qui change, c'est *comment* les
données circulent à l'intérieur. Chaque optimisation mérite un commentaire qui donne son équivalent GPU : c'est
là que le projet reste didactique.

---

## 10. Mise à jour du 6 octobre 2026 : nouvelles fonctions et état d'avancement

### 10.1 Ce qui a changé dans le code

Entre la version 1 (commit `6d7a016`) et la branche `Refactoring` actuelle (`d3a2a47`), les commits concernant le
moteur sont :

- **shadow maps en perspective** (`ShadowingLight` généralisé) et **ombres des spots** ;
- **ombres des lumières ponctuelles** : cube map à 6 faces (`PointLight`, 512 × 512 par face par défaut). Seules
  les faces qui voient une partie de la scène sont calculées ;
- **clipping du plan proche** (`NearPlaneClipper`), dans la passe principale et dans les passes d'ombre en
  perspective ;
- **ombres douces PCF 3 × 3** (`ShadowFilter`), avec un *receiver plane depth bias* calculé à chaque pixel. Elles
  sont activées par défaut pour le soleil d'`UrbanScape` ;
- **lumières visibles** (`LightGlowRenderer`) : halo des lumières ponctuelles et des spots, disque et halo du
  soleil, lens flare, rayons de lumière (*light shafts*), en post-traitement sur l'image terminée ;
- le reste du diff (environ 100 fichiers) est l'en-tête de licence et la documentation.

Le **chemin par pixel de la passe principale n'a pas changé** : sans ombres, l'image produite est identique au bit
près à celle de la version 1, et le temps est le même. Aucune des propositions des phases 1 à 3 n'a encore été
réalisée, à part le clipping du plan proche (R2, en partie).

### 10.2 Mesures

Même benchmark qu'au §1 (`UrbanScape`, 1280 × 720, 8 912 triangles, `INTERPOLATE` + spéculaire). Médianes de 2 ou 3
exécutions alternées, 40 images, sur 2 cœurs.

⚠ La machine de test était **environ 1,8 fois plus lente** que le 30 septembre, et plus bruitée (±10 %) : l'ancien
code, mesuré à nouveau dans la même session, donne 214 ms sans ombres et 413 ms avec. Comparer les colonnes
**entre elles**, pas avec les chiffres du §0.

| Configuration | Temps par image | Par rapport à « sans ombres » |
|---|---:|---:|
| Version 1, sans ombres | 214 ms | — |
| **Actuel, sans ombres** | **≈ 210 ms** | ×1 (image identique) |
| Version 1, soleil avec ombres | 413 ms | ×1,9 |
| Actuel, soleil, ombres dures (`HARD`) | ≈ 430 ms | ×2,1 |
| **Actuel, soleil, PCF 3 × 3** (défaut d'`UrbanScape`) | **≈ 720 ms** | **×3,4** (+65 % par rapport à `HARD`) |
| Actuel, soleil `HARD` + **1 lumière ponctuelle** avec ombres | ≈ 680 ms | ×3,2 (+250 ms) |
| Actuel, soleil `HARD` + **1 spot** avec ombres | ≈ 680 ms | ×3,2 (+250 ms) |
| Vue tournée vers le soleil, sans lumières visibles | ≈ 40 ms | — |
| La même vue, avec halo, disque et rayons de lumière | ≈ 120 ms | +80 ms pour l'effet |

Où va le temps (JFR, part des échantillons) :

| Configuration | Passes d'ombre | `shadowFactorAt()` | Autres points notables |
|---|---:|---:|---|
| Soleil `HARD` | ≈ 30 % | 34 % | écriture de la shadow map (`MapView.set`) en tête des méthodes les plus chères |
| Soleil PCF 3 × 3 | ≈ 15-20 % (même coût absolu qu'en `HARD`) | **59 %** | dont **23 %** pour les 2 projections supplémentaires par pixel (`posInLightSpace1`) |
| + lumière ponctuelle | ≈ 26 % | 43 % | jusqu'à 6 passes d'ombre ; direction et distance de la lumière recalculées plusieurs fois par pixel |
| + spot | ≈ 36 % | 42 % | la shadow map est consultée même pour les pixels hors du cône |
| Lumières visibles | — | — | rayons de lumière **73 %** de l'effet, `addPixel()` (via `getRGB`/`setRGB`) **41 %** |

### 10.3 Gains rapides, mesurés à nouveau sur le code actuel

Prototype sur une copie jetable : **D1** (compteurs), **D7** (`MapView` en `float[]` 1D), **D6** (`int[]` direct,
y compris dans `addPixel()`) et **A4** (`Matrix4 × Vector4` déroulé). Les images sont identiques au bit près dans
les six configurations.

| Configuration | Actuel | Avec les 4 gains rapides | Gain |
|---|---:|---:|---:|
| Sans ombres | ≈ 218 ms | ≈ 207 ms | −5 % (dans le bruit ce jour-là ; −26 % le 30 septembre avec A2 en plus) |
| Soleil `HARD` | ≈ 427 ms | ≈ 373 ms | −13 % |
| Soleil PCF 3 × 3 | ≈ 772 ms | ≈ 625 ms | **−19 %** |
| + lumière ponctuelle | ≈ 665 ms | ≈ 557 ms | −16 % |
| + spot | ≈ 695 ms | ≈ 650 ms | −7 % (mesure très bruitée) |
| Vue vers le soleil avec lumières visibles | ≈ 118 ms | ≈ 79 ms | −33 % (le coût de l'effet est divisé par 2) |

Ces quatre changements sont petits, sans effet sur la lisibilité, et ils profitent davantage aux nouvelles
fonctions qu'à l'ancienne passe principale.

#### Avec D2a en plus (couleurs des lumières préparées une fois, accumulateur → `int` direct)

Mesuré dans une seconde série alternée (même jour, mêmes réserves sur le bruit), images toujours identiques au bit
près :

| Configuration | Actuel | 4 gains rapides | **+ D2a** | Gain total |
|---|---:|---:|---:|---:|
| Sans ombres | ≈ 222 ms | ≈ 173 ms | **≈ 122 ms** | **−45 %** |
| Soleil PCF 3 × 3 | ≈ 659 ms | ≈ 581 ms | ≈ 557 ms | −15 % |
| + lumière ponctuelle | ≈ 677 ms | ≈ 616 ms | ≈ 532 ms | −21 % |

| Allocations, sans ombres | 4 gains rapides | + D2a |
|---|---:|---:|
| Mémoire allouée par image | ≈ 134 Mo | **≈ 21 Mo** |
| dont `Color` et leurs `float[]` | ≈ 87 Mo | ≈ 1 Mo |
| Nombre de GC (40 images) | 47 | 17 |

Lecture :

- dans cette série, les 4 gains rapides donnent −22 % sans ombres. Le −5 % du tableau précédent était dû au bruit
  de la machine ;
- **D2a est le gain rapide le plus rentable de tous** sur la passe principale : il supprime presque toutes les
  allocations restantes ;
- avec PCF, le gain de D2a est plus faible (−4 %) : le temps y est dominé par `shadowFactorAt()` (N1). Avec une
  lumière ponctuelle, il reste −14 %, parce que la lumière ambiante et le soleil n'allouent plus rien. La couleur
  atténuée de la lumière ponctuelle, elle, est toujours recréée à chaque pixel, ce que corrigera D2b.

### 10.4 Nouvelles propositions

#### N1. PCF : calculer le plan du récepteur une fois par triangle, pas à chaque pixel

*`ShadowingLight.shadowFactorAt(MapView, …)`, `posInLightSpace1()`*

Pour son *receiver plane depth bias*, le PCF mesure à chaque pixel le gradient de profondeur de la surface :
2 tangentes (produits vectoriels et normalisations), puis **2 projections supplémentaires** dans l'espace de la
lumière par le chemin lent de `Matrix4 × Vector4`, soit environ 10 allocations par pixel et par lumière. C'est 23 %
du temps total d'une image en PCF.

Or, sur un triangle plat, **ce gradient est constant** : il ne dépend que du plan du triangle et de la matrice de la
lumière. Il peut être calculé **une fois par triangle et par lumière** (au moment où le triangle est préparé), à
partir des 3 sommets projetés dans l'espace de la lumière. Le BACKLOG le pressent déjà (« a gradient computed from
the geometry instead of two projections »). Pour les surfaces lisses (`INTERPOLATE`), le plan du triangle reste une
excellente approximation à l'échelle d'un texel.

Deux détails en plus :

- dans le chemin PCF, `map.getInterpolation(s, t)` (lecture bilinéaire de 4 texels) est calculé puis **jamais
  utilisé** (il ne sert qu'à `HARD`) ;
- le noyau « 3 × 3 » lit en réalité **16 texels** (4 × 4 pondérés de façon bilinéaire), ce qui est correct pour la
  qualité. On peut ajouter une **sortie anticipée** classique : lire d'abord les 4 coins, et s'ils donnent tous
  « éclairé » ou tous « dans l'ombre », ne pas lire les 12 autres. C'est le cas de la grande majorité des pixels,
  loin des bords d'ombre.

**Gain** e : important en PCF (de l'ordre de −30 %) · **Effort** M · **Lisibilité** + (un calcul géométrique nommé,
au lieu d'une mesure par différences finies).

#### N2. Ne pas consulter la shadow map d'une lumière qui n'éclaire pas le pixel

*`ShadingConsumer.consume()`*

C'est R10 élargi aux nouvelles lumières. Aujourd'hui, `shadowFactorAt()` est appelé **avant** de savoir si la
lumière contribue au pixel : un pixel **hors du cône d'un spot** ou **hors de portée d'une lumière ponctuelle**
(intensité nulle) paie quand même la projection, la sélection de la face et la lecture de la shadow map (16 texels en
PCF). Il faut tester dans cet ordre, du moins cher au plus cher : intensité > 0 (portée, cône), puis
`dotNL > 0`, puis l'ombre.
**Gain** : dans UrbanScape, le spot de test éclaire une grande partie de l'écran, et le gain est resté dans le
bruit. Il sera **proportionnel à la part de l'écran hors du cône ou hors de portée** : élevé pour un lampadaire ou un
spot de scène · **Effort** S · **Lisibilité** +.

#### N3. Lumières ponctuelles et spots : un seul calcul de direction et de distance par pixel

Pour une `PointLight` ou un `SpotLight`, un même pixel calcule le vecteur lumière → point **3 ou 4 fois**, avec à
chaque fois un `new Vector3`, une racine carrée et une normalisation : dans `getIntensity()` (atténuation),
`getLightVectorAtPoint()` (éclairage), `coneFactor()` (spot, qui appelle à nouveau les deux précédents) et
`shadowFactorAt()` (sélection de la face, puis encore `getLightVectorAtPoint()` pour le biais).
**Proposition** : un petit objet réutilisable `LightSample` (direction unitaire, distance, atténuation, facteur de
cône), calculé une fois par pixel et par lumière, puis passé à l'éclairage et à l'ombre. Le code devient en plus
plus lisible : il dit clairement ce qui est calculé une fois.
**Gain** e : moyen · **Effort** S à M · **Lisibilité** +.

#### N4. Passes d'ombre : éliminer, mettre en cache, réutiliser

La passe d'ombre représente maintenant 25 à 35 % du temps, et une `PointLight` peut en faire 6 par image. Chacune
de ces passes :

- transforme **tous les sommets** et parcourt **tous les triangles** de la scène, sans élimination par élément ;
- alloue un **nouveau `ZBuffer`** (6 × 513² floats, soit environ 6 Mo par image et par lumière ponctuelle ; 4 Mo pour
  un spot en 1000²) ;
- recalcule tout, même quand ni la lumière ni la scène n'ont bougé.

Propositions, par ordre de simplicité :

1. **Réutiliser les buffers** d'une image à l'autre (S).
2. **Élimination par portée** : un élément entièrement hors de la sphère de portée d'une lumière ponctuelle (ou du
   cône d'un spot) ne peut pas faire d'ombre sur un point que cette lumière éclaire, puisque le segment lumière →
   point reste dans la sphère. On peut donc l'ignorer dans la passe d'ombre. Avec des sphères englobantes (R1),
   c'est un test par élément (M).
3. **Élimination par face** de la cube map (R1 dans le frustum de chaque face) (M).
4. **Cache avec indicateur de changement** (D8, déjà noté dans le BACKLOG) : une lampe fixe dans une scène fixe
   n'a besoin de sa cube map qu'**une seule fois** (M).
5. Le **`castShadows` par élément** prévu dans le BACKLOG (S).

#### N5. `NearPlaneClipper` : chemin rapide

`NearPlaneClipper.clip()` alloue 3 `Corner`, 2 `ArrayList`, au moins un `ClippedTriangle` et un itérateur **pour
chaque triangle** rendu en perspective, alors que dans la quasi-totalité des cas les 3 sommets sont devant le plan
proche et que rien n'est clippé.
**Proposition** : si les 3 `w` sont au-dessus du plan proche, rasteriser directement le triangle d'origine. Une
ligne, sans effet sur le reste. Le coût mesuré est faible (≈ 0,1 %), mais c'est du bruit d'allocation en moins et
cela prépare le test par outcodes (R2), qui donne le même résultat sans aucune division.
**Effort** S · **Lisibilité** =.

#### N6. Lumières visibles : écrire dans le `int[]`, et paralléliser plus tard

`LightGlowRenderer` est bien conçu pour la performance : calculs sur des `float`, rayons de lumière calculés à 1/4 de
la résolution puis interpolés, visibilité estimée avec 32 à 48 échantillons du Z-buffer. Son coût vient
surtout de l'écriture : `addPixel()` passe par `getRGB()`/`setRGB()` pour presque chaque pixel de l'écran (rayons de
lumière). Avec un accès direct au `int[]` (D6), le coût de l'effet est **divisé par 2** environ (mesuré : de ≈ 80 à ≈ 40 ms). Le
masque des rayons lit aussi le Z-buffer 4 fois par point de grille, via le stockage par colonnes (D7).
Plus tard, l'effet se parallélise par bandes sans aucune précaution (§6).

#### N7. Éclairage : ce que les nouvelles fonctions ajoutent à A2 et D5

- `shadowFactorAt()` appelle `normal.normalize()`, qui **modifie la normale du fragment** (D5) : le résultat est
  correct aujourd'hui, mais c'est un effet de bord à supprimer avant le multithreading.
- Avec le PCF, `shadowFactorAt()` renvoie une valeur entre 0 et 1, et la contribution est multipliée par ce facteur.
  La structure de `ShadingConsumer` (une boucle sur les lumières) se prête bien à la réorganisation proposée en A2,
  N2 et N3 : **calculer une fois** la couleur de base, la normale unitaire et la direction de vue, puis pour chaque
  lumière **`LightSample` → test de contribution → ombre → accumulation**.

### 10.5 État d'avancement des propositions

| Id | Proposition | État au 6 octobre |
|---|---|---|
| D1 | Compteurs statiques des vecteurs | **Fait** (phase 1, étape 2) : neutre en mono-thread (±5 %, dans le bruit), prérequis du multithreading levé |
| D7 | `FloatMap` en `float[]` 1D | **Stockage fait** (phase 1, étape 3) : `MapView` en `float[]` ligne par ligne, −10 % sans ombres et en PCF, −15 à −20 % avec une lumière ponctuelle ou un spot (passes d'ombre −15 à −18 %). Reste l'extraction de `FloatMap` hors du paquet `view` (BACKLOG §2) |
| D6 | Écriture directe dans `int[]` (+ `addPixel()`) | À faire (gain re-mesuré en v2, N6) |
| A4 | `Matrix4 × Vector4` déroulé | À faire (enjeu accru : PCF et passes d'ombre) |
| A1, A2, A3, A5, A6, A7 | Chemin par pixel, éclairage, textures | À faire |
| D2 à D5, D8, D9 | Conception | À faire (D8 référencé dans le BACKLOG pour les ombres) |
| D10 | Rendu hors de l'EDT | Fait dans `SceneViewer` ; reste `MovingCamera` et `FractalLandscape_MouseMoving` |
| R1, R3 à R7 | Élimination en amont | À faire |
| R2 | Outcodes et clipping du plan proche | **Partiel** : clipping fait ; outcodes et chemin rapide (N5) à faire |
| R9 | Passe d'ombre | À faire, enjeu multiplié (N4) |
| R10 | Sortie anticipée de l'éclairage | À faire, gain dépendant de la scène (N2) |
| §6 | Multithreading | À faire, après D1, A3, A6, D3 et D5 |
| §7 | Chronométrage par phase, `PerfBench` dans le dépôt | À faire |

### 10.6 Feuille de route : ce qui change

La phase 1 du §8 reste valable telle quelle. On y ajoute **N2, N3, N5** et le point 1 de **N4** (réutiliser les
buffers d'ombre), tous de petite taille. **N1** (gradient du PCF par triangle) et les points 2 à 4 de **N4**
(élimination et cache des passes d'ombre) rejoignent la phase 2, avec R1 et D8 dont ils dépendent.

Les décisions en attente dans le BACKLOG peuvent maintenant s'appuyer sur ces chiffres :

- **PCF 3 × 3 par défaut ?** Aujourd'hui, il coûte environ +65 à +80 % par image avec une seule lumière. Après les
  gains rapides et N1, l'écart devrait nettement baisser (estimation : de l'ordre de +25 %, à mesurer). Il vaut mieux attendre N1 avant d'en faire le comportement par
  défaut.
- **Ombres des lumières ponctuelles** : 6 passes par image, c'est le cas où le cache (N4.4) et l'élimination par
  portée (N4.2) rapportent le plus.
