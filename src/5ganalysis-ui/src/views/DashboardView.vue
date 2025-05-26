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
            只能上传Excel文件（.xls/.xlsx），且不超过100MB
            <el-popover
              placement="bottom"
              title="Excel数据要求"
              :width="400"
              trigger="hover"
            >
              <template #reference>
                <el-link type="primary" class="file-format-link"> 查看文件格式要求 </el-link>
              </template>
              <div class="format-requirements">
                <p>Excel文件必须包含以下列（列序号从1开始）：</p>
                <ol>
                  <li><strong>第1列</strong>：测试时间（日期时间格式）</li>
                  <li><strong>第2列</strong>：RSRP（数值，单位dBm）</li>
                  <li><strong>第3列</strong>：SINR（数值，单位dB）</li>
                  <li><strong>第4列</strong>：MAC吞吐量（数值，单位Mbps）</li>
                  <li><strong>第5列</strong>：Rank（整数，范围1-8）</li>
                  <li><strong>第6列</strong>：MCS（整数，范围0-28）</li>
                  <li><strong>第7列</strong>：PRB数（整数，范围0-275）</li>
                </ol>
                <p>BLER值可以放在第8或第11列（K列）</p>
                <p>所有数值列必须包含有效的数字，不能有文本或公式错误</p>
              </div>
            </el-popover>
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
                <span v-if="totalRows > 0">(总行数: {{ totalRows }}, 跳过: {{ skippedRows }})</span>
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
    <!-- 速率对比分析模块 -->
    <rate-comparison-chart
          :has-data="hasData"
          :time-range="{ start: null, end: null }"
    />

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
                  :key="item.value"
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
                @change="validateDlSlots"
              />
              <div class="form-item-tip">必须大于0</div>
            </el-form-item>
          </template>

          <!-- 基本参数输入 -->
          <el-form-item label="系统带宽(MHz)" prop="bandwidth">
            <el-input-number
              v-model="peakRateForm.bandwidth"
              :min="1"
              :step="10"
              :precision="0"
              placeholder="请输入系统带宽"
              @change="validateBandwidth"
            />
            <div class="form-item-tip">有效范围：1-1000 MHz</div>
          </el-form-item>

          <!-- 调制阶数 -->
          <el-form-item label="调制阶数" prop="modulationOrder">
            <div class="input-with-switch">
              <el-input-number
                v-model="peakRateForm.modulationOrder"
                :min="2"
                :max="8"
                :step="2"
                :precision="0"
                controls-position="right"
                :disabled="peakRateForm.useDefaultModulation"
                placeholder="请选择调制阶数"
                @change="validateModulationOrder"
              />
              <el-switch
                v-model="peakRateForm.useDefaultModulation"
                inactive-text="自定义"
                active-text="默认值"
                @change="handleModulationSwitchChange"
              />
            </div>
            <div class="form-item-tip">{{ peakRateForm.useDefaultModulation ? '使用默认值: 8' : '有效值: 2, 4, 6, 8' }}</div>
          </el-form-item>

          <!-- DL流数 -->
          <el-form-item label="DL流数" prop="dlStreams">
            <div class="input-with-switch">
              <el-input-number
                v-model="peakRateForm.dlStreams"
                :min="1"
                :max="8"
                :precision="0"
                controls-position="right"
                :disabled="peakRateForm.useDefaultDlStreams"
                placeholder="请选择DL流数"
                @change="validateDlStreams"
              />
              <el-switch
                v-model="peakRateForm.useDefaultDlStreams"
                inactive-text="自定义"
                active-text="默认值"
                @change="handleDlStreamsSwitchChange"
              />
            </div>
            <div class="form-item-tip">{{ peakRateForm.useDefaultDlStreams ? '使用默认值: 4' : '有效值: 1, 2, 4, 8' }}</div>
          </el-form-item>

          <!-- 子载波间隔 -->
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
                <li>使用参数：调制阶数 = {{ peakRateForm.useDefaultModulation ? 8 : peakRateForm.modulationOrder }}，DL流数 = {{ peakRateForm.useDefaultDlStreams ? 4 : peakRateForm.dlStreams }}</li>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import request from '@/utils/request'
import RateComparisonChart from '@/components/RateComparisonChart.vue'

