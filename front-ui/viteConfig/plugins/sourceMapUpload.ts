import sourceMapUpload from '@heimdallr-sdk/vite-plugin-sourcemap-upload';

export default function setupSourceMapUpload() {
  return sourceMapUpload({
    app_name: 'AntdX_Chat',
    url: `http://localhost:7001/sourcemap/upload`
  });
}
