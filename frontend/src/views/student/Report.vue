<template>
  <div v-loading="loading">
    <div class="page-card">
      <div class="card-title">
        <span>学习报告</span>
        <el-button size="small" :icon="Refresh" @click="load">刷新数据</el-button>
      </div>
      <div class="stat-grid">
        <div class="stat-box">
          <div class="value">{{ stats.totalAnswered || 0 }}</div>
          <div class="label">累计作答</div>
        </div>
        <div class="stat-box">
          <div class="value">{{ stats.totalCorrect || 0 }}</div>
          <div class="label">累计答对</div>
        </div>
        <div class="stat-box">
          <div class="value">{{ stats.accuracy || 0 }}%</div>
          <div class="label">总正确率</div>
        </div>
        <div class="stat-box">
          <div class="value">{{ stats.masteredCount || 0 }}</div>
          <div class="label">已掌握错题</div>
        </div>
        <div class="stat-box">
          <div class="value">{{ stats.examAvgScore || 0 }}</div>
          <div class="label">考试平均分</div>
        </div>
        <div class="stat-box">
          <div class="value">{{ timeText }}</div>
          <div class="label">累计答题时长</div>
        </div>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="12">
        <div class="page-card">
          <div class="card-title">最近 7 天作答趋势</div>
          <div ref="trendRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <div class="card-title">各知识点正确率</div>
          <div ref="knowledgeRef" class="chart"></div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card">
      <div class="card-title">知识点掌握明细</div>
      <el-table :data="stats.knowledgeStats || []" stripe>
        <el-table-column prop="knowledgePointName" label="知识点" min-width="180" />
        <el-table-column prop="total" label="作答" width="100" />
        <el-table-column prop="correct" label="答对" width="100" />
        <el-table-column prop="rate" label="正确率" width="180">
          <template #default="{ row }">
            <el-progress :percentage="row.rate" :color="rateColor(row.rate)" />
          </template>
        </el-table-column>
        <el-table-column label="掌握情况" width="130">
          <template #default="{ row }">
            <el-tag :type="row.rate >= 80 ? 'success' : (row.rate >= 60 ? 'warning' : 'danger')" effect="plain">
              {{ row.rate >= 80 ? '熟练' : (row.rate >= 60 ? '一般' : '薄弱') }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { practiceApi } from '../../api'

const stats = ref({})
const loading = ref(false)
const trendRef = ref(null)
const knowledgeRef = ref(null)
let trendChart = null
let knowledgeChart = null

const timeText = computed(() => {
  const seconds = Number(stats.value.totalTimeSpent || 0)
  if (seconds < 60) return `${seconds} 秒`
  if (seconds < 3600) return `${Math.round(seconds / 60)} 分钟`
  return `${(seconds / 3600).toFixed(1)} 小时`
})

function rateColor(rate) {
  if (rate >= 80) return '#67c23a'
  if (rate >= 60) return '#e6a23c'
  return '#f56c6c'
}

function renderCharts() {
  const trend = stats.value.recentTrend || []
  if (trendRef.value) {
    trendChart = trendChart || echarts.init(trendRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { top: 0 },
      grid: { left: 40, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: trend.map((i) => i.date.slice(5)) },
      yAxis: { type: 'value' },
      series: [
        { name: '作答', type: 'bar', itemStyle: { color: '#409eff' }, data: trend.map((i) => i.answered) },
        { name: '答对', type: 'bar', itemStyle: { color: '#67c23a' }, data: trend.map((i) => i.correct) },
        {
          name: '正确率(%)', type: 'line', smooth: true, yAxisIndex: 0,
          itemStyle: { color: '#e6a23c' }, data: trend.map((i) => i.accuracy)
        }
      ]
    })
  }

  const knowledge = (stats.value.knowledgeStats || []).slice(0, 8)
  if (knowledgeRef.value) {
    knowledgeChart = knowledgeChart || echarts.init(knowledgeRef.value)
    knowledgeChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 110, right: 40, top: 20, bottom: 30 },
      xAxis: { type: 'value', max: 100 },
      yAxis: { type: 'category', data: knowledge.map((i) => i.knowledgePointName).reverse() },
      series: [{
        name: '正确率(%)',
        type: 'bar',
        data: knowledge.map((i) => ({
          value: i.rate,
          itemStyle: { color: rateColor(i.rate) }
        })).reverse(),
        label: { show: true, position: 'right', formatter: '{c}%' }
      }]
    })
  }
}

async function load() {
  loading.value = true
  try {
    stats.value = (await practiceApi.stats()) || {}
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
  window.addEventListener('resize', () => {
    trendChart && trendChart.resize()
    knowledgeChart && knowledgeChart.resize()
  })
})
</script>

<style scoped>
.chart {
  width: 100%;
  height: 300px;
}
</style>
