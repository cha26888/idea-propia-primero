(() => {
    "use strict";

    const appearance = window.AppTheme;
    const themeSelect = document.querySelector("#theme");
    const colorInput = document.querySelector("#accent-color");
    const colorValue = document.querySelector("#color-value");
    const resetButton = document.querySelector("#reset-color");

    if (!appearance || !themeSelect || !colorInput) return;

    function syncControls() {
        const preferences = appearance.getPreferences();

        themeSelect.value = preferences.theme;
        colorInput.value = preferences.color;

        if (colorValue) {
            colorValue.textContent = preferences.color.toUpperCase();
        }
    }

    themeSelect.addEventListener("change", () => {
        appearance.setTheme(themeSelect.value);
    });

    colorInput.addEventListener("input", () => {
        appearance.setColor(colorInput.value);
        syncControls();
    });

    resetButton?.addEventListener("click", () => {
        appearance.setColor(appearance.DEFAULT_COLOR);
        syncControls();
    });

    syncControls();
})();