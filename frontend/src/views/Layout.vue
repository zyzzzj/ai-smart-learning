<template>
  <el-container class="layout">
    <el-aside width="228px" class="aside">
      <div class="logo">
        <span class="dot">AI</span>
        <span class="txt">智能学习系统</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="#1f2d3d"
               text-color="#c0c4cc" active-text-color="#67c23a">
        <template v-for="group in menus" :key="group.title">
          <div class="menu-group">{{ group.title }}</div>
          <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="left">
          <span class="page-title">{{ $route.meta.title }}</span>
          <el-tag size="small" type="success" effect="plain" v-if="aiInfo.provider">
            AI: {{ aiInfo.provider }} / {{ aiInfo.model }}
          </el-tag>
        </div>
        <div class="right">
          <el-dropdown @command="onCommand">
            <span class="user">
              <el-avatar :size="30" style="background:#409eff">{{ avatarText }}</el-avatar>
              <span class="name">{{ userStore.nickname }}</span>
              <el-tag size="small" :type="userStore.isAdmin ? 'warning' : 'primary'" effect="plain">
                {{ userStore.isAdmin ? '管理员' : '学生' }}
              </el-tag>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <keep-alive :max="6">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import { aiApi } from '../api'

const router = useRouter()
const userStore = useUserStore()
const aiInfo = ref({})

const studentMenus = [
  { path: '/home', title: '学习首页', icon: 'HomeFilled' },
  { path: '/practice', title: '智能刷题', icon: 'EditPen' },
  { path: '/wrong-book', title: '错题本', icon: 'Notebook' },
  { path: '/exams', title: '考试中心', icon: 'Trophy' },
  { path: '/report', title: '学习报告', icon: 'DataAnalysis' },
  { path: '/history', title: '刷题记录', icon: 'Clock' },
  { path: '/profile', title: '个人中心', icon: 'User' }
]

const adminMenus = [
  { path: '/admin/questions', title: '题库管理', icon: 'Files' },
  { path: '/admin/ai-generate', title: 'AI 智能出题', icon: 'MagicStick' },
  { path: '/admin/exams', title: '考试与组卷', icon: 'Tickets' },
  { path: '/admin/knowledge', title: '科目与知识点', icon: 'Collection' },
  { path: '/admin/files', title: '资源管理', icon: 'FolderOpened' },
  { path: '/admin/tasks', title: 'AI 任务记录', icon: 'List' }
]

const menus = computed(() => {
  const list = [{ title: '学习中心', items: studentMenus }]
  if (userStore.isAdmin) {
    list.push({ title: '管理后台', items: adminMenus })
  }
  return list
})

const avatarText = computed(() => (userStore.nickname || 'U').slice(0, 1).toUpperCase())

function onCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

onMounted(async () => {
  try {
    if (!userStore.user) {
      await userStore.fetchMe()
    }
    aiInfo.value = await aiApi.status()
  } catch (e) {
    // 忽略:页面仍可用
  }
})
</script>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background: #1f2d3d;
  overflow-y: auto;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  color: #fff;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, .08);
}

.logo .dot {
  width: 30px;
  height: 30px;
  line-height: 30px;
  text-align: center;
  border-radius: 8px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  font-size: 13px;
}

.menu-group {
  padding: 14px 18px 6px;
  font-size: 12px;
  color: #6b7d90;
  letter-spacing: 1px;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
}

.left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 17px;
  font-weight: 600;
}

.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user .name {
  font-size: 14px;
}

.main {
  background: #f5f7fa;
  padding: 18px;
}
</style>
