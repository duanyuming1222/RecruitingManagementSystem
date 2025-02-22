<template>
  <div class="content-list">
    <div class="list-title">投递管理</div>
    <div class="filter-bar">
      <a-radio-group v-model:value="filterStatus" @change="handleFilterChange">
        <a-radio-button value="">全部</a-radio-button>
        <a-radio-button value="pending">待处理</a-radio-button>
        <a-radio-button value="interviewing">面试中</a-radio-button>
        <a-radio-button value="hired">已录用</a-radio-button>
        <a-radio-button value="rejected">未通过</a-radio-button>
      </a-radio-group>
    </div>
    <a-spin :spinning="loading" style="min-height: 200px;">
      <div class="list-content">
        <div class="order-item-view" v-for="(item, index) in postData" :key="index">
          <div class="header flex-view">
            <div class="left">
              <span class="text">投递编号</span>
              <span class="num mg-4">#</span>
              <span class="num">{{item.id}}</span>
            </div>
            <div class="right">
              <span class="text">状态:</span>
              <span :class="['state', getStatusClass(item.status)]">{{ getStatusText(item.status) }}</span>
            </div>
          </div>
          <div class="bottom flex-view">
            <div class="left">
              <div class="job-info">
                <div class="job-title">{{ item.thingDetail?.title || '未知职位' }}</div>
                <div class="job-meta">
                  <span>{{ formatSalary(item.thingDetail?.minSalary, item.thingDetail?.maxSalary) }}</span>
                  <span class="divider">·</span>
                  <span>{{ item.thingDetail?.location || '地点待定' }}</span>
                  <span class="divider">·</span>
                  <span>{{ item.thingDetail?.education || '学历不限' }}</span>
                  <span class="action-links">
                    <span class="open" @click="handleDetail(item.thingId)">查看详情</span>
                  </span>
                </div>
              </div>
            </div>
            <div class="right-actions">
              <div class="action-btns">
                <a-dropdown v-if="item.status !== 'rejected' && item.status !== 'hired'" :trigger="['click']">
                  <a-button type="primary" size="small">
                    操作 <down-outlined />
                  </a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item v-if="item.status === 'pending'" @click="handleInvite(item)">
                        邀请面试
                      </a-menu-item>
                      <a-menu-item v-if="item.status === 'interviewing'" @click="handleModifyInterview(item)">
                        修改面试
                      </a-menu-item>
                      <a-menu-item v-if="item.status === 'pending'" @click="handleResult(item, 'rejected', '简历筛选未通过')">
                        拒绝简历
                      </a-menu-item>
                      <a-menu-item v-if="item.status === 'interviewing'" @click="handleResult(item, 'hired')">
                        通过面试
                      </a-menu-item>
                      <a-menu-item v-if="item.status === 'interviewing'" @click="handleResult(item, 'rejected', '面试未通过')">
                        面试未通过
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </div>
              <div class="info-group">
                <div class="info-item">
                  <span class="label">投递人</span>
                  <span class="value">{{ item.name }}</span>
                  <a-button 
                    type="link" 
                    size="small" 
                    @click="handleResume(item)"
                    class="resume-btn"
                  >
                    <template #icon><file-text-outlined /></template>
                    查看简历
                  </a-button>
                </div>
                <div class="info-item">
                  <span class="label">投递时间</span>
                  <span class="value">{{ getFormatTime(item.createTime, true) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <template v-if="!postData || postData.length <= 0">
          <a-empty style="width: 100%;margin-top: 200px;"/>
        </template>
      </div>
    </a-spin>

    <!-- 邀请面试对话框 -->
    <a-modal
      v-model:visible="inviteModalVisible"
      :title="currentPost?.status === 'interviewing' ? '修改面试信息' : '邀请面试'"
      @ok="confirmInvite"
      @cancel="cancelInvite"
    >
      <a-form v-if="currentPost" :model="inviteForm">
        <a-form-item label="面试时间">
          <a-date-picker 
            v-model:value="inviteForm.interviewTime"
            :show-time="true"
            format="YYYY-MM-DD HH:mm:ss"
            :show-now="false"
          />
        </a-form-item>
        <a-form-item label="面试地点">
          <a-input v-model:value="inviteForm.interviewLocation" />
        </a-form-item>
        <a-form-item label="备注信息">
          <a-textarea v-model:value="inviteForm.notes" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import {message} from "ant-design-vue";
import {listCompanyPostApi, updatePostStatusApi} from '/@/api/post'
import {listUserCompanyApi} from '/@/api/company'
import {BASE_URL} from "/@/store/constants";
import {useUserStore} from "/@/store";
import {getFormatTime} from "/@/utils";
import {DownOutlined, FileTextOutlined} from '@ant-design/icons-vue';
import {ref, reactive} from 'vue';
import { useRouter, useRoute } from 'vue-router';
import dayjs from 'dayjs';
import {detailApi as getThingDetail} from '/@/api/thing';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const loading = ref(false)
const postData = ref([]);
const allPostData = ref([]); // 存储所有数据
const orderStatus = ref('')

const inviteModalVisible = ref(false);
const currentPost = ref(null);
const inviteForm = reactive({
  interviewTime: undefined,
  interviewLocation: '',
  notes: ''
});

const filterStatus = ref('');

const formatSalary = (min, max) => {
  // 确保输入是数字
  min = Number(min);
  max = Number(max);
  
  // 检查是否为有效数字
  if (isNaN(min) && isNaN(max)) return '薪资面议';
  if (isNaN(max)) return `${min}K+`;
  if (isNaN(min)) return `${max}K以内`;
  return `${min}-${max}K`;
};

onMounted(()=>{
  getList()
})

const getList = async () => {
  loading.value = true;
  let userId = userStore.user_id;

  try {
    const companyRes = await listUserCompanyApi({userId: userId});
    if(companyRes.data && companyRes.data.length > 0) {
      let company = companyRes.data[0];
      const postsRes = await listCompanyPostApi({companyId: company.id});
      
      // 按创建时间倒序排序
      const sortedData = postsRes.data.sort((a, b) => {
        return new Date(b.createTime) - new Date(a.createTime);
      });

      // 获取每个投递记录对应的岗位信息
      for (let post of sortedData) {
        try {
          const thingRes = await getThingDetail({id: post.thingId});
          post.thingDetail = thingRes.data;
        } catch (err) {
          console.error('获取岗位信息失败:', err);
        }
      }

      allPostData.value = sortedData;
      handleFilterChange();
    } else {
      message.warn("请完善公司资料");
    }
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const handleResume = (item) => {
  if (!item.resumeId) {
    message.warning('未找到简历信息');
    return;
  }
  if (!item.raw) {
    message.warning('简历文件不存在');
    return;
  }

  // 创建简历操作的下拉菜单
  const menu = document.createElement('div');
  menu.className = 'resume-action-menu';
  
  // 预览按钮
  const previewBtn = document.createElement('a');
  previewBtn.textContent = '预览简历';
  previewBtn.onclick = () => {
    const previewUrl = `${BASE_URL}/api/resume/file/${item.raw}`;
    window.open(previewUrl, '_blank');
    document.body.removeChild(menu);
  };
  
  // 下载按钮
  const downloadBtn = document.createElement('a');
  downloadBtn.textContent = '下载简历';
  downloadBtn.onclick = () => {
    const downloadUrl = `${BASE_URL}/api/resume/download/${item.raw}`;
    window.open(downloadUrl, '_blank');
    document.body.removeChild(menu);
  };
  
  menu.appendChild(previewBtn);
  menu.appendChild(downloadBtn);
  
  // 定位菜单
  const rect = event.target.getBoundingClientRect();
  menu.style.position = 'fixed';
  menu.style.top = `${rect.top - 20}px`;  // 稍微向上偏移
  menu.style.left = `${rect.right + 5}px`;  // 显示在按钮右侧
  
  // 添加到页面
  document.body.appendChild(menu);
  
  // 点击其他地方关闭菜单
  const closeMenu = (e) => {
    if (!menu.contains(e.target)) {
      document.body.removeChild(menu);
      document.removeEventListener('click', closeMenu);
    }
  };
  setTimeout(() => document.addEventListener('click', closeMenu), 0);
};

const handleDetail =(thingId) =>{
  // 跳转新页面
  let text = router.resolve({name: 'detail', query: {id: thingId}})
  window.open(text.href, '_blank')
}

const getStatusText = (status) => {
  const statusMap = {
    'pending': '待处理',
    'interviewing': '面试中',
    'hired': '已录用',
    'rejected': '未通过'
  };
  return statusMap[status] || '待处理';
};

const getStatusClass = (status) => {
  const classMap = {
    'pending': 'status-pending',
    'interviewing': 'status-interviewing',
    'hired': 'status-hired',
    'rejected': 'status-rejected'
  };
  return classMap[status] || 'status-pending';
};

const handleInvite = (post) => {
  currentPost.value = post;
  inviteModalVisible.value = true;
};

const handleModifyInterview = (post) => {
  if (!post) return;

  // 重置表单
  inviteForm.interviewTime = undefined;
  inviteForm.location = '';
  inviteForm.notes = '';

  currentPost.value = post;
  // 处理日期格式
  if (post.interviewTime) {
    inviteForm.interviewTime = dayjs(post.interviewTime);
  }
  inviteForm.location = post.interviewLocation || '';
  inviteForm.notes = post.notes || '';

  // 打开模态框
  inviteModalVisible.value = true;
};

const confirmInvite = async () => {
  if (!currentPost.value) {
    message.error('数据错误');
    return;
  }

  if (!inviteForm.interviewTime || !inviteForm.interviewLocation) {
    message.error('请填写完整的面试信息');
    return;
  }

  try {
    await updatePostStatusApi({
      id: currentPost.value.id,
      status: 'interviewing',
      interviewTime: inviteForm.interviewTime,
      interviewLocation: inviteForm.interviewLocation,
      notes: inviteForm.notes
    });
    message.success(currentPost.value.status === 'interviewing' ? '已更新面试信息' : '已发送面试邀请');
    inviteModalVisible.value = false;
    getList();
  } catch (err) {
    console.error('更新面试信息失败:', err);
    message.error('操作失败');
  }
};

const cancelInvite = () => {
  currentPost.value = null;
  inviteModalVisible.value = false;
  inviteForm.interviewTime = undefined;
  inviteForm.location = '';
  inviteForm.notes = '';
};

const handleResult = async (post, status, reason = '') => {
  try {
    console.log("post:" + post.id)
    await updatePostStatusApi({
      id: post.id,
      status: status,
      notes: reason
    });
    message.success('状态更新成功');
    getList();
  } catch (err) {
    message.error('操作失败');
  }
};

const handleFilterChange = () => {
  if (!filterStatus.value) {
    postData.value = [...allPostData.value];
  } else {
    postData.value = allPostData.value.filter(post => post.status === filterStatus.value);
  }
};

</script>
<style scoped lang="less">
.flex-view {
  display: -webkit-box;
  display: -ms-flexbox;
  display: flex;
}

.content-list {
  -webkit-box-flex: 1;
  -ms-flex: 1;
  flex: 1;

  .list-title {
    color: #152844;
    font-weight: 600;
    font-size: 18px;
    line-height: 24px;
    height: 24px;
    margin-bottom: 4px;
  }

  .filter-bar {
    margin-bottom: 16px;
    padding: 16px;
    background: #fff;
    border-radius: 4px;
    
    .ant-radio-group {
      display: flex;
      gap: 8px;
    }
  }
}

.order-item-view {
  background: #f7f9fb;
  border-radius: 4px;
  padding: 16px;
  margin-top: 12px;

  .header {
    border-bottom: 1px solid #cedce4;
    padding-bottom: 12px;
    -webkit-box-pack: justify;
    -ms-flex-pack: justify;
    justify-content: space-between;
    font-size: 14px;

    .text {
      color: #6f6f6f;
    }

    .mg-4 {
      margin-left: 4px;
    }

    .num {
      font-weight: 500;
      color: #152844;
    }

    .num {
      font-weight: 500;
      color: #152844;
    }

    .time {
      margin-left: 16px;
      color: #a1adc5;
    }

    .state {
      color: #ff7b31;
      font-weight: 600;
      margin-left: 10px;
    }
  }

  .content {
    padding: 12px 0;
    overflow: hidden;

    .left-list {
      overflow: hidden;
      height: 132px;
      -webkit-box-flex: 2;
      -ms-flex: 2;
      flex: 2;
      padding-right: 16px;

      .list-item {
        height: 60px;
        margin-bottom: 12px;
        overflow: hidden;
        cursor: pointer;
      }

      .thing-img {
        width: 48px;
        height: 100%;
        margin-right: 12px;
      }

      .detail {
        -webkit-box-flex: 1;
        -ms-flex: 1;
        flex: 1;
        -webkit-box-orient: vertical;
        -webkit-box-direction: normal;
        -ms-flex-direction: column;
        flex-direction: column;
      }

      .flex-between {
        -webkit-box-pack: justify;
        -ms-flex-pack: justify;
        justify-content: space-between;
      }

      .flex-between {
        -webkit-box-pack: justify;
        -ms-flex-pack: justify;
        justify-content: space-between;
      }

      .flex-top {
        -webkit-box-align: start;
        -ms-flex-align: start;
        align-items: flex-start;
      }

      .name {
        color: #152844;
        font-weight: 600;
        font-size: 14px;
        line-height: 18px;
      }

      .count {
        color: #484848;
        font-size: 12px;
      }

      .flex-between {
        -webkit-box-pack: justify;
        -ms-flex-pack: justify;
        justify-content: space-between;
      }

      .flex-center {
        -webkit-box-align: center;
        -ms-flex-align: center;
        align-items: center;
      }

      .type {
        color: #6f6f6f;
        font-size: 12px;
      }

      .price {
        color: #ff7b31;
        font-weight: 600;
        font-size: 14px;
      }
    }

    .right-info {
      -webkit-box-flex: 1;
      -ms-flex: 1;
      flex: 1;
      border-left: 1px solid #cedce4;
      padding-left: 12px;
      line-height: 22px;
      font-size: 14px;

      .title {
        color: #6f6f6f;
      }

      .name {
        color: #152844;
      }

      .text {
        color: #484848;
      }

      .mg {
        margin-bottom: 4px;
      }
    }
  }

  .bottom {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding: 16px 0;
    border-top: 1px solid #f0f0f0;
  }
}

.order-item-view:first-child {
  margin-top: 16px;
}

.state {
  margin-left: 8px;
  font-weight: 500;
  
  &.status-pending {
    color: #faad14;
  }
  
  &.status-interviewing {
    color: #1890ff;
  }
  
  &.status-hired {
    color: #52c41a;
  }
  
  &.status-rejected {
    color: #ff4d4f;
  }
}

.left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.job-info {
  flex: 1;
  
  .job-title {
    font-size: 16px;
    font-weight: 500;
    color: #1a1a1a;
    margin-bottom: 4px;
  }
  
  .job-meta {
    font-size: 14px;
    color: #666;
    display: flex;
    align-items: center;
    
    .divider {
      margin: 0 8px;
      color: #ddd;
    }

    .action-links {
      margin-left: 16px;
      
      .open {
        color: #4684e2;
        cursor: pointer;
        
        &:hover {
          color: #1890ff;
        }
      }
    }
  }
}

.right-actions {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-left: auto;
}

.info-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  
  .label {
    color: #666;
    font-size: 14px;
  }
  
  .value {
    color: #333;
    font-size: 14px;
  }
  
  .resume-btn {
    padding: 0 8px;
    height: 24px;
    line-height: 24px;
    
    &:hover {
      background-color: #e6f4ff;
    }
    
    .anticon {
      font-size: 14px;
      margin-right: 4px;
    }
  }
}

.action-btns {
  display: flex;
  align-items: center;
  gap: 8px;
}

.open {
  color: #4684e2;
  cursor: pointer;
  white-space: nowrap;
}

.resume-action-menu {
  background-color: #ffffff;
  border-radius: 4px;
  box-shadow: 0 3px 6px -4px rgba(0, 0, 0, 0.12), 
              0 6px 16px 0 rgba(0, 0, 0, 0.08), 
              0 9px 28px 8px rgba(0, 0, 0, 0.05);
  padding: 8px;
  z-index: 9999;
  width: 100px;
  border: 1px solid #f0f0f0;
  position: relative;
  background: #ffffff;
  transform-origin: left top;
  animation: slideIn 0.2s ease-out;
  display: flex;
  flex-direction: column;
  gap: 4px;
  
  a {
    display: block;
    padding: 6px 12px;
    color: #333;
    cursor: pointer;
    text-decoration: none;
    transition: all 0.3s;
    background-color: #ffffff;
    position: relative;
    margin: 0;
    border-radius: 2px;
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    
    &:hover {
      background: #e6f4ff;
      color: #4684e2;
    }
    
    &:active {
      background: #bae0ff;
    }

    &::before {
      font-family: 'anticon';
      font-size: 13px;
    }
  }

  a:first-child::before {
    content: '\e7b9';  // 预览图标
  }

  a:last-child::before {
    content: '\e664';  // 下载图标
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-5px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

</style>
