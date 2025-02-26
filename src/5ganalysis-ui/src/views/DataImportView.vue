<template>
  <div class="data-import">
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <span>Excel数据导入</span>
        </div>
      </template>
<!-- 数据导入视图
│   功能：
│   1. Excel文件上传功能
│      - 支持拖拽上传
│      - 支持点击选择文件
│      - 文件格式验证(.xls/.xlsx)
│   2. 上传进度显示
│   3. 上传结果反馈
│   4. 数据预览功能-->   
 
      <el-upload
        class="upload-demo"
        drag
        :action="uploadUrl"
        :headers="headers"
        :on-success="handleSuccess"
        :on-error="handleError"
        :before-upload="beforeUpload"
        accept=".xls,.xlsx"
        :show-file-list="true"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            只能上传 .xls/.xlsx 格式的Excel文件
          </div>
        </template>
      </el-upload>

      <!-- 上传结果展示 -->
      <el-result
        v-if="uploadResult.show"
        :icon="uploadResult.success ? 'success' : 'error'"
        :title="uploadResult.title"
        :sub-title="uploadResult.message"
      >
      </el-result>
    </el-card>
  </div>
</template>

<script>
import { ref } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'DataImportView',
  components: {
    UploadFilled
  },
  setup() {
    const uploadUrl = '/api/file/upload'
    const headers = {
      'Accept': 'application/json'
    }

    const uploadResult = ref({
      show: false,
      success: false,
      title: '',
      message: ''
    })

    const beforeUpload = (file) => {
      const isExcel = file.type === 'application/vnd.ms-excel' || 
                     file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      if (!isExcel) {
        ElMessage.error('只能上传Excel文件！')
        return false
      }
      return true
    }

    const handleSuccess = (response) => {
      console.log('Upload success:', response)
      if (response.code === 200) {
        uploadResult.value = {
          show: true,
          success: true,
          title: '上传成功',
          message: `成功导入 ${response.data.importCount} 条数据`
        }
        ElMessage.success('文件上传成功')
      } else {
        handleError(response)
      }
    }

    const handleError = (err) => {
      console.error('Upload error:', err)
      uploadResult.value = {
        show: true,
        success: false,
        title: '上传失败',
        message: err.message || '文件上传失败，请重试'
      }
      ElMessage.error('文件上传失败：' + (err.message || '未知错误'))
    }

    return {
      uploadUrl,
      headers,
      uploadResult,
      beforeUpload,
      handleSuccess,
      handleError
    }
  }
}
</script>

<style scoped>
.data-import {
  padding: 20px;
}

.upload-card {
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-demo {
  width: 100%;
}

.el-upload__tip {
  margin-top: 10px;
  color: #909399;
}
</style> 