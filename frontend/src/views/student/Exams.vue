<template>
  <div>
    <div class="page-card">
      <div class="card-title">
        <span>考试中心</span>
        <el-button size="small" :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="name" label="考试名称" min-width="220" />
        <el-table-column prop="subjectName" label="科目" width="150" />
        <el-table-column prop="questionCount" label="题数" width="80" />
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column prop="passScore" label="及格分" width="90" />
        <el-table-column prop="duration" label="时长" width="90">
          <template #default="{ row }">{{ row.duration }} 分钟</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.myRecordStatus)" effect="plain">
              {{ row.myRecordStatus || '未参加' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="myScore" label="我的得分" width="110" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.myRecordStatus || row.myRecordStatus === '未参加'" type="primary" size="small"
                       @click="start(row)">开始考试</el-button>
            <el-button v-else-if="row.myRecordStatus === 'ONGOING'" type="warning" size="small"
                       @click="start(row)">继续考试</el-button>
            <el-button v-else type="success" size="small" @click="viewResult(row)">查看成绩</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !list.length" description="暂无已发布的考试" />
    </div>

    <!-- 成绩排行 -->
    <div class="page-card" v-if="rankExam">
      <div class="card-title">成绩排行 · {{ rankExam }}</div>
      <el-table :data="rankList" stripe>
        <el-table-column type="index" label="名次" width="80">
          <template #default="{ $index }">
            <el-tag :type="$index < 3 ? 'warning' : 'info'" effect="plain" size="small">{{ $index + 1 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="学生" min-width="150" />
        <el-table-column prop="totalScore" label="总分" width="110" />
        <el-table-column prop="submitTime" label="交卷时间" width="180" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { examApi } from '../../api'
import { statusType } from '../../utils/format'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const rankExam = ref('')
const rankList = ref([])

async function load() {
  loading.value = true
  try {
    const data = await examApi.list({ page: 1, size: 50 })
    list.value = data.records || []
    // 顺带展示第一场已参加考试的排行
    const done = list.value.find((e) => e.myRecordStatus && e.myRecordStatus !== '未参加')
    if (done) {
      rankExam.value = done.name
      rankList.value = await examApi.rank(done.id, 10)
    }
  } finally {
    loading.value = false
  }
}

async function start(row) {
  const minutes = row.duration
  if (!row.myRecordStatus || row.myRecordStatus === '未参加') {
    await ElMessageBox.confirm(
      `本场考试共 ${row.questionCount} 题,限时 ${minutes} 分钟,交卷后由 AI 对主观题评分。确定开始吗?`,
      '开始考试',
      { type: 'warning', confirmButtonText: '开始答题' }
    )
  }
  router.push(`/exam/${row.id}`)
}

function viewResult(row) {
  router.push(`/exam/${row.id}/result`)
}

onMounted(load)
</script>
