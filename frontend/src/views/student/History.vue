<template>
  <div>
    <div class="page-card">
      <div class="card-title">
        <span>我的刷题记录</span>
        <el-button size="small" :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="mode" label="模式" width="130">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ modeText(row.mode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="questionCount" label="题数" width="80" />
        <el-table-column prop="answeredCount" label="已答" width="80" />
        <el-table-column prop="correctCount" label="答对" width="80" />
        <el-table-column prop="accuracy" label="正确率" width="100">
          <template #default="{ row }">{{ row.accuracy }}%</template>
        </el-table-column>
        <el-table-column prop="totalScore" label="得分" width="90" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="finishTime" label="结束时间" width="180" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination style="margin-top:14px;justify-content:flex-end" background
                     layout="total, prev, pager, next" :total="total"
                     :page-size="query.size" :current-page="query.page"
                     @current-change="onPageChange" />

      <el-empty v-if="!loading && !list.length" description="还没有刷题记录,去练习一下吧" />
    </div>

    <el-dialog v-model="detailVisible" title="刷题详情" width="760px">
      <div v-for="(item, index) in detail.records || []" :key="item.questionId" class="detail-item">
        <div class="flex-between">
          <div>
            <b>第 {{ index + 1 }} 题</b>
            <el-tag size="small" style="margin-left:8px">{{ typeText(item.type) }}</el-tag>
            <el-tag size="small" :type="item.isCorrect ? 'success' : 'danger'" effect="plain"
                    style="margin-left:8px">{{ item.isCorrect ? '正确' : '错误' }}</el-tag>
          </div>
          <span>得分 {{ item.score }}</span>
        </div>
        <div style="margin:8px 0">{{ item.stem }}</div>
        <div class="text-muted">我的作答:{{ item.userAnswer || '未作答' }}</div>
        <div class="text-muted">参考答案:{{ item.correctAnswer }}</div>
        <div v-if="item.aiFeedback" class="analysis-box" style="margin-top:8px">
          <b>AI 评语:</b>{{ item.aiFeedback }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { practiceApi } from '../../api'
import { modeText, statusText, statusType, typeText } from '../../utils/format'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })
const detailVisible = ref(false)
const detail = ref({})

async function load() {
  loading.value = true
  try {
    const data = await practiceApi.sessions({ ...query })
    list.value = data.records || []
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onPageChange(page) {
  query.page = page
  load()
}

async function showDetail(row) {
  detail.value = await practiceApi.sessionDetail(row.id)
  detailVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.detail-item {
  padding: 12px 0;
  border-bottom: 1px solid #f2f4f7;
}
</style>
