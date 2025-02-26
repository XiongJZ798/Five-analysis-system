<template>
  <div class="dashboard">
    <!-- 页面标题 -->
    <div class="dashboard-header">
      <h1>5G空口速率分析系统</h1>
      <div class="dashboard-desc">用于分析5G网络性能数据，提供数据可视化和峰值计算功能</div>
    </div>
<!--
    主面板视图
│   功能：
│   1. 整合所有功能模块的容器组件
│   2. 提供统一的布局和导航
│   3. 管理全局状态和数据流-->

    <!-- 1. 文件导入模块 -->
    <el-card class="module-card">
      <template #header>
        <div class="card-header">
          <span>文件导入</span>
        </div>
      </template>
      
      <el-upload
        class="upload-demo"
        :action="uploadUrl"
        :headers="headers"
        :before-upload="beforeUpload"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :show-file-list="true"
        :file-list="fileList"
        accept=".xls,.xlsx"
        :limit="1"
      >
        <template #trigger>
          <el-button type="primary">选择文件</el-button>
        </template>
        <template #tip>
          <div class="el-upload__tip">
            只能上传Excel文件（.xls/.xlsx），且不超过10MB
          </div>
        </template>
      </el-upload>

      <!-- 显示当前数据文件信息 -->
      <div v-if="currentFile" class="current-file-info">
        <el-alert
          title="当前数据文件"
          type="success"
          :closable="false"
          show-icon
        >
          <template #default>
            <div class="file-details">
              <span class="file-name">{{ currentFile.name }}</span>
              <span class="file-size">({{ formatFileSize(currentFile.size) }})</span>
              <span class="import-count" v-if="importCount">
                已导入 {{ importCount }} 条数据
              </span>
            </div>
          </template>
        </el-alert>
      </div>
    </el-card>

    <!-- 2. 数据分析模块 -->
    <el-card class="module-card">
      <template #header>
        <div class="card-header">
          <span>选择维度分析</span>
        </div>
      </template>

      <el-form :model="timeSeriesForm" label-width="120px" class="analysis-form">
        <el-form-item label="分析维度">
          <el-select
            v-model="timeSeriesForm.dimensions"
            multiple
            placeholder="请选择分析维度"
            :disabled="!hasData"
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
          <el-button 
            type="primary" 
            @click="queryTimeSeriesData"
            :disabled="!hasData || timeSeriesForm.dimensions.length === 0"
          >
            分析
          </el-button>
        </el-form-item>
      </el-form>

      <div class="chart-section" :class="{ 'no-data': !hasData }">
        <div v-if="!hasData" class="no-data-tip">
          请先上传数据文件
        </div>
        <div ref="timeSeriesChart" class="chart-container"></div>
      </div>
    </el-card>

    <!-- 3. 速率分布分析模块 -->
    <el-card class="module-card">
      <template #header>
        <div class="card-header">
          <span>速率分布分析</span>
        </div>
      </template>

      <div class="distribution-container">
        <!-- 速率区间选择 -->
        <div class="analysis-section">
          <el-form :model="rateAnalysisForm" label-width="120px" class="analysis-form">
            <el-form-item label="速率区间">
              <el-select
                v-model="rateAnalysisForm.selectedRanges"
                multiple
                collapse-tags
                placeholder="请选择要查看的速率区间"
                style="width: 100%"
              >
                <el-option
                  v-for="item in defaultRateRanges"
                  :key="item.label"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button 
                type="primary" 
                @click="updateRateRanges"
                :disabled="!hasData"
              >查看选中区间</el-button>
              <el-button 
                @click="showAllRanges"
                :disabled="!hasData"
              >查看所有区间</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 饼图 -->
        <div class="chart-section">
          <div v-if="!hasData" class="no-data-tip">
            请先上传数据文件
          </div>
          <div ref="rateDistributionChart" class="chart-container"></div>
        </div>
      </div>
    </el-card>

    <!-- 4. 峰值速率计算模块 -->
    <el-card class="module-card">
      <template #header>
        <div class="card-header">
          <span>5G下行峰值速率计算</span>
          <el-tooltip content="计算5G下行链路的理论峰值速率" placement="top">
            <el-icon><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
      </template>

      <div class="calculator-container">
        <el-form
          ref="rateFormRef"
          :model="peakRateForm"
          :rules="peakRateRules"
          label-width="120px"
          class="peak-rate-form"
        >
          <!-- 计算模式选择 -->
          <el-form-item label="计算模式" prop="mode">
            <el-radio-group v-model="peakRateForm.mode">
              <el-radio label="FDD">FDD (频分双工)</el-radio>
              <el-radio label="TDD">TDD (时分双工)</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- TDD模式下的帧结构选择 -->
          <el-form-item 
            v-if="peakRateForm.mode === 'TDD'" 
            label="帧结构" 
            prop="frameType"
          >
            <el-select v-model="peakRateForm.frameType" placeholder="请选择帧结构类型">
              <el-option label="帧结构1 (2ms)" value="FRAME1" />
              <el-option label="帧结构2 (2.5ms)" value="FRAME2" />
              <el-option label="帧结构3 (2.5ms双)" value="FRAME3" />
              <el-option label="帧结构4 (5ms)" value="FRAME4" />
              <el-option label="帧结构5 (1ms)" value="FRAME5" />
            </el-select>
            <div class="form-item-tip">不同帧结构的周期不同</div>
          </el-form-item>

          <!-- TDD模式下的时隙配置 -->
          <template v-if="peakRateForm.mode === 'TDD' && peakRateForm.frameType">
            <el-form-item label="下行slot数" prop="dlSlots">
              <el-input-number 
                v-model="peakRateForm.dlSlots"
                :min="1"
                :precision="0"
                placeholder="请输入下行slot数"
              />
              <div class="form-item-tip">必须大于0</div>
            </el-form-item>

            <el-form-item label="特殊slot数" prop="specialSlots">
              <el-input-number 
                v-model="peakRateForm.specialSlots"
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
          <template v-if="peakRateForm.mode === 'FDD'">
            <el-form-item label="下行slot数" prop="dlSlots">
              <el-input-number 
                v-model="peakRateForm.dlSlots"
                :min="1"
                :precision="0"
                placeholder="请输入下行slot数"
              />
              <div class="form-item-tip">必须大于0</div>
            </el-form-item>
          </template>

          <!-- 基本参数输入 -->
          <el-form-item label="系统带宽(MHz)" prop="bandwidth">
            <el-input-number 
              v-model="peakRateForm.bandwidth"
              :min="1"
              :max="1000"
              :step="10"
              :precision="0"
              placeholder="请输入系统带宽"
            />
            <div class="form-item-tip">有效范围：1-1000 MHz</div>
          </el-form-item>

          <!-- 固定参数显示 -->
          <el-form-item label="调制阶数">
            <el-input 
              value="8" 
              disabled
            >
              <template #append>固定值</template>
            </el-input>
            <div class="form-item-tip">固定使用8</div>
          </el-form-item>

          <el-form-item label="DL流数">
            <el-input 
              value="4" 
              disabled
            >
              <template #append>固定值</template>
            </el-input>
            <div class="form-item-tip">固定使用4</div>
          </el-form-item>

          <el-form-item label="子载波间隔">
            <el-input 
              value="30" 
              disabled
            >
              <template #append>固定值</template>
            </el-input>
            <div class="form-item-tip">固定使用30</div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="calculatePeakRate">计算峰值速率</el-button>
            <el-button @click="resetPeakRateForm">重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 计算结果展示 -->
        <div v-if="peakRateResult !== null" class="result-panel">
          <div class="result-title">计算结果</div>
          <div class="result-value">{{ formatPeakRate(peakRateResult) }}</div>
          <div class="result-formula">
            <div class="formula-title">计算公式：</div>
            <div class="formula-content">
              峰值速率 = (TBS × (下行slot数 × 1000) / 周期) / 10^9
            </div>
            <div class="formula-detail">
              其中：
              <ul>
                <li>TBS = 8 × 调制阶数 × Math.floor((Nifo01 + 24)/(8 × 调制阶数))</li>
                <li>Nifo01 = 2^15 × Math.floor((Nifo - 24)/2^15)</li>
                <li>Nifo = Math.floor(RE × PRB × 调制阶数 × DL流数 × 0.925)</li>
                <li>RE = 12 × (14-1)</li>
                <li>PRB = Math.floor((系统带宽 × 1000 - 2 × 保护间隔) / (12 × SCS))</li>
                <li>周期：FDD固定为10ms，TDD根据帧结构类型确定</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { UploadFilled, QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import request from '@/utils/request'

export default {
  name: 'DashboardView',
  components: {
    UploadFilled,
    QuestionFilled
  },
  setup() {
    // 分析维度选项
    const dimensionOptions = [
      { label: 'RSRP', value: 'rsrp' },
      { label: 'SINR', value: 'sinr' },
      { label: 'MAC层下行速率', value: 'macThroughput' },
      { label: 'Rank', value: 'rank' },
      { label: 'MCS', value: 'mcs' },
      { label: 'RB数', value: 'rbNum' },
      { label: 'BLER', value: 'bler' }
    ]

    // 默认速率区间
    const defaultRateRanges = [
      { label: '0-100 Mbps', value: [0, 100] },
      { label: '100-200 Mbps', value: [100, 200] },
      { label: '200-300 Mbps', value: [200, 300] },
      { label: '300-400 Mbps', value: [300, 400] },
      { label: '400-500 Mbps', value: [400, 500] },
      { label: '500+ Mbps', value: [500, null] }
    ]

    // 文件上传相关
    const uploadUrl = 'http://localhost:8080/api/file/upload'
    const headers = {
      'Accept': 'application/json'
    }

    const handleFileUpload = async (file) => {
      try {
        const formData = new FormData()
        formData.append('file', file)
        
        const response = await request.post('/api/file/upload', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        
        handleUploadSuccess(response)
      } catch (error) {
        handleUploadError(error)
      }
    }

    // 添加文件列表和当前文件状态
    const fileList = ref([])
    const currentFile = ref(null)
    const importCount = ref(0)

    // 格式化文件大小
    const formatFileSize = (bytes) => {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }

    const handleUploadSuccess = (response) => {
      if (response.code === 200) {
        ElMessage.success(`成功导入 ${response.data.importCount} 条数据`)
        hasData.value = true
        currentFile.value = fileList.value[0]
        importCount.value = response.data.importCount
        if (timeSeriesForm.value.dimensions.length > 0) {
          queryTimeSeriesData()
        }
        queryRateDistribution()
      } else {
        ElMessage.error(response.message || '上传失败')
        fileList.value = []
        currentFile.value = null
        importCount.value = 0
      }
    }

    const handleUploadError = (error) => {
      console.error('Upload error:', error)
      ElMessage.error('文件上传失败：' + (error.message || '未知错误'))
      fileList.value = []
      currentFile.value = null
      importCount.value = 0
    }

    const beforeUpload = (file) => {
      const isExcel = file.type === 'application/vnd.ms-excel' || 
                     file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      if (!isExcel) {
        ElMessage.error('只能上传Excel文件')
        return false
      }
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) {
        ElMessage.error('文件大小不能超过10MB')
        return false
      }
      fileList.value = [file]
      handleFileUpload(file)
      return false
    }

    // 时序数据分析相关
    const timeSeriesChart = ref(null)
    let timeSeriesChartInstance = null
    const timeSeriesForm = ref({
      dimensions: []
    })

    // 速率分布相关
    const rateDistributionChart = ref(null)
    let rateDistributionChartInstance = null

    // 速率分析表单
    const rateAnalysisForm = ref({
      selectedRanges: []  // 选中的速率区间
    })

    // 添加数据状态标志
    const hasData = ref(false)

    // 峰值计算相关
    const peakRateForm = ref({
      mode: 'FDD',
      frameType: '',
      bandwidth: 100,
      dlSlots: 10,
      specialSlots: 2
    })

    const peakRateResult = ref(null)

    // 表单验证规则
    const peakRateRules = {
      mode: [
        { required: true, message: '请选择计算模式', trigger: 'change' }
      ],
      frameType: [
        { required: true, message: '请选择帧结构类型', trigger: 'change' }
      ],
      bandwidth: [
        { required: true, message: '请输入系统带宽', trigger: 'blur' },
        { type: 'number', min: 1, max: 1000, message: '系统带宽必须在1-1000MHz之间', trigger: 'blur' }
      ],
      dlSlots: [
        { required: true, message: '请输入下行slot数', trigger: 'blur' },
        { 
          validator: (rule, value, callback) => {
            if (value <= 0) {
              callback(new Error('下行slot数必须大于0'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ],
      specialSlots: [
        { required: true, message: '请输入特殊slot数', trigger: 'blur' },
        { type: 'number', min: 0, max: 14, message: '特殊slot数必须在0-14之间', trigger: 'blur' }
      ]
    }

    const rateFormRef = ref(null)

    // 查询时序数据
    const queryTimeSeriesData = async () => {
      try {
        if (!hasData.value) {
          ElMessage.warning('请先上传数据文件')
          return
        }

        if (timeSeriesForm.value.dimensions.length === 0) {
          ElMessage.warning('请选择至少一个分析维度')
          return
        }

        const response = await request.post('/analysis/time-series-chart', {
          dimensions: timeSeriesForm.value.dimensions
        })

        if (response.code === 200 && response.data) {
          updateTimeSeriesChart(response.data)
        } else {
          ElMessage.warning(response.message || '获取时序数据失败')
        }
      } catch (error) {
        console.error('Query time series data error:', error)
        ElMessage.error('获取时序数据失败')
      }
    }

    // 获取Y轴单位
    const getYAxisUnit = (dimensionName) => {
      const unitMap = {
        'rsrp': 'dBm',
        'sinr': 'dB',
        'macThroughput': 'Mbps',
        'rank': '',
        'mcs': '',
        'rbNum': '',
        'bler': '%'
      }
      return unitMap[dimensionName] || ''
    }

    // 更新时序图表
    const updateTimeSeriesChart = (data) => {
      if (!timeSeriesChartInstance) {
        console.error('Failed to initialize time series chart')
        return
      }

      const option = {
        title: {
          text: data.series[0]?.name + ' 随时间变化的趋势',
          left: 'center',
          top: 10,
          textStyle: {
            fontSize: 16,
            fontWeight: 'normal'
          }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'line',
            lineStyle: {
              color: '#409EFF',
              width: 1,
              type: 'solid'
            }
          },
          formatter: function(params) {
            const time = params[0].value[0]
            let result = time + '<br/>'
            params.forEach(param => {
              const unit = getYAxisUnit(param.seriesName)
              const value = parseFloat(param.value[1]).toFixed(2)
              result += param.marker + param.seriesName + ': ' + value + ' ' + unit + '<br/>'
            })
            return result
          }
        },
        toolbox: {
          feature: {
            dataZoom: {
              yAxisIndex: 'none'
            },
            restore: {},
            saveAsImage: {}
          },
          right: 20,
          top: 10
        },
        dataZoom: [
          {
            type: 'slider',
            show: true,
            xAxisIndex: [0],
            start: 0,
            end: 100,
            bottom: 10,
            height: 30,
            borderColor: 'transparent',
            backgroundColor: '#f5f7fa',
            fillerColor: 'rgba(64, 158, 255, 0.2)',
            handleStyle: {
              color: '#409EFF'
            }
          },
          {
            type: 'inside',
            xAxisIndex: [0],
            start: 0,
            end: 100
          }
        ],
        grid: {
          left: '10%',
          right: '4%',
          bottom: 80,
          top: 60,
          containLabel: false
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: data.series[0].data.map(item => item[0]),
          axisLine: {
            lineStyle: {
              color: '#666'
            }
          },
          axisLabel: {
            rotate: 45,
            interval: Math.floor(data.series[0].data.length / 20),
            textStyle: {
              fontSize: 12
            },
            formatter: function(value) {
              // 只保留时间部分 HH:mm:ss
              return value.split(' ')[1] || value;
            }
          },
          splitLine: {
            show: true,
            lineStyle: {
              type: 'dashed',
              color: '#E9E9E9'
            }
          }
        },
        yAxis: {
          type: 'value',
          name: data.series[0]?.name,
          nameLocation: 'middle',
          nameGap: 65,
          nameTextStyle: {
            fontSize: 14,
            padding: [0, 0, 0, 80],  // 增加左侧padding
            fontWeight: 'bold'
          },
          axisLabel: {
            formatter: (value) => {
              return value + ' ' + getYAxisUnit(data.series[0]?.name)
            },
            margin: 24  // 增加标签与轴的距离
          },
          splitLine: {
            show: true,
            lineStyle: {
              type: 'dashed',
              color: '#E9E9E9'
            }
          }
        },
        series: data.series.map(series => ({
          name: series.name,
          type: 'line',
          data: series.data.map(item => item[1]),
          showSymbol: false,
          connectNulls: true,
          emphasis: {
            focus: 'series'
          },
          lineStyle: {
            width: 2,
            type: 'solid'
          },
          smooth: false,
          animation: false
        }))
      }

      try {
        timeSeriesChartInstance.setOption(option, true)
      } catch (error) {
        console.error('Error updating chart:', error)
        ElMessage.error('更新图表失败')
      }
    }

    // 查询速率分布数据
    const queryRateDistribution = async () => {
      try {
        if (!hasData.value) {
          ElMessage.warning('请先上传数据文件')
          return
        }

        const response = await request.post('/analysis/rate-distribution-chart')
        
        if (response.code === 200 && response.data) {
          updateRateDistributionChart(response.data)
        } else {
          ElMessage.warning(response.message || '获取速率分布数据失败')
        }
      } catch (error) {
        console.error('Query rate distribution error:', error)
        ElMessage.error('获取速率分布数据失败')
      }
    }

    // 更新速率分布图表
    const updateRateDistributionChart = (data) => {
      if (!rateDistributionChartInstance) {
        console.error('Failed to initialize rate distribution chart')
        return
      }

      const option = {
        title: {
          text: '速率分布',
          left: 'center',
          top: 20
        },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}%'
        },
        legend: {
          orient: 'vertical',
          left: '5%',
          top: 'middle',
          type: 'scroll'
        },
        series: [
          {
            name: '速率分布',
            type: 'pie',
            center: ['60%', '50%'],
            radius: ['40%', '70%'],
            avoidLabelOverlap: true,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: true,
              position: 'outside',
              formatter: '{b}\n{c}%'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: '16',
                fontWeight: 'bold'
              }
            },
            data: data.pieData
              .filter(item => parseFloat(item.value) > 0)  // 只显示有数据的区间
              .map(item => ({
                name: item.name,
                value: parseFloat(item.value.toFixed(2))
              }))
          }
        ]
      }

      try {
        rateDistributionChartInstance.setOption(option, true)
      } catch (error) {
        console.error('Error updating chart:', error)
        ElMessage.error('更新图表失败')
      }
    }

    const handleResize = () => {
      timeSeriesChartInstance?.resize()
      rateDistributionChartInstance?.resize()
    }

    // 更新选中区间的范围
    const updateRateRanges = async () => {
      try {
        if (!hasData.value) {
          ElMessage.warning('请先上传数据文件')
          return
        }

        if (rateAnalysisForm.value.selectedRanges.length === 0) {
          return queryRateDistribution()
        }

        // 转换中的区间为后端需要的格式
        const ranges = rateAnalysisForm.value.selectedRanges.map(range => ({
          min: range[0],
          max: range[1]
        }))

        const response = await request.post('/analysis/rate-distribution-chart', ranges, {
          headers: {
            'Content-Type': 'application/json'
          }
        })
        
        if (response.code === 200 && response.data) {
          updateRateDistributionChart(response.data)
        } else {
          ElMessage.warning(response.message || '更新速率分布失败')
        }
      } catch (error) {
        console.error('Update rate distribution error:', error)
        ElMessage.error('更新速率分布失败')
      }
    }

    // 显示所有区间分布
    const showAllRanges = () => {
      rateAnalysisForm.value.selectedRanges = []
      queryRateDistribution()
    }

    // 初始化图表
    const initCharts = () => {
      nextTick(() => {
        if (timeSeriesChart.value && !timeSeriesChartInstance) {
          timeSeriesChartInstance = echarts.init(timeSeriesChart.value)
          window.addEventListener('resize', handleResize)
        }
        
        if (rateDistributionChart.value && !rateDistributionChartInstance) {
          rateDistributionChartInstance = echarts.init(rateDistributionChart.value)
        }
      })
    }

    // 组件挂载和卸载
    onMounted(() => {
      initCharts()
    })

    onUnmounted(() => {
      window.removeEventListener('resize', handleResize)
      if (timeSeriesChartInstance) {
        timeSeriesChartInstance.dispose()
      }
      if (rateDistributionChartInstance) {
        rateDistributionChartInstance.dispose()
      }
    })

    // 计算峰值速率
    const calculatePeakRate = async () => {
      if (!rateFormRef.value) return

      try {
        await rateFormRef.value.validate()

        const requestData = {
          mode: peakRateForm.value.mode,
          bandwidth: Number(peakRateForm.value.bandwidth),
          dlSlots: Number(peakRateForm.value.dlSlots)
        }

        // 只有TDD模式才需要这些参数
        if (peakRateForm.value.mode === 'TDD') {
          requestData.frameType = peakRateForm.value.frameType
          requestData.specialSlots = Number(peakRateForm.value.specialSlots)
          
          // TDD模式的特殊验证
          if (requestData.dlSlots + requestData.specialSlots > 14) {
            ElMessage.error('下行时隙数和特殊时隙数之和不能超过14')
            return
          }
        }

        // 验证数据
        if (isNaN(requestData.bandwidth) || requestData.bandwidth <= 0) {
          ElMessage.error('带宽必须为正数')
          return
        }

        console.log('Sending data:', requestData)
        const response = await request.post('/analysis/calculate-peak-rate', requestData)

        if (response.code === 200) {
          peakRateResult.value = response.data
          console.log('Calculation result:', response.data)
          ElMessage.success('计算成功')
        } else {
          peakRateResult.value = null
          ElMessage.error(response.message || '计算失败')
        }
      } catch (error) {
        console.error('Calculate peak rate error:', error)
        peakRateResult.value = null
        if (error.response?.data?.message) {
          ElMessage.error(error.response.data.message)
        } else {
          ElMessage.error('计算失败，请检查输入参数')
        }
      }
    }

    // 格式化峰值速率显示
    const formatPeakRate = (value) => {
      if (value === null || value === undefined || isNaN(value)) return '0.00 Gbit/s'
      return `${value.toFixed(2)} Gbit/s`
    }

    // 重置峰值计算表单
    const resetPeakRateForm = () => {
      peakRateForm.value = {
        mode: 'FDD',
        frameType: '',
        bandwidth: 100,
        dlSlots: 10,
        specialSlots: 2
      }
      peakRateResult.value = null
      if (rateFormRef.value) {
        rateFormRef.value.resetFields()
      }
    }

    return {
      // 文件上传
      uploadUrl,
      headers,
      handleUploadSuccess,
      handleUploadError,
      beforeUpload,

      // 时序数据分析
      timeSeriesChart,
      timeSeriesForm,
      dimensionOptions,
      queryTimeSeriesData,

      // 速率分布
      rateDistributionChart,

      // 速率区间选择
      defaultRateRanges,
      rateAnalysisForm,
      updateRateRanges,
      showAllRanges,

      // 数据状态
      hasData,

      // 峰值计算相关
      peakRateForm,
      peakRateResult,
      peakRateRules,
      rateFormRef,
      calculatePeakRate,
      formatPeakRate,
      resetPeakRateForm,

      fileList,
      currentFile,
      importCount,
      formatFileSize,
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.dashboard-header {
  text-align: center;
  margin-bottom: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #409EFF 0%, #36cfc9 100%);
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.dashboard-header h1 {
  font-size: 32px;
  color: #ffffff;
  margin-bottom: 10px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.dashboard-desc {
  color: #ffffff;
  font-size: 16px;
  opacity: 0.9;
}

.module-card {
  margin-bottom: 25px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  background-color: #ffffff;
  transition: all 0.3s ease;
}

.module-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #ebeef5;
  background: linear-gradient(to right, #f8f9fa, #ffffff);
}

.card-header span {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.chart-container {
  height: 500px;
  width: 100%;
  padding: 20px;
  border-radius: 4px;
  background-color: #ffffff;
}

.analysis-form {
  max-width: 600px;
  margin-bottom: 20px;
  padding: 20px;
}

.peak-rate-form {
  max-width: 500px;
  margin: 0 auto;
  padding: 20px;
}

.el-upload {
  width: 100%;
  border: 2px dashed #409EFF;
  border-radius: 8px;
  transition: all 0.3s;
}

.el-upload:hover {
  border-color: #66b1ff;
  background-color: rgba(64, 158, 255, 0.05);
}

.el-upload__text {
  color: #606266;
  font-size: 16px;
  margin-top: 10px;
}

.el-upload__text em {
  color: #409EFF;
  font-style: normal;
  font-weight: 600;
}

.result-panel {
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 8px;
  padding: 25px;
  text-align: center;
  margin-top: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  border: 1px solid #ebeef5;
}

.result-title {
  font-size: 18px;
  color: #303133;
  margin-bottom: 15px;
  font-weight: 600;
}

.result-value {
  font-size: 36px;
  background: linear-gradient(45deg, #409EFF, #36cfc9);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: bold;
}

.no-data-tip {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #909399;
  font-size: 16px;
  padding: 20px;
  background-color: rgba(245, 247, 250, 0.8);
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 美化表单控件 */
:deep(.el-input__inner) {
  border-radius: 4px;
  transition: all 0.3s;
}

:deep(.el-input__inner:focus) {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

:deep(.el-select .el-input__inner:hover) {
  border-color: #409EFF;
}

:deep(.el-button) {
  border-radius: 4px;
  font-weight: 500;
  transition: all 0.3s;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #409EFF 0%, #36cfc9 100%);
  border: none;
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.2);
}

:deep(.el-button--primary:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(64, 158, 255, 0.3);
}

/* 图表区域美化 */
.chart-section {
  width: 100%;
  height: 600px;
  position: relative;
  margin-top: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

/* 响应式优化 */
@media (max-width: 768px) {
  .dashboard {
    padding: 10px;
  }
  
  .dashboard-header h1 {
    font-size: 24px;
  }

  .dashboard-desc {
    font-size: 14px;
  }

  .result-value {
    font-size: 28px;
  }
}

.current-file-info {
  margin-top: 20px;
  padding: 10px;
}

.file-details {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 5px;
}

.file-name {
  font-weight: 500;
  color: #409EFF;
}

.file-size {
  color: #909399;
  font-size: 0.9em;
}

.import-count {
  color: #67C23A;
  font-weight: 500;
}

.el-upload {
  width: 100%;
  border: 2px dashed #409EFF;
  border-radius: 8px;
  transition: all 0.3s;
  padding: 20px;
  text-align: center;
}

.el-upload:hover {
  border-color: #66b1ff;
  background-color: rgba(64, 158, 255, 0.05);
}

.el-upload__tip {
  color: #909399;
  font-size: 14px;
  margin-top: 10px;
}

.form-item-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.result-formula {
  text-align: left;
  margin-top: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.formula-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.formula-content {
  color: #606266;
  font-family: monospace;
  font-size: 14px;
}

.formula-detail {
  margin-top: 10px;
  font-size: 14px;
  color: #606266;
}

.formula-detail ul {
  margin: 10px 0;
  padding-left: 20px;
}

.formula-detail li {
  margin: 5px 0;
  font-family: monospace;
}
</style> 
