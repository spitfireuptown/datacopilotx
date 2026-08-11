import { ServerOptions } from 'vite';

export default function (env: Record<string, string>) {
  const server: ServerOptions = {
    // 服务器主机名，如果允许外部访问，可设置为"0.0.0.0"
    host: '0.0.0.0',
    // 服务器端口号
    port: env.VITE_APP_PORT as unknown as number,
    // 是否自动打开浏览器
    open: true,
    proxy: {
      ['/api']: {
        target: 'http://localhost:34567',
        changeOrigin: true,
        rewrite: (path: string) =>
          path.replace(new RegExp('^' + '/api'), ''),
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setTimeout(600000);
          });
          proxy.on('error', (err) => {
            console.error('代理请求错误:', err.message);
          });
        }
      }
    }
  };
  return server;
}
