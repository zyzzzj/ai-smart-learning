<template>
  <div>
    <!-- 数据概览 -->
    <div class="page-card">
      <div class="card-title">
        <span>学习概览</span>
        <el-button size="small" :icon="Refresh" @click="loadAll">刷新</el-button>
      </div>
      <div class="stat-grid">
        <div class="stat-box">
          <div class="value">{{ stats.totalAnswered || 0 }}</div>
          <div class="label">累计作答</div>
        </div>
        <div class="stat-box">
          <div class="value">{{ stats.accuracy || 0 }}%</div>
          <div class="label">总正确率</div>
        </div>
        <div class="stat-box">
          <div class="value">{{ stats.sessionCount || 0 }}</div>
          <div class="label">刷题次数</div>
        </div>
        <div class="stat-box">
          <div class="value">{{ stats.wrongCount || 0 }}</div>
          <div class="label">待复习错题</div>
        </div>
        <div class="stat-box">
          <div class="value">{{ stats.examCount || 0 }}</div>
          <div class="label">参加考试</div>
        </div>
        <div class="stat-box">
          <div class="value">{{ stats.examAvgScore || 0 }}</div>
          <div class="label">考试平均分</div>
        </div>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- AI 学习建议 -->
      <el-col :span="14">
        <div class="page-card">
          <div class="card-title">
            <span>
              <el-icon><MagicStick /></el-icon>
              AI 学习建议
            </span>
            <el-button size="small" type="primary" :loading="adviceLoading" @click="generateAdvice">
              生成/更新建议
            </el-button>
          </div>
          <div v-if="advice.content" class="markdown-body advice" v-html="renderedAdvice"></div>
          <el-empty v-else description="还没有学习建议,点击右上角按你的练习数据生成" :image-size="90" />
          <div class="text-muted" v-if="advice.createTime">生成时间:{{ advice.createTime }}</div>
        </div>
      </el-col>

      <!-- 薄弱知识点 + 快捷入口 -->
      <el-col :span="10">
        <div class="page-card">
          <div class="card-title">薄弱知识点</div>
          <div v-if="stats.weakPoints && stats.weakPoints.length">
            <el-tag v-for="point in stats.weakPoints" :key="point" type="danger" effect="plain"
                    class="weak-tag">{{ point }}</el-tag>
            <div class="text-muted" style="margin-top:10px">建议优先做这些知识点的专项练习</div>
          </div>
          <el-empty v-else description="暂无薄弱知识点(需至少作答 2 题)" :image-size="80" />
        </div>

        <div class="page-card">
          <div class="card-title">快捷入口</div>
          <el-space wrap>
            <el-button type="primary" :icon="EditPen" @click="$router.push('/practice')">开始刷题</el-button>
            <el-button type="warning" :icon="Notebook" @click="$router.push('/wrong-book')">复习错题</el-button>
            <el-button type="success" :icon="Trophy" @click="$router.push('/exams')">参加考试</el-button>
            <el-button :icon="DataAnalysis" @click="$router.push('/report')">学习报告</el-button>
          </el-space>
        </div>
      </el-col>
    </el-row>

    <!-- 热门题目 -->
    <div class="page-card">
      <div class="card-title">热门题目(Redis 排行)</div>
      <el-table :data="hotQuestions" v-loading="loading" stripe>
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="stem" label="题干" min-width="320" show-overflow-tooltip />
        <el-table-column prop="type" label="题型" width="110">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="knowledgePointName" label="知识点" width="150" />
        <el-table-column prop="difficulty" label="难度" width="90">
          <template #default="{ row }">
            <el-rate :model-value="row.difficulty" disabled :max="5" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="作答次数" width="100" />
        <el-table-column prop="correctCount" label="答对次数" width="100" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DataAnalysis, EditPen, MagicStick, Notebook, Refresh, Trophy } from '@element-plus/icons-vue'
import { aiApi, practiceApi, questionApi } from '../../api'
import { renderMarkdown, typeText } from '../../utils/format'

const stats = ref({})
const advice = ref({})
const hotQuestions = ref([])
const loading = ref(false)
const adviceLoading = ref(false)

const renderedAdvice = computed(() => renderMarkdown(advice.value.content))

async function loadAll() {
  loading.value = true
  try {
    const [statsData, hot] = await Promise.all([
      practiceApi.stats(),
      questionApi.hot(null, 8)
    ])
    stats.value = statsData || {}
    hotQuestions.value = hot || []
    try {
      advice.value = (await aiApi.latestAdvice()) || {}
    } catch (e) {
      advice.value = {}
    }
  } finally {
    loading.value = false
  }
}

async function generateAdvice() {
  adviceLoading.value = true
  try {
    advice.value = await aiApi.advice()
    ElMessage.success('学习建议已更新')
  } finally {
    adviceLoading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.advice {
  max-height: 320px;
  overflow-y: auto;
  line-height: 1.8;
}

.weak-tag {
  margin: 0 8px 8px 0;
}
</style>
