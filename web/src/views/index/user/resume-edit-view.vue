<template>
  <div class="content-list">
    <div class="list-title">我的简历</div>
    <a-spin :spinning="loading" style="min-height: 200px;">
      <div class="list-content">
      <div class="edit-view">
        <div class="item flex-view">
          <div class="label">头像</div>
          <div class="right-box avatar-box flex-view">
            <img v-if="tData.form && tData.form.cover" :src="tData.form.coverUrl" class="avatar">
            <img v-else :src="AvatarIcon" class="avatar">
            <div class="change-tips flex-view">
                <a-upload
                  name="coverFile"
                  accept="image/*"
                  :multiple="false"
                  :before-upload="beforeUpload"
                >
                  <label>点击更换头像</label>
                </a-upload>
              <p class="tip">图片格式支持 GIF、PNG、JPEG，尺寸不小于 200 PX，小于 4 MB</p>
            </div>
          </div>
        </div>
        <div class="item flex-view">
          <div class="label">姓名</div>
          <div class="right-box">
            <input type="text" v-model="tData.form.name" placeholder="请输入姓名" maxlength="20" class="input-dom">
            <p class="tip">支持中英文，长度不能超过 20 个字符</p>
          </div>
        </div>
        <div class="item flex-view">
          <div class="label">性别</div>
          <div class="right-box">
            <input type="text" v-model="tData.form.sex" placeholder="请输入性别" maxlength="100" class="input-dom web-input">
          </div>
        </div>
        <div class="item flex-view">
          <div class="label">学历</div>
          <div class="right-box">
            <input type="text" v-model="tData.form.education" placeholder="请输入学历" maxlength="100" class="input-dom web-input">
          </div>
        </div>
        <div class="item flex-view">
          <div class="label">毕业学校</div>
          <div class="right-box">
            <input type="text" v-model="tData.form.school" placeholder="请输入学校" maxlength="100" class="input-dom web-input">
          </div>
        </div>
        <div class="item flex-view">
          <div class="label">手机号</div>
          <div class="right-box">
            <input type="text" v-model="tData.form.mobile" placeholder="请输入邮箱" maxlength="100" class="input-dom web-input">
          </div>
        </div>
        <div class="item flex-view">
          <div class="label">邮箱</div>
          <div class="right-box">
            <input type="text" v-model="tData.form.email" placeholder="请输入邮箱" maxlength="100" class="input-dom web-input">
          </div>
        </div>
        <div class="item flex-view">
          <div class="label">简历附件</div>
          <div class="right-box">
            <div v-if="tData.form.raw" class="file-info">
              <div class="file-display">
                <file-pdf-outlined v-if="isPdf" class="file-icon pdf" />
                <file-word-outlined v-else-if="isWord" class="file-icon word" />
                <file-outlined v-else class="file-icon" />
                <span class="file-name">{{ getDisplayFileName }}</span>
              </div>
              <div class="file-actions">
                <a-button type="link" @click="handlePreview" v-if="isPdf">
                  <eye-outlined /> 预览
                </a-button>
                <a-button type="link" @click="handleDownload">
                  <download-outlined /> 下载
                </a-button>
              </div>
            </div>
            
            <a-upload
              :multiple="false"
              :before-upload="beforeUpload1"
              accept=".pdf,.doc,.docx"
            >
              <a-button>
                <upload-outlined />
                {{ tData.form.raw ? '重新上传' : '点击选择文件' }}
              </a-button>
              <span class="upload-tip">（支持 PDF、DOC、DOCX 格式）</span>
            </a-upload>
          </div>
        </div>
        <button class="save mg" @click="submit()">保存</button>
      </div>
    </div>
    </a-spin>
  </div>
</template>

<script setup>
import {message} from "ant-design-vue";
import {
  detailApi, createApi, updateApi, getPreviewUrl, getDownloadUrl
} from '/@/api/resume'
import {BASE_URL} from "/@/store/constants";
import {useUserStore} from "/@/store";
import AvatarIcon from '/@/assets/images/avatar.jpg'
import { computed } from 'vue';
import { 
  UploadOutlined, 
  EyeOutlined, 
  DownloadOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FileOutlined
} from '@ant-design/icons-vue';

const router = useRouter();
const userStore = useUserStore();

let loading = ref(false)
let tData = reactive({
  form:{
    id: undefined,
    name: undefined,
    sex: undefined,
    birthday: undefined,
    education: undefined,
    cover: undefined,
    coverUrl: undefined,
    coverFile: undefined,
    rawFile: undefined,
    raw: undefined,
    email: undefined,
    mobile: undefined,
    description: undefined,
    downloadUrl: undefined,
  }
})

onMounted(()=>{
  getDetail()
})

const beforeUpload = (file) => {
  // 检查文件类型
  const isImage = file.type.startsWith('image/');
  if (!isImage) {
    message.error('只能上传图片文件!');
    return false;
  }

  // 检查文件大小（4MB）
  const isLt4M = file.size / 1024 / 1024 < 4;
  if (!isLt4M) {
    message.error('图片必须小于4MB!');
    return false;
  }

  // 保存文件
  tData.form.coverFile = file;
  return false;
}

const isPdf = computed(() => {
  const filename = tData.form.raw;
  return filename && filename.toLowerCase().endsWith('.pdf');
});

const isWord = computed(() => {
  const filename = tData.form.raw;
  if (!filename) return false;
  const lower = filename.toLowerCase();
  return lower.endsWith('.doc') || lower.endsWith('.docx');
});

const getDisplayFileName = computed(() => {
  const name = tData.form.raw;
  if (!name) return '';
  const uuidLength = 36; // UUID的长度
  if (name.length > uuidLength) {
    return name.substring(uuidLength + 1); // +1 是为了跳过连字符
  }
  return name;
});

