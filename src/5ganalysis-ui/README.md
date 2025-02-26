# 5G空口速率分析系统 - 前端

## 项目介绍
本项目是5G空口速率分析系统的前端部分，基于Vue 3 + Vite + Element Plus开发。

## 技术栈
- Vue 3 (Composition API)
- Vite
- Element Plus
- ECharts
- Axios
- Vue Router

## 目录结构
```
src/
├── assets/          # 静态资源
├── components/      # 通用组件
├── router/          # 路由配置
├── utils/          # 工具函数
└── views/          # 页面组件
```

## 开发环境
- Node.js >= 16
- npm >= 8

## 快速开始

### 安装依赖
```bash
npm install
```

### 开发模式
```bash
npm run dev
```

### 生产构建
```bash
npm run build
```

### 代码检查
```bash
npm run lint
```

## 环境变量
项目使用以下环境变量：
- `VITE_APP_API_BASE_URL`: API基础URL
- `VITE_APP_TITLE`: 应用标题
- `VITE_APP_ENV`: 环境标识

## 主要功能
1. 数据导入
   - Excel文件上传
   - 数据预览
   - 导入进度显示

2. 时序数据分析
   - 多维度数据展示
   - 交互式图表
   - 数据筛选

3. 速率分布分析
   - 自定义区间
   - 饼图展示
   - 实时统计

4. 峰值速率计算
   - 参数配置
   - 实时计算
   - 结果展示

## 开发规范
1. 组件命名使用PascalCase
2. 文件命名使用kebab-case
3. 使用Composition API编写组件
4. 使用TypeScript进行类型检查
5. 遵循ESLint规则

## 构建部署
1. 修改生产环境配置
2. 执行构建命令
3. 将dist目录部署到Web服务器

## 注意事项
1. 开发时需要后端服务支持
2. 注意跨域配置
3. 生产环境需要正确的API地址配置

## 常见问题
1. 跨域问题解决
2. 环境变量配置
3. 构建优化
4. 性能优化

## 更新日志
### v1.0.0
- 初始版本发布
- 实现基础功能
- 完成与后端对接
