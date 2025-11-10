import type { PluginOption } from 'vite';
import setupAutoImport from './autoImport';
import setupCommonPlugins from './common';
import setupStaticPerf from './staticPerf';
import setupAutoImportComponents from './autoComponents';

export default function () {
  const plugins: PluginOption[] = [
    ...setupCommonPlugins(),
    setupAutoImport(),
    setupAutoImportComponents(),
    ...setupStaticPerf()
  ];

  return plugins;
}
