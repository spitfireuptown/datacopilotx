import path from 'path';
import { AliasOptions, ResolveOptions } from 'vite';

type Resolve = ResolveOptions & {
  alias?: AliasOptions;
};

export default function (): Resolve {
  // 获取项目根目录路径
  const projectRoot = process.cwd();

  // 别名配置相对于当前文件路径
  const alias: AliasOptions = {
    '@': path.resolve(projectRoot, 'src')
  };
  return { alias };
}
