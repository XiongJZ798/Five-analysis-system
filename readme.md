# 5G空口速率分析系统

## 项目简介
5G空口速率分析系统是一个用于分析5G网络性能数据的综合平台。系统提供Excel数据导入、多维度时序数据分析、速率分布分析以及5G下行峰值速率计算等功能，帮助用户更好地理解和分析5G网络性能。

## 主要功能
1. **数据导入功能**
   - 支持Excel文件(.xls/.xlsx)上传
   - 自动数据格式验证
   - 批量数据导入
   - 实时导入进度显示

2. **时序数据分析**
   - 多维度指标分析（RSRP、SINR、吞吐量等）
   - 交互式时序图表
   - 数据缩放和平移
   - 自动采样优化

3. **速率分布分析**
   - 自定义速率区间
   - 饼图可视化展示
   - 百分比分布统计
   - 实时数据更新

4. **峰值速率计算**
   - 支持FDD/TDD模式
   - 多种帧结构支持
   - 自动参数验证
   - 实时计算结果

## 技术架构

### 前端技术栈
- Vue 3 (Composition API)
- Vite 构建工具
- Element Plus UI框架
- ECharts 图表库
- Axios HTTP客户端

### 后端技术栈
- Spring Boot 3.4.0
- Spring Data JPA
- MySQL 8.0
- Spring Security
- Apache POI

## 项目结构
```
Five-analysis-system/
├── src/
│   ├── main/                    # 后端代码
│   │   ├── java/
│   │   │   └── com/_5ganalysisrate/g5rate/
│   │   │       ├── config/      # 安全配置、跨域配置等
│   │   │       ├── controller/  # 接口控制器
│   │   │       ├── dto/         # 数据传输对象
│   │   │       ├── model/       # 数据模型
│   │   │       ├── repository/  # 数据访问层
│   │   │       └── service/     # 业务逻辑层
│   │   └── resources/          # 配置文件
│   └── 5ganalysis-ui/          # 前端代码
│       ├── src/
│       │   ├── assets/         # 静态资源
│       │   ├── components/     # 通用组件
│       │   ├── router/         # 路由配置
│       │   ├── utils/         # 工具类
│       │   └── views/         # 页面组件
│       └── public/            # 公共资源
```

## 快速开始

### 环境要求
- JDK 17+
- Node.js 16+
- MySQL 8.0+
- Maven 3.8+

### 后端启动
1. 创建MySQL数据库
```sql
CREATE DATABASE g5rate DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改数据库配置
编辑 `src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/g5rate?useUnicode=true&characterEncoding=utf8
    username: your_username
    password: your_password
```

3. 启动后端服务
```bash
mvn clean install
mvn spring-boot:run
```

### 前端启动
1. 安装依赖
```bash
cd src/5ganalysis-ui
npm install
```

2. 启动开发服务器
```bash
npm run dev
```

## 部署说明

### Docker部署
使用Docker Compose一键部署：
```bash
docker-compose up -d
```

### 传统部署
1. 构建前端
```bash
cd src/5ganalysis-ui
npm run build
```

2. 构建后端
```bash
mvn clean package
```

3. 运行应用
```bash
java -jar target/g5rate-1.0-SNAPSHOT.jar
```

## API文档

### 数据导入
- POST `/api/file/upload`
  - 上传Excel文件
  - 支持格式：.xls/.xlsx
  - 最大文件大小：10MB

### 时序数据分析
- POST `/api/analysis/time-series-chart`
  - 获取时序图表数据
  - 支持多维度分析

### 速率分布分析
- POST `/api/analysis/rate-distribution-chart`
  - 获取速率分布数据
  - 支持自定义区间

### 峰值速率计算
- POST `/api/analysis/calculate-peak-rate`
  - 计算5G下行峰值速率
  - 支持FDD/TDD模式

## 开发规范
1. 代码风格
   - 使用EditorConfig保持代码风格一致
   - 遵循ESLint规则
   - 使用Prettier格式化代码

2. 提交规范
   - 使用语义化的提交信息
   - 提交前进行代码检查

3. 文档规范
   - 及时更新API文档
   - 编写清晰的注释
   - 保持README文档最新

## 注意事项
1. 数据导入
   - Excel文件大小限制为10MB
   - 必须包含所有必需字段
   - 数据格式需符合要求

2. 性能优化
   - 大数据量时使用分页加载
   - 启用数据库索引
   - 使用数据采样优化图表展示

3. 安全配置
   - 已配置跨域安全策略
   - 文件上传限制
   - SQL注入防护

## 常见问题
1. 跨域问题：检查SecurityConfig和CorsConfig配置
2. 文件上传失败：检查文件大小和格式
3. 数据导入错误：检查Excel格式是否符合要求
4. 图表显示异常：检查数据格式和采样设置

## 更新日志
### v1.0.0 (2024-03)
- 实现基础数据导入功能
- 完成时序数据分析模块
- 实现速率分布分析
- 添加峰值速率计算功能
- 优化图表展示效果
- 完善错误处理机制

## 维护者
- 开发团队

## 许可证
MIT License 