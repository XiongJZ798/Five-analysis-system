<template>
  <div class="peak-rate">
    <el-row :gutter="20">
      <!-- 计算器 -->
      <el-col :span="12">
        <el-card class="calculator-card">
          <template #header>
            <div class="card-header">
              <span>峰值速率计算器</span>
            </div>
          </template>
<!--速率分析视图
    功能：
    1. 速率分布分析
       - 预设速率区间选择
       - 自定义区间分析
       - 饼图展示分布情况
    2. 峰值速率计算
       - 参数输入：
         * 带宽(MHz)
         * MIMO层数
         * 调制阶数
         * 编码率
       - 实时计算结果显示
       - 参数验证
    3. 结果展示
       - 图形化展示
       - 数值展示-->

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="120px"
            class="calculator-form"
          >
            <el-form-item label="带宽(MHz)" prop="bandwidth">
              <el-input-number
                v-model="form.bandwidth"
                :min="5"
                :max="100"
                :step="5"
              />
            </el-form-item>

            <el-form-item label="MIMO层数" prop="mimoLayers">
              <el-input-number
                v-model="form.mimoLayers"
                :min="1"
                :max="8"
                :step="1"
              />
            </el-form-item>

            <el-form-item label="调制阶数" prop="modulationOrder">
              <el-select v-model="form.modulationOrder" placeholder="请选择调制阶数">
                <el-option label="QPSK (2)" :value="2" />
                <el-option label="16QAM (4)" :value="4" />
                <el-option label="64QAM (6)" :value="6" />
                <el-option label="256QAM (8)" :value="8" />
              </el-select>
            </el-form-item>

            <el-form-item label="编码率(%)" prop="codingRate">
              <el-input-number
                v-model="form.codingRate"
                :min="0"
                :max="100"
                :step="1"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="calculate">计算</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 计算结果 -->
          <div v-if="calculationResult" class="calculation-result">
            <el-divider>计算结果</el-divider>
            <h3>峰值速率：{{ calculationResult.peakRate.toFixed(2) }} Gbps</h3>
          </div>
        </el-card>
      </el-col>

      <!-- 历史记录 -->
      <el-col :span="12">
        <el-card class="history-card">
          <template #header>
            <div class="card-header">
              <span>计算历史</span>
              <el-button @click="refreshHistory">刷新</el-button>
            </div>
          </template>

          <el-table :data="historyList" style="width: 100%" stripe>
            <el-table-column prop="bandwidth" label="带宽(MHz)" width="100" />
            <el-table-column prop="mimoLayers" label="MIMO层数" width="100" />
            <el-table-column prop="modulationOrder" label="调制阶数" width="100">
              <template #default="scope">
                {{ getModulationName(scope.row.modulationOrder) }}
              </template>
            </el-table-column>
            <el-table-column prop="codingRate" label="编码率(%)" width="100" />
            <el-table-column prop="peakRate" label="峰值速率(Gbps)">
              <template #default="scope">
                {{ scope.row.peakRate.toFixed(2) }}
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

export default {
  name: 'PeakRateView',
  setup() {
    // 表单数据
    const formRef = ref(null)
    const form = ref({
      bandwidth: 100,
      mimoLayers: 4,
      modulationOrder: 6,
      codingRate: 90
    })

    // 表单验证规则
    const rules = {
      bandwidth: [
        { required: true, message: '请输入带宽', trigger: 'blur' },
        { type: 'number', min: 5, max: 100, message: '带宽必须在5-100MHz之间', trigger: 'blur' }
      ],
      mimoLayers: [
        { required: true, message: '请输入MIMO层数', trigger: 'blur' },
        { type: 'number', min: 1, max: 8, message: 'MIMO层数必须在1-8之间', trigger: 'blur' }
      ],
      modulationOrder: [
        { required: true, message: '请选择调制阶数', trigger: 'change' }
      ],
      codingRate: [
        { required: true, message: '请输入编码率', trigger: 'blur' },
        { type: 'number', min: 0, max: 100, message: '编码率必须在0-100%之间', trigger: 'blur' }
      ]
    }

    // 计算结果
    const calculationResult = ref(null)

    // 历史记录
    const historyList = ref([])
    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)

    // 计算峰值速率
    const calculate = async () => {
      if (!formRef.value) return
      
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            const response = await axios.post('/api/peak-rate/calculate', form.value)
            if (response.data.code === 200) {
              calculationResult.value = response.data.data
              refreshHistory()
            }
          } catch (error) {
            ElMessage.error('计算失败：' + error.message)
          }
        }
      })
    }

    // 重置表单
    const resetForm = () => {
      if (formRef.value) {
        formRef.value.resetFields()
      }
      calculationResult.value = null
    }

    // 获取历史记录
    const refreshHistory = async () => {
      try {
        const response = await axios.get('/api/peak-rate/history', {
          params: {
            page: currentPage.value - 1,
            size: pageSize.value
          }
        })
        if (response.data.code === 200) {
          historyList.value = response.data.data.content
          total.value = response.data.data.totalElements
        }
      } catch (error) {
        ElMessage.error('获取历史记录失败')
      }
    }

    // 处理分页
    const handleSizeChange = (val) => {
      pageSize.value = val
      refreshHistory()
    }

    const handleCurrentChange = (val) => {
      currentPage.value = val
      refreshHistory()
    }

    // 获取调制方式名称
    const getModulationName = (order) => {
      const map = {
        2: 'QPSK',
        4: '16QAM',
        6: '64QAM',
        8: '256QAM'
      }
      return map[order] || order
    }

    return {
      formRef,
      form,
      rules,
      calculationResult,
      historyList,
      currentPage,
      pageSize,
      total,
      calculate,
      resetForm,
      refreshHistory,
      handleSizeChange,
      handleCurrentChange,
      getModulationName
    }
  }
}
</script>

<style scoped>
.peak-rate {
  padding: 20px;
}

.calculator-card,
.history-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.calculator-form {
  max-width: 500px;
}

.calculation-result {
  text-align: center;
  margin-top: 20px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}
</style> 