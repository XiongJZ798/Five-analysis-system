# 5G空口速率分析系统

## 项目简介
5G空口速率分析系统是一个用于分析5G网络性能数据的综合平台。系统提供Excel数据导入、多维度时序数据分析、速率分布分析以及5G下行峰值速率计算等功能，帮助用户更好地理解和分析5G网络性能。

从 v2.0 起，系统升级为 **"规则引擎 + AI 双引擎"** 架构，文件上传后自动触发规则分析与 LLM 智能诊断，生成包含根因推断、优化建议的结构化分析报告。

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

5. **AI 驱动分析（v2.0 新增）**
   - 上传后自动触发 Rule + AI 双引擎分析
   - AI 智能诊断：根因推断、优化建议、3GPP 合规检查
   - 结构化 JSON 报告输出
   - 异步任务状态追踪（PENDING / RUNNING / SUCCESS / PARTIAL_SUCCESS / FAILED）
   - AI 失败自动重试（最多 3 次），规则成功则返回 PARTIAL_SUCCESS
   - 工程师反馈闭环（ACCEPT / REJECT / PARTIAL）

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
- **AI 分析模块（v2.0）**：OpenAI 兼容 LLM 客户端 + 异步任务编排

## 项目结构
```
Five-analysis-system/
├── src/
│   ├── main/                    # 后端代码
│   │   ├── java/
│   │   │   └── com/_5ganalysisrate/g5rate/
│   │   │       ├── config/              # 安全、跨域、异步线程池配置
│   │   │       ├── controller/          # 接口控制器（含 AnalysisTaskController）
│   │   │       ├── domain/
│   │   │       │   ├── entity/          # JPA 实体（AnalysisTask 等）
│   │   │       │   └── enums/           # TaskType、TaskStatus、Severity、Priority
│   │   │       ├── dto/                 # 请求/响应 DTO
│   │   │       ├── model/               # TestData 等基础模型
│   │   │       ├── repository/          # JPA Repository（含4个新分析表 repo）
│   │   │       └── service/
│   │   │           ├── (现有服务)
│   │   │           ├── AnalysisTaskService.java
│   │   │           ├── RuleAnalysisService.java
│   │   │           ├── AiAnalysisService.java
│   │   │           ├── ReportService.java
│   │   │           ├── orchestrator/    # AnalysisOrchestrator（异步编排）
│   │   │           ├── prompt/          # PromptTemplateService（中文提示词）
│   │   │           └── llm/             # LlmClient 接口 + OpenAI 兼容实现
│   │   └── resources/                  # 配置文件、数据库 schema
│   └── 5ganalysis-ui/          # 前端代码
```

## 快速开始

### 环境要求
- JDK 17+
- Node.js 16+
- MySQL 8.0+
- Maven 3.8+

### 环境变量（AI 分析功能）

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `AI_LLM_API_URL` | LLM API 地址（OpenAI 兼容） | `https://api.openai.com/v1/chat/completions` |
| `AI_LLM_API_KEY` | API Key（**必填，生产环境通过环境变量注入**） | `sk-xxx` |
| `AI_LLM_MODEL` | 模型名称 | `gpt-4o-mini` |
| `AI_LLM_TIMEOUT_SECONDS` | 调用超时（秒） | `120` |
| `AI_LLM_MAX_TOKENS` | 最大 Token 数 | `4096` |
| `AI_LLM_TEMPERATURE` | 温度（0.0-1.0） | `0.2` |
| `AI_ASYNC_CORE_POOL_SIZE` | 异步线程池核心数 | `5` |

> 若未配置 `AI_LLM_API_KEY`，任务类型 `RULE_AI` 会以 `PARTIAL_SUCCESS` 结束（规则结果可用）。

支持其他 OpenAI 兼容模型，例如：
- **通义千问**：`AI_LLM_API_URL=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`
- **DeepSeek**：`AI_LLM_API_URL=https://api.deepseek.com/v1/chat/completions`
- **私有部署 Ollama**：`AI_LLM_API_URL=http://localhost:11434/v1/chat/completions`

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

3. 配置 AI 分析（可选）
```bash
export AI_LLM_API_KEY=sk-your-api-key
export AI_LLM_MODEL=gpt-4o-mini
```

