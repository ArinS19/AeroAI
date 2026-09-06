/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        // ─── Brand tokens (IMPLEMENT_v2.md §4.2 — DO NOT change these values) ───
        'aero-base':    '#0A0E12',   // page background
        'aero-surface': '#12171D',   // elevated surfaces / panels
        'aero-border':  '#1E2A35',   // subtle borders
        'aero-muted':   '#263344',   // muted surface
        'aero-cyan':    '#22D3EE',   // primary accent / live indicator
        'aero-blue':    '#3B82F6',   // secondary blue / CTA
        // Semantic state colors
        'state-green':  '#22C55E',   // low risk / on-time / landed
        'state-amber':  '#F59E0B',   // medium risk / delayed data
        'state-red':    '#EF4444',   // high risk / error
        'state-gray':   '#6B7280',   // unavailable
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'ui-monospace', 'monospace'],
      },
      fontSize: {
        'tnum': ['inherit', { fontVariantNumeric: 'tabular-nums' }],
      },
      animation: {
        'pulse-slow': 'pulse 3s ease-in-out infinite',
        'fade-in': 'fadeIn 150ms ease-out',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0', transform: 'translateY(4px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
      transitionDuration: {
        DEFAULT: '200ms',
      },
      transitionTimingFunction: {
        DEFAULT: 'ease',
      },
    },
  },
  plugins: [],
}
