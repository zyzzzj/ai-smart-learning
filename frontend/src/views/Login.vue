<template>
  <div class="login-page">
    <div class="login-box">
      <div class="brand">
        <div class="logo">AI</div>
        <h1>基于AI大模型的智能学习系统</h1>
        <p class="text-muted">智能出题 · 智能刷题 · 智能考试</p>
      </div>

      <el-tabs v-model="tab">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" @submit.prevent>
            <el-form-item>
              <el-input v-model="loginForm.username" size="large" placeholder="用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" size="large" type="password" show-password
                        placeholder="密码" :prefix-icon="Lock" @keyup.enter="doLogin" />
            </el-form-item>
            <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doLogin">
              登 录
            </el-button>
          </el-form>
          <div class="tips">
            <span>演示账号:</span>
            <el-link type="primary" @click="fill('admin')">admin / 123456(管理员)</el-link>
            <el-divider direction="vertical" />
            <el-link type="primary" @click="fill('student')">student / 123456(学生)</el-link>
          </div>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" @submit.prevent>
            <el-form-item>
              <el-input v-model="registerForm.username" size="large" placeholder="用户名(3-20 位)" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="registerForm.nickname" size="large" placeholder="昵称" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="registerForm.password" size="large" type="password" show-password placeholder="密码(至少 6 位)" />
            </el-form-item>
            <el-button type="success" size="large" style="width:100%" :loading="loading" @click="doRegister">
              注册并登录
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="ai-status">
        <el-tag size="small" :type="aiInfo.provider === 'ollama' ? 'success' : 'info'">
          AI:{{ aiInfo.provider || '-' }}({{ aiInfo.mode || '-' }})
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'
import { aiApi, authApi } from '../api'

const router = useRouter()
const userStore = useUserStore()
const tab = ref('login')
const loading = ref(false)
const aiInfo = reactive({ provider: '', mode: '' })

const loginForm = reactive({ username: 'admin', password: '123456' })
const registerForm = reactive({ username: '', nickname: '', password: '' })

function fill(role) {
  loginForm.username = role
  loginForm.password = '123456'
}

async function doLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await userStore.login({ ...loginForm })
    ElMessage.success(`欢迎回来,${data.user.nickname || data.user.username}`)
    router.push('/home')
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('请填写用户名与密码')
    return
  }
  loading.value = true
  try {
    await authApi.register(registerForm)
    ElMessage.success('注册成功,正在登录')
    await userStore.login({ username: registerForm.username, password: registerForm.password })
    router.push('/home')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const status = await aiApi.status()
    aiInfo.provider = status.provider
    aiInfo.mode = status.mode
  } catch (e) {
    // 未登录时也能看到 AI 状态;失败忽略
  }
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #3a5169 60%, #4f6d8f 100%);
}

.login-box {
  width: 420px;
  background: #fff;
  border-radius: 14px;
  padding: 32px 30px 22px;
  box-shadow: 0 18px 50px rgba(0, 0, 0, .28);
}

.brand {
  text-align: center;
  margin-bottom: 18px;
}

.logo {
  width: 54px;
  height: 54px;
  line-height: 54px;
  margin: 0 auto 12px;
  border-radius: 14px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  color: #fff;
  font-weight: 700;
  font-size: 20px;
}

.brand h1 {
  font-size: 19px;
  margin: 0 0 6px;
}

.tips {
  margin-top: 14px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.ai-status {
  margin-top: 14px;
  text-align: center;
}
</style>
