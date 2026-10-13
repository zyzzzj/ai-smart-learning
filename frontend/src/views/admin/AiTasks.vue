<template>
  <div>
    <div class="page-card">
      <div class="card-title">
        <span>AI 任务记录</span>
        <div>
          <el-select v-model="query.type" size="small" clearable placeholder="全部类型" style="width:180px"
                     @change="load">
            <el-option label="智能出题" value="GENERATE_QUESTION" />
            <el-option label="主观题评分" value="GRADE_SUBJECTIVE" />
            <el-option label="学习建议" value="LEARNING_ADVICE" />
          </el-select>
          <el-button size="small" :icon="Refresh" style="margin-left:8px" @click="load">刷新</el-button>
        </div>
      </div>

      <el-alert type="info" :closable="false" show-icon style="margin-bottom:14px">
        <template #title>
          每次 AI 调用都会留痕:提示词、模型原始返回、解析结果、耗时与错误。点击「详情」可以查看完整调用过程。
        </template>
      </el-alert>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="type" label="任务类型" width="150">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="provider" label="提供方" width="110" />
        <el-table-column prop="model" label="模型" width="170" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'SUCCESS' ? 'success' : (row.status === 'FAILED' ? 'danger' : 'warning')">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时" width="100">
          <template #default="{ row }">{{ row.costMs }} ms</template>
        </el-table-column>
        <el-table-column prop="error" label="错误" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination style="margin-top:14px;justify-content:flex-end" background
                     layout="total, prev, pager, next" :total="total"
                     :page-size="query.size" :current-page="query.page"
                     @current-change="(p) => { query.page = p; load() }" />
    </div>

    <el-dialog v-model="detailVisible" title="AI 调用详情" width="820px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务 id">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ typeLabel(detail.type) }}</el-descriptions-item>
        <el-descriptions-item label="提供方">{{ detail.provider }}</el-descriptions-item>
        <el-descriptions-item label="模型">{{ detail.model }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costMs }} ms</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ detail.finishTime }}</el-descriptions-item>
      </el-descriptions>

      <el-tabs style="margin-top:16px">
        <el-tab-pane label="提示词">
          <pre class="code-block">{{ detail.prompt }}</pre>
        </el-tab-pane>
        <el-tab-pane label="模型原始返回">
          <pre class="code-block">{{ detail.response || '(空)' }}</pre>
        </el-tab-pane>
        <el-tab-pane label="解析结果">
          <pre class="code-block">{{ prettyResult }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { aiApi } from '../../api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref({})
const query = reactive({ page: 1, size: 10, type: null })

const TYPE_LABELS = {
  GENERATE_QUESTION: '智能出题',
  GRADE_SUBJECTIVE: '主观题评分',
  LEARNING_ADVICE: '学习建议'
}

function typeLabel(type) {
  return TYPE_LABELS[type] || type
}

const prettyResult = computed(() => {
  if (!detail.value.result) return '(空)'
  try {
    return JSON.stringify(JSON.parse(detail.value.result), null, 2)
  } catch (e) {
    return detail.value.result
  }
})

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (!params.type) delete params.type
    const data = await aiApi.tasks(params)
    list.value = data.records || []
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function showDetail(row) {
  detail.value = await aiApi.task(row.id)
  detailVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.code-block {
  background: #1f2d3d;
  color: #e6e6e6;
  padding: 14px;
  border-radius: 8px;
  max-height: 320px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
