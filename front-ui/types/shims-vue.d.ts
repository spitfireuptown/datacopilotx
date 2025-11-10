declare module '@kangc/v-md-editor/lib/preview' {
  import { App, Component } from 'vue';

  interface VMdPreviewInstance extends Component {
    use: (theme: any, options?: any) => any;
    install: (app: App) => void;
  }

  const VMdPreview: VMdPreviewInstance;
  export default VMdPreview;
}

declare module '@kangc/v-md-editor/lib/theme/vuepress.js' {
  const component: any;
  export default component;
}
