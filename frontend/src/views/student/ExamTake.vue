<template>
  <div v-loading="loading">
    <!-- 顶部:倒计时与答题卡 -->
    <div class="page-card exam-header" v-if="paper.examId">
      <div>
        <div class="exam-name">{{ paper.name }}</div>
        <div class="text-muted">
          共 {{ paper.questions.length }} 题 · 总分 {{ paper.totalScore }} · 及格 {{ paper.passScore }}
        </div>
      </div>
      <div class="timer" :class="{ danger: remaining < 300 }">
        <el-icon><Timer /></el-icon>
        <span>{{ clock }}</span>
      </div>
      <div>
        <el-button type="success" :loading="submitting" @click="submit">交卷</el-button>
      </div>
    </div>

    <div class="page-card" v-if="paper.examId">
      <div class="card-title">答题卡</div>
      <div class="answer-sheet">
        <span v-for="(q, index) in paper.questions" :key="q.questionId"
              :class="['sheet-item', currentIndex === index ? 'current' : '', answers[q.questionId] ? 'done' : '']"
              @click="currentIndex = index">{{ index + 1 }}</span>
      </div>
    </div>

    <!-- 题目 -->
    <div class="page-card" v-if="current.questionId">
      <div class="flex-between" style="margin-bottom:10px">
        <div>
          <el-tag size="small">{{ typeText(current.type) }}</el-tag>
          <el-tag size="small" type="warning" style="margin-left:8px">{{ current.score }} 分</el-tag>
          <el-tag size="small" type="info" style="margin-left:8px" v-if="current.knowledgePointName">
            {{ current.knowledgePointName }}
          </el-tag>
        </div>
        <span class="text-muted">第 {{ currentIndex + 1 }} / {{ paper.questions.length }} 题</span>
      </div>

      <div class="question-stem">{{ current.stem }}</div>

      <div v-if="current.type === 'SINGLE' || current.type === 'MULTIPLE'">
        <div v-for="option in current.options" :key="option.key"
             :class="['option-item', isPicked(option.key) ? 'active' : '']" @click="pick(option.key)">
          <b>{{ option.key }}.</b> {{ option.content }}
        </div>
      </div>

      <div v-else-if="current.type === 'JUDGE'">
        <div v-for="option in ['对', '错']" :key="option"
             :class="['option-item', isPicked(option) ? 'active' : '']" @click="pick(option)">
          <b>{{ option }}</b>
        </div>
      </div>

      <div v-else>
        <el-input v-if="current.type === 'FILL'" v-model="answers[current.questionId]" size="large"
                  placeholder="请输入答案" @change="saveAnswer(current.questionId)" />
        <el-input v-else v-model="answers[current.questionId]" type="textarea" :rows="6"
                  placeholder="请分点作答,交卷后由 AI 依据要点覆盖情况评分"
                  @change="saveAnswer(current.questionId)" />
      </div>

      <div class="text-muted" style="margin-top:10px">
        作答会自动暂存;若超时未交卷,系统会自动交卷并判分。
      </div>

      <div style="margin-top:18px;display:flex;gap:12px">
        <el-button :disabled="currentIndex === 0" @click="currentIndex -= 1">上一题</el-button>
        <el-button :disabled="currentIndex >= paper.questions.length - 1" @click="currentIndex += 1">下一题</el-button>
        <el-button type="primary" @click="saveAnswer(current.questionId, true)">暂存本题</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Timer } from '@element-plus/icons-vue'
import { examApi } from '../../api'
import { secondsToClock, typeText } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const examId = route.params.id

const paper = ref({})
const answers = ref({})
const currentIndex = ref(0)
const remaining = ref(0)
const loading = ref(false)
const submitting = ref(false)
let timer = null

const current = computed(() => paper.value.questions ? paper.value.questions[currentIndex.value] || {} : {})
const clock = computed(() => secondsToClock(remaining.value))

function isPicked(key) {
  const value = answers.value[current.value.questionId]
  if (!value) return false
  if (current.value.type === 'MULTIPLE') {
    return value.split(',').includes(key)
  }
  return value === key
}

function pick(key) {
  const qid = current.value.questionId
  if (current.value.type === 'MULTIPLE') {
    const parts = answers.value[qid] ? answers.value[qid].split(',') : []
    const index = parts.indexOf(key)
    if (index >= 0) parts.splice(index, 1)
    else parts.push(key)
    answers.value[qid] = parts.sort().join(',')
  } else {
    answers.value[qid] = key
  }
  saveAnswer(qid)
}

async function saveAnswer(questionId, tip = false) {
  const value = answers.value[questionId]
  if (value === undefined || value === null) return
  try {
    await examApi.save(examId, questionId, value)
    if (tip) ElMessage.success('已暂存')
  } catch (e) {
    // 静默失败,交卷时仍会带上最终答案
  }
}

async function submit(auto = false) {
  if (!auto) {
    const unanswered = paper.value.questions.filter((q) => !answers.value[q.questionId]).length
    const tip = unanswered > 0
      ? `还有 ${unanswered} 题未作答,确定交卷吗?`
      : '确定交卷吗?交卷后主观题将由 AI 评分。'
    await ElMessageBox.confirm(tip, '交卷确认', { type: 'warning', confirmButtonText: '交卷' })
  }
  submitting.value = true
  try {
    const payload = {
      answers: Object.keys(answers.value).map((questionId) => ({
        questionId: Number(questionId),
        userAnswer: answers.value[questionId]
      }))
    }
    const result = await examApi.submit(examId, payload)
    ElMessage.success(`交卷成功,总分 ${result.totalScore}`)
    router.replace(`/exam/${examId}/result`)
  } finally {
    submitting.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const data = await examApi.start(examId)
    paper.value = data
    remaining.value = data.remainingSeconds
    const init = {}
    data.questions.forEach((q) => {
      if (q.userAnswer) init[q.questionId] = q.userAnswer
    })
    answers.value = init
    if (data.savedCount > 0) {
      ElMessage.info(`已恢复 ${data.savedCount} 道题的暂存作答`)
    }
    timer = setInterval(() => {
      remaining.value -= 1
      if (remaining.value <= 0) {
        clearInterval(timer)
        ElMessage.warning('考试时间到,正在自动交卷')
        submit(true)
      }
    }, 1000)
  } catch (e) {
    setTimeout(() => router.replace('/exams'), 1200)
  } finally {
    loading.value = false
  }
}

onMounted(load)
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.exam-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.exam-name {
  font-size: 18px;
  font-weight: 600;
}

.timer {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 26px;
  font-weight: 700;
  color: #409eff;
  font-variant-numeric: tabular-nums;
}

.timer.danger {
  color: #f56c6c;
}

.answer-sheet {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.sheet-item {
  width: 38px;
  height: 38px;
  line-height: 38px;
  text-align: center;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.sheet-item.done {
  background: #f0f9eb;
  border-color: #67c23a;
  color: #529b2e;
}

.sheet-item.current {
  border-color: #409eff;
  background: #ecf5ff;
  font-weight: 700;
}
</style>
