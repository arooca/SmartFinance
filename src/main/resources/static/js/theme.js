(function () {
    const storageKey = "smartfinance-theme";
    const darkModePreference = window.matchMedia("(prefers-color-scheme: dark)");

    function getTheme() {
        try {
            return localStorage.getItem(storageKey) || (darkModePreference.matches ? "dark" : "light");
        } catch (error) {
            return darkModePreference.matches ? "dark" : "light";
        }
    }

    function applyTheme(theme) {
        document.body.dataset.theme = theme;

        const textColor = getComputedStyle(document.body).getPropertyValue("--text").trim();
        const borderColor = getComputedStyle(document.body).getPropertyValue("--border").trim();

        if (window.Chart) {
            Chart.defaults.color = textColor;
            Chart.defaults.borderColor = borderColor;
            Object.values(Chart.instances).forEach((chart) => chart.update());
        }

        document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
            const isDark = theme === "dark";
            const label = button.querySelector("[data-theme-label]");
            const icon = button.querySelector("[data-theme-icon]");
            const action = isDark ? "Activar modo claro" : "Activar modo oscuro";

            button.setAttribute("aria-label", action);
            button.setAttribute("title", action);

            if (label) {
                label.textContent = isDark ? "Modo claro" : "Modo oscuro";
            }

            if (icon) {
                icon.classList.toggle("bi-sun", isDark);
                icon.classList.toggle("bi-moon-stars", !isDark);
            }
        });
    }

    function saveTheme(theme) {
        try {
            localStorage.setItem(storageKey, theme);
        } catch (error) {
            // The current page still changes theme when storage is unavailable.
        }
    }

    applyTheme(getTheme());

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
            button.addEventListener("click", function () {
                const nextTheme = document.body.dataset.theme === "dark" ? "light" : "dark";
                saveTheme(nextTheme);
                applyTheme(nextTheme);
            });
        });

        darkModePreference.addEventListener("change", function (event) {
            try {
                if (!localStorage.getItem(storageKey)) {
                    applyTheme(event.matches ? "dark" : "light");
                }
            } catch (error) {
                applyTheme(event.matches ? "dark" : "light");
            }
        });
    });
})();
