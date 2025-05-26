import { createVNode, render } from 'vue';
import CustomAlert from '../components/CustomAlert.vue';

// 当前显示的警告列表
const alerts = [];

// 创建DOM容器
const createContainer = () => {
  const container = document.createElement('div');
  container.className = 'custom-alert-container';
  document.body.appendChild(container);
  return container;
};

// 创建并显示警告
const createAlert = (options) => {
  const container = document.querySelector('.custom-alert-container') || createContainer();

  // 创建警告组件实例
  const vnode = createVNode(CustomAlert, {
    ...options,
    onClose: () => {
      // 从DOM中移除
      render(null, container);
      // 从列表中移除
      const index = alerts.indexOf(vnode);
      if (index > -1) {
        alerts.splice(index, 1);
      }
      // 调用原始onClose回调
      if (options.onClose) {
        options.onClose();
      }
    }
  });

  // 渲染到DOM
  render(vnode, container);

  // 添加到列表
  alerts.push(vnode);

  return vnode;
};

// 关闭所有警告
const closeAll = () => {
  alerts.forEach((alert) => {
    if (alert.component && alert.component.exposed) {
      alert.component.exposed.closeAlert();
    }
  });
};

// 创建插件
const AlertPlugin = {
  install(app) {
    // 添加全局方法
    app.config.globalProperties.$alert = {
      success(message, options = {}) {
        return createAlert({ message, type: 'success', ...options });
      },
      error(message, options = {}) {
        return createAlert({ message, type: 'error', ...options });
      },
      info(message, options = {}) {
        return createAlert({ message, type: 'info', ...options });
      },
      warning(message, options = {}) {
        return createAlert({ message, type: 'warning', ...options });
      },
      closeAll
    };
  }
};

export default AlertPlugin;
