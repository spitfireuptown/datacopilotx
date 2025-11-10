import minipic from 'vite-plugin-minipic';
import svgLoader from 'vite-svg-loader';

export default function setupStaticPerf() {
  return [
    minipic({
      // @ts-ignore-next-line
      sharpOptions: {
        gif: {
          reuse: true
        }
      },
      limitInputPixels: true
    }),
    svgLoader()
  ];
}
