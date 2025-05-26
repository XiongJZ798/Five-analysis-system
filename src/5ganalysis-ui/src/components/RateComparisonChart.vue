<template>
  <div class="rate-comparison-container">
    <el-card class="control-card">
      <template #header>
        <div class="card-header">
          <span>速率对比分析</span>
          <el-tooltip content="比较测试数据中的MAC层速率与理论计算速率" placement="top">
            <el-icon><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
      </template>

      <el-form :model="comparisonForm" label-width="120px" size="default">
        <!-- 计算模式选择 -->
        <el-form-item label="计算模式" prop="mode">
          <el-radio-group v-model="comparisonForm.mode">
            <el-radio label="FDD">FDD (频分双工)</el-radio>
            <el-radio label="TDD">TDD (时分双工)</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- TDD模式下的帧结构选择 -->
        <el-form-item
          v-if="comparisonForm.mode === 'TDD'"
          label="帧结构"
          prop="frameType"
        >
          <el-select v-model="comparisonForm.frameType" placeholder="请选择帧结构类型">
            <el-option label="帧结构1 (2ms)" value="FRAME1" />
            <el-option label="帧结构2 (2.5ms)" value="FRAME2" />
            <el-option label="帧结构3 (2.5ms双)" value="FRAME3" />
            <el-option label="帧结构4 (5ms)" value="FRAME4" />
            <el-option label="帧结构5 (1ms)" value="FRAME5" />
          </el-select>
          <div class="form-item-tip">不同帧结构的周期不同</div>
        </el-form-item>

        <!-- TDD模式下的时隙配置 -->
        <template v-if="comparisonForm.mode === 'TDD' && comparisonForm.frameType">
          <el-form-item label="下行slot数" prop="dlSlots">
            <el-input-number
              v-model="comparisonForm.dlSlots"
              :min="1"
              :precision="0"
              placeholder="请输入下行slot数"
            />
            <div class="form-item-tip">必须大于0</div>
          </el-form-item>

          <el-form-item label="特殊slot数" prop="specialSlots">
            <el-input-number
              v-model="comparisonForm.specialSlots"
              :min="0"
              :max="14"
              :step="1"
              :precision="0"
              placeholder="请输入特殊slot数"
            />
            <div class="form-item-tip">有效范围：0-14</div>
          </el-form-item>
        </template>

        <!-- FDD模式下的时隙配置 -->
        <template v-if="comparisonForm.mode === 'FDD'">
          <el-form-item label="下行slot数" prop="dlSlots">
            <el-input-number
              v-model="comparisonForm.dlSlots"
              :min="1"
              :precision="0"
              placeholder="请输入下行slot数"
            />
            <div class="form-item-tip">必须大于0</div>
          </el-form-item>
        </template>

        <!-- 按钮区 -->
        <el-form-item>
          <el-button type="primary" @click="generateComparisonChart" :disabled="!hasData">生成对比图</el-button>
          <el-button @click="resetForm">恢复默认参数</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 对比图表显示区域 -->
    <div v-if="showChart" class="chart-wrapper">
      <div v-if="isLoading" class="loading-overlay">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <span>数据计算中...</span>
      </div>
      <div ref="comparisonChartRef" class="comparison-chart"></div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { QuestionFilled, Loading } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

