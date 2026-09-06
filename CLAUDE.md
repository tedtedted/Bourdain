# Bourdain

Chicago food inspections, searchable. Java 25 / Spring Boot 4 / Spring Modulith /
PostgreSQL / Flyway / Thymeleaf + htmx. See [README.md](README.md) for modules,
local run, and deploy.

## Design Context

Full strategic context lives in [PRODUCT.md](PRODUCT.md); visual system in
DESIGN.md. Read PRODUCT.md before any UI work.

- **Register: product.** Design serves the task. Search and inspection history
  are the product; the home page is an entry point, not a marketing page.
- **Users:** curious Chicago locals browsing places they already know, mostly on
  desktop, one search leading to the next.
- **Voice:** homage to Anthony Bourdain's *No Reservations* — unsentimental,
  curious, respectful. Homage, not impersonation: never attribute invented words
  to a real person.
- **Not:** chispections.com (the site this borrowed its concept from — its
  styling came along and is being fully reset), generic SaaS dashboards,
  government data portals.

Principles, in brief:

1. **The record speaks** — present what the city found; no scores, no rankings.
2. **Closed is a claim, not a fact** — status always shows its work.
3. **Reward the next search** — design for browsing, not a single verdict.
4. **Density is respect** — typography and structure carry the page.
5. **Homage, not costume** — wit never at the expense of a real business.

Accessibility bar is WCAG 2.2 AA.
