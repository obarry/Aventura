# Aventura — Audit de la Javadoc

*Octobre 2026. Périmètre : la Javadoc générée par `mvn javadoc:javadoc` (`target/reports/apidocs`), les sources
`src/main/java` (101 fichiers, 19 packages) et la configuration `maven-javadoc-plugin` du `pom.xml`.*

L'objectif est de faire de la Javadoc **la référence d'une API de moteur de rendu 3D** : un utilisateur doit pouvoir
découvrir le moteur, écrire sa première scène et étendre le moteur (forme, matériau, lumière, vue) sans ouvrir le
code. Le projet a déjà presque tout le contenu nécessaire dans le README, `DESIGN.md`, `GEOMETRY_COOKBOOK.md` et
`BACKLOG.md` : l'essentiel du travail consiste à **le ramener dans la Javadoc, au bon endroit**, plutôt qu'à
l'écrire. Les identifiants (J1, S2, C3…) sont repris dans la feuille de route du §7.

## Contenu

0. [Synthèse](#0-synthèse)
1. [Méthode et mesures](#1-méthode-et-mesures)
2. [Configuration de la génération (J)](#2-configuration-de-la-génération-j)
3. [Structure : vue d'ensemble, packages, groupes (S)](#3-structure--vue-densemble-packages-groupes-s)
4. [Contenu des commentaires (C)](#4-contenu-des-commentaires-c)
5. [Surface de l'API (A)](#5-surface-de-lapi-a)
6. [Réutiliser les documents existants](#6-réutiliser-les-documents-existants)
7. [Feuille de route proposée](#7-feuille-de-route-proposée)
8. [Ce qu'il vaut mieux ne pas faire](#8-ce-quil-vaut-mieux-ne-pas-faire)

---

## 0. Synthèse

**Le problème n'est pas d'abord le manque de commentaires, c'est leur forme.** Les classes sont toutes commentées,
mais la Javadoc générée est difficile à lire pour quatre raisons, toutes corrigeables mécaniquement :

1. **La licence MIT est dans le commentaire Javadoc de 100 classes sur 101.** Javadoc la prend pour la description
   de la classe : dans **toutes** les tables de résumé (pages de package, index, « All Classes »), la première
   phrase affichée est `------------------------------------------------------------------------------ MIT
   License Copyright (c) 2016-2026 Olivier BARRY…`. C'est ce qui donne l'impression d'une documentation « pas
   présentable ». Corriger ce seul point change déjà l'aspect de tout le site.
2. **Aucune page d'accueil, aucune description de package.** Pas d'`overview.html`, aucun `package-info.java`
   (0 sur 19) : la page d'index est une liste brute de 19 noms de packages, sans phrase, sans exemple, sans ordre de
   lecture. Les démos (`com.aventura.demo`) y apparaissent au même niveau que l'API.
3. **Les commentaires sont écrits en texte brut, pas en Javadoc.** 0 `{@link}`, 0 `{@code}`, 0 `<p>` dans tout
   `src/main`. Les listes à tirets, les retours à la ligne et les **6 schémas ASCII** (pipeline de `RenderEngine`,
   volume de vue de `Perspective`, `Torus`, `SpotLight`…) sont fusionnés en un seul paragraphe illisible, et les noms
   de classes cités ne sont pas cliquables.
4. **La configuration masque tous les problèmes** : `<doclint>none</doclint>`, `<failOnError>false</failOnError>`
   et `<quiet>true</quiet>`. Un passage avec `-Xdoclint:all` révèle **72 erreurs et 1 148 avertissements**.

Ensuite seulement vient la couverture : **43 % des méthodes et constructeurs publics ou protégés** ont un
commentaire, et des packages centraux pour l'utilisateur sont peu documentés (`model.world` 15 %,
`model.world.shape` 21 %, `model.light` 38 %, `context` 39 %).

Il y a aussi de bonnes nouvelles :

- Le contenu existe déjà, souvent excellent : les tableaux de packages et d'options du README, l'architecture, les
  points d'extension et les conventions de `DESIGN.md`, les contrats C1 à C10 du cookbook, et les pièges d'API
  relevés dans `BACKLOG.md`.
- Les exemples sont **déjà compilés et testés** (`HelloAventura`, `PyramidFrustum`, `ClosedConeFrustum`,
  `StreetLamp` dans `src/test/java`) : avec `{@snippet}` (Java 18+, tu es en Java 21), la Javadoc peut les inclure
  sans copier-coller, et ils ne se périmeront jamais.
- La bibliothèque mathématique (`math.vector`, 70 %) et `tools.color` (88 %) sont déjà bien commentées.
- Les liens vers la Javadoc du JDK sont déjà générés automatiquement (`java.awt.Color`, `Object`…).

**Effort estimé** : environ 1 jour pour un résultat déjà présentable (phases 0 et 1), puis 3 à 5 jours étalés pour
une Javadoc de niveau « API publique » (phases 2 et 3).

---

## 1. Méthode et mesures

- Lecture de la Javadoc générée (`target/reports/apidocs`, maven-javadoc-plugin 3.11.2, JDK 21).
- Génération de contrôle sur une copie des sources : `javadoc -protected -Xdoclint:all` (JDK 21), sans rien
  modifier dans ton dépôt.
- Comptage par script des déclarations `public`/`protected` documentées ou non, et des balises utilisées.

### Résultat de `-Xdoclint:all`

| Type | Nombre | Nature |
|---|---:|---|
| **Erreurs** | **72** | |
| `malformed HTML` | 38 | schémas ASCII et `<`, `>` littéraux (`RenderEngine`, `Perspective`, `Torus`, `SpotLight`…) |
| `@param name not found` | 26 | paramètres renommés sans mise à jour du commentaire |
| `unknown tag: date` | 6 | balise `@date` non standard (classes de `math.vector`) |
| autres (`@return` mal placé, `P1` pris pour une balise) | 2 | |
| **Avertissements** | **1 148** | |
| `no comment` | 754 | élément public ou protégé sans commentaire (dont ≈ 200 champs protégés) |
| `no @param` | 173 | |
| `no description` | 113 | balise présente mais vide (`@param x`, `@return`) |
| `no main description` | 56 | commentaire réduit à des balises |
| `no @return` | 35 | |
| constructeur par défaut non documenté | 14 | |

### Couverture par package (déclarations publiques et protégées commentées)

| Package | Couverture | Remarque |
|---|---:|---|
| `tools.color` | 88 % | |
| `math.vector` | 70 % | bon niveau, mais `@date` et compteurs publics |
| `model.camera` | 64 % | |
| `engine` | 59 % | |
| `math.transform` | 47 % | |
| `context` | 39 % | point d'entrée de la configuration : prioritaire |
| `model.light` | 38 % | |
| `math.projection` | 33 % | |
| `model.texture`, `demo` | 33 % | |
| `model.perspective` | 32 % | |
| `model.world.triangle` | 31 % | |
| `view` | 30 % | |
| `model.material` | 30 % | point d'extension : prioritaire |
| `model.world.shape` | 21 % | ce que l'utilisateur manipule le plus : prioritaire |
| `model.world` | 15 % | `World`, `Element`, `GenerativeElement` : prioritaire |
| `math.tools` | 13 % | |
| `tools.tracing` | 4 % | outil interne, à sortir de l'API (§5) |

---

## 2. Configuration de la génération (J)

### J1. Sortir la licence du commentaire Javadoc — **priorité 1**

Dans chaque fichier, l'en-tête MIT et la description de la classe partagent le même bloc `/** … */`. Il faut les
séparer : la licence dans un commentaire ordinaire `/* … */` en tête de fichier (avant `package`, la convention
habituelle), la description seule dans le `/** … */` au-dessus de la classe.

```java
/*
 * MIT License
 * Copyright (c) 2016-2026 Olivier BARRY
 * ...
 */
package com.aventura.engine;

import ...;

/**
 * The core of the Aventura rendering pipeline: renders a {@link World} seen through a {@link Camera}...
 *
 * @author Olivier BARRY
 */
public class RenderEngine {
```

Les 100 fichiers ont la même structure (licence entre deux lignes de tirets, puis la description) : un petit
script peut faire la transformation sans risque, et `git diff` permet de la vérifier. La licence n'a pas besoin
d'apparaître dans la Javadoc : elle est dans le pied de page (J2) et dans `LICENSE.md`.

### J2. Corriger le pied de page

Le plugin ajoute par défaut `Copyright © 2016–2026. All rights reserved.` sur chaque page, ce qui **contredit la
licence MIT**. À remplacer, par exemple :

```xml
<bottom><![CDATA[Copyright &#169; 2016&#x2013;2026 Olivier Barry.
  Released under the <a href="https://opensource.org/licenses/MIT">MIT License</a>.]]></bottom>
```

### J3. Réactiver doclint progressivement

Aujourd'hui `doclint none` cache les 72 erreurs. Proposition en deux temps :

1. `<doclint>all,-missing</doclint>` : toutes les vérifications **sauf** les commentaires manquants. Les 72 erreurs
   se corrigent en une demi-journée (§4, C1), puis on garde `failOnError` à `true` pour qu'elles ne reviennent pas.
2. Quand la phase 3 est terminée : `all`, avec `failOnWarnings` dans l'intégration continue.

Retirer aussi `<quiet>true</quiet>` pendant le travail : les messages sont la liste de tâches.

### J4. Options de présentation

| Option | Valeur proposée | Pourquoi |
|---|---|---|
| `<overview>` | `src/main/javadoc/overview.html` | page d'accueil (S1) ; le plugin cherche déjà `src/main/javadoc` |
| `<groups>` | voir S3 | regrouper les 19 packages par rôle |
| `<excludePackageNames>` | `com.aventura.demo` | les démos ne sont pas l'API (ou un groupe « Demos » séparé, à la fin) |
| `<additionalOptions>` | `--snippet-path ${project.basedir}/src/test/java` | exemples testés (C4) |
| `<docfilessubdirs>` | `true` | images dans `doc-files` (S4) |
| `<author>` | `false` | `@author` sur 82 classes n'apporte rien au lecteur d'une API mono-auteur |
| `<show>` | `protected` (inchangé) | nécessaire pour les points d'extension ; mais voir A3 pour les champs |
| `<doctitle>` | `Aventura ${project.version} — pure Java 3D rendering engine` | |

---

## 3. Structure : vue d'ensemble, packages, groupes (S)

### S1. Une page d'accueil (`overview.html`)

C'est la page que l'on voit en premier. Elle peut reprendre presque tel quel le début du README :

- la phrase d'accroche (« A lightweight software 3D rendering engine, 100% pure Java… ») et l'image Earth and Moon ;
- **Hello, Aventura** : le programme complet, inclus par `{@snippet class=com.aventura.demo.HelloAventura}` depuis
  `src/test/java` (il est déjà compilé et produit l'image de la galerie) ;
- « The scene and the engine » : `RenderEngine` + `World` + `Camera` + `Lighting` + les deux contextes, avec le
  schéma du pipeline (S4) ;
- un **ordre de lecture** : « Start with `RenderEngine`, then `World` and the shapes, then `RenderContext` » ;
- le tableau des points d'extension de DESIGN §9, avec des `{@link}` ;
- les conventions de DESIGN §12 (Z vers le haut, sens des lumières directionnelles, ordre S → R → T) : ce sont des
  informations dont l'utilisateur a besoin **avant** d'écrire sa première scène ;
- des liens vers DESIGN, le cookbook et le dépôt GitHub pour aller plus loin.

### S2. Un `package-info.java` par package

19 fichiers courts (5 à 20 lignes). La première phrase s'affiche dans les tables : elle doit dire **à quoi sert le
package**, du point de vue de l'utilisateur. Le contenu est déjà dans le tableau « Packages » du README et dans
DESIGN §2. Exemples :

```java
/**
 * Built-in shapes ({@link Box}, {@link Sphere}, {@link Torus}, {@link Trellis}...) ready to be added to a
 * {@link com.aventura.model.world.World World}.
 *
 * <p>Every shape is a {@link com.aventura.model.world.GenerativeElement GenerativeElement}: its constructor
 * stores the parameters, and {@code build()} generates the vertices, the triangles and the normals.
 * To write your own shape, see the contracts on {@code GenerativeElement} and the
 * <a href="https://github.com/obarry/Aventura/blob/master/docs/GEOMETRY_COOKBOOK.md">Geometry Cookbook</a>.
 *
 * <img src="doc-files/cookbook_gallery.png" alt="Built-in shapes">
 */
package com.aventura.model.world.shape;
```

Pour `com.aventura.engine`, le package-info est le bon endroit pour « One frame, step by step » (DESIGN §4) et les
espaces de coordonnées ; pour `model.light`, le tableau des types de lumière (DESIGN §6) ; pour `context`, les
tableaux d'options et de presets du README.

### S3. Regrouper les packages par rôle

L'index affiche aujourd'hui 19 packages par ordre alphabétique (`context` en premier, `view` en dernier). Avec
`<groups>`, l'index suit les couches de DESIGN §2 :

| Groupe | Packages |
|---|---|
| **Scene API** — build and render a scene | `engine` (pour `RenderEngine`), `context`, `model.world`, `model.world.shape`, `model.light`, `model.camera`, `model.perspective`, `model.material`, `model.texture` |
| **Display** | `view` |
| **Geometry building blocks** | `model.world.triangle`, `math.transform` |
| **Math library** | `math`, `math.vector`, `math.projection`, `math.tools` |
| **Utilities** | `tools.color`, `tools.tracing` |

### S4. Les images et les schémas

- Copier les images de `resources/doc/images` dans des dossiers `doc-files` (à côté des `package-info.java`, ou
  sous `src/main/javadoc/doc-files` pour la page d'accueil) : rendus de la galerie, éclairage, `UrbanScape`.
- Les schémas **Mermaid** de DESIGN (architecture, pipeline, contextes, points d'extension) ne sont pas interprétés
  par Javadoc. Le plus simple et le plus durable : les exporter une fois en SVG (`mermaid-cli`) dans `doc-files`.
  Charger Mermaid en JavaScript (`--add-script`) est possible mais rend la Javadoc dépendante d'un script externe,
  y compris dans le jar `-javadoc`.
- Les schémas ASCII existants (volume de vue de `Perspective`, cône de `SpotLight`, `Torus`) sont utiles et
  didactiques : il suffit de les mettre dans `<pre>` (en échappant `<` et `>`), voir C1.

---

## 4. Contenu des commentaires (C)

### C1. Corriger les 72 erreurs — mécanique, ½ journée

- Schémas ASCII et formules → `<pre>…</pre>`, avec `&lt;` et `&gt;` (ou `{@code}` dans le texte courant).
- `@date` (6 classes de `math.vector`) → supprimer, ou `@since` (voir A2).
- 26 `@param` dont le nom ne correspond plus à la signature → renommer.
- Listes à tirets → `<ul><li>` ; paragraphes séparés par `<p>`.

### C2. Une règle de rédaction (à ajouter à DESIGN §12)

Une API de rendu 3D a des questions récurrentes auxquelles chaque commentaire doit répondre quand elles s'appliquent.
Elles sont aujourd'hui dispersées (ou absentes) :

1. **Première phrase** : ce que fait la méthode ou ce qu'est la classe, à la 3e personne (« Returns… »,
   « Renders… »). C'est la seule phrase visible dans les tables.
2. **Espace de coordonnées** : monde, repère de l'élément, caméra, clip, NDC, écran. C'est la première source
   d'erreur en 3D, et le pipeline les nomme déjà (DESIGN §4).
3. **Unités et conventions** : unités du monde ou pixels, angles en radians ou degrés, Z vers le haut, sens d'une
   direction (vers la scène ou vers la lumière), enroulement anti-horaire vu de l'extérieur.
4. **Mutation ou nouvel objet** : la bibliothèque mathématique est « à moitié immuable » (audit de performance,
   D5). Chaque méthode doit dire si elle modifie `this` ou renvoie un nouvel objet, et si le résultat peut être
   modifié sans risque (les accesseurs `xAxis()` le font déjà très bien).
5. **`null`** : accepté ? signifie quoi ? (« `null` inherits from the parent element », contrat C8 du cookbook).
6. **Cycle de vie** : à appeler avant ou après `build()`, une fois par image, etc.
7. **Exceptions** : `@throws` pour les exceptions déclarées (34 déjà présentes) et pour les
   `IllegalStateException` des presets immuables de `RenderContext`.
8. **Liens** : `{@link}` vers les classes citées, `{@code}` pour le code, `@see` vers la méthode complémentaire.

Les balises Java 9+ `@apiNote` (conseil d'usage), `@implSpec` (contrat pour une sous-classe) et `@implNote`
(détail d'implémentation) conviennent bien à un projet didactique : elles séparent ce que l'utilisateur doit savoir
de ce qui explique l'algorithme, sans rien supprimer. Elles s'activent avec trois `<tags>` dans le plugin.

### C3. Documenter en priorité le chemin de l'utilisateur

Plutôt que de viser 100 % partout, suivre le parcours d'un utilisateur, dans cet ordre :

| Priorité | Classes | Source du contenu |
|---|---|---|
| 1. Rendre une scène | `RenderEngine`, `World`, `Camera`, `Lighting`, `ImageView`, `SwingView` | README « Hello, Aventura », « How it works » |
| 2. Configurer | `RenderContext` (options, presets, `RenderingType`), `PerspectiveContext`, `PerspectiveType` | README « Rendering options », DESIGN §8 |
| 3. Peupler la scène | `Element`, les 16 formes de `model.world.shape`, `Texture`, `SolidMaterial`, `TexturedMaterial` | README, cookbook §1 et §3 |
| 4. Éclairer | `AmbientLight`, `DirectionalLight`, `PointLight`, `SpotLight`, `ShadowingLight` | DESIGN §6 et §7, README « Lights and shadows » |
| 5. Étendre | `GenerativeElement`, `Material`, `FragmentConsumer`, `Fragment`, `GUIView`, `Light` | DESIGN §9, cookbook §2 (contrats C1 à C10) |
| 6. Mathématiques | compléter `math.transform`, `math.projection`, `math.tools` | tests unitaires |

Les **contrats C1 à C10 du cookbook** ont leur place naturelle dans la Javadoc de `GenerativeElement` (avec
`@implSpec` sur `generateVertices()` et `generateTriangles()`) : c'est là que les cherche quelqu'un qui écrit une
forme. Le cookbook garde les recettes détaillées et renvoie à la Javadoc pour les contrats.

### C4. Des exemples testés avec `{@snippet}`

`{@snippet}` inclut une région d'une classe compilée, au lieu de recopier du code dans un commentaire :

```java
/**
 * ...
 * {@snippet class=com.aventura.cookbook.PyramidFrustum region=vertices}
 */
```

Les exemples existent déjà dans `src/test/java` et sont vérifiés par `mvn test` :

| Exemple | Où l'inclure |
|---|---|
| `demo.HelloAventura` | `overview.html`, `RenderEngine` |
| `cookbook.PyramidFrustum` | `GenerativeElement` (nouvelle forme) |
| `cookbook.ClosedConeFrustum` | `GenerativeElement` (enrichir par héritage) |
| `cookbook.StreetLamp` | `Element` (assemblage, pivot, sous-éléments) |
| presets de `RenderContext` (README) | `RenderContext` |

Il suffit d'ajouter des marqueurs `// @start region=…` / `// @end` dans ces classes. Des extraits courts en ligne
(`{@snippet : … }`) suffisent pour les méthodes simples.

### C5. Les pièges connus, écrits là où on tombe dedans

`BACKLOG.md` et DESIGN listent des pièges que l'utilisateur ne peut pas deviner. Tant qu'ils ne sont pas corrigés,
ils doivent apparaître dans la Javadoc (`@apiNote` ou un paragraphe « <b>Note:</b> ») :

- `PerspectiveContext` : la surcharge `(int, int, …)` est en **pixels**, `(float, float, …)` en **unités** ;
  l'ordre `(top, bottom, right, left, far, near)` diffère de celui de `Perspective` ; la taille en pixels est fixée
  à la construction (BACKLOG §1).
- `ShadowingLight(int)` (type de boîte) et `ShadowingLight(float)` (intensité) se confondent facilement ;
  `SHADOWING_BOX_ELEMENT` et `SHADOWING_BOX_SPECIFIC` retombent sur `SHADOWING_BOX_WORLD` (BACKLOG §3).
- `Triangle.setRectoVerso()` et `Vertex.setColor()` sont sans effet sur le rendu (BACKLOG §5).
- Ne jamais conserver un `Fragment` après `consume()` (DESIGN §12).
- Les chemins de textures sont relatifs au répertoire de travail (DESIGN §12, « Known limitations »).

### C6. Sortir l'historique des commentaires

43 passages décrivent l'histoire du code plutôt que son usage : « CHANGED BEHAVIOR: this used to… »
(`DirectionalLight`), « the former Rasterizer façade », « Legacy-parity constants », « formerly on… ». Ils sont
précieux pour toi mais déroutants pour un lecteur de l'API. Proposition : garder dans la Javadoc **le comportement
actuel** (au besoin avec un `@apiNote` court), et déplacer le récit dans un `CHANGELOG.md` ou dans les messages de
commit. Les commentaires `//` internes peuvent rester tels quels : ils n'apparaissent pas dans la Javadoc.

---

## 5. Surface de l'API (A)

Ce que la Javadoc montre, c'est ce qui est `public` ou `protected`. Quelques éléments affichés aujourd'hui ne
devraient pas faire partie de l'API, ou demandent une décision.

### A1. Champs publics modifiables

19 champs `public static` non `final`, visibles et modifiables par tout utilisateur :

- ~~`Vector3.nb_vectors`, `Vector4.nb_vectors`, `nb_to_display`~~ : compteurs d'instrumentation, **supprimés**
  (octobre 2026, audit de performance D1).
- `Light.DEFAULT_LIGHT_INTENSITY` : une constante **non `final`**, qu'un utilisateur peut changer pour tout le
  moteur. À rendre `final`.
- Les drapeaux de `Tracer` (`function`, `error`, `exception`…) : à cacher derrière des méthodes, ou à sortir de
  l'API publique.

### A2. `@since` et `@deprecated`

- `@since` contient des dates (« May 2016 », « Nov 2023 », « 2026 »…). Dans une API, `@since` indique la
  **version** à partir de laquelle un élément existe. Proposition : supprimer les dates actuelles (l'historique est
  dans git) et utiliser `@since 0.1` à partir de la première version publiée.
- Un seul `@deprecated` (`Vector2`) : bien rédigé, mais à accompagner de `@Deprecated(forRemoval = true)` et d'un
  `{@link}` vers la remplaçante.

### A3. Distinguer l'API utilisateur des rouages du moteur

`engine` expose en `public` des classes internes du pipeline (`ElementTransform`, `ViewProjection`,
`NearPlaneClipper`, `ZBuffer`, `TriangleRasterizer`, les consumers…), et l'option `-protected` affiche environ
200 champs `protected` non documentés (`light_vector`, composantes des vecteurs…). Trois options, de la plus légère à
la plus structurante :

1. **Documenter le statut** : un paragraphe « Internal: part of the rendering pipeline, not needed to render a
   scene » dans leur Javadoc, et un groupe « Rendering pipeline (internals) » séparé.
2. **Réduire la visibilité** là où c'est possible (champs `protected` → `private` + accesseur, classes →
   package-private), au fil des refactorisations de l'audit de performance (D4, D9).
3. **`module-info.java`** n'exportant que les packages de l'API : la Javadoc ne montre plus le reste. C'est la
   solution la plus propre, mais elle demande de réorganiser `engine` (API publique d'un côté, pipeline de l'autre).

Pour un projet didactique, l'option 1 suffit à court terme : les internes restent visibles pour qui veut apprendre,
mais l'utilisateur sait qu'il peut les ignorer.

---

## 6. Réutiliser les documents existants

| Document | Section | Destination dans la Javadoc |
|---|---|---|
| README | accroche, image, Highlights | `overview.html` |
| README | Hello, Aventura | `overview.html` et `RenderEngine` (`{@snippet}` de `HelloAventura`) |
| README | The scene and the engine | `overview.html`, `RenderEngine` |
| README | Packages (tableau) | première phrase des 19 `package-info.java` |
| README | Rendering options, presets | `RenderContext`, `RenderingType`, `package-info` de `context` |
| README | Lights and shadows | `package-info` de `model.light` |
| README, DESIGN | Building your own geometry, §9 Extension points | `overview.html`, `GenerativeElement`, `Material`, `FragmentConsumer`, `GUIView`, `Light` |
| DESIGN | §2 Architecture (schéma) | `overview.html` (SVG) et groupes (S3) |
| DESIGN | §4 Pipeline, coordonnées, une image pas à pas | `package-info` de `engine` |
| DESIGN | §5 Rasterisation et fragments | `TriangleRasterizer`, `Fragment`, `FragmentConsumer` |
| DESIGN | §6 Modèle d'éclairage, types de lumières | `model.light`, chaque classe de lumière |
| DESIGN | §7 Shadow mapping | `ShadowingLight` |
| DESIGN | §8 Les deux contextes | `RenderContext`, `PerspectiveContext` |
| DESIGN | §12 Conventions | `overview.html` (section « Conventions ») |
| Cookbook | §1 Le modèle en cinq minutes | `package-info` de `model.world` |
| Cookbook | §2 Contrats C1–C10 | `GenerativeElement`, `Element` (`@implSpec`) |
| Cookbook | §3 Boîte à outils | `package-info` de `model.world.triangle` et `math.transform` |
| Cookbook | §4–§7 Recettes | `{@snippet}` des classes du cookbook, lien vers le cookbook |
| Cookbook | §10 Problèmes connus des formes | `Sphere` (enroulement, normales) |
| BACKLOG | §1, §3, §5 | `@apiNote` des pièges (C5) |
| Audit de performance | D1, D5 | A1, C2 (mutation ou nouvel objet) |

Le principe : **la Javadoc dit quoi et comment l'utiliser, DESIGN et le cookbook disent pourquoi et comment c'est
construit**, et chacun renvoie à l'autre. On évite ainsi de maintenir deux fois le même texte.

---

## 7. Feuille de route proposée

### Phase 0 — Rendre le site présentable (½ journée, mécanique)

| # | Action | Effet |
|---|---|---|
| J1 | Licence en `/* */` en tête de fichier (script sur les 100 fichiers) | tables de résumé lisibles partout |
| J2 | Pied de page MIT | cohérence avec la licence |
| J4 | Exclure `demo`, `author=false`, titre | index centré sur l'API |
| C1 | Corriger les 72 erreurs (`<pre>`, `@date`, `@param`) | schémas lisibles |
| J3 | `doclint all,-missing`, `failOnError=true` | plus de régression |

### Phase 1 — Structure (½ à 1 journée, surtout du déplacement de texte)

| # | Action |
|---|---|
| S1 | `overview.html` : accroche, Hello Aventura en `{@snippet}`, schéma, ordre de lecture, conventions |
| S2 | 19 `package-info.java` à partir du README et de DESIGN |
| S3 | Groupes de packages |
| S4 | `doc-files` : images de la galerie, schémas Mermaid exportés en SVG |

À ce stade, la Javadoc est déjà une vraie porte d'entrée vers le moteur.

### Phase 2 — Le chemin de l'utilisateur (2 à 3 jours, étalés)

| # | Action |
|---|---|
| C2 | Règle de rédaction ajoutée à DESIGN §12 |
| C3 | Priorités 1 à 4 : `RenderEngine`, `World`, contextes, formes, lumières, vues |
| C4 | `{@snippet}` des classes du cookbook et des presets |
| C5 | Pièges connus en `@apiNote` |
| C6 | Historique déplacé vers `CHANGELOG.md` |

### Phase 3 — Extension et finitions (1 à 2 jours)

| # | Action |
|---|---|
| C3 | Priorités 5 et 6 : points d'extension (`@implSpec`), bibliothèque mathématique |
| A1 | Champs publics modifiables (en lien avec D1 de l'audit de performance) |
| A2 | `@since` en versions, `@Deprecated(forRemoval = true)` |
| A3 | Statut « internal » des classes du pipeline, groupe séparé |
| J3 | `doclint all` + `failOnWarnings` en intégration continue |

### Phase 4 — Optionnel

- Publier la Javadoc sur **GitHub Pages** (ou javadoc.io une fois sur Maven Central) et la lier depuis le README.
- Une feuille de style légère (`--add-stylesheet`) aux couleurs du projet.
- `module-info.java` (A3, option 3), à coordonner avec la phase 2 de l'audit de performance (D9).
- Commentaires en Markdown (`///`, Java 23+) si tu passes un jour à une version plus récente du JDK : plus agréables
  à écrire, mais pas nécessaires.

Chaque phase se valide en régénérant la Javadoc (`mvn javadoc:javadoc`) et en ouvrant
`target/reports/apidocs/index.html` : page d'accueil, une page de package, `RenderEngine`, `Sphere`, `RenderContext`.

---

## 8. Ce qu'il vaut mieux ne pas faire

- **Générer des commentaires automatiquement** (« @param x the x ») pour faire disparaître les avertissements :
  le compteur baisse, la documentation n'est pas meilleure, et ces commentaires vides cachent ceux qui manquent
  vraiment.
- **Viser 100 % dès le départ** : les accesseurs évidents (`getX()`) et les classes internes peuvent attendre.
  Le chemin de l'utilisateur (C3) d'abord.
- **Copier DESIGN ou le cookbook dans la Javadoc** : lier, résumer, et inclure le code par `{@snippet}`.
- **Supprimer les explications didactiques** (schémas ASCII, équivalent GPU, raisons d'un algorithme) : elles font
  la valeur du projet. Les mettre en forme (`<pre>`) et, si elles décrivent l'implémentation plutôt que l'usage,
  sous `@implNote`.
- **Laisser `doclint none`** une fois le nettoyage fait : sans contrôle automatique, la Javadoc se dégrade à chaque
  refactorisation (26 `@param` obsolètes le montrent).
