<template>
  <div v-loading="loading">
    <div class="page-card" v-if="result.recordId">
      <div class="card-title">
        <span>{{ result.examName }} · 成绩单</span>
        <el-button size="small" @click="$router.push('/exams')">返回考试中心</el-button>
      </div>

      <el-result :icon="result.pass ? 'success' : 'warning'"
                 :title="result.pass ? '恭喜,考试通过' : '未达及格线,继续加油'"
                 :sub-title="`总分 ${result.totalScore} / 满分 ${questionTotal} · 及格分 ${result.passScore} · 得分率 ${result.rate}%`">
        <template #extra>
          <div class="stat-grid" style="margin-bottom:14px">
            <div class="stat-box">
              <div class="value">{{ result.objectiveScore }}</div>
              <div class="label">客观题得分</div>
            </div>
            <div class="stat-box">
              <div class="value">{{ result.subjectiveScore }}</div>
              <div class="label">主观题得分(AI 评分)</div>
            </div>
            <div class="stat-box">
              <div class="value">{{ result.correctCount }}/{{ result.questionCount }}</div>
              <div class="label">答对题数</div>
            </div>
            <div class="stat-box">
              <div class="value">{{ result.submitTime }}</div>
              <div class="label">交卷时间</div>
            </div>
          </div>
        </template>
      </el-result>
    </div>

    <!-- AI 学习建议 -->
    <div class="page-card" v-if="result.aiComment">
      <div class="card-title">
        <span><el-icon><MagicStick /></el-icon> AI 学习建议(基于本次考试表现)</span>
      </div>
      <div class="markdown-body" v-html="adviceHtml"></div>
    </div>

    <!-- 逐题解析 -->
    <div class="page-card">
      <div class="card-title">逐题解析</div>
      <div v-for="(item, index) in result.details" :key="item.questionId" class="detail-item">
        <div class="flex-between">
          <div>
            <b>第 {{ index + 1 }} 题</b>
            <el-tag size="small" style="margin-left:8px">{{ typeText(item.type) }}</el-tag>
            <el-tag size="small" :type="item.isCorrect ? 'success' : 'danger'" effect="plain"
                    style="margin-left:8px">
              {{ item.isCorrect ? '正确' : '错误' }}
            </el-tag>
          </div>
          <span>得分 <b :style="{ color: item.isCorrect ? '#67c23a' : '#f56c6c' }">{{ item.score }}</b> / {{ item.fullScore }}</span>
        </div>
        <div class="question-stem" style="font-size:15px;margin:8px 0">{{ item.stem }}</div>
        <div class="text-muted">我的作答:{{ item.userAnswer || '未作答' }}</div>
        <div class="text-muted">参考答案:{{ item.correctAnswer }}</div>
        <div v-if="item.analysis" class="analysis-box" style="margin-top:8px">{{ item.analysis }}</div>
        <div v-if="item.aiFeedback" class="analysis-box" style="border-left-color:#e6a23c;margin-top:8px">
          <b>AI 评分说明:</b>{{ item.aiFeedback }}
        </div>
      </div>
      <el-empty v-if="!result.details || !result.details.length" description="暂无解析" />
    </div>

    <!-- 排行 -->
    <div class="page-card">
      <div class="card-title">成绩排行</div>
      <el-table :data="rankList" stripe>
        <el-table-column type="index" label="名次" width="80" />
        <el-table-column prop="nickname" label="学生" min-width="140" />
        <el-table-column prop="totalScore" label="总分" width="110" />
        <el-table-column prop="submitTime" label="交卷时间" width="180" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { MagicStick } from '@element-plus/icons-vue'
import { examApi } from '../../api'
import { renderMarkdown, typeText } from '../../utils/format'

const route = useRoute()
const examId = route.params.id
const result = ref({})
const rankList = ref([])
const loading = ref(false)

const adviceHtml = computed(() => renderMarkdown(result.value.aiComment))
const questionTotal = computed(() => {
  if (!result.value.details) return 0
  return result.value.details.reduce((sum, item) => sum + Number(item.fullScore || 0), 0)
})

async function load() {
  loading.value = true
  try {
    result.value = await examApi.result(examId)
    rankList.value = await examApi.rank(examId, 10)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.detail-item {
  padding: 14px 0;
  border-bottom: 1px solid #f2f4f7;
}

.detail-item:last-child {
  border-bottom: none;
}
</style>
