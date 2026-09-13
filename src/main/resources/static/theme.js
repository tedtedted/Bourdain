// The saved theme is already on <html> — the inline script in <head> put it
// there before first paint. This only drives the header button.
//
// Two states on screen, three underneath: a press flips to the opposite of
// what's showing, and a press that lands back on the system's own theme
// forgets the override, so the page follows the system again.
(() => {
    const KEY = "theme";
    const root = document.documentElement;
    const meta = document.querySelector('meta[name="color-scheme"]');
    const button = document.querySelector(".theme-toggle");
    if (!button) return;

    const systemDark = matchMedia("(prefers-color-scheme: dark)");
    const systemTheme = () => (systemDark.matches ? "dark" : "light");
    const shownTheme = () => root.dataset.theme ?? systemTheme();

    // The icon follows the page in CSS; only the state and hint live here.
    const sync = () => {
        const dark = shownTheme() === "dark";
        button.setAttribute("aria-pressed", String(dark));
        button.title = dark ? "Switch to light theme" : "Switch to dark theme";
    };

    const apply = (theme) => {
        if (theme === "light" || theme === "dark") {
            root.dataset.theme = theme;
            meta.content = theme;
        } else {
            delete root.dataset.theme;
            meta.content = "light dark";
        }
        sync();
    };

    let saved = null;
    try { saved = localStorage.getItem(KEY); } catch {}
    apply(saved);
    button.hidden = false;

    button.addEventListener("click", () => {
        const next = shownTheme() === "dark" ? "light" : "dark";
        const override = next === systemTheme() ? null : next;
        try {
            if (override) localStorage.setItem(KEY, override);
            else localStorage.removeItem(KEY);
        } catch {}
        // Every token flips at once; without this, anything with a colour
        // transition fades across from the old theme for a beat.
        root.classList.add("theme-changing");
        apply(override);
        requestAnimationFrame(() => requestAnimationFrame(() => root.classList.remove("theme-changing")));
    });

    // The system can change under a page that follows it (sunset schedules).
    systemDark.addEventListener("change", sync);

    // Keep other open tabs in step.
    window.addEventListener("storage", (event) => {
        if (event.key === KEY) apply(event.newValue);
    });
})();
