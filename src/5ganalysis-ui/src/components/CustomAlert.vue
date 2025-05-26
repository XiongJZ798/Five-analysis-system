<template>
  <div
    v-if="visible"
    class="custom-alert"
    :class="[type, { 'with-icon': showIcon }]"
    @click="closeAlert"
    ref="alertEl"
  >
    <i v-if="showIcon" class="alert-icon" :class="iconClass"></i>
    <div class="alert-content">
      <div v-if="title" class="alert-title">{{ title }}</div>
      <div class="alert-message">{{ message }}</div>
    </div>
    <i v-if="closable" class="el-icon-close alert-close" @click.stop="closeAlert">✕</i>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount } from 'vue';

export default {
  name: 'CustomAlert',
  props: {
    message: {
      type: String,
      required: true
    },
    title: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      default: 'info',
      validator: (value) => ['success', 'warning', 'info', 'error'].includes(value)
    },
    duration: {
      type: Number,
      default: 3000
    },
    showIcon: {
      type: Boolean,
      default: true
    },
    closable: {
      type: Boolean,
      default: true
    },
    onClose: {
      type: Function,
      default: () => {}
    }
  },
  setup(props) {
    const visible = ref(true);
    const alertEl = ref(null);
    let timer = null;

    const iconClass = {
      success: 'el-icon-success',
      warning: 'el-icon-warning',
      info: 'el-icon-info',
      error: 'el-icon-error'
    }[props.type];

    const closeAlert = () => {
      visible.value = false;
      if (timer) {
        clearTimeout(timer);
        timer = null;
      }
      props.onClose();
    };

    onMounted(() => {
      if (props.duration > 0) {
        timer = setTimeout(() => {
          closeAlert();
        }, props.duration);
      }

      // 添加动画效果
      if (alertEl.value) {
        alertEl.value.style.top = '20px';
        alertEl.value.style.opacity = '1';
      }
    });

    onBeforeUnmount(() => {
      if (timer) {
        clearTimeout(timer);
      }
    });

    return {
      visible,
      iconClass,
      closeAlert,
      alertEl
    };
  }
};
</script>

<style scoped>
.custom-alert {
  position: fixed;
  top: -50px;
  left: 50%;
  transform: translateX(-50%);
  min-width: 300px;
  max-width: 500px;
  padding: 15px 20px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  transition: top 0.3s ease, opacity 0.3s ease;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  background-color: #fff;
  z-index: 2000;
  opacity: 0;
}

.custom-alert.success {
  background-color: #f0f9eb;
  color: #67c23a;
  border: 1px solid #e1f3d8;
}

.custom-alert.warning {
  background-color: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #faecd8;
}

.custom-alert.error {
  background-color: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fde2e2;
}

.custom-alert.info {
  background-color: #f4f4f5;
  color: #909399;
  border: 1px solid #e9e9eb;
}

.alert-icon {
  margin-right: 10px;
  font-size: 16px;
}

.alert-content {
  flex: 1;
}

.alert-title {
  font-weight: bold;
  margin-bottom: 5px;
}

.alert-message {
  font-size: 14px;
}

.alert-close {
  margin-left: 10px;
  cursor: pointer;
  font-size: 16px;
  opacity: 0.7;
}

.alert-close:hover {
  opacity: 1;
}
</style>
