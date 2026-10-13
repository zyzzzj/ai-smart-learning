<template>
  <div>
    <!-- 第一步:选择练习方式 -->
    <div class="page-card" v-if="!session">
      <div class="card-title">开始一次智能练习</div>
      <el-form :model="form" label-width="110px" style="max-width:720px">
        <el-form-item label="科目">
          <el-select v-model="form.subjectId" placeholder="不限科目" clearable style="width:100%"
                     @change="onSubjectChange">
            <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="练习模式">
          <el-radio-group v-model="form.mode">
            <el-radio-button value="RANDOM">随机练习</el-radio-button>
            <el-radio-button value="KNOWLEDGE">知识点专项</el-radio-button>
            <el-radio-button value="DIFFICULTY">难度专项</el-radio-button>
            <el-radio-button value="WRONG">错题重做</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="知识点" v-if="form.mode === 'KNOWLEDGE'">
          <el-select v-model="form.knowledgePointId" placeholder="请选择知识点" style="width:100%">
            <el-option v-for="k in knowledgePoints" :key="k.id" :label="k.name" :value="k.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="难度" v-if="form.mode === 'DIFFICULTY'">
          <el-slider v-model="form.difficulty" :min="1" :max="5" :marks="{ 1: '入门', 2: '简单', 3: '中等', 4: '较难', 5: '困难' }" />
        </el-form-item>

        <el-form-item label="题型">
          <el-select v-model="form.type" placeholder="混合题型" clearable style="width:100%">
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTIPLE" />
            <el-option label="判断题" value="JUDGE" />
            <el-option label="填空题" value="FILL" />
            <el-option label="主观题" value="SUBJECTIVE" />
          </el-select>
        </el-form-item>

        <el-form-item label="题目数量">
          <el-input-number v-model="form.count" :min="1" :max="30" />
          <span class="text-muted" style="margin-left:12px">主观题由 AI 自动评分</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="starting" @click="start">
            <el-icon><Promotion /></el-icon>&nbsp;开始练习
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 第二步:答题 -->
    <div v-if="session">
      <div class="page-card">
        <div class="flex-between">
          <div>
            <el-tag type="primary">{{ modeText(session.mode) }}</el-tag>
            <span class="progress-text">
              第 {{ currentIndex + 1 }} / {{ session.questions.length }} 题
            </span>
          </div>
          <div>
            <el-tag type="success" effect="plain">答对 {{ session.correctCount }}</el-tag>
            <el-tag type="warning" effect="plain" style="margin-left:8px">得分 {{ session.totalScore }}</el-tag>
            <el-button size="small" style="margin-left:12px" @click="finish">结束练习</el-button>
          </div>
        </div>
        <el-progress :percentage="progressPercent" :show-text="false" style="margin-top:12px" />
      </div>

      <div class="page-card">
        <div class="flex-between" style="margin-bottom:10px">
          <div>
            <el-tag size="small">{{ typeText(current.type) }}</el-tag>
            <el-tag size="small" type="info" style="margin-left:8px">难度 {{ current.difficulty }}</el-tag>
            <el-tag size="small" type="warning" style="margin-left:8px">{{ current.score }} 分</el-tag>
          </div>
          <span class="text-muted" v-if="current.source === 'AI'">AI 生成</span>
        </div>

        <div class="question-stem">{{ current.stem }}</div>

        <!-- 选择题 -->
        <div v-if="current.type === 'SINGLE' || current.type === 'MULTIPLE'">
          <div v-for="option in current.options" :key="option.key"
               :class="['option-item', optionClass(option)]" @click="pickOption(option.key)">
            <b>{{ option.key }}.</b> {{ option.content }}
          </div>
        </div>

        <!-- 判断题 -->
        <div v-else-if="current.type === 'JUDGE'">
          <div v-for="option in ['对', '错']" :key="option"
               :class="['option-item', optionClass(option)]" @click="pickOption(option)">
            <b>{{ option }}</b>
          </div>
        </div>

        <!-- 填空题 / 主观题 -->
        <div v-else>
          <el-input v-if="current.type === 'FILL'" v-model="answer" size="large"
                    placeholder="请输入答案" :disabled="answered" />
          <el-input v-else v-model="answer" type="textarea" :rows="5" :disabled="answered"
                    placeholder="请分点作答,AI 会依据参考答案的要点覆盖情况评分" />
        </div>

        <!-- 判分结果 -->
        <div v-if="answered" class="analysis-box">
          <div class="flex-between">
            <b :style="{ color: result.correct ? '#67c23a' : '#f56c6c' }">
              {{ result.correct ? '✅ 回答正确' : '❌ 回答错误' }}
            </b>
            <span>本题得分 <b class="price">{{ result.score }}</b> / {{ result.fullScore }}</span>
          </div>
          <div style="margin-top:8px">参考答案:<b>{{ result.correctAnswer }}</b></div>
          <div v-if="result.analysis" style="margin-top:8px">解析:{{ result.analysis }}</div>
          <div v-if="result.aiFeedback" style="margin-top:8px;color:#409eff">
            <b>AI 评语:</b>{{ result.aiFeedback }}
          </div>
          <div v-if="result.missingPoints && result.missingPoints.length" style="margin-top:8px">
            <b>遗漏要点:</b>
            <el-tag v-for="p in result.missingPoints" :key="p" size="small" type="warning" effect="plain"
                    style="margin:4px 6px 0 0">{{ p }}</el-tag>
          </div>
          <div v-if="result.addedToWrongBook" class="text-muted" style="margin-top:8px">
            已自动加入错题本
          </div>
        </div>

        <div style="margin-top:18px;display:flex;gap:12px">
          <el-button v-if="!answered" type="primary" size="large" :loading="submitting"
                     :disabled="!answer" @click="submitAnswer">提交答案</el-button>
          <el-button v-else type="primary" size="large" @click="nextQuestion">
            {{ currentIndex >= session.questions.length - 1 ? '完成练习' : '下一题' }}
          </el-button>
          <el-button v-if="!answered" @click="skip">跳过</el-button>
          <el-button @click="finish">结束练习</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'
