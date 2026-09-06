# Product

## Register

product

## Users

Curious Chicago locals, browsing rather than deciding. They already eat at these
places; they're looking up a name they know to see what the city found. Usually
desktop, usually unhurried, usually one search leading to another. Nobody
arrives with a task list — they arrive with a name, and the interesting part is
what's underneath it.

The secondary user is the same person on a phone, standing somewhere, checking
one thing. That path has to work, but it isn't what the product is for.

## Product Purpose

Bourdain mirrors Chicago's Food Inspections and active Business Licenses
datasets and makes the inspection record legible: search by name, address, or
ZIP, then read an establishment's full history — results, types, violation codes
and the inspector's own comments.

It exists because the raw feed lies by omission. Inspections only know about
licenses that have been inspected, so an establishment that moves reads as
permanently closed (the Duke of Perth: closed on Clark in 2024, thriving on
Broadway since 2025). Cross-referencing active licenses by normalized name lets
Bourdain say **RELOCATED** and point at the new address instead of leaving a
false obituary standing.

Success is a local looking up a restaurant they love, reading something
genuinely unflattering, and trusting the page anyway — because the page never
overstated it.

## Brand Personality

Homage to Anthony Bourdain's *No Reservations*: curious, unsentimental,
plainspoken, on the side of the people in the kitchen. Dry rather than jokey.
Never squeamish about what's in a violation, never gleeful about it either.
Anti-pretension is the through-line — no food-porn gloss, no star ratings, no
lifestyle veneer over a public health record.

Three words: **unsentimental, curious, respectful**.

Homage, not impersonation. The voice can borrow the register; it must never
attribute invented words to a real person or imply an endorsement.

## Anti-references

- **chispections.com** — the site whose concept this project borrowed. The
  current dark / monospace / orange-accent styling came over with it and is
  being fully reset. Nobody should see the two side by side and call this a
  reskin.
- **Generic SaaS dashboard** — no KPI hero tiles, no gradient accents, no
  repeating icon-plus-heading card grids.
- **Government data portal** — no unstyled dense tables, no bureaucratic tone,
  no Socrata-dataset-viewer energy. The data is civic; the presentation isn't.

## Design Principles

1. **The record speaks.** Present what the city found; never editorialize,
   score, or rank on top of it. No composite grades, no verdicts the data
   doesn't support.
2. **Closed is a claim, not a fact.** The product's entire reason for existing
   is refusing to assert an ending it can't back. Status must always show its
   work — what the license says, what the inspections say, when each was last
   seen.
3. **Reward the next search.** Users arrive with one name and leave with three.
   Design for browsing and lateral movement, not for a single lookup that ends
   at a verdict.
4. **Density is respect.** These are readers, not scanners. Typography and
   structure carry the page; decoration earns nothing here.
5. **Homage, not costume.** The voice is borrowed; the credibility has to be
   ours. Wit never comes at the expense of a real business's record.

## Accessibility & Inclusion

WCAG 2.2 AA. Body text ≥4.5:1, large text ≥3:1, visible focus on every
interactive element, full keyboard path through search and history. Every
animation needs a `prefers-reduced-motion` alternative. Inspection results and
establishment status must stay readable when color is unavailable — the badge
labels are text, and they stay text.