export default {
  name: 'DashboardView',
  components: {
    UploadFilled,
    QuestionFilled,
    RateComparisonChart
  },
  setup() {
    // 分析维度选项
    const dimensionOptions = [
      { label: 'RSRP', value: 'rsrp' },
      { label: 'SINR', value: 'sinr' },
      { label: 'mac层', value: 'macThroughput' },
      { label: 'Rank', value: 'rank' },
      { label: 'MCS', value: 'mcs' },
      { label: 'PRB数', value: 'prbNum' },
      { label: 'BLER', value: 'bler' }
    ]

    // 默认速率区间
    const defaultRateRanges = [
      // 当最大速率≤50Mbps时：每10Mbps一个区间
      { label: '0.0-10.0 Mbps', value: '0-10', range: [0, 10] },
      { label: '10.0-20.0 Mbps', value: '10-20', range: [10, 20] },
      { label: '20.0-30.0 Mbps', value: '20-30', range: [20, 30] },
      { label: '30.0-40.0 Mbps', value: '30-40', range: [30, 40] },
      { label: '40.0-50.0 Mbps', value: '40-50', range: [40, 50] },

      // 当最大速率在50-100Mbps之间：分两个区间
      { label: '50.0-75.0 Mbps', value: '50-75', range: [50, 75] },
      { label: '75.0-100.0 Mbps', value: '75-100', range: [75, 100] },

      // 当最大速率在100-500Mbps之间：每50Mbps一个区间
      { label: '100.0-150.0 Mbps', value: '100-150', range: [100, 150] },
      { label: '150.0-200.0 Mbps', value: '150-200', range: [150, 200] },
      { label: '200.0-250.0 Mbps', value: '200-250', range: [200, 250] },
      { label: '250.0-300.0 Mbps', value: '250-300', range: [250, 300] },
      { label: '300.0-350.0 Mbps', value: '300-350', range: [300, 350] },
      { label: '350.0-400.0 Mbps', value: '350-400', range: [350, 400] },
      { label: '400.0-450.0 Mbps', value: '400-450', range: [400, 450] },
      { label: '450.0-500.0 Mbps', value: '450-500', range: [450, 500] },

      // 当最大速率>500Mbps时：每100Mbps一个区间
      { label: '500.0-600.0 Mbps', value: '500-600', range: [500, 600] },
      { label: '600.0-700.0 Mbps', value: '600-700', range: [600, 700] },
      { label: '700.0-800.0 Mbps', value: '700-800', range: [700, 800] },
      { label: '800.0-900.0 Mbps', value: '800-900', range: [800, 900] },
      { label: '900.0-1000.0 Mbps', value: '900-1000', range: [900, 1000] },
      { label: '1000.0+ Mbps', value: '1000+', range: [1000, null] }
    ]

    // 文件上传相关
    const uploadUrl = '/api/file/upload'
    const headers = {
      'Accept': 'application/json'
    }

    const handleUploadSuccess = (response) => {
      console.log('Upload success response:', response)
      if (response.code === 200) {
        // 检查导入数量是否合理
        if (response.data.importCount === 0) {
          ElMessage.error('导入失败：未能成功导入任何数据')
          fileList.value = []
          currentFile.value = null
          importCount.value = 0
          return
        } else {
          // 直接处理数据，不检查数据量是否超出预期
          processImportedData(response)
        }
      } else {
        ElMessage.error(response.message || '上传失败')
        fileList.value = []
        currentFile.value = null
        importCount.value = 0
      }
    }

    // 提取数据处理逻辑到单独的函数
    const processImportedData = (response) => {
              // 获取文件名和导入数量
    const fileName = response.data.fileName || "";
    let importCountToShow = response.data.importCount;

    // 更新统计数据
    totalRows.value = response.data.totalRows || 0;
    skippedRows.value = response.data.skippedRows || 0;
    importCount.value = importCountToShow;

    ElMessage.success(`成功导入 ${importCountToShow} 条数据 (总行数: ${totalRows.value}, 跳过: ${skippedRows.value})`)

      // 显示可能的警告信息
      if (response.data.warnings && response.data.warnings.length > 0) {
        console.warn('导入警告:', response.data.warnings)

        // 如果警告数量很多，提供查看详情的选项
        if (response.data.warnings.length > 10) {
          ElMessageBox.alert(
            response.data.warnings.slice(0, 10).join('\n') +
            `\n...(共${response.data.warnings.length}条警告)`,
            '导入警告',
            {
              confirmButtonText: '确定',
              callback: () => {
                ElMessage.warning(`导入过程中有${response.data.warnings.length}条警告，可能影响数据质量`)
              }
            }
          )
        } else {
          setTimeout(() => {
            ElMessage.warning({
              message: `导入过程中有${response.data.warnings.length}条警告，请检查数据`,
              duration: 5000
            })
          }, 500)
        }
      }

      hasData.value = true
      currentFile.value = fileList.value[0]
      importCount.value = importCountToShow; // 使用修正后的计数值

      // 在重新上传文件时先清除现有图表实例
      if (timeSeriesChartInstance) {
        console.log('Disposing existing time series chart instance')
        timeSeriesChartInstance.dispose()
        timeSeriesChartInstance = null
      }

      if (rateDistributionChartInstance) {
        console.log('Disposing existing rate distribution chart instance')
        rateDistributionChartInstance.dispose()
        rateDistributionChartInstance = null
      }

      // 重置全局BLER值数组
      resetBlerValues();
      // 重置处理后的BLER值数组
      window.processedBlerValues = [];
      // 重置RSRP值数组
      resetRsrpValues();

      // 确保DOM更新后再初始化和更新图表
      nextTick(() => {
        try {
          console.log('Reinitializing charts after upload')
          initCharts()  // 初始化图表

          // 延迟执行数据查询，确保图表实例已经准备好
          setTimeout(() => {
            console.log('Querying data for charts')
            if (timeSeriesForm.value.dimensions.length > 0) {
              queryTimeSeriesData()
            }
            queryRateDistribution()
          }, 500)
        } catch (error) {
          console.error('Error updating charts after upload:', error)
          ElMessage.error('更新图表失败：' + error.message)
        }
      })
    }

    const handleUploadError = (error) => {
      console.error('上传错误详情:', error)
      let errorMessage = '文件上传失败'

      if (error.response) {
        // 服务器返回了错误信息
        if (error.response.data?.message) {
          const message = error.response.data.message;

          // 检查是否包含文件格式错误或列缺失错误
          if (message.includes('Excel文件格式错误') ||
              message.includes('缺少必要列') ||
              message.includes('数据类型错误')) {
            // 显示格式化的错误信息
            ElMessageBox.alert(
              message.replace(/\n/g, '<br>'),
              '文件格式错误',
              {
                dangerouslyUseHTMLString: true,
                confirmButtonText: '确定',
                type: 'error'
              }
            );
          } else {
            // 其他错误使用标准错误提示
            errorMessage = message;
            ElMessage.error(errorMessage);
          }
        } else {
          errorMessage = '服务器处理文件失败';
          ElMessage.error(errorMessage);
        }
      } else if (error.request) {
        // 请求已发出但没有收到响应
        errorMessage = '服务器无响应，请检查网络连接'
        ElMessage.error(errorMessage);
      } else {
        // 请求设置时出现问题
        errorMessage = '上传配置错误：' + error.message
        ElMessage.error(errorMessage);
      }

      // 重置上传状态
      fileList.value = []
      currentFile.value = null
      importCount.value = 0
    }

    const beforeUpload = (file) => {
      console.log('准备上传文件:', file)
      const isExcel = file.type === 'application/vnd.ms-excel' ||
                     file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      if (!isExcel) {
        ElMessage.error('只能上传Excel文件')
        return false
      }
      const isLt100M = file.size / 1024 / 1024 < 100
      if (!isLt100M) {
        ElMessage.error('文件大小不能超过100MB')
        return false
      }
      fileList.value = [file]
      return true  // 让组件自动处理上传
    }

    // 添加文件列表和当前文件状态
    const fileList = ref([])
    const currentFile = ref(null)
    const importCount = ref(0)
    const totalRows = ref(0)
    const skippedRows = ref(0)

    // 格式化文件大小
    const formatFileSize = (bytes) => {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
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
      specialSlots: 2,
      modulationOrder: 8,
      useDefaultModulation: true,
      dlStreams: 4,
      useDefaultDlStreams: true
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
        { type: 'number', min: 1, message: '系统带宽必须大于0', trigger: 'blur' }
      ],
      dlSlots: [
        { required: true, message: '请输入下行slot数', trigger: 'blur' },
        {
          validator: (_, value, callback) => {
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
      ],
      modulationOrder: [
        { required: true, message: '请选择调制阶数', trigger: 'change' }
      ],
      dlStreams: [
        { required: true, message: '请选择DL流数', trigger: 'change' }
      ]
    }

    const rateFormRef = ref(null)

    // 重置全局BLER值数组
    const resetBlerValues = () => {
      window.allBlerValues = [];
      console.log('重置BLER值数组');
    }

    // 重置全局RSRP值数组
    const resetRsrpValues = () => {
      window.allRsrpValues = [];
      console.log('重置RSRP值数组');
    }

    // 查询时序数据
    const queryTimeSeriesData = async () => {
      // 在查询前重置BLER值数组
      resetBlerValues();
      // 重置处理后的BLER值数组
      window.processedBlerValues = [];
      // 重置RSRP值数组
      resetRsrpValues();
      console.log('开始查询多维度分析数据，维度:', timeSeriesForm.value.dimensions);
      try {
        if (!hasData.value) {
          ElMessage.warning('请先上传数据文件')
          return
        }

        // 确保图表实例存在
        if (!timeSeriesChartInstance && timeSeriesChart.value) {
          console.log('Creating time series chart instance before query')
          timeSeriesChartInstance = echarts.init(timeSeriesChart.value)
        }

        // 显示加载状态
        if (timeSeriesChartInstance) {
          timeSeriesChartInstance.showLoading({
            text: '数据加载中...',
            maskColor: 'rgba(255, 255, 255, 0.8)'
          })
        } else {
          console.error('Time series chart instance not available')
          ElMessage.warning('图表实例不可用，请刷新页面')
          return
        }

        const response = await request.post('/analysis/time-series-chart', {
          dimensions: timeSeriesForm.value.dimensions
        })

        // 隐藏加载状态
        if (timeSeriesChartInstance) {
          timeSeriesChartInstance.hideLoading()
        }

        if (response.code === 200 && response.data) {
          // 预处理数据，确保所有数据点都有效
          const processedData = {
            ...response.data,
            series: response.data.series.map(series => ({
              ...series,
              data: series.data
                .filter(item => Array.isArray(item) && item.length === 2)
                // 对数据进行排序，确保按时间顺序显示
                .sort((a, b) => {
                  // 将时间字符串转换为时间戳进行比较
                  const timeA = new Date(a[0]).getTime();
                  const timeB = new Date(b[0]).getTime();
                  return timeA - timeB;
                })
                .map(item => {
                  // 确保时间值是字符串
                  const timeValue = typeof item[0] === 'string' ? item[0] : String(item[0]);

                  // 处理数值部分
                  let value = typeof item[1] === 'number' ? item[1] : parseFloat(item[1]);
                  if (isNaN(value)) {
                    console.warn(`无效的数值 ${series.name}: ${item[1]}`);
                    return null;
                  }

                  // 限制异常值范围，但对BLER不做任何限制
                  switch(series.name) {
                    case 'rsrp':
                      value = Math.max(-140, Math.min(value, -30));
                      // 记录RSRP值以便后续分析
                      if (!window.allRsrpValues) window.allRsrpValues = [];
                      window.allRsrpValues.push(value);
                      break;
                    case 'sinr':
                      value = Math.max(-20, Math.min(value, 60));
                      break;
                    case 'mcs':
                      value = Math.max(0, Math.min(value, 28));
                      break;
                    case 'prbNum':
                    case 'rbNum':
                      value = Math.max(0, Math.min(value, 275));
                      break;
                    case 'rank':
                      value = Math.max(0, Math.min(value, 8));
                      break;
                    case 'bler':
                      // 完全不限制BLER值范围，只确保非负值
                      value = Math.max(0, value);
                      // 确保很小的BLER值也能显示
                      if (value < 0.0001 && value > 0) {
                        value = 0.0001; // 设置最小值为0.0001%
                      }
                      // 记录所有BLER值以便调试
                      console.log(`BLER处理后值: ${value}`);
                      // 将BLER值存入全局数组以便后续分析
                      if (!window.allBlerValues) window.allBlerValues = [];
                      window.allBlerValues.push(value);
                      break;
                  }

                  return [timeValue, value];
                })
                .filter(item => item !== null)
            }))
          }

          console.log('处理后的数据:', processedData)
          updateTimeSeriesChart(processedData)
        } else {
          ElMessage.warning(response.message || '获取时序数据失败')
        }
      } catch (error) {
        console.error('Query time series data error:', error)
        ElMessage.error('获取时序数据失败：' + error.message)
        if (timeSeriesChartInstance) {
          timeSeriesChartInstance.hideLoading()
        }
      }
    }

    // 获取Y轴单位
    const getYAxisUnit = (dimension) => {
      switch (dimension) {
        case 'rsrp':
          return 'dBm'
        case 'sinr':
          return 'dB'
        case 'macThroughput':
          return 'Mbps'
        case 'rank':
          return ''
        case 'mcs':
          return ''
        case 'rbNum':
        case 'prbNum':
          return ''
        case 'bler':
          return '%'
        default:
          return ''
      }
    }

    // 更新时序图表
    const updateTimeSeriesChart = (data) => {
      try {
        console.log('Updating time series chart with data:', data)

        // 确保图表实例存在并正确初始化
        if (!timeSeriesChartInstance) {
          if (timeSeriesChart.value) {
            console.log('Initializing time series chart')
            // 确保容器尺寸正确
            const chartContainer = timeSeriesChart.value;
            const width = chartContainer.clientWidth || 800;
            const height = chartContainer.clientHeight || 400;

            // 明确设置容器尺寸
            chartContainer.style.width = width + 'px';
            chartContainer.style.height = height + 'px';

            // 初始化ECharts实例
            timeSeriesChartInstance = echarts.init(chartContainer);

            // 注册图表事件
            timeSeriesChartInstance.on('rendered', () => {
              console.log('Chart rendered successfully');
            });

            timeSeriesChartInstance.on('error', (error) => {
              console.error('Chart error:', error);
              ElMessage.error('图表渲染出错: ' + error.message);
            });

            console.log('Chart instance created:', timeSeriesChartInstance);
          } else {
            console.error('Chart DOM element not found');
            ElMessage.error('找不到图表容器元素，请刷新页面重试');
            return;
          }
        }

      if (!timeSeriesChartInstance) {
        console.error('Failed to initialize time series chart')
          ElMessage.error('图表初始化失败，请刷新页面重试')
          return
        }

        if (!data || !data.series || data.series.length === 0) {
          console.error('Invalid data format:', data)
          ElMessage.error('数据格式错误或数据为空')
        return
      }

        // 计算合适的Y轴范围
        const calculateAxisRange = (dimensionData, dimensionName) => {
          if (!dimensionData || dimensionData.length === 0) {
            // 提供合理的默认范围
            return getDefaultRange(dimensionName)
          }

          // 提取数值部分
          const values = dimensionData
            .filter(item => Array.isArray(item) && item.length === 2 && item[1] !== null)
            .map(item => parseFloat(item[1]))
            .filter(v => !isNaN(v))

          if (values.length === 0) return getDefaultRange(dimensionName)

          // 计算最小值和最大值
          const minValue = Math.min(...values)
          const maxValue = Math.max(...values)

          // BLER 特殊处理，完全根据实际数据动态调整范围
          if (dimensionName === 'bler') {
            // 使用全局收集的所有BLER值进行分析，确保捕获到所有数据
            let allValues = window.allBlerValues || [];
            // 如果全局数组为空，则使用当前维度数据
            if (allValues.length === 0) {
              allValues = values;
            }

            console.log('分析BLER数据 - 数据点数量:', allValues.length);

            // 对数据进行排序，便于分析
            const sortedValues = [...allValues].sort((a, b) => a - b);

            // 计算各种统计指标
            const min = 0;  // BLER 最小值固定为 0
            const realMaxValue = Math.max(...allValues);
            const avg = allValues.reduce((sum, val) => sum + val, 0) / allValues.length;
            const median = sortedValues[Math.floor(sortedValues.length / 2)];

            // 打印详细的统计信息
            console.log(`BLER统计信息 - 最大值: ${realMaxValue}, 平均值: ${avg}, 中位数: ${median}`);
            console.log(`BLER数据分布 - 前10个值: ${sortedValues.slice(0, 10).join(', ')}`);
            console.log(`BLER数据分布 - 后10个值: ${sortedValues.slice(-10).join(', ')}`);

            // 确保最大值至少比实际数据最大值大30%，以确保所有数据都能显示
            // 如果最大值很小，至少保证有一个单位的范围
            const max = Math.max(realMaxValue * 1.3, 1);

            // 计算合适的刻度间隔
            let interval;
            if (max <= 0.1) {
              interval = 0.02;       // 非常小的范围
            } else if (max <= 0.5) {
              interval = 0.1;        // 很小的范围
            } else if (max <= 1) {
              interval = 0.2;        // 小范围
            } else if (max <= 2) {
              interval = 0.5;        // 中小范围
            } else if (max <= 5) {
              interval = 1;          // 中等范围
            } else if (max <= 10) {
              interval = 2;          // 中等范围
            } else if (max <= 20) {
              interval = 5;          // 中大范围
            } else if (max <= 50) {
              interval = 10;         // 大范围
            } else if (max <= 100) {
              interval = 20;         // 超大范围
            } else {
              interval = 50;         // 极大范围
            }

            // 将最大值调整为间隔的整数倍，使刻度更整齐
            const adjustedMax = Math.ceil(max / interval) * interval;

            // 确保范围至少是25，以包含所有可能的BLER值
            const finalMax = Math.max(adjustedMax, 25);

            console.log(`BLER原始数据最大值: ${realMaxValue}, Y轴范围调整为: 0-${finalMax}, 间隔: ${interval}`);
            return { min, max: finalMax, interval }
          }

          // RSRP 特殊处理，确保显示所有数据点
          if (dimensionName === 'rsrp') {
            // 使用全局收集的所有RSRP值进行分析，确保捕获到所有数据
            let allValues = window.allRsrpValues || [];
            // 如果全局数组为空，则使用当前维度数据
            if (allValues.length === 0) {
              allValues = values;
            }

            console.log('分析RSRP数据 - 数据点数量:', allValues.length);

            // 对数据进行排序，便于分析
            const sortedValues = [...allValues].sort((a, b) => a - b);
            const minValue = Math.min(...allValues);
            const maxValue = Math.max(...allValues);

            console.log(`RSRP数据范围: ${minValue} 到 ${maxValue}`);
            console.log(`RSRP数据分布 - 前10个值: ${sortedValues.slice(0, 10).join(', ')}`);
            console.log(`RSRP数据分布 - 后10个值: ${sortedValues.slice(-10).join(', ')}`);

            // 确保最小值和最大值之间有足够的间距，至少覆盖10dBm
            let min = Math.min(minValue, -86);  // 确保至少显示到-86dBm
            let max = Math.max(maxValue, -80);  // 确保至少显示到-80dBm

            // 确保最小值比实际最小值还要小一些，以显示完整的数据
            min = Math.floor(min) - 3;  // 向下取整并再减3
            max = Math.ceil(max) + 2;   // 向上取整并再加2

            // 确保范围至少有8dBm，以便有足够的显示空间
            if (max - min < 8) {
              max = min + 8;
            }

            // 计算合适的间隔，RSRP通常使用0.5或1dBm的间隔
            const range = max - min;
            let interval;
            if (range <= 5) {
              interval = 0.5;  // 小范围使用0.5dBm间隔
            } else if (range <= 15) {
              interval = 1;    // 中等范围使用1dBm间隔
            } else if (range <= 30) {
              interval = 2;    // 较大范围使用2dBm间隔
            } else {
              interval = 5;    // 大范围使用5dBm间隔
            }

            console.log(`RSRP Y轴范围调整为: ${min}dBm 到 ${max}dBm, 间隔: ${interval}`);
            return { min, max, interval };
          }

          // 其他维度的处理
          const min = Math.floor(minValue * 0.9)
          const max = Math.ceil(maxValue * 1.1)

          // 计算合适的间隔
          const range = max - min
          const interval = calcNiceInterval(range)

          return { min, max, interval }
        }

        // 提供各维度的默认范围（仅在没有有效数据时使用）
        const getDefaultRange = (dimensionName) => {
          switch(dimensionName) {
            case 'rsrp': return { min: -140, max: -44, interval: 20 } // 5G RSRP范围
            case 'sinr': return { min: -20, max: 40, interval: 10 }   // 5G SINR范围
            case 'macThroughput': return { min: 0, max: 1000, interval: 200 } // 更大范围
            case 'rank': return { min: 0, max: 8, interval: 1 }
            case 'mcs': return { min: 0, max: 28, interval: 4 }
            case 'prbNum': case 'rbNum': return { min: 0, max: 275, interval: 50 } // 5G最多275个PRB
            case 'bler': return { min: 0, max: 25, interval: 5 } // BLER的最小默认范围，确保能显示所有可能的值
            default: return { min: 0, max: 100, interval: 20 }
          }
        }

        // 计算美观的刻度间隔
        const calcNiceInterval = (range) => {
          if (range <= 0) return 1
          const exponent = Math.floor(Math.log10(range))
          const fraction = range / Math.pow(10, exponent)

          let interval
          if (fraction <= 1.5) interval = 1
          else if (fraction <= 3) interval = 2
          else if (fraction <= 7) interval = 5
          else interval = 10

          return interval * Math.pow(10, exponent - 1)
        }

        // 动态确定Y轴分组
        const determineYAxisGroups = () => {
          // 定义维度的默认顺序和位置
          const dimensionOrder = [
            { dim: 'rsrp', position: 'left' },
            { dim: 'sinr', position: 'left' },
            { dim: 'macThroughput', position: 'left' },
            { dim: 'rank', position: 'right' },
            { dim: 'mcs', position: 'right' },
            { dim: 'prbNum', position: 'right' },
            { dim: 'rbNum', position: 'right' },
            { dim: 'bler', position: 'right' }
          ];

          // 获取当前选择的维度
          const selectedDimensions = [...new Set(data.series.map(s => s.name))];
          console.log('当前选择的维度:', selectedDimensions);

          // 按照默认顺序对选择的维度进行排序
          const orderedDimensions = dimensionOrder
            .filter(item => selectedDimensions.includes(item.dim))
            .map(item => item.dim);

          // 分离左右两侧的维度
          const leftDimensions = orderedDimensions.filter(dim =>
            dimensionOrder.find(item => item.dim === dim)?.position === 'left'
          );

          const rightDimensions = orderedDimensions.filter(dim =>
            dimensionOrder.find(item => item.dim === dim)?.position === 'right'
          );

          // 特殊处理 prbNum 和 rbNum，如果两者都存在，则合并处理
          const hasPrbNum = rightDimensions.includes('prbNum');
          const hasRbNum = rightDimensions.includes('rbNum');

          // 如果两者都存在，则移除rbNum，因为它们将共享一个Y轴
          if (hasPrbNum && hasRbNum) {
            const rbNumIndex = rightDimensions.indexOf('rbNum');
            if (rbNumIndex !== -1) {
              rightDimensions.splice(rbNumIndex, 1);
            }
          }

          // 生成结果数组
          const result = [];

          // 处理左侧维度
          leftDimensions.forEach((dim, index) => {
            const seriesData = data.series.find(s => s.name === dim)?.data || [];
            const range = calculateAxisRange(seriesData, dim);

            // 计算偏移量 - 均匀分布
            const offset = leftDimensions.length > 1 ?
                          index * (120 / (leftDimensions.length - 1)) : 0;

            result.push({
              name: dim === 'macThroughput' ? 'mac层' : dim,
              position: 'left',
              offset: offset,
              min: range.min,
              max: range.max,
              interval: range.interval,
              unit: getYAxisUnit(dim),
              dims: [dim]
            });
          });

          // 处理右侧维度
          rightDimensions.forEach((dim, index) => {
            // 如果是prbNum，可能需要包含rbNum
            let dims = [dim];
            let displayName = dim;

            if (dim === 'prbNum' && hasRbNum) {
              dims = ['prbNum', 'rbNum'];
              displayName = 'PRB数';
            } else if (dim === 'rbNum' && !hasPrbNum) {
              displayName = 'PRB数';
            }

            const seriesData = data.series.find(s => s.name === dim)?.data || [];
            const range = calculateAxisRange(seriesData, dim);

            // 计算偏移量 - 均匀分布
            const offset = rightDimensions.length > 1 ?
                          index * (120 / (rightDimensions.length - 1)) : 0;

            result.push({
              name: displayName,
              position: 'right',
              offset: offset,
              min: range.min,
              max: range.max,
              interval: range.interval,
              unit: getYAxisUnit(dim),
              dims: dims
            });
          });

          console.log('生成的Y轴分组:', result);
          return result;
        }

        // 生成Y轴配置
        const generateYAxisConfig = () => {
          const yAxisGroups = determineYAxisGroups()

          // 固定的颜色映射，确保每个维度始终使用相同的颜色
          const colorMap = {
            'rsrp': '#5470c6',     // 蓝色
            'sinr': '#91cc75',     // 绿色
            'macThroughput': '#fac858', // 黄色
            'mac层': '#fac858',    // 黄色
            'rank': '#ee6666',     // 红色
            'mcs': '#73c0de',      // 青色
            'prbNum': '#3ba272',   // 深绿色
            'rbNum': '#3ba272',    // 与prbNum保持相同颜色
            'PRB数': '#3ba272',   // 深绿色
            'bler': '#fc8452'      // 橙色
          };

          return yAxisGroups.map((group, index) => {
            // 使用固定的颜色映射，或者默认颜色列表
            const color = colorMap[group.name] || ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452'][index % 7];

            // 使用组定义的位置和偏移
            let position = group.position;
            let offset = group.offset;

            return {
              name: group.name,
              type: 'value',
              position: position,
              offset: offset,
              min: group.min,
              max: group.max,
              interval: group.interval,
              axisLine: {
                show: true,
                lineStyle: {
                  color: color,
                  width: group.name === 'bler' ? 2 : 1.5 // BLER的轴线加粗
                }
              },
              axisTick: {
                show: true
              },
              axisLabel: {
                show: true,
                formatter: function(value) {
                  // 对不同量级的数据采用不同显示策略
                  if (group.name === 'bler') {
                    // BLER特殊处理为百分比，根据数值大小决定小数位数
                    if (value < 0.1) {
                      return value.toFixed(4) + group.unit; // 极小值显示4位小数
                    } else if (value < 1) {
                      return value.toFixed(3) + group.unit; // 非常小的值显示3位小数
                    } else if (value < 10) {
                      return value.toFixed(2) + group.unit; // 小值显示2位小数
                    } else {
                      return value.toFixed(1) + group.unit; // 大值显示1位小数
                    }
                  } else if (Math.abs(value) >= 1000) {
                    return (value / 1000).toFixed(1) + 'k' + group.unit;
                  }
                  return value + group.unit;
                },
                color: group.name === 'bler' ? color : undefined // BLER的标签颜色与轴线一致
              },
              splitLine: {
                show: index === 0,
                lineStyle: {
                  type: 'dashed',
                  color: '#E9E9E9'
                }
              },
              // 为BLER添加额外的样式
              nameTextStyle: group.name === 'bler' ? {
                color: color,
                fontWeight: 'bold'
              } : undefined
            }
          })
      }

      const option = {
        title: {
            text: '多维度分析',
          left: 'center',
            top: 10,
          textStyle: {
              fontSize: 18,
              fontWeight: 'bold',
              color: '#303133'
            }
          },
          grid: {
            left: '10%',
            right: '10%',
            bottom: '15%',
            top: '120px',
            containLabel: true
          },
          toolbox: {
            show: true,
            orient: 'horizontal',
            feature: {
              dataZoom: {
                show: true,
                yAxisIndex: 'none',
                title: {
                  zoom: '区域缩放',
                  back: '还原缩放'
                }
              },
              restore: {
                show: true,
                title: '还原'
              },
              saveAsImage: {
                show: true,
                title: '保存为图片'
              }
            },
            right: 20,
            top: 20,
            itemSize: 20,
            itemGap: 15,
            z: 100
          },
          legend: {
            show: true,
            type: 'plain',
            data: data.series.map(item => {
              // 确保所有维度都有正确的显示名称
              if (item.name === 'macThroughput') return 'mac层'
              if (item.name === 'prbNum' || item.name === 'rbNum') return 'PRB数'
              return item.name
            }),
            top: 60,
            left: 'center',
            itemWidth: 25,        // 色块宽度
            itemHeight: 14,       // 色块高度
            textGap: 10,          // 色块与文本之间的距离
            itemGap: 30,          // 每项之间的间距
            icon: 'roundRect',
            textStyle: {
              color: '#303133',
              fontSize: 13
            },
            orient: 'horizontal',
            backgroundColor: 'rgba(250, 250, 250, 0.6)',
            borderRadius: 4,
            padding: [8, 20],
            zlevel: 100,          // 确保在最上层
            shadowColor: 'rgba(0, 0, 0, 0.1)',
            shadowBlur: 5
          },
          xAxis: {
            type: 'category',
            boundaryGap: false,
            axisLine: {
              lineStyle: {
                color: '#666',
                width: 2
              }
            },
            axisLabel: {
              rotate: 30,
              margin: 15,
              fontSize: 12,
              textStyle: {
                color: '#303133'
              }
            },
            splitLine: {
              show: true,
              lineStyle: {
                type: 'dashed',
                color: 'rgba(220, 220, 220, 0.8)'
            }
          },
            data: data.series[0]?.data.map(item => item[0]) || [],
            // 确保数据点按时间顺序排列
            axisPointer: {
              snap: true,
              label: {
                formatter: function (params) {
                  return params.value;
                }
              }
            }
          },
          yAxis: generateYAxisConfig(),
          dataZoom: [{
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
          }, {
            type: 'inside',
            xAxisIndex: [0],
            start: 0,
            end: 100
          }],
            tooltip: {
            trigger: 'axis',
            formatter: function(params) {
              let result = params[0].name + '<br/>'; // 时间标签

              params.forEach(param => {
                // 根据数值大小选择合适的格式
                let value = param.value[1];
                let formattedValue;
                const dimension = param.seriesName === 'mac层' ? 'macThroughput' :
                                param.seriesName === 'PRB数' ? 'rbNum' : param.seriesName;
                let unit = getYAxisUnit(dimension);

                // 特殊处理BLER（显示为百分比）
                if (dimension === 'bler') {
                  // 确保BLER值以百分比格式显示，根据数值大小决定小数位数
                  if (value < 0.1) {
                    formattedValue = value.toFixed(4); // 极小值显示4位小数
                  } else if (value < 1) {
                    formattedValue = value.toFixed(3); // 非常小的值显示3位小数
                  } else if (value < 10) {
                    formattedValue = value.toFixed(2); // 小值显示2位小数
                  } else {
                    formattedValue = value.toFixed(1); // 大值显示1位小数
                  }
                  unit = '%'; // 覆盖单位以确保正确显示
                } else if (Math.abs(value) >= 1000000) {
                  formattedValue = (value / 1000000).toFixed(2) + 'M';
                } else if (Math.abs(value) >= 1000) {
                  formattedValue = (value / 1000).toFixed(2) + 'k';
            } else {
                  formattedValue = value.toFixed(2);
                }

                result += `<span style="display:inline-block;margin-right:5px;border-radius:10px;width:10px;height:10px;background-color:${param.color}"></span>`;
                result += `${param.seriesName}: ${formattedValue}${unit}<br/>`;
              });

              return result;
            }
          },
          series: data.series.map((series, index) => {
            // 固定颜色映射，确保各维度颜色一致
            const colorMap = {
              'rsrp': '#5470c6',     // 蓝色
              'sinr': '#91cc75',     // 绿色
              'macThroughput': '#fac858', // 黄色
              'rank': '#ee6666',     // 红色
              'mcs': '#73c0de',      // 青色
              'prbNum': '#3ba272',   // 深绿色
              'rbNum': '#3ba272',    // 与prbNum保持相同颜色
              'bler': '#fc8452'      // 橙色
            }

            const color = colorMap[series.name] || ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452'][index % 7]

            // 确定该维度应该使用哪个Y轴
            const getYAxisIndex = (seriesName) => {
              const yAxisGroups = determineYAxisGroups()

              // 正常处理所有维度
              for (let i = 0; i < yAxisGroups.length; i++) {
                if (yAxisGroups[i].dims.includes(seriesName)) {
                  return i
                }
              }

              // 如果没有找到匹配的Y轴，使用第一个
              console.warn(`没有为 ${seriesName} 找到匹配的Y轴，使用默认Y轴`);
              return 0 // 默认使用第一个Y轴
            }

            // 确保每个维度都有正确的显示名称
            const getDisplayName = (name) => {
              if (name === 'macThroughput') return 'mac层'
              if (name === 'prbNum' || name === 'rbNum') return 'PRB数'
              return name
            }

            return {
              name: getDisplayName(series.name),
          type: 'line',
              yAxisIndex: getYAxisIndex(series.name),
              data: series.data,
              showSymbol: series.data.length < 200, // 数据点少时显示符号
              symbolSize: series.name === 'bler' ? 8 : 6, // BLER的数据点更大
              symbol: 'circle', // 所有维度都使用圆形标记以保持一致性
              sampling: 'average',
              // 确保数据点按时间顺序连接
              connectNulls: false,
              // 对BLER特殊处理，确保按时间顺序显示
              sortData: series.name === 'bler',
          emphasis: {
            focus: 'series',
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.3)'
            }
          },
          lineStyle: {
                width: series.name === 'bler' ? 3 : 2, // BLER曲线稍微加粗
              type: 'solid', // 所有维度都使用实线
                color: color
              },
              itemStyle: {
                color: color
          },
          smooth: false,
          animation: false
      }
          })
        };

        console.log('Setting chart option:', option)

        // 清除旧的数据
        timeSeriesChartInstance.clear()

        // 设置新的配置
        timeSeriesChartInstance.setOption(option, true)

        // 添加错误处理
        timeSeriesChartInstance.on('error', (error) => {
          console.error('Chart error:', error)
          ElMessage.error('图表渲染出错，请重试')
        })
      } catch (error) {
        console.error('Error updating chart:', error)
        ElMessage.error('更新图表失败：' + error.message)
      }
    }

    // 查询速率分布数据
    const queryRateDistribution = async () => {
      console.log('开始查询速率分布数据');
      try {
        if (!hasData.value) {
          ElMessage.warning('请先上传数据文件')
          return
        }

        // 显示加载状态
        if (rateDistributionChartInstance) {
          rateDistributionChartInstance.showLoading({
            text: '数据加载中...',
            maskColor: 'rgba(255, 255, 255, 0.8)'
          })
        }

        // 增加请求超时设置
        const response = await request.post('/analysis/rate-distribution-chart', null, {
          timeout: 60000  // 设置60秒超时
        })

        // 隐藏加载状态
        if (rateDistributionChartInstance) {
          rateDistributionChartInstance.hideLoading()
        }

        if (response.code === 200 && response.data) {
          updateRateDistributionChart(response.data)
        } else {
          ElMessage.warning(response.message || '获取速率分布数据失败')
        }
      } catch (error) {
        console.error('Query rate distribution error:', error)
        ElMessage.error('获取速率分布数据失败')
        if (rateDistributionChartInstance) {
          rateDistributionChartInstance.hideLoading()
        }
      }
    }

    // 更新速率分布图表
    const updateRateDistributionChart = (data) => {
      try {
        console.log('Updating rate distribution chart with data:', data)

        // 确保图表实例存在
        if (!rateDistributionChartInstance && rateDistributionChart.value) {
          console.log('Initializing rate distribution chart')
          rateDistributionChartInstance = echarts.init(rateDistributionChart.value)
        }

      if (!rateDistributionChartInstance) {
        console.error('Failed to initialize rate distribution chart')
          ElMessage.error('速率分布图表初始化失败，请刷新页面重试')
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

        // 清除旧的数据
        rateDistributionChartInstance.clear()

        // 设置新的配置
        rateDistributionChartInstance.setOption(option, true)

        // 添加错误处理
        rateDistributionChartInstance.on('error', (error) => {
          console.error('Chart error:', error)
          ElMessage.error('图表渲染出错，请重试')
        })
      } catch (error) {
        console.error('Error updating rate distribution chart:', error)
        ElMessage.error('更新速率分布图表失败：' + error.message)
      }
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

        // 转换选中的区间为后端需要的格式
        const ranges = rateAnalysisForm.value.selectedRanges.map(rangeId => {
          const rangeItem = defaultRateRanges.find(item => item.value === rangeId)
          return {
            min: rangeItem.range[0],
            max: rangeItem.range[1]
          }
        })

        const requestData = { ranges }

        const response = await request.post('/analysis/rate-distribution-chart', requestData, {
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
      rateAnalysisForm.value.selectedRanges = defaultRateRanges.map(item => item.value)
      queryRateDistribution()
    }

    // 初始化图表实例
    const initCharts = () => {
      console.log('Initializing charts')

      // 销毁已存在的实例
      if (timeSeriesChartInstance) {
        console.log('Disposing existing time series chart instance during init')
        timeSeriesChartInstance.dispose()
        timeSeriesChartInstance = null
      }

      if (rateDistributionChartInstance) {
        console.log('Disposing existing rate distribution chart instance during init')
        rateDistributionChartInstance.dispose()
        rateDistributionChartInstance = null
      }

      // 初始化时序图
      if (timeSeriesChart.value) {
        try {
          console.log('Initializing time series chart, DOM element:', timeSeriesChart.value)

          // 确保容器尺寸正确
          const chartContainer = timeSeriesChart.value
          chartContainer.style.height = '500px'
          chartContainer.style.width = '100%'

          // 记录容器尺寸
          console.log('Chart container size:', chartContainer.clientWidth, 'x', chartContainer.clientHeight)

          // 初始化实例
          timeSeriesChartInstance = echarts.init(chartContainer)
          console.log('Time series chart instance created:', timeSeriesChartInstance)

          // 显示空图表
          timeSeriesChartInstance.setOption({
            title: {
              text: '多维度分析',
              left: 'center',
              top: 10,
              textStyle: {
                fontSize: 18,
                fontWeight: 'bold',
                color: '#303133'
              }
            },
            grid: {
              left: '10%',
              right: '10%',
              bottom: '15%',
              top: '120px',
              containLabel: true
            },
            toolbox: {
              show: true,
              orient: 'horizontal',
              feature: {
                dataZoom: {
                  show: true,
                  yAxisIndex: 'none'
                },
                restore: {
                  show: true
                },
                saveAsImage: {
                  show: true
                }
              },
              right: 20,
              top: 20,
              itemSize: 20,
              itemGap: 15,
              z: 100
            }
          })
        } catch (error) {
          console.error('Failed to initialize time series chart:', error)
          ElMessage.error('初始化多维度分析图表失败: ' + error.message)
        }
          } else {
        console.log('Time series chart DOM element not available')
      }

      // 初始化速率分布图
      if (rateDistributionChart.value) {
        try {
          console.log('Initializing rate distribution chart')
          rateDistributionChartInstance = echarts.init(rateDistributionChart.value)
        } catch (error) {
          console.error('Failed to initialize rate distribution chart:', error)
          ElMessage.error('初始化速率分布图表失败: ' + error.message)
        }
      }
    }

    // 组件挂载时的处理
    onMounted(() => {
      console.log('Component mounted')
      // 初始化全局BLER值数组
      window.allBlerValues = [];
      window.addEventListener('resize', handleResize)

      // 确保DOM完全加载后再初始化图表
      nextTick(() => {
        console.log('Next tick after mount, initializing charts in 500ms')
      setTimeout(() => {
      initCharts()
          console.log('Chart containers after init:', {
            timeSeriesChart: timeSeriesChart.value,
            rateDistributionChart: rateDistributionChart.value
          })
        }, 500)
      })
    })

    // 监听窗口大小变化
    const handleResize = () => {
      if (timeSeriesChartInstance) {
        timeSeriesChartInstance.resize()
      }
      if (rateDistributionChartInstance) {
        rateDistributionChartInstance.resize()
      }
    }

    // 计算峰值速率
    const calculatePeakRate = async () => {
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

        // 添加调制阶数参数
        if (!peakRateForm.value.useDefaultModulation) {
          requestData.modulationOrder = Number(peakRateForm.value.modulationOrder)
        }

        // 添加DL流数参数
        if (!peakRateForm.value.useDefaultDlStreams) {
          requestData.dlStreams = Number(peakRateForm.value.dlStreams)
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
        specialSlots: 2,
        modulationOrder: 8,
        useDefaultModulation: true,
        dlStreams: 4,
        useDefaultDlStreams: true
      }
      peakRateResult.value = null
      if (rateFormRef.value) {
        rateFormRef.value.resetFields()
      }
    }

    // 处理调制阶数切换
    const handleModulationSwitchChange = () => {
      if (peakRateForm.value.useDefaultModulation) {
        // 切换到默认值
        peakRateForm.value.modulationOrder = 8;
      } else {
        // 如果当前值是默认值8，则选择另一个值
        if (peakRateForm.value.modulationOrder === 8) {
          peakRateForm.value.modulationOrder = 6; // 选择一个不同于默认值的选项
        }
      }
    }

    // 处理DL流数切换
    const handleDlStreamsSwitchChange = () => {
      if (peakRateForm.value.useDefaultDlStreams) {
        // 切换到默认值
        peakRateForm.value.dlStreams = 4;
      } else {
        // 如果当前值是默认值4，则选择另一个值
        if (peakRateForm.value.dlStreams === 4) {
          peakRateForm.value.dlStreams = 2; // 选择一个不同于默认值的选项
        }
      }
    }

    // 添加调制阶数变化监听器
    const validateModulationOrder = (value) => {
      if (peakRateForm.value.useDefaultModulation) {
        return;
      }

      // 调制阶数必须是2,4,6,8中的一个
      const validValues = [2, 4, 6, 8];
      if (!validValues.includes(value)) {
        // 寻找最接近的有效值
        let closest = validValues[0];
        for (const validValue of validValues) {
          if (Math.abs(validValue - value) < Math.abs(closest - value)) {
            closest = validValue;
          }
        }
        // 显示错误提示
        ElMessage.warning(`输入的调制阶数(${value})超出有效范围，已自动调整为最接近的有效值(${closest})`);

        // 设置为最接近的有效值
        setTimeout(() => {
          peakRateForm.value.modulationOrder = closest;
        }, 0);
      }
    }

    // 添加DL流数变化监听器
    const validateDlStreams = (value) => {
      if (peakRateForm.value.useDefaultDlStreams) {
        return;
      }

      // DL流数必须是1,2,4,8中的一个
      const validValues = [1, 2, 4, 8];
      if (!validValues.includes(value)) {
        // 寻找最接近的有效值
        let closest = validValues[0];
        for (const validValue of validValues) {
          if (Math.abs(validValue - value) < Math.abs(closest - value)) {
            closest = validValue;
          }
        }
        // 显示错误提示
        ElMessage.warning(`输入的DL流数(${value})超出有效范围，已自动调整为最接近的有效值(${closest})`);

        // 设置为最接近的有效值
        setTimeout(() => {
          peakRateForm.value.dlStreams = closest;
        }, 0);
      }
    }

    // 添加带宽变化监听器
    const validateBandwidth = (value) => {
      if (isNaN(value) || value < 1 || value > 1000) {
        ElMessage.warning('系统带宽必须在1-1000MHz之间')
      }
    }

    // 添加下行时隙变化监听器
    const validateDlSlots = (value) => {
      if (isNaN(value) || value < 1) {
        ElMessage.warning('下行slot数必须大于0')
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
      totalRows,
      skippedRows,
      formatFileSize,

      // 调制阶数和DL流数切换
      handleModulationSwitchChange,
      handleDlStreamsSwitchChange,
      validateModulationOrder,
      validateDlStreams,

      // 带宽变化
      validateBandwidth,

      // 下行时隙变化
      validateDlSlots,

    }
  }
}
</script>

<style scoped>
/* 全局样式 */
.dashboard {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-header {
  margin-bottom: 30px;
  text-align: center;
}

.dashboard-desc {
  color: #606266;
  font-size: 14px;
  margin-top: 10px;
}

/* 卡片模块美化 */
.module-card {
  margin-bottom: 30px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

/* 图表容器 */
.chart-container {
  height: 500px;
  width: 100%;
  margin-top: 20px;
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  position: relative;
  overflow: visible;
  z-index: 1;
}

/* 图表区域美化 */
.chart-section {
  position: relative;
  margin-top: 20px;
  padding: 16px;
  background-color: #fff;
  border-radius: 8px;
  transition: all 0.3s;
  min-height: 550px; /* 确保足够高度容纳图表和图例 */
}

.chart-section.no-data {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  border: 2px dashed #e4e7ed;
}

.chart-section .no-data-tip {
  font-size: 16px;
  color: #909399;
  text-align: center;
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
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: bold;
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

.input-with-switch {
  display: flex;
  align-items: center;
  gap: 15px;
}

.input-with-switch .el-input-number,
.input-with-switch .el-select {
  flex: 1;
}

.input-with-switch .el-switch {
  min-width: 100px;
}

.file-format-link {
  margin-left: 10px;
  font-size: 13px;
}

.format-requirements {
  font-size: 14px;
  line-height: 1.5;
}

.format-requirements p {
  margin: 8px 0;
}

.format-requirements ol {
  margin: 8px 0;
  padding-left: 20px;
}

.format-requirements li {
  margin: 4px 0;
}
</style>

