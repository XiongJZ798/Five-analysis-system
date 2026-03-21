# 5G空口速率分析系统

基于 **Java (Spring Boot) + Vue 3** 构建的 5G 网络性能智能分析平台。

---

## 功能概述

### 原有功能
- Excel 数据导入（5G 测试数据批量解析）
- 多维度时序分析（RSRP / SINR / MAC吞吐量 / MCS / PRB / Rank / BLER）
- 速率分布统计（饼图 / 可自定义区间）
- 3GPP 峰值速率计算（FDD / TDD 模式）
- 速率对比分析

### v2.0 新增：AI 驱动双引擎分析
上传 Excel 文件后自动触发 **规则引擎 + AI 大模型** 双引擎分析：

- **规则分析层**：确定性计算，结果可复现可审计
- **AI 分析层**：调用 OpenAI 兼容 API，生成结构化 JSON 诊断报告
  - `summary`：总体结论
  - `findings`：发现（含证据指标、严重等级）
  - `root_causes`：根因分析（含置信度）
  - `actions`：优化建议（P0/P1/P2 优先级）
  - `compliance_check`：3GPP 合规校验
  - `limitations`：分析局限性
- **容错机制**：AI 失败不影响规则分析结果（`PARTIAL_SUCCESS` 状态）
- **重试机制**：LLM 调用最多自动重试 3 次

---

## 新增 REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/file/upload` | 上传 Excel，自动触发分析任务 |
| `POST` | `/api/analysis/tasks` | 手动创建分析任务 |
| `GET`  | `/api/analysis/tasks/{id}` | 查询任务状态与进度 |
| `GET`  | `/api/analysis/tasks/{id}/rule-result` | 获取规则分析结果 |
| `GET`  | `/api/analysis/tasks/{id}/ai-result` | 获取 AI 分析结果 |
| `GET`  | `/api/analysis/tasks/{id}/report` | 获取合并报告 |
| `POST` | `/api/analysis/tasks/{id}/feedback` | 提交工程师反馈 |

---

## 新增数据库表

| 表名 | 说明 |
|------|------|
| `analysis_task` | 分析任务主表（状态机） |
| `analysis_result_rule` | 规则分析结果（KPI / 时序 / 分布 / 峰值速率） |
| `analysis_result_ai` | AI 分析结果（模型输出 JSON / Token / 耗时） |
| `analysis_feedback` | 工程师反馈（ACCEPT / REJECT / PARTIAL） |

---

## 快速启动

### 环境要求
- Java 21+
- Maven 3.9+
- MySQL 8.0+（或 H2 用于测试）

### 配置 AI 分析（可选）

在 `application.yml` 或环境变量中配置：

```yaml
ai:
  llm:
    api-url: https://api.openai.com/v1/chat/completions  # 或兼容接口
    api-key: sk-xxx                                       # 必填
    model: gpt-4o-mini
```

若未配置 `api-key`，规则分析正常运行，AI 分析模块自动跳过（任务状态为 `PARTIAL_SUCCESS`）。

### 启动后端

```bash
mvn spring-boot:run
```

### 启动前端

```bash
cd src/5ganalysis-ui
npm install
npm run dev
```

---

## 项目结构（后端核心）

```
src/main/java/com/_5ganalysisrate/g5rate/
├── controller/
│   ├── AnalysisController.java        # 原有分析接口
│   ├── AnalysisTaskController.java    # 新增：AI任务接口
│   └── FileController.java            # 文件上传（升级：自动触发任务）
├── domain/
│   ├── entity/                        # JPA实体（AnalysisTask等4个）
│   └── enums/                         # 状态枚举（TaskStatus/TaskType等）
├── service/
│   ├── AiAnalysisService.java         # AI分析（LLM调用+验证+重试）
│   ├── RuleAnalysisService.java       # 规则分析（封装现有能力）
│   ├── AnalysisTaskService.java       # 任务CRUD+反馈
│   ├── ReportService.java             # 报告聚合
│   ├── llm/                           # LLM客户端（OpenAI兼容）
│   ├── orchestrator/                  # 异步流水线编排器
│   └── prompt/                        # 提示词模板服务
└── repository/                        # JPA仓库（4个新增）
```

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v2.0 | 2026-03 | AI驱动双引擎分析，异步任务编排，工程师反馈闭环 |
| v1.0 | 2025 | 基础5G速率分析功能上线 |
