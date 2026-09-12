import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: ["class"],
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        // NOVA Design Tokens
        nova: {
          bg: "#060a14",
          surface: "#0d1629",
          surface2: "#111d35",
          border: "#1a2744",
          border2: "#243560",
          cyan: "#00d4ff",
          "cyan-dim": "#00a8cc",
          blue: "#2563eb",
          "blue-bright": "#3b82f6",
          critical: "#ff3b3b",
          "critical-dim": "#cc2020",
          high: "#ff7a00",
          medium: "#ffd700",
          low: "#22c55e",
          text: "#e8f0fe",
          "text-dim": "#8899bb",
          "text-muted": "#4a5a7a",
        },
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        chart: {
          "1": "hsl(var(--chart-1))",
          "2": "hsl(var(--chart-2))",
          "3": "hsl(var(--chart-3))",
          "4": "hsl(var(--chart-4))",
          "5": "hsl(var(--chart-5))",
        },
      },
      fontFamily: {
        sans: ["var(--font-inter)", "Inter", "system-ui", "sans-serif"],
        mono: ["var(--font-mono)", "JetBrains Mono", "monospace"],
        display: ["var(--font-space)", "Space Grotesk", "sans-serif"],
      },
      backgroundImage: {
        "nova-gradient": "linear-gradient(135deg, #060a14 0%, #0d1629 50%, #060a14 100%)",
        "cyan-glow": "radial-gradient(ellipse at center, rgba(0, 212, 255, 0.15) 0%, transparent 70%)",
        "hero-grid": "linear-gradient(rgba(0,212,255,0.03) 1px, transparent 1px), linear-gradient(90deg, rgba(0,212,255,0.03) 1px, transparent 1px)",
        "glass": "linear-gradient(135deg, rgba(255,255,255,0.05) 0%, rgba(255,255,255,0.02) 100%)",
        "critical-glow": "radial-gradient(ellipse at center, rgba(255,59,59,0.2) 0%, transparent 70%)",
      },
      backgroundSize: {
        "grid-40": "40px 40px",
      },
      boxShadow: {
        "nova": "0 0 0 1px rgba(0,212,255,0.1), 0 4px 24px rgba(0,0,0,0.4)",
        "nova-cyan": "0 0 20px rgba(0,212,255,0.3), 0 0 40px rgba(0,212,255,0.1)",
        "nova-critical": "0 0 20px rgba(255,59,59,0.4), 0 0 40px rgba(255,59,59,0.2)",
        "nova-card": "0 0 0 1px rgba(26,39,68,0.8), 0 8px 32px rgba(0,0,0,0.5)",
        "glow-sm": "0 0 8px rgba(0,212,255,0.4)",
        "glow-md": "0 0 16px rgba(0,212,255,0.4)",
        "glow-lg": "0 0 32px rgba(0,212,255,0.3)",
      },
      animation: {
        "pulse-slow": "pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite",
        "ping-slow": "ping 2s cubic-bezier(0, 0, 0.2, 1) infinite",
        "spin-slow": "spin 3s linear infinite",
        "float": "float 6s ease-in-out infinite",
        "glow-pulse": "glow-pulse 2s ease-in-out infinite",
        "slide-in-right": "slide-in-right 0.3s ease-out",
        "slide-in-left": "slide-in-left 0.3s ease-out",
        "fade-up": "fade-up 0.4s ease-out",
        "counter": "counter 2s ease-out forwards",
        "scan": "scan 2s linear infinite",
        "ticker": "ticker 30s linear infinite",
      },
      keyframes: {
        float: {
          "0%, 100%": { transform: "translateY(0px)" },
          "50%": { transform: "translateY(-10px)" },
        },
        "glow-pulse": {
          "0%, 100%": { boxShadow: "0 0 10px rgba(0,212,255,0.3)" },
          "50%": { boxShadow: "0 0 30px rgba(0,212,255,0.8), 0 0 60px rgba(0,212,255,0.4)" },
        },
        "slide-in-right": {
          from: { transform: "translateX(100%)", opacity: "0" },
          to: { transform: "translateX(0)", opacity: "1" },
        },
        "slide-in-left": {
          from: { transform: "translateX(-100%)", opacity: "0" },
          to: { transform: "translateX(0)", opacity: "1" },
        },
        "fade-up": {
          from: { transform: "translateY(20px)", opacity: "0" },
          to: { transform: "translateY(0)", opacity: "1" },
        },
        scan: {
          "0%": { transform: "translateY(-100%)" },
          "100%": { transform: "translateY(100%)" },
        },
        ticker: {
          "0%": { transform: "translateX(0)" },
          "100%": { transform: "translateX(-50%)" },
        },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
    },
  },
  plugins: [],
};

export default config;
