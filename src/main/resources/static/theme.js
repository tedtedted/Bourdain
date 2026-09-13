// The saved theme is already on <html> — the inline script in <head> put it
// there before first paint. This only drives the header control.
(() => {
    const KEY = "theme";
    const root = document.documentElement;
    const meta = document.querySelector('meta[name="color-scheme"]');
    const control = document.querySelector(".theme-switch");
    if (!control) return;

    const apply = (theme) => {
        if (theme === "light" || theme === "dark") {
            root.dataset.theme = theme;
            meta.content = theme;
        } else {
            theme = "system";
            delete root.dataset.theme;
            meta.content = "light dark";
        }
        control.querySelector(`input[value="${theme}"]`).checked = true;
    };

    let saved = null;
    try { saved = localStorage.getItem(KEY); } catch {}
    apply(saved);
    control.hidden = false;

    control.addEventListener("change", (event) => {
        const theme = event.target.value;
        try {
            if (theme === "system") localStorage.removeItem(KEY);
            else localStorage.setItem(KEY, theme);
        } catch {}
        // Every token flips at once; without this, anything with a colour
        // transition fades across from the old theme for a beat.
        root.classList.add("theme-changing");
        apply(theme);
        requestAnimationFrame(() => requestAnimationFrame(() => root.classList.remove("theme-changing")));
    });

    // Keep other open tabs in step.
    window.addEventListener("storage", (event) => {
        if (event.key === KEY) apply(event.newValue);
    });
})();
