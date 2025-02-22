<template>
  <div class="content-list">
    <div class="list-title">我的投递</div>
    <a-spin :spinning="loading" style="min-height: 200px;">
      <div class="list-content">
      <div class="order-item-view" v-for="(item, index) in postData" :key="index">
        <div class="header flex-view">
          <div class="left">
            <span class="text">投递编号</span>
            <span class="num mg-4">#</span>
            <span class="num">{{ item.id }}</span>
          </div>
          <div class="right">
            <span class="text">状态:</span>
            <span :class="['state', getStatusClass(item.status)]">{{ getStatusText(item.status) }}</span>
          </div>
        </div>
        <div class="bottom flex-view">
          <div class="left">
            <span class="text">{{ item.thingTitle }}</span>
            <span class="open" @click="handleDetail(item.thingId)">岗位详情</span>
            <a-button 
              v-if="item.status === 'pending'" 
              type="link" 
              danger 
              @click="handleCancel(item)"
            >
              取消投递
            </a-button>
          </div>
          <div class="right flex-view">
            <span class="text">投递时间</span>
            <span class="money">{{ getFormatTime(item.createTime, true) }}</span>
            <template v-if="item.status === 'interviewing'">
              <span class="text mg-left">面试时间</span>
              <span class="money">{{ formatInterviewTime(item.interviewTime) }}</span>
              <span class="text mg-left">面试地点</span>
              <span class="money">{{ item.interviewLocation }}</span>
            </template>
          </div>
        </div>
        <div v-if="item.notes" class="notes">
          备注：{{ item.notes }}
        </div>
      </div>
      <template v-if="!postData || postData.length <= 0">
        <a-empty style="width: 100%;margin-top: 200px;"/>
      </template>
    </div>
    </a-spin>
  </div>
</template>

<script setup>
import {message} from "ant-design-vue";
import {listUserPostApi, updatePostStatusApi} from '/@/api/post'
import {useUserStore} from "/@/store";
import {getFormatTime} from "/@/utils";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const loading = ref(false)
const postData = ref([
])
const orderStatus = ref('')

onMounted(()=>{
  getList()
})


const getList= ()=> {
  loading.value = true
  let userId = userStore.user_id
  listUserPostApi({userId: userId}).then(res => {
    postData.value = res.data
    loading.value = false
  }).catch(err => {
    console.log(err)
    loading.value = false
  })
}
const handleDetail =(thingId) =>{
  // 跳转新页面
  let text = router.resolve({name: 'detail', query: {id: thingId}})
  window.open(text.href, '_blank')
}

const getStatusText = (status) => {
  const statusMap = {
    pending: '待处理',
    interviewing: '面试中',
    hired: '已录用',
    rejected: '未通过',
    cancelled: '已取消'
  };
  return statusMap[status] || '待处理';
};

const getStatusClass = (status) => {
  const classMap = {
    pending: 'status-pending',
    interviewing: 'status-interviewing',
    hired: 'status-hired',
    rejected: 'status-rejected',
    cancelled: 'status-cancelled'
  };
  return classMap[status] || 'status-pending';
};

const handleCancel = async (post) => {
  try {
    await updatePostStatusApi({
      id: post.id,
      status: 'cancelled',
      notes: '用户取消投递'
    });
    message.success('已取消投递');
    getList();
  } catch (err) {
    message.error('操作失败');
  }
};

const formatInterviewTime = (timeStr) => {
  if (!timeStr) return '';
  // 将UTC时间转换为本地时间
  const date = new Date(timeStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).replace(/\//g, '-');
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
    -webkit-box-align: center;
    -ms-flex-align: center;
    align-items: center;
    -webkit-box-pack: justify;
    -ms-flex-pack: justify;
    justify-content: space-between;
    font-size: 14px;
    padding-top: 14px;

    .text {
      color: #6f6f6f;
    }

    .open {
      color: #4684e2;
      margin-left: 8px;
      cursor: pointer;
    }

    .right {
      -webkit-box-align: center;
      -ms-flex-align: center;
      align-items: center;
    }

    .text {
      color: #6f6f6f;
    }

    .num {
      color: #152844;
      margin: 0 40px 0 8px;
    }

    .money {
      font-size: 14px;
      color: #6f6f6f;
      margin-left: 8px;
    }
  }

}

.order-item-view:first-child {
  margin-top: 16px;
}

.mg-left {
  margin-left: 16px;
}

.notes {
  margin-top: 12px;
  color: #666;
  font-size: 14px;
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
  
  &.status-cancelled {
    color: #999;
  }
}

</style>
