<template>
  <div>
    <div class="page-card">
      <div class="card-title">
        <span>题库管理</span>
        <div>
          <el-button size="small" type="primary" :icon="Plus" @click="openDialog()">新增题目</el-button>
          <el-button size="small" type="danger" plain :disabled="!selection.length" @click="removeBatch">
            批量删除({{ selection.length }})
          </el-button>
        </div>
      </div>

      <el-form :inline="true" :model="query" style="margin-bottom:12px">
        <el-form-item label="科目">
          <el-select v-model="query.subjectId" clearable placeholder="全部" style="width:150px"
                     @change="onSubjectChange">
            <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-select v-model="query.knowledgePointId" clearable placeholder="全部" style="width:150px">
            <el-option v-for="k in knowledgePoints" :key="k.id" :label="k.name" :value="k.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="query.type" clearable placeholder="全部" style="width:130px">
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTIPLE" />
            <el-option label="判断题" value="JUDGE" />
            <el-option label="填空题" value="FILL" />
            <el-option label="主观题" value="SUBJECTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="query.source" clearable placeholder="全部" style="width:120px">
            <el-option label="手工录入" value="MANUAL" />
            <el-option label="AI 生成" value="AI" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="题干包含" clearable style="width:160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" stripe @selection-change="(v) => (selection = v)">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="stem" label="题干" min-width="300" show-overflow-tooltip />
        <el-table-column prop="type" label="题型" width="100">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="knowledgePointName" label="知识点" width="140" />
        <el-table-column prop="difficulty" label="难度" width="80" />
        <el-table-column prop="score" label="分值" width="80" />
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.source === 'AI' ? 'success' : 'info'" effect="plain">
              {{ row.source === 'AI' ? 'AI 生成' : '手工' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="正确率" width="100">
          <template #default="{ row }">{{ row.correctRate }}%</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination style="margin-top:14px;justify-content:flex-end" background
                     layout="total, prev, pager, next" :total="total"
                     :page-size="query.size" :current-page="query.page"
                     @current-change="(p) => { query.page = p; load() }" />
    </div>

    <!-- 编辑/新增 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑题目' : '新增题目'" width="720px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="科目">
          <el-select v-model="form.subjectId" style="width:100%" @change="onFormSubjectChange">
            <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-select v-model="form.knowledgePointId" clearable style="width:100%">
            <el-option v-for="k in formKnowledgePoints" :key="k.id" :label="k.name" :value="k.id" />
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
        <el-form-item label="题干">
          <el-input v-model="form.stem" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="选项" v-if="['SINGLE', 'MULTIPLE'].includes(form.type)">
          <div style="width:100%">
            <div v-for="(opt, index) in form.options" :key="index" class="option-row">
              <el-input v-model="opt.key" style="width:70px" placeholder="A" />
              <el-input v-model="opt.content" placeholder="选项内容" />
              <el-button link type="danger" @click="form.options.splice(index, 1)">删除</el-button>
            </div>
            <el-button link type="primary" @click="addOption">+ 添加选项</el-button>
          </div>
        </el-form-item>
        <el-form-item label="参考答案">
          <el-input v-model="form.answer"
                    :placeholder="form.type === 'MULTIPLE' ? '如 A,C' : (form.type === 'JUDGE' ? '对 / 错' : '参考答案')" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="难度">
          <el-rate v-model="form.difficulty" :max="5" />
        </el-form-item>
        <el-form-item label="分值">
          <el-input-number v-model="form.score" :min="1" :max="50" :precision="1" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="逗号分隔,如:集合,并发" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { knowledgeApi, questionApi, subjectApi } from '../../api'
import { typeText } from '../../utils/format'

const subjects = ref([])
const knowledgePoints = ref([])
const formKnowledgePoints = ref([])
const list = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const selection = ref([])
const dialogVisible = ref(false)

const query = reactive({ page: 1, size: 10, subjectId: null, knowledgePointId: null, type: null, source: null, keyword: '' })
const form = reactive({ id: null, subjectId: null, knowledgePointId: null, type: 'SINGLE', stem: '', options: [], answer: '', analysis: '', difficulty: 3, score: 5, tags: '' })

async function load() {
  loading.value = true
  try {
    const data = await questionApi.page({ ...query })
    list.value = data.records || []
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function onSubjectChange(subjectId) {
  query.knowledgePointId = null
  knowledgePoints.value = subjectId ? await knowledgeApi.list(subjectId) : []
}

async function onFormSubjectChange(subjectId) {
  form.knowledgePointId = null
  formKnowledgePoints.value = subjectId ? await knowledgeApi.list(subjectId) : []
}

function addOption() {
  const keys = ['A', 'B', 'C', 'D', 'E', 'F']
  form.options.push({ key: keys[form.options.length] || '', content: '' })
}

async function openDialog(row) {
  if (row) {
    const detail = await questionApi.detail(row.id)
    Object.assign(form, {
      id: detail.id,
      subjectId: detail.subjectId,
      knowledgePointId: detail.knowledgePointId,
      type: detail.type,
      stem: detail.stem,
      options: (detail.options || []).map((o) => ({ key: o.key, content: o.content })),
      answer: detail.answer,
      analysis: detail.analysis,
      difficulty: detail.difficulty,
      score: Number(detail.score),
      tags: detail.tags
    })
    formKnowledgePoints.value = detail.subjectId ? await knowledgeApi.list(detail.subjectId) : []
  } else {
    Object.assign(form, {
      id: null, subjectId: query.subjectId || (subjects.value[0] && subjects.value[0].id),
      knowledgePointId: null, type: 'SINGLE', stem: '', options: [], answer: '', analysis: '',
      difficulty: 3, score: 5, tags: ''
    })
    addOption(); addOption(); addOption(); addOption()
    formKnowledgePoints.value = form.subjectId ? await knowledgeApi.list(form.subjectId) : []
  }
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const payload = {
      ...form,
      score: Number(form.score),
      options: ['SINGLE', 'MULTIPLE'].includes(form.type) ? form.options : null
    }
    if (form.id) {
      await questionApi.update(payload)
    } else {
      await questionApi.create(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除这道题吗?', '提示', { type: 'warning' })
  await questionApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

async function removeBatch() {
  await ElMessageBox.confirm(`确定删除选中的 ${selection.value.length} 道题吗?`, '提示', { type: 'warning' })
  await questionApi.removeBatch(selection.value.map((r) => r.id))
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  subjects.value = await subjectApi.list()
  load()
})
</script>

<style scoped>
.option-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
</style>
