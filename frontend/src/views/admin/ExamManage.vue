<template>
  <div>
    <div class="page-card">
      <div class="card-title">
        <span>考试与组卷</span>
        <div>
          <el-button size="small" type="primary" :icon="MagicStick" @click="composeVisible = true">
            AI 自动组卷
          </el-button>
          <el-button size="small" :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="考试名称" min-width="200" />
        <el-table-column prop="subjectName" label="科目" width="140" />
        <el-table-column prop="questionCount" label="题数" width="80" />
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column prop="passScore" label="及格分" width="90" />
        <el-table-column prop="duration" label="时长" width="90">
          <template #default="{ row }">{{ row.duration }} 分钟</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" v-if="row.status === 'DRAFT'"
                       @click="changeStatus(row, 'PUBLISHED')">发布</el-button>
            <el-button link type="warning" v-if="row.status === 'PUBLISHED'"
                       @click="changeStatus(row, 'CLOSED')">关闭</el-button>
            <el-button link type="primary" @click="showRank(row)">成绩排行</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- AI 自动组卷 -->
    <el-dialog v-model="composeVisible" title="AI 自动组卷" width="680px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:14px">
        按题型与难度配比自动抽题组卷;题库题量不足时会尽力抽取,建议先用 AI 出题扩充题库。
      </el-alert>
      <el-form :model="composeForm" label-width="110px">
        <el-form-item label="考试名称">
          <el-input v-model="composeForm.name" placeholder="如:Java 综合能力测评" />
        </el-form-item>
        <el-form-item label="科目">
          <el-select v-model="composeForm.subjectId" style="width:100%">
            <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="总题数">
          <el-input-number v-model="composeForm.questionCount" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="时长(分钟)">
          <el-input-number v-model="composeForm.duration" :min="10" :max="300" :step="5" />
        </el-form-item>
        <el-form-item label="题型配比">
          <div style="width:100%">
            <div v-for="item in typeRows" :key="item.type" class="ratio-row">
              <span class="ratio-label">{{ item.label }}</span>
              <el-input-number v-model="item.count" :min="0" :max="30" size="small" />
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="composeVisible = false">取消</el-button>
        <el-button type="primary" :loading="composing" @click="autoCompose">生成试卷</el-button>
      </template>
    </el-dialog>

    <!-- 排行 -->
    <el-dialog v-model="rankVisible" :title="`成绩排行 · ${rankExamName}`" width="620px">
      <el-table :data="rankList" stripe>
        <el-table-column type="index" label="名次" width="80" />
        <el-table-column prop="nickname" label="学生" min-width="140" />
        <el-table-column prop="totalScore" label="总分" width="110" />
        <el-table-column prop="submitTime" label="交卷时间" width="180" />
      </el-table>
      <el-empty v-if="!rankList.length" description="还没有学生完成该考试" />
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Refresh } from '@element-plus/icons-vue'
import { examApi, subjectApi } from '../../api'
import { statusText, statusType } from '../../utils/format'

const list = ref([])
const subjects = ref([])
const loading = ref(false)
const composing = ref(false)
const composeVisible = ref(false)
const rankVisible = ref(false)
const rankList = ref([])
const rankExamName = ref('')

const composeForm = reactive({
  name: '',
  subjectId: null,
  questionCount: 10,
  duration: 60,
  typeDistribution: {}
})

const typeRows = reactive([
  { type: 'SINGLE', label: '单选题', count: 6 },
  { type: 'MULTIPLE', label: '多选题', count: 2 },
  { type: 'JUDGE', label: '判断题', count: 1 },
  { type: 'FILL', label: '填空题', count: 0 },
  { type: 'SUBJECTIVE', label: '主观题', count: 1 }
])

async function load() {
  loading.value = true
  try {
    const data = await examApi.manageList({ page: 1, size: 50 })
    list.value = data.records || []
  } finally {
    loading.value = false
  }
}

async function autoCompose() {
  if (!composeForm.name || !composeForm.subjectId) {
    ElMessage.warning('请填写考试名称并选择科目')
    return
  }
  const distribution = {}
  typeRows.forEach((row) => {
    if (row.count > 0) distribution[row.type] = row.count
  })
  composing.value = true
  try {
    const exam = await examApi.autoCompose({
      name: composeForm.name,
      subjectId: composeForm.subjectId,
      questionCount: composeForm.questionCount,
      duration: composeForm.duration,
      typeDistribution: distribution
    })
    ElMessage.success(`组卷成功:${exam.questionCount} 题,总分 ${exam.totalScore}(草稿状态,确认后可发布)`)
    composeVisible.value = false
    load()
  } finally {
    composing.value = false
  }
}

async function changeStatus(row, status) {
  await examApi.changeStatus(row.id, status)
  ElMessage.success(status === 'PUBLISHED' ? '已发布,学生可见' : '已关闭')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除考试「${row.name}」吗?`, '提示', { type: 'warning' })
  await examApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

async function showRank(row) {
  rankExamName.value = row.name
  rankList.value = await examApi.rank(row.id, 50)
  rankVisible.value = true
}

onMounted(async () => {
  subjects.value = await subjectApi.list()
  load()
})
</script>

<style scoped>
.ratio-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.ratio-label {
  width: 90px;
  color: #606266;
  font-size: 14px;
}
</style>
