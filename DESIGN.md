---
name: Bourdain
description: Chicago's food inspection record, read like a catalog rather than a database.
---

<!-- SEED: re-run /impeccable document once there's code to capture the actual tokens and components. -->

# Design System: Bourdain

## 1. Overview

**Creative North Star: "The Night Catalog"**

An archive you browse after hours. Criterion Channel supplies the posture — every
entry is a record with provenance, the catalog rewards wandering, and the design
never sells you the thing it's describing. Transit departure boards supply the
discipline — a fixed status vocabulary, stated the same way every time, trusted
precisely because it never editorializes. Between them sits the voice from
PRODUCT.md: unsentimental, curious, respectful.

The system is Restrained by strategy. Neutrals do the architecture; a single rose
accent signs the work and nothing else. This is deliberate against the temptation
the subject matter creates: inspection data is lurid, and a lurid interface would
make the record less believable, not more. The restraint is what makes an
unflattering page trustworthy.

It explicitly rejects **chispections.com** (the site this project borrowed its
concept from — its dark monospace-and-orange styling arrived with the concept and
is fully discarded here), the **generic SaaS dashboard** (KPI tiles, gradient
accents, repeating icon-heading card grids), and the **government data portal**
(unstyled dense tables, bureaucratic chrome).

**Key Characteristics:**

- Restrained color: neutrals carry the surface, rose accent stays under 10%
- Serif for names, sans for everything the city generated
- Flat by default; depth comes from tonal layering, never from shadow decoration
- Motion conveys state only — no choreography, no scroll reveals
- Both themes are the same design, not two identities
- WCAG 2.2 AA, and status never depends on color alone

## 2. Colors

A near-neutral surface in both themes, signed by a single saturated rose. The
rose reads as late-window neon rather than anything appetizing — anti-pretension
is the point, and a food-colored palette would be exactly the lifestyle veneer
PRODUCT.md rules out.

### Primary

- **Rose** (anchor: OKLCH hue ~343°, lightness and chroma *[to be resolved during
  implementation]*): the signature. Links, focus rings, current selection, the
  wordmark. Never a background for large areas; never used to mean "bad".

### Neutral

- **Ground** *[to be resolved during implementation]*: pure white in light theme,
  near-black at chroma 0 in dark. No hidden warmth in either — the rose carries
  all the personality the surface needs.
- **Surface** *[to be resolved]*: ground pulled 10–15% toward ink. Panels,
  the establishment header block, the relocation notice.
- **Ink** *[to be resolved]*: body text, ≥7:1 against ground.
- **Muted** *[to be resolved]*: secondary text (dates, addresses, inspector
  comments), ≥4.5:1 against ground — not the 3:1 floor. This data is meant to be
  read, not skimmed past.

### Semantic status

A separate vocabulary from the brand, owned by the data: **pass**, **fail**,
**relocated**, and **neutral** for inspection types that assert nothing. Values
*[to be resolved during implementation]*.

### Named Rules

**The Ten Percent Rule.** Rose covers no more than 10% of any screen. Its rarity
is what makes a focus ring or an active link register at all.

**The Status Is Not Brand Rule.** Semantic status colors and the brand accent are
disjoint vocabularies. Rose never means "fail". Fail-red never appears as
decoration. If a reader has to ask whether a color is describing the data or the
site, the system has already broken.

**The One Design Rule.** Dark theme is the same Restrained strategy at a
different lightness — same accent, same status colors, same discipline. Only the
neutral tokens flip on `prefers-color-scheme`. A `FAIL` badge is the same color
in both themes; two screenshots of one fact must never disagree.

## 3. Typography

**Display Font:** *[serif pairing to be chosen at implementation]*
**Body Font:** *[neutral sans to be chosen at implementation]*

**Character:** Editorial. The serif says a person compiled this; the sans says the
city generated it. That split is the whole idea — reportage wrapped around a
public record, with a visible seam between the two.

### Hierarchy

- **Display** — the home page's one question and nothing else. Fixed rem sizing,
  not fluid clamp; this is product register.
- **Headline** — establishment names, on the detail page and in results.
- **Title** — section headings ("Inspection history"), set against the sans.
- **Body** — violation descriptions and inspector comments. 65–75ch.
- **Label** — dates, license numbers, violation codes, status badges. Sans,
  tabular figures where numbers align in a column.

### Named Rules

**The Serif Names The Place Rule.** The serif is reserved for names a person
gave something — the establishment, the site itself. Everything the city
generated (codes, dates, results, license numbers, addresses) is sans. No
exceptions, including in search results.

**The Badge Stays Text Rule.** Status badges are readable words, never color
chips or icons alone. Strip the color and the page still says what it means.

## 4. Elevation

Flat. Depth comes from tonal layering — surface against ground, a hairline rule
where a boundary is real — never from a shadow vocabulary. Motion is restrained
to state changes (focus, hover, htmx swap-in), 150–250ms, with a
`prefers-reduced-motion` alternative on every one of them.

### Named Rules

**The Flat-By-Default Rule.** Surfaces are flat at rest. If an element appears
lifted, it is because the user is doing something to it right now.

## 5. Components

*Omitted — no components exist for this system yet. The current templates are
inherited styling being discarded. Re-run `/impeccable document` after the
rebuild to capture real component specs and generate the sidecar.*

## 6. Do's and Don'ts

### Do:

- **Do** keep rose under 10% of any screen (The Ten Percent Rule).
- **Do** give every status badge a text label that survives color removal.
- **Do** set the establishment name in the serif and everything the city
  generated in the sans.
- **Do** show status's work — what the license says, what the inspections say,
  when each was last seen. PRODUCT.md: *"Closed is a claim, not a fact."*
- **Do** hold muted text to 4.5:1, not 3:1. These readers are reading.
- **Do** ship both themes as one design with flipped neutral tokens.

### Don't:

- **Don't** reproduce **chispections.com**: no dark-plus-monospace-plus-orange
  system, no hero-search-over-card-grid composition. Nobody should see the two
  side by side and call this a reskin.
- **Don't** build a **generic SaaS dashboard**: no KPI hero tiles, no gradient
  accents, no repeating icon-plus-heading card grids.
- **Don't** build a **government data portal**: no unstyled dense tables, no
  bureaucratic chrome, no Socrata-dataset-viewer energy.
- **Don't** use rose to mean "bad", or fail-red as decoration.
- **Don't** score, grade, or rank an establishment. PRODUCT.md: *"The record
  speaks."*
- **Don't** add food photography, star ratings, or appetite appeal.
- **Don't** animate anything that isn't reporting a state change.
- **Don't** use `border-left` over 1px as a colored accent stripe, gradient text,
  or decorative glassmorphism.
