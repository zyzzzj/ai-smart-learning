<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="10">
        <div class="page-card">
          <div class="card-title">
            <span>科目 / 课程</span>
            <el-button size="small" type="primary" @click="openSubjectDialog()">新增科目</el-button>
          </div>
          <el-table :data="subjects" v-loading="loading" stripe>
            <el-table-column prop="name" label="名称" min-width="140" />
            <el-table-column prop="sort" label="排序" width="80" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="plain">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openSubjectDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="removeSubject(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <el-col :span="14">
        <div class="page-card">
          <div class="card-title">
            <span>知识点树</span>
            <div>
              <el-select v-model="currentSubjectId" size="small" style="width:160px;margin-right:8px"
                         @change="loadTree">
                <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
              <el-button size="small" type="primary" :disabled="!currentSubjectId"
                         @click="openKnowledgeDialog()">新增知识点</el-button>
            </div>
          </div>

          <el-tree :data="tree" node-key="id" default-expand-all :props="{ label: 'name', children: 'children' }">
            <template #default="{ data }">
              <span class="tree-node">
                <span>{{ data.name }}</span>
                <el-tag size="small" type="info" effect="plain" style="margin-left:8px">
                  难度 {{ data.difficulty }} · 题量 {{ data.questionCount }}
                </el-tag>
                <span class="tree-actions">
                  <el-button link type="primary" size="small" @click.stop="openKnowledgeDialog(data)">编辑</el-button>
                  <el-button link type="primary" size="small" @click.stop="openKnowledgeDialog(null, data)">加子节点</el-button>
                  <el-button link type="danger" size="small" @click.stop="removeKnowledge(data)">删除</el-button>
                </span>
              </span>
            </template>
          </el-tree>
          <el-empty v-if="!tree.length" description="该科目下还没有知识点" />
        </div>
      </el-col>
    </el-row>

    <!-- 科目弹窗 -->
    <el-dialog v-model="subjectDialog" :title="subjectForm.id ? '编辑科目' : '新增科目'" width="480px">
      <el-form :model="subjectForm" label-width="90px">
        <el-form-item label="名称"><el-input v-model="subjectForm.name" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="subjectForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="subjectForm.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="subjectForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subjectDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSubject">保存</el-button>
      </template>
    </el-dialog>

    <!-- 知识点弹窗 -->
    <el-dialog v-model="knowledgeDialog" :title="knowledgeForm.id ? '编辑知识点' : '新增知识点'" width="520px">
      <el-form :model="knowledgeForm" label-width="100px">
        <el-form-item label="名称"><el-input v-model="knowledgeForm.name" /></el-form-item>
        <el-form-item label="父知识点">
          <el-select v-model="knowledgeForm.parentId" style="width:100%">
            <el-option label="(根节点)" :value="0" />
            <el-option v-for="k in flatKnowledge" :key="k.id" :label="k.name" :value="k.id"
                       :disabled="k.id === knowledgeForm.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-rate v-model="knowledgeForm.difficulty" :max="5" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="knowledgeForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="knowledgeForm.sort" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="knowledgeDialog = false">取消</el-button>
        <el-button type="primary" @click="saveKnowledge">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeApi, subjectApi } from '../../api'

const subjects = ref([])
const flatKnowledge = ref([])
const tree = ref([])
const currentSubjectId = ref(null)
const loading = ref(false)
const subjectDialog = ref(false)
const knowledgeDialog = ref(false)

const subjectForm = reactive({ id: null, name: '', description: '', sort: 0, status: 1 })
const knowledgeForm = reactive({ id: null, subjectId: null, parentId: 0, name: '', description: '', difficulty: 3, sort: 0 })

async function loadSubjects() {
  loading.value = true
  try {
    subjects.value = await subjectApi.listAll()
    if (subjects.value.length && !currentSubjectId.value) {
      currentSubjectId.value = subjects.value[0].id
      await loadTree()
    }
  } finally {
    loading.value = false
  }
}

async function loadTree() {
  if (!currentSubjectId.value) return
  tree.value = await knowledgeApi.tree(currentSubjectId.value)
  flatKnowledge.value = await knowledgeApi.list(currentSubjectId.value)
}

function openSubjectDialog(row) {
  Object.assign(subjectForm, row
    ? { id: row.id, name: row.name, description: row.description, sort: row.sort, status: row.status }
    : { id: null, name: '', description: '', sort: 0, status: 1 })
  subjectDialog.value = true
}

async function saveSubject() {
  if (!subjectForm.name) {
    ElMessage.warning('请填写科目名称')
    return
  }
  if (subjectForm.id) await subjectApi.update({ ...subjectForm })
  else await subjectApi.save({ ...subjectForm })
  ElMessage.success('保存成功')
  subjectDialog.value = false
  await loadSubjects()
}

async function removeSubject(row) {
  await ElMessageBox.confirm(`确定删除科目「${row.name}」吗?`, '提示', { type: 'warning' })
  await subjectApi.remove(row.id)
  ElMessage.success('已删除')
  await loadSubjects()
}

function openKnowledgeDialog(row, parent) {
  Object.assign(knowledgeForm, row
    ? { id: row.id, subjectId: row.subjectId, parentId: row.parentId, name: row.name,
        description: row.description, difficulty: row.difficulty, sort: row.sort }
    : { id: null, subjectId: currentSubjectId.value, parentId: parent ? parent.id : 0, name: '',
        description: '', difficulty: 3, sort: 0 })
  knowledgeDialog.value = true
}

async function saveKnowledge() {
  if (!knowledgeForm.name) {
    ElMessage.warning('请填写知识点名称')
    return
  }
  if (knowledgeForm.id) await knowledgeApi.update({ ...knowledgeForm })
  else await knowledgeApi.save({ ...knowledgeForm })
  ElMessage.success('保存成功')
  knowledgeDialog.value = false
  await loadTree()
}

async function removeKnowledge(row) {
  await ElMessageBox.confirm(`确定删除知识点「${row.name}」吗?`, '提示', { type: 'warning' })
  await knowledgeApi.remove(row.id)
  ElMessage.success('已删除')
  await loadTree()
}

onMounted(loadSubjects)
</script>

<style scoped>
.tree-node {
  display: flex;
  align-items: center;
  width: 100%;
}

.tree-actions {
  margin-left: auto;
  opacity: 0;
  transition: opacity .2s;
}

.tree-node:hover .tree-actions {
  opacity: 1;
}
</style>
