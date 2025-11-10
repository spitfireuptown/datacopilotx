import { defineConfig, loadEnv } from 'vite';
import viteConfig from './viteConfig';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());
  const {
    loadPlugins,
    loadViteResolve,
    loadViteCss,
    loadViteServer,
    loadViteBuild,
    loadInitLog
  } = viteConfig;
  loadInitLog();
  return {
    // 移除所有console+debugger,使用默认的esbuild,比 terser 快 20-40倍,压缩率只差 1%-2%。
    esbuild: {
      drop:
        env.VITE_BUILD_DROP_CONSOLE === 'true' ? ['console', 'debugger'] : []
    },
    // 插件配置
    plugins: loadPlugins(),
    // 解析配置
    resolve: loadViteResolve(),
    // css 配置
    css: loadViteCss(),
    // 开发服务器配置
    server: loadViteServer(env),
    // 构建配置
    build: loadViteBuild(env, mode)
  };
});
