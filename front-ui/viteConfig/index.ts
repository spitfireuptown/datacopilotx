import loadPlugins from './plugins';
import loadViteResolve from './resolve';
import loadViteServer from './server';
import loadViteBuild from './build';
import loadViteCss from './css';

const loadInitLog = () => {};
export default {
  loadPlugins,
  loadViteResolve,
  loadViteServer,
  loadViteBuild,
  loadViteCss,
  loadInitLog
};
