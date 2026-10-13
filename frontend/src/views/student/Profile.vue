<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="10">
        <div class="page-card">
          <div class="card-title">账号信息</div>
          <div class="profile-head">
            <el-avatar :size="60" style="background:#409eff;font-size:24px">{{ avatarText }}</el-avatar>
            <div>
              <div class="name">{{ user.nickname || user.username }}</div>
              <el-tag size="small" :type="user.role === 'ADMIN' ? 'warning' : 'primary'" effect="plain">
                {{ user.role === 'ADMIN' ? '管理员' : '学生' }}
              </el-tag>
            </div>
          </div>
          <el-descriptions :column="1" border style="margin-top:16px">
            <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ user.email || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ user.phone || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="最后登录">{{ user.lastLogin || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ user.createTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-col>

      <el-col :span="14">
        <div class="page-card">
          <div class="card-title">修改密码</div>
          <el-form :model="pwdForm" label-width="100px" style="max-width:460px">
            <el-form-item label="原密码">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="pwdForm.confirm" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="changePassword">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="page-card">
          <div class="card-title">系统状态</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="AI 提供方">{{ aiInfo.provider || '-' }}</el-descriptions-item>
            <el-descriptions-item label="AI 模型">{{ aiInfo.model || '-' }}</el-descriptions-item>
            <el-descriptions-item label="运行模式">{{ aiInfo.mode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="对象存储">{{ storageType || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="text-muted" style="margin-top:10px">
            切换到真实大模型:设置环境变量 <code>AI_PROVIDER=ollama</code> 并启动 Ollama
            (<code>ollama pull deepseek-r1:7b</code>)。
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aiApi, authApi, fileApi } from '../../api'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()
const user = computed(() => userStore.user || {})
const avatarText = computed(() => (userStore.nickname || 'U').slice(0, 1).toUpperCase())

const aiInfo = ref({})
const storageType = ref('')
const saving = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function changePassword() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写原密码与新密码')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  saving.value = true
  try {
    await authApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功,请重新登录')
    userStore.logout()
    setTimeout(() => location.reload(), 800)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    userStore.user = await authApi.me()
    aiInfo.value = await aiApi.status()
    storageType.value = await fileApi.storage()
  } catch (e) {
    // 忽略
  }
})
</script>

<style scoped>
.profile-head {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-head .name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
}

code {
  background: #f5f7fa;
  padding: 1px 6px;
  border-radius: 4px;
}
</style>