4. 启动后端服务
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
export AI_LLM_API_KEY=sk-your-api-key
java -jar target/g5rate-1.0-SNAPSHOT.jar
```

## API文档

### 数据导入
- `POST /api/file/upload`
  - 上传Excel文件
  - 支持格式：.xls/.xlsx
  - 上传成功后**自动创建分析任务**
  - 可选参数：`taskType=RULE_ONLY|RULE_AI`（默认 RULE_AI）
  - 响应中包含 `analysisTask.taskId` 供后续查询

### 时序数据分析
- `POST /api/analysis/time-series-chart`
  - 获取时序图表数据

### 速率分布分析
- `POST /api/analysis/rate-distribution-chart`
  - 获取速率分布数据

### 峰值速率计算
- `POST /api/analysis/calculate-peak-rate`
  - 计算5G下行峰值速率

### AI 分析任务 API（v2.0 新增）

#### 创建分析任务
- `POST /api/analysis/tasks`
```json
{
  "fileId": "test_data.xlsx",
  "taskType": "RULE_AI"
}
```
响应包含 `taskId`，任务异步执行。

#### 查询任务状态
- `GET /api/analysis/tasks/{taskId}`
```json
{
  "code": 200,
  "data": {
    "taskId": 1,
    "status": "RUNNING",
    "progress": 50,
    "taskType": "RULE_AI"
  }
}
```
`status` 枚举值：`PENDING` | `RUNNING` | `SUCCESS` | `PARTIAL_SUCCESS` | `FAILED`

#### 获取规则分析结果
- `GET /api/analysis/tasks/{taskId}/rule-result`
```json
{
  "code": 200,
  "data": {
    "kpiSummary": {"DL_throughput_p50_Mbps": 85.2},
    "peakRate": {"FDD_100MHz_theoretical_peak_Mbps": 3000.0}
  }
}
```

#### 获取 AI 分析结果
- `GET /api/analysis/tasks/{taskId}/ai-result`
```json
{
  "code": 200,
  "data": {
    "summary": "网络性能整体稳定，存在轻微吞吐量波动",
    "findings": [...],
    "rootCauses": [...],
    "actions": [...],
    "complianceCheck": {"is_3gpp_aligned": true}
  }
}
```

#### 获取合并报告
- `GET /api/analysis/tasks/{taskId}/report`
  - 返回规则结果 + AI 结果的合并报告

#### 提交工程师反馈
- `POST /api/analysis/tasks/{taskId}/feedback`
```json
{
  "findingId": "F1",
  "feedbackType": "ACCEPT",
  "comment": "建议已采纳，将安排下周实施"
}
```
`feedbackType` 枚举值：`ACCEPT` | `REJECT` | `PARTIAL`

## AI 输出 Schema

AI 模块严格输出以下结构（服务端校验，不符合则重试）：

```json
{
  "summary": "string",
  "findings": [
    {
      "id": "F1",
      "title": "string",
      "description": "string",
      "evidence": [{"metric": "DL_throughput_p50", "value": 85.2, "unit": "Mbps"}],
      "severity": "HIGH|MEDIUM|LOW"
    }
  ],
  "root_causes": [
    {"finding_id": "F1", "cause": "string", "confidence": 0.75}
  ],
  "actions": [
    {
      "priority": "P0|P1|P2",
      "action": "string",
      "expected_gain": "string",
      "risk": "string"
    }
  ],
  "compliance_check": {"is_3gpp_aligned": true, "notes": "string"},
  "limitations": ["string"]
}
```

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

2. AI 分析
   - 若未配置 `AI_LLM_API_KEY`，AI 分析将跳过，任务置为 `PARTIAL_SUCCESS`
   - AI 分析最多重试 3 次，每次递增延迟（1s、2s、3s）
   - 建议使用私有化部署模型以避免数据隐私风险

3. 性能优化
   - 大数据量时使用分页加载
   - 启用数据库索引
   - 使用数据采样优化图表展示

4. 安全配置
   - 已配置跨域安全策略
   - 文件上传限制
   - SQL注入防护
   - **API Key 必须通过环境变量注入，不能提交到代码仓库**

## 常见问题
1. 跨域问题：检查SecurityConfig和CorsConfig配置
2. 文件上传失败：检查文件大小和格式
3. 数据导入错误：检查Excel格式是否符合要求
4. 图表显示异常：检查数据格式和采样设置
5. AI 分析未执行：检查 `AI_LLM_API_KEY` 是否已配置
6. AI 分析返回 PARTIAL_SUCCESS：查看 `errorMessage` 字段了解 AI 失败原因

## 更新日志
### v2.0.0 (2025)
- 新增 AI 驱动双引擎分析流水线（Rule + AI）
- 文件上传后自动触发分析任务
- 新增分析任务状态机（PENDING/RUNNING/SUCCESS/PARTIAL_SUCCESS/FAILED）
- 新增 AI 分析服务（LLM 调用、重试、JSON 校验）
- 新增 AnalysisTaskController（6 个 RESTful 接口）
- 新增中文 5G 优化提示词模板（版本化管理）
- 新增工程师反馈闭环接口
- 新增 4 张分析持久化数据表

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