const handlePreview = () => {
  if (!tData.form.raw) return;
  if (isPdf.value) {
    const filename = tData.form.raw.split('/').pop();
    window.open(`${BASE_URL}/api/resume/file/${filename}`, '_blank');
  }
};

const handleDownload = () => {
  if (!tData.form.raw) return;
  const filename = tData.form.raw.split('/').pop();
  window.location.href = `${BASE_URL}/api/resume/download/${filename}`;
};

const beforeUpload1 = (file) => {
  // 检查文件类型
  const isValidType = ['.pdf', '.doc', '.docx'].some(ext => 
    file.name.toLowerCase().endsWith(ext)
  );
  
  if (!isValidType) {
    message.error('只能上传 PDF、DOC、DOCX 格式的文件!');
    return false;
  }
  
  // 检查文件大小（10MB）
  const isLt10M = file.size / 1024 / 1024 < 10;
  if (!isLt10M) {
    message.error('文件必须小于10MB!');
    return false;
  }

  tData.form.rawFile = file;
  return false;
};

const getDetail =()=> {
  loading.value = true
  let userId = userStore.user_id
  detailApi({userId: userId}).then(res => {
    if(res.data){
      tData.form = res.data
      if (tData.form.cover) {
        tData.form.coverUrl = `${BASE_URL}/api/upload/resume/${tData.form.cover}`;
      }
      if(tData.form.raw) {
        tData.form.downloadUrl = BASE_URL + tData.form.raw
      }
    }

    loading.value = false
  }).catch(err => {
    console.log(err)
    loading.value = false
  })
}
const submit =()=> {
  let formData = new FormData()
  let userId = userStore.user_id
  formData.append('userId', userId)

  if (tData.form.id) {
    formData.append('id', tData.form.id)
  }
  if (tData.form.coverFile) {
    formData.append('coverFile', tData.form.coverFile)
  }
  if (tData.form.rawFile) {
    formData.append('rawFile', tData.form.rawFile)
  }
  if (tData.form.name) {
    formData.append('name', tData.form.name)
  }
  if (tData.form.sex) {
    formData.append('sex', tData.form.sex)
  }
  if (tData.form.email) {
    formData.append('email', tData.form.email)
  }
  if (tData.form.mobile) {
    formData.append('mobile', tData.form.mobile)
  }
  if (tData.form.education) {
    formData.append('education', tData.form.education)
  }
  if (tData.form.school) {
    formData.append('school', tData.form.school)
  }

  if(tData.form.id){
    updateApi(formData).then(res => {
      message.success('保存成功')
      getDetail()
    }).catch(err => {
      console.log(err)
    })
  }else {
    createApi(formData).then(res => {
      message.success('保存成功')
      getDetail()
    }).catch(err => {
      console.log(err)
    })
  }

}

</script>

<style scoped lang="less">
input, textarea {
  border-style: none;
  outline: none;
  margin: 0;
  padding: 0;
}

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
    line-height: 48px;
    height: 48px;
    margin-bottom: 4px;
    border-bottom: 1px solid #cedce4;
  }

  .edit-view {
    .item {
      -webkit-box-align: center;
      -ms-flex-align: center;
      align-items: center;
      margin: 24px 0;

      .label {
        width: 100px;
        color: #152844;
        font-weight: 600;
        font-size: 14px;
      }

      .right-box {
        -webkit-box-flex: 1;
        -ms-flex: 1;
        flex: 1;
      }

      .avatar {
        width: 64px;
        height: 64px;
        border-radius: 50%;
        margin-right: 16px;
      }

      .change-tips {
        -webkit-box-align: center;
        -ms-flex-align: center;
        align-items: center;
        -ms-flex-wrap: wrap;
        flex-wrap: wrap;
      }

      label {
        color: #4684e2;
        font-size: 14px;
        line-height: 22px;
        height: 22px;
        cursor: pointer;
        width: 100px;
        display: block;
      }

      .tip {
        color: #6f6f6f;
        font-size: 14px;
        height: 22px;
        line-height: 22px;
        margin: 0;
        width: 100%;
      }

      .right-box {
        -webkit-box-flex: 1;
        -ms-flex: 1;
        flex: 1;
      }

      .input-dom {
        width: 400px;
      }

      .input-dom {
        background: #f8fafb;
        border-radius: 4px;
        height: 40px;
        line-height: 40px;
        font-size: 14px;
        color: #152844;
        padding: 0 12px;
      }

      .tip {
        font-size: 12px;
        line-height: 16px;
        color: #6f6f6f;
        height: 16px;
        margin-top: 4px;
      }

      .intro {
        resize: none;
        background: #f8fafb;
        width: 100%;
        padding: 8px 12px;
        height: 82px;
        line-height: 22px;
        font-size: 14px;
        color: #152844;
      }
    }

    .save {
      background: #4684e2;
      border-radius: 32px;
      width: 96px;
      height: 32px;
      line-height: 32px;
      font-size: 14px;
      color: #fff;
      border: none;
      outline: none;
      cursor: pointer;
    }

    .mg {
      margin-left: 100px;
    }
  }
}

.file-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f8fafb;
  border-radius: 4px;
  margin-bottom: 12px;
}

.file-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  font-size: 20px;
  color: #666;
  
  &.pdf {
    color: #ff4d4f;
  }
  
  &.word {
    color: #1890ff;
  }
}

.file-name {
  color: #333;
  font-size: 14px;
}

.file-actions {
  display: flex;
  gap: 8px;
}

.upload-tip {
  margin-left: 8px;
  color: #666;
  font-size: 14px;
}

</style>
