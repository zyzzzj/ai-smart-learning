<template>
  <div>
    <div class="page-card">
      <div class="card-title">
        <span>错题本</span>
        <el-radio-group v-model="query.mastered" size="small" @change="load">
          <el-radio-button :value="0">待复习</el-radio-button>
          <el-radio-button :value="1">已掌握</el-radio-button>
          <el-radio-button :value="null">全部</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div style="padding:10px 20px">
              <div class="question-stem">{{ row.stem }}</div>
              <div class="analysis-box">
                <div>我的上次作答:<b style="color:#f56c6c">{{ row.lastAnswer || '未作答' }}</b></div>
                <div style="margin-top:6px">参考答案:<b style="color:#67c23a">{{ row.referenceAnswer }}</b></div>
                <div v-if="row.analysis" style="margin-top:6px">解析:{{ row.analysis }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="stem" label="题干" min-width="300" show-overflow-tooltip />
        <el-table-column prop="type" label="题型" width="100">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="knowledgePointName" label="知识点" width="140" />
        <el-table-column prop="wrongCount" label="错误次数" width="100" sortable />
        <el-table-column prop="lastWrongTime" label="最近错误" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.mastered === 1 ? 'success' : 'danger'" effect="plain">
              {{ row.mastered === 1 ? '已掌握' : '待复习' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="practiceWrong(row)">重做</el-button>
            <el-button link type="success" v-if="row.mastered !== 1" @click="markMastered(row)">标记已掌握</el-button>
            <el-button link type="danger" @click="remove(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !list.length" description="错题本是空的,继续保持!" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { practiceApi } from '../../api'
import { typeText } from '../../utils/format'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const query = reactive({ mastered: 0 })

async function load() {
  loading.value = true
  try {
    const params = { limit: 100 }
    if (query.mastered !== null) params.mastered = query.mastered
    list.value = await practiceApi.wrongBook(params)
  } finally {
    loading.value = false
  }
}

function practiceWrong() {
  router.push('/practice')
  ElMessage.info('在练习页选择「错题重做」模式即可专项练习')
}

async function markMastered(row) {
  await practiceApi.removeWrong(row.questionId)
  ElMessage.success('已标记为掌握')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('确定从错题本移除这道题吗?', '提示', { type: 'warning' })
  await practiceApi.removeWrong(row.questionId)
  ElMessage.success('已移除')
  load()
}

onMounted(load)
</script>
