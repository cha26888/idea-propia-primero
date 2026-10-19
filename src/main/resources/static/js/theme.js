(() => {
    "use strict";

    const DEFAULT_COLOR = "#235f49";
    const root = document.documentElement;
    const system = window.matchMedia("(prefers-color-scheme: dark)");

    function read(key) {
        try {
            return localStorage.getItem(key);
        } catch {
            return null;
        }
    }

    function save(key, value) {
        try {
            localStorage.setItem(key, value);
        } catch {
            // Si el almacenamiento está bloqueado, funciona en esta página.
        }
    }

    function validColor(value) {
        return /^#[0-9a-f]{6}$/i.test(value ?? "");
    }

    const savedColor = read("accent-color");
    const savedTheme = read("theme");

    let color = validColor(savedColor) ? savedColor : DEFAULT_COLOR;
    let theme = ["light", "dark", "system"].includes(savedTheme)
        ? savedTheme
        : "system";

    function rgb(hex) {
        return hex.match(/[a-f\d]{2}/gi)
            .map(value => parseInt(value, 16));
    }

    // Mezcla dos colores. amount = proporción del segundo color.
    function mix(a, b, amount) {
        const target = rgb(b);

        return "#" + rgb(a).map((value, index) =>
            Math.round(value + (target[index] - value) * amount)
                .toString(16)
                .padStart(2, "0")
        ).join("");
    }

    function luminance(hex) {
        const [r, g, b] = rgb(hex).map(value => {
            value /= 255;

            return value <= 0.04045
                ? value / 12.92
                : ((value + 0.055) / 1.055) ** 2.4;
        });

        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    function contrast(a, b) {
        const x = luminance(a);
        const y = luminance(b);

        return (Math.max(x, y) + 0.05)
            / (Math.min(x, y) + 0.05);
    }
    function pastel(hex, lightness) {
        const [r, g, b] = rgb(hex).map(value => value / 255);
        const max = Math.max(r, g, b);
        const min = Math.min(r, g, b);
        const difference = max - min;
        const originalLightness = (max + min) / 2;

        let hue = 0;
        let saturation = 0;

        if (difference > 0) {
            saturation = difference
                / (1 - Math.abs(2 * originalLightness - 1));

            if (max === r) {
                hue = (g - b) / difference;
            } else if (max === g) {
                hue = (b - r) / difference + 2;
            } else {
                hue = (r - g) / difference + 4;
            }

            hue = (hue * 60 + 360) % 360;

            // Refuerza el color sin llevarlo a una saturación extrema.
            saturation = Math.min(0.80, saturation * 1.3);
        }

        const amplitude = saturation * Math.min(lightness, 1 - lightness);

        function channel(offset) {
            const k = (offset + hue / 30) % 12;
            const value = lightness - amplitude
                * Math.max(-1, Math.min(k - 3, 9 - k, 1));

            return Math.round(value * 255).toString(16).padStart(2, "0");
        }

        return "#" + channel(0) + channel(8) + channel(4);
    }
    function apply() {
        const dark = theme === "dark"
            || (theme === "system" && system.matches);
            
                root.dataset.theme = dark ? "dark" : "light";
                root.style.colorScheme = dark ? "dark" : "light";
        
               
        const bg = dark
            ? mix(color, "#101010", 0.92)
            : pastel(color, 0.90);
            
        const surface = dark
            ? mix(color, "#181818", 0.88)
            : pastel(color, 0.97);
            
        const agent = dark
            ? mix(color, "#202020", 0.84)
            : pastel(color, 0.86);
                const target = dark ? "#ffffff" : "#000000";
                const backgrounds = [bg, surface, agent];
            
                // Ajusta la intensidad del acento hasta hacerlo legible.
                let primary = color;
            
                for (
                    let step = 1;
                    step <= 100
                        && backgrounds.some(bg => contrast(primary, bg) < 4.5);
                    step++
                ) {
            primary = mix(color, target, step / 100);
        }

        const palette = {
            "--color-bg": bg,
            "--color-surface": surface,
            "--color-agent-bg": agent,
            "--color-text": mix(
                color, dark ? "#ffffff" : "#000000", dark ? 0.94 : 0.82
            ),
            "--color-muted": mix(
                color, dark ? "#cccccc" : "#444444", 0.85
            ),
            "--color-border": mix(
                color, dark ? "#888888" : "#777777", 0.75
            ),
            "--color-primary": primary,
            "--color-primary-hover": mix(primary, target, 0.12),
            "--color-on-primary": dark ? "#000000" : "#ffffff",
            "--color-user-bg": primary,
            "--color-footer-bg": mix(color, "#101010", 0.85)
        };

        for (const [name, value] of Object.entries(palette)) {
            root.style.setProperty(name, value);
        }

        // Error, éxito y advertencia siguen definidos en theme.css.
    }

    window.AppTheme = {
        DEFAULT_COLOR,

        getPreferences() {
            return { color, theme };
        },

        setColor(value) {
            if (!validColor(value)) return;

            color = value.toLowerCase();
            save("accent-color", color);
            apply();
        },

        setTheme(value) {
            if (!["light", "dark", "system"].includes(value)) return;

            theme = value;
            save("theme", theme);
            apply();
        }
    };

    system.addEventListener("change", () => {
        if (theme === "system") apply();
    });

    apply();
})();