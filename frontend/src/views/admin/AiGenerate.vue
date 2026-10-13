<template>
  <div>
    <div class="page-card">
      <div class="card-title">
        <span>AI 智能出题</span>
        <el-tag :type="aiInfo.provider === 'ollama' ? 'success' : 'info'" effect="plain">
          当前:{{ aiInfo.provider }} / {{ aiInfo.model }}({{ aiInfo.mode }})
        </el-tag>
      </div>

      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>
          按知识点与难度要求调用大模型自动生成题目,生成结果会先做结构校验,再入库(来源标记为 AI);
          切换真实大模型只需设置 <code>AI_PROVIDER=ollama</code> 并启动 Ollama,业务代码无需改动。
        </template>
      </el-alert>

      <el-form :model="form" label-width="100px" style="max-width:760px">
        <el-form-item label="科目">
          <el-select v-model="form.subjectId" style="width:100%" @change="onSubjectChange">
            <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-select v-model="form.knowledgePointId" clearable placeholder="不选则按科目整体出题" style="width:100%">
            <el-option v-for="k in knowledgePoints" :key="k.id" :label="k.name" :value="k.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-radio-group v-model="form.type">
            <el-radio-button value="SINGLE">单选</el-radio-button>
            <el-radio-button value="MULTIPLE">多选</el-radio-button>
            <el-radio-button value="JUDGE">判断</el-radio-button>
            <el-radio-button value="FILL">填空</el-radio-button>
            <el-radio-button value="SUBJECTIVE">主观</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="难度">
          <el-slider v-model="form.difficulty" :min="1" :max="5" :marks="{ 1: '入门', 3: '中等', 5: '困难' }" />
        </el-form-item>
        <el-form-item label="生成数量">
          <el-input-number v-model="form.count" :min="1" :max="10" />
          <span class="text-muted" style="margin-left:12px">单次最多 10 道</span>
        </el-form-item>
        <el-form-item label="直接入库">
          <el-switch v-model="form.autoSave" />
          <span class="text-muted" style="margin-left:12px">
            开启后生成的题目会直接写入题库;关闭则仅预览
          </span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="generating" @click="generate">
            <el-icon><MagicStick /></el-icon>&nbsp;开始生成
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card" v-if="results.length">
      <div class="card-title">
        <span>生成结果({{ results.length }} 道,校验通过 {{ validCount }} 道)</span>
      </div>
      <div v-for="(item, index) in results" :key="index" class="result-item">
        <div class="flex-between">
          <div>
            <b>第 {{ index + 1 }} 题</b>
            <el-tag size="small" style="margin-left:8px">{{ typeText(item.type) }}</el-tag>
            <el-tag size="small" type="info" style="margin-left:8px">难度 {{ item.difficulty }}</el-tag>
            <el-tag size="small" :type="item.valid ? 'success' : 'danger'" effect="plain" style="margin-left:8px">
              {{ item.valid ? '校验通过' : '校验失败:' + item.invalidReason }}
            </el-tag>
            <el-tag size="small" type="success" v-if="item.questionId" style="margin-left:8px">
              已入库 #{{ item.questionId }}
            </el-tag>
          </div>
          <span>{{ item.score }} 分</span>
        </div>
        <div class="question-stem" style="font-size:15px;margin:8px 0">{{ item.stem }}</div>
        <div v-for="opt in item.options" :key="opt.key" class="text-muted">{{ opt.key }}. {{ opt.content }}</div>
        <div style="margin-top:8px">参考答案:<b style="color:#67c23a">{{ item.answer }}</b></div>
        <div v-if="item.analysis" class="analysis-box" style="margin-top:8px">{{ item.analysis }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { aiApi, knowledgeApi, subjectApi } from '../../api'
import { typeText } from '../../utils/format'

const subjects = ref([])
const knowledgePoints = ref([])
const results = ref([])
const aiInfo = ref({})
const generating = ref(false)

const form = reactive({
  subjectId: null,
  knowledgePointId: null,
  type: 'SINGLE',
  difficulty: 3,
  count: 3,
  autoSave: true
})

const validCount = computed(() => results.value.filter((r) => r.valid).length)

async function onSubjectChange(subjectId) {
  form.knowledgePointId = null
  knowledgePoints.value = subjectId ? await knowledgeApi.list(subjectId) : []
}

async function generate() {
  if (!form.subjectId) {
    ElMessage.warning('请选择科目')
    return
  }
  generating.value = true
  results.value = []
  try {
    const data = form.autoSave ? await aiApi.generate({ ...form }) : await aiApi.preview({ ...form })
    results.value = data || []
    ElMessage.success(`生成完成:${results.value.length} 道,校验通过 ${validCount.value} 道`)
  } finally {
    generating.value = false
  }
}

onMounted(async () => {
  subjects.value = await subjectApi.list()
  if (subjects.value.length) {
    form.subjectId = subjects.value[0].id
    knowledgePoints.value = await knowledgeApi.list(form.subjectId)
  }
  aiInfo.value = await aiApi.status()
})
</script>

<style scoped>
.result-item {
  padding: 14px 0;
  border-bottom: 1px solid #f2f4f7;
}

.result-item:last-child {
  border-bottom: none;
}

code {
  background: #f5f7fa;
  padding: 1px 6px;
  border-radius: 4px;
}
</style>