import { knowledgeApi, practiceApi, subjectApi } from '../../api'
import { modeText, typeText } from '../../utils/format'

const subjects = ref([])
const knowledgePoints = ref([])
const starting = ref(false)
const submitting = ref(false)
const answered = ref(false)

const form = reactive({
  subjectId: null,
  mode: 'RANDOM',
  knowledgePointId: null,
  difficulty: 3,
  type: null,
  count: 5
})

const session = ref(null)
const currentIndex = ref(0)
const answer = ref('')
const result = ref({})

const current = computed(() => (session.value ? session.value.questions[currentIndex.value] : {}))
const progressPercent = computed(() => {
  if (!session.value) return 0
  return Math.round((currentIndex.value / session.value.questions.length) * 100)
})

async function loadSubjects() {
  subjects.value = await subjectApi.list()
}

async function onSubjectChange(subjectId) {
  form.knowledgePointId = null
  knowledgePoints.value = subjectId ? await knowledgeApi.list(subjectId) : []
}

async function start() {
  starting.value = true
  try {
    const data = await practiceApi.start({ ...form })
    session.value = { ...data, correctCount: 0, totalScore: 0 }
    currentIndex.value = 0
    resetAnswer()
    ElMessage.success(`已生成 ${data.total} 道题,开始吧!`)
  } finally {
    starting.value = false
  }
}

function resetAnswer() {
  answer.value = ''
  answered.value = false
  result.value = {}
}

function pickOption(key) {
  if (answered.value) return
  if (current.value.type === 'MULTIPLE') {
    const parts = answer.value ? answer.value.split(',') : []
    const index = parts.indexOf(key)
    if (index >= 0) {
      parts.splice(index, 1)
    } else {
      parts.push(key)
    }
    answer.value = parts.sort().join(',')
  } else {
    answer.value = key
  }
}

function optionClass(option) {
  const classes = []
  if (current.value.type === 'MULTIPLE') {
    if (answer.value && answer.value.split(',').includes(option)) classes.push('active')
  } else if (answer.value === option) {
    classes.push('active')
  }
  if (answered.value && result.value.correctAnswer) {
    const correctKeys = String(result.value.correctAnswer).split(/[,，]/).map((s) => s.trim())
    if (correctKeys.includes(option)) classes.push('correct')
    else if (answer.value === option) classes.push('wrong')
  }
  return classes
}

async function submitAnswer() {
  if (!answer.value) {
    ElMessage.warning('请先作答')
    return
  }
  submitting.value = true
  try {
    const data = await practiceApi.submit(session.value.sessionId, {
      questionId: current.value.id,
      userAnswer: answer.value,
      timeSpent: 30
    })
    result.value = data
    answered.value = true
    session.value.correctCount = data.correctCount
    session.value.totalScore = data.totalScore
  } finally {
    submitting.value = false
  }
}

function nextQuestion() {
  if (currentIndex.value >= session.value.questions.length - 1) {
    finish()
    return
  }
  currentIndex.value += 1
  resetAnswer()
}

function skip() {
  nextQuestion()
}

async function finish() {
  if (!session.value) return
  try {
    await practiceApi.finish(session.value.sessionId)
  } catch (e) {
    // 忽略
  }
  const total = session.value.questions.length
  const correct = session.value.correctCount
  const score = session.value.totalScore
  await ElMessageBox.alert(
    `本次练习:作答 ${currentIndex.value + (answered.value ? 1 : 0)} / ${total} 题,答对 ${correct} 题,得分 ${score}`,
    '练习结束',
    { confirmButtonText: '查看学习报告' }
  ).then(() => {
    session.value = null
    resetAnswer()
    window.location.hash = ''
  })
  location.reload()
}

onMounted(loadSubjects)
</script>

<style scoped>
.progress-text {
  margin-left: 12px;
  font-size: 14px;
  color: #606266;
}

.price {
  color: #f56c6c;
}
</style>
