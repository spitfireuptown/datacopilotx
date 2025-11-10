import { CSSOptions } from 'vite';
import autoprefixer from 'autoprefixer';
import tailwindcss from 'tailwindcss';

export default function (): CSSOptions {
  const preprocessorOptions = {
    scss: {
      api: 'modern-compiler',
      additionalData: '$color: red;'
    }
  };
  const postcss = {
    plugins: [
      autoprefixer({
        overrideBrowserslist: [
          'Android 4.1',
          'iOS 7.1',
          'Chrome > 31',
          'ff > 31',
          'ie >= 8',
          'last 2 versions'
        ],
        grid: true
      }),
      tailwindcss()
    ]
  };
  return { preprocessorOptions, postcss };
}
