<template>
  <div class="data-analysis">
    <!-- 时序数据分析 -->
    <el-card class="analysis-card">
      <template #header>
        <div class="card-header">
          <span>时序数据分析</span>
        </div>
      </template>
<!----
      数据分析视图
│   功能：
│   1. 多维度数据分析
│      - RSRP (信号接收功率)
│      - SINR (信噪比)
│      - MAC层下行速率
│      - Rank (MIMO层数)
│      - MCS (调制编码方案)
│      - RB数 (资源块数)
│      - BLER (误块率)
│   2. 时序图表展示
│      - 支持多维度数据对比
│      - 图表缩放功能
│      - 数据点悬浮提示
│   3. 数据筛选功能
│   4. 实时数据更新
-->
      <!-- 查询条件 -->
      <el-form :model="queryForm" label-width="100px" class="query-form">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="分析维度">
          <el-select
            v-model="queryForm.dimensions"
            multiple
            placeholder="请选择分析维度"
          >
            <el-option
              v-for="item in dimensionOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="queryData">查询</el-button>
        </el-form-item>
      </el-form>

      <!-- 图表展示 -->
      <div ref="timeSeriesChart" class="chart-container"></div>
    </el-card>

    <!-- 速率分布分析 -->
    <el-card class="analysis-card">
      <template #header>
        <div class="card-header">
          <span>速率分布分析</span>
          <el-button type="primary" @click="queryRateDistribution">刷新</el-button>
        </div>
      </template>

      <!-- 饼图展示 -->
      <div ref="rateDistributionChart" class="chart-container"></div>
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

export default {
  name: 'DataAnalysisView',
  setup() {
    // 图表实例
    let timeSeriesChartInstance = null
    let rateDistributionChartInstance = null
    const timeSeriesChart = ref(null)
    const rateDistributionChart = ref(null)

    // 查询表单
    const queryForm = ref({
      timeRange: [],
      dimensions: []
    })

    // 维度选项
    const dimensionOptions = [
      { label: 'RSRP (dBm)', value: 'rsrp(dBm)' },
      { label: 'SINR (dB)', value: 'sinr(dB)' },
      { label: 'MAC吞吐量 (Mbps)', value: 'Mac层下行速率(Mbps)' },
      { label: 'MIMO层数', value: 'rank(层数)' },
      { label: 'MCS', value: 'mcs(调制编码方案)' },
      { label: 'RB数', value: 'rbNum(资源块数)' },
      { label: 'BLER (%)', value: 'bler(误块率)' }
    ]

    // 初始化图表
    const initCharts = () => {
      timeSeriesChartInstance = echarts.init(timeSeriesChart.value)
      rateDistributionChartInstance = echarts.init(rateDistributionChart.value)
      
      // 监听窗口大小变化
      window.addEventListener('resize', handleResize)
    }

    // 处理窗口大小变化
    const handleResize = () => {
      timeSeriesChartInstance?.resize()
      rateDistributionChartInstance?.resize()
    }

    // 查询时序数据
    const queryData = async () => {
      if (!queryForm.value.timeRange[0] || !queryForm.value.timeRange[1]) {
        ElMessage.warning('请选择时间范围')
        return
      }
      if (queryForm.value.dimensions.length === 0) {
        ElMessage.warning('请选择至少一个分析维度')
        return
      }

      try {
        const response = await axios.post('/api/analysis/chart-data', {
          startTime: queryForm.value.timeRange[0],
          endTime: queryForm.value.timeRange[1],
          dimensions: queryForm.value.dimensions
        })

        if (response.data.code === 200) {
          updateTimeSeriesChart(response.data.data)
        }
      } catch (error) {
        ElMessage.error('查询数据失败')
      }
    }

    // 查询速率分布
    const queryRateDistribution = async () => {
      try {
        const response = await axios.get('/api/analysis/rate-distribution-chart')
        if (response.data.code === 200) {
          updateRateDistributionChart(response.data.data)
        }
      } catch (error) {
        ElMessage.error('查询速率分布失败')
      }
    }

    // 更新时序图表
    const updateTimeSeriesChart = (data) => {
      const option = {
        title: {
          text: data.title
        },
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: data.series.map(s => s.name)
        },
        xAxis: {
          type: 'category',
          data: data.xAxisData,
          name: data.xAxisName
        },
        yAxis: {
          type: 'value',
          name: data.yAxisName
        },
        dataZoom: [
          {
            type: 'slider',
            show: true,
            xAxisIndex: [0],
            start: 0,
            end: 100
          }
        ],
        series: data.series
      }
      timeSeriesChartInstance.setOption(option)
    }

    // 更新速率分布图表
    const updateRateDistributionChart = (data) => {
      const option = {
        title: {
          text: data.title
        },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}%'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [
          {
            type: 'pie',
            radius: '50%',
            data: data.xAxisData.map((name, index) => ({
              name,
              value: data.series[0].data[index]
            })),
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      }
      rateDistributionChartInstance.setOption(option)
    }

    // 生命周期钩子
    onMounted(() => {
      initCharts()
      queryRateDistribution()
    })

    onUnmounted(() => {
      window.removeEventListener('resize', handleResize)
      timeSeriesChartInstance?.dispose()
      rateDistributionChartInstance?.dispose()
    })

    return {
      timeSeriesChart,
      rateDistributionChart,
      queryForm,
      dimensionOptions,
      queryData,
      queryRateDistribution
    }
  }
}
</script>

<style scoped>
.data-analysis {
  padding: 20px;
}

.analysis-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.query-form {
  margin-bottom: 20px;
}

.chart-container {
  height: 400px;
  width: 100%;
}
</style> 