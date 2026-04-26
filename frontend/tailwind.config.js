/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        border: "#E5E7EB",
        input: "#E5E7EB",
        ring: "#111827",
        background: "#F7F7F4",
        foreground: "#111827",
        primary: {
          DEFAULT: "#111827",
          foreground: "#F9FAFB"
        },
        secondary: {
          DEFAULT: "#EEF2F7",
          foreground: "#111827"
        },
        muted: {
          DEFAULT: "#F3F4F6",
          foreground: "#6B7280"
        },
        accent: {
          DEFAULT: "#E8F1FF",
          foreground: "#0F172A"
        },
        card: {
          DEFAULT: "#FFFFFF",
          foreground: "#111827"
        }
      },
      borderRadius: {
        xl: "1rem",
        "2xl": "1.25rem"
      },
      boxShadow: {
        soft: "0 16px 40px rgba(15, 23, 42, 0.08)"
      }
    }
  },
  plugins: []
};
