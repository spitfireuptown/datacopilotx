/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          primary: 'var(--brand-primary)',
          DEFAULT: 'var(--brand-primary)'
        },
        surface: {
          page: 'var(--bg-page)',
          sidebar: 'var(--bg-sidebar)',
          elevated: 'var(--bg-elevated)',
          subtle: 'var(--bg-subtle)'
        }
      }
    }
  },
  plugins: []
};