export default {
  name: 'RateComparisonChart',
  components: {
    QuestionFilled,
    Loading
  },
  props: {
    hasData: {
      type: Boolean,
      default: false
    },
    timeRange: {
      type: Object,
      default: () => ({
        start: null,
        end: null
      })
    }
  },
  setup(props) {
    // 图表实例
    const comparisonChartRef = ref(null)
    let chartInstance = null

    // 表单数据
    const comparisonForm = reactive({
      mode: 'FDD',
      frameType: '',
      dlSlots: 10,
      specialSlots: 2
    })

    // 图表状态
    const showChart = ref(false)
    const isLoading = ref(false)

    // 图表数据
    const chartData = reactive({
      timePoints: [],
      macRates: [],
      theoreticalRates: [],
      parameters: {}
    })

    // 初始化图表
    const initChart = () => {
      if (chartInstance) {
        chartInstance.dispose()
      }

      nextTick(() => {
        if (comparisonChartRef.value) {
          chartInstance = echarts.init(comparisonChartRef.value)

          // 设置基本配置
          const option = {
            title: {
              text: 'MAC层速率与理论速率对比',
              left: 'center',
              subtext: formatParameters(chartData.parameters),
              subtextStyle: {
                color: '#666',
                fontSize: 12,
                align: 'center'
              }
            },
            tooltip: {
              trigger: 'axis',
              formatter: function(params) {
                let result = params[0].axisValue + '<br/>';
                params.forEach(param => {
                  result += `${param.seriesName}: ${param.value.toFixed(2)} Mbps<br/>`;
                });
                return result;
              }
            },
            legend: {
              data: ['MAC层速率', '理论速率'],
              top: 40
            },
            grid: {
              left: '10%',
              right: '5%',
              bottom: '10%',
              top: '80px',
              containLabel: true
            },
            toolbox: {
              feature: {
                saveAsImage: { title: '保存为图片' },
                dataZoom: { title: { zoom: '区域缩放', back: '还原缩放' } },
                restore: { title: '还原' }
              },
              right: '20px',
              top: '20px'
            },
            xAxis: {
              type: 'category',
              boundaryGap: false,
              data: chartData.timePoints,
              axisLabel: {
                rotate: 30,
                margin: 15,
                fontSize: 11
              }
            },
            yAxis: {
              type: 'value',
              name: '速率 (Mbps)',
              nameLocation: 'middle',
              nameGap: 50,
              axisLabel: {
                formatter: '{value}',
                margin: 12,
                interval: 'auto'
              },
              min: 0,
              splitLine: {
                show: true,
                lineStyle: {
                  type: 'dashed'
                }
              },
              axisPointer: {
                label: {
                  formatter: '{value} Mbps'
                }
              }
            },
            series: [
              {
                name: 'MAC层速率',
                type: 'line',
                data: chartData.macRates,
                smooth: true,
                lineStyle: {
                  width: 2,
                  color: '#5470c6'
                },
                symbol: 'circle',
                symbolSize: 5,
                tooltip: {
                  valueFormatter: (value) => `${value} Mbps`
                }
              },
              {
                name: '理论速率',
                type: 'line',
                data: chartData.theoreticalRates,
                smooth: true,
                lineStyle: {
                  width: 2,
                  color: '#91cc75'
                },
                symbol: 'circle',
                symbolSize: 5,
                sampling: 'average',
                connectNulls: true,
                smoothMonotone: 'x',
                tooltip: {
                  valueFormatter: (value) => `${value} Mbps`
                }
              }
            ]
          }

          chartInstance.setOption(option)
          window.addEventListener('resize', handleResize)
        }
      })
    }

    // 格式化参数显示
    const formatParameters = (params) => {
      if (!params || Object.keys(params).length === 0) {
        return '';
      }

      let result = [];
      if (params.mode) {
        result.push(`模式: ${params.mode}`);
      }
      if (params.frameType) {
        result.push(`帧结构: ${params.frameType}`);
      }
      if (params.dlSlots !== undefined) {
        result.push(`下行时隙: ${params.dlSlots}`);
      }
      if (params.specialSlots !== undefined && params.mode === 'TDD') {
        result.push(`特殊时隙: ${params.specialSlots}`);
      }

      return result.join(' | ');
    }

    // 更新图表数据
    const updateChart = () => {
      if (chartInstance) {
        chartInstance.setOption({
          title: {
            subtext: formatParameters(chartData.parameters)
          },
          xAxis: {
            data: chartData.timePoints
          },
          series: [
            {
              name: 'MAC层速率',
              data: chartData.macRates
            },
            {
              name: '理论速率',
              data: chartData.theoreticalRates,
              smooth: true,
              sampling: 'average',
              connectNulls: true,
              smoothMonotone: 'x'
            }
          ]
        })
      }
    }

    // 生成对比图
    const generateComparisonChart = async () => {
      if (!props.hasData) {
        ElMessage.warning('请先上传测试数据文件')
        return
      }

      // 表单验证
      if (comparisonForm.mode === 'TDD' && !comparisonForm.frameType) {
        ElMessage.warning('TDD模式下必须选择帧结构类型')
        return
      }

      if (comparisonForm.dlSlots <= 0) {
        ElMessage.warning('下行slot数必须大于0')
        return
      }

      if (comparisonForm.mode === 'TDD') {
        if (comparisonForm.specialSlots < 0 || comparisonForm.specialSlots > 14) {
          ElMessage.warning('特殊slot数必须在0-14之间')
          return
        }

        if (comparisonForm.dlSlots + comparisonForm.specialSlots > 14) {
          ElMessage.warning('下行时隙数和特殊时隙数之和不能超过14')
          return
        }
      }

      try {
        isLoading.value = true
        showChart.value = true

        // 准备请求数据
        const requestData = {
          mode: comparisonForm.mode,
          dlSlots: comparisonForm.dlSlots,
          timeRange: props.timeRange
        }

        if (comparisonForm.mode === 'TDD') {
          requestData.frameType = comparisonForm.frameType
          requestData.specialSlots = comparisonForm.specialSlots
        }

        // 发送请求
        const response = await request.post('/analysis/rate-comparison', requestData)

        if (response.code === 200 && response.data) {
          // 更新图表数据
          chartData.timePoints = response.data.timePoints || [];
          chartData.macRates = response.data.macRates || [];
          chartData.theoreticalRates = response.data.theoreticalRates || [];
          chartData.parameters = response.data.parameters || {};

          console.log('收到理论速率数据:', chartData.theoreticalRates);
          console.log('计算参数:', chartData.parameters);

          // 初始化或更新图表
          if (!chartInstance) {
            initChart()
          } else {
            updateChart()
          }

          ElMessage.success('对比图生成成功')
        } else {
          ElMessage.error(response.message || '生成对比图失败')
        }
      } catch (error) {
        console.error('生成对比图出错:', error)
        ElMessage.error('生成对比图失败: ' + (error.message || '未知错误'))
      } finally {
        isLoading.value = false
      }
    }

    // 重置表单
    const resetForm = () => {
      comparisonForm.mode = 'FDD'
      comparisonForm.frameType = ''
      comparisonForm.dlSlots = 10
      comparisonForm.specialSlots = 2
    }

    // 响应窗口大小变化
    const handleResize = () => {
      if (chartInstance) {
        chartInstance.resize()
      }
    }

    // 组件挂载时
    onMounted(() => {
      if (showChart.value && !chartInstance) {
        initChart()
      }
    })

    // 组件卸载前清理资源
    onBeforeUnmount(() => {
      if (chartInstance) {
        chartInstance.dispose()
        chartInstance = null
      }
      window.removeEventListener('resize', handleResize)
    })

    return {
      comparisonChartRef,
      comparisonForm,
      showChart,
      isLoading,
      generateComparisonChart,
      resetForm
    }
  }
}
</script>

<style scoped>
.rate-comparison-container {
  margin-top: 20px;
}

.control-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.form-item-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.chart-wrapper {
  position: relative;
  height: 500px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.comparison-chart {
  width: 100%;
  height: 100%;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.7);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 10;
}

.loading-icon {
  font-size: 24px;
  margin-bottom: 10px;
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
