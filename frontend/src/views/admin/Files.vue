<template>
  <div>
    <div class="page-card">
      <div class="card-title">
        <span>学习资源管理</span>
        <el-tag effect="plain" type="success">对象存储:{{ storageType || '-' }}</el-tag>
      </div>

      <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>
          视频、图片等资源统一由对象存储托管:小文件可走服务端中转上传,大文件建议用<b>预签名直传</b>
          (前端直接 PUT 到对象存储,不占后端带宽)。
        </template>
      </el-alert>

      <el-form :inline="true" style="margin-bottom:12px">
        <el-form-item label="业务类型">
          <el-select v-model="query.bizType" clearable placeholder="全部" style="width:150px" @change="load">
            <el-option label="题目" value="QUESTION" />
            <el-option label="科目" value="SUBJECT" />
            <el-option label="知识点" value="KNOWLEDGE" />
            <el-option label="头像" value="AVATAR" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.category" clearable placeholder="全部" style="width:130px" @change="load">
            <el-option label="视频" value="VIDEO" />
            <el-option label="图片" value="IMAGE" />
            <el-option label="文档" value="DOC" />
            <el-option label="其它" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务 id">
          <el-input v-model="query.bizId" placeholder="如题目 id" clearable style="width:130px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <el-upload :show-file-list="false" :http-request="doUpload" style="margin-bottom:16px">
        <el-button type="success" :icon="Upload">上传资源(服务端中转)</el-button>
        <template #tip>
          <span class="text-muted" style="margin-left:10px">
            默认关联到「题目」业务,上传后自动识别 VIDEO/IMAGE/DOC 分类
          </span>
        </template>
      </el-upload>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="预览" width="90">
          <template #default="{ row }">
            <el-image v-if="row.category === 'IMAGE'" :src="row.url" fit="cover"
                      style="width:52px;height:52px;border-radius:6px" :preview-src-list="[row.url]" />
            <el-icon v-else-if="row.category === 'VIDEO'" :size="26"><VideoCamera /></el-icon>
            <el-icon v-else :size="26"><Document /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="objectKey" label="对象键" min-width="260" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="90" />
        <el-table-column prop="sizeText" label="大小" width="100" />
        <el-table-column prop="bizType" label="业务" width="110" />
        <el-table-column prop="createTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="copyUrl(row)">复制地址</el-button>
            <el-button link type="success" @click="refreshUrl(row)">刷新地址</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !list.length" description="还没有上传任何资源" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Upload, VideoCamera } from '@element-plus/icons-vue'
import { fileApi } from '../../api'

const list = ref([])
const loading = ref(false)
const storageType = ref('')
const query = reactive({ bizType: null, category: null, bizId: null })

async function load() {
  loading.value = true
  try {
    const params = {}
    if (query.bizType) params.bizType = query.bizType
    if (query.category) params.category = query.category
    if (query.bizId) params.bizId = Number(query.bizId)
    list.value = await fileApi.list(params)
  } finally {
    loading.value = false
  }
}

async function doUpload(option) {
  try {
    const data = await fileApi.upload(option.file, 'QUESTION', query.bizId ? Number(query.bizId) : null)
    ElMessage.success(`上传成功:${data.category} · ${data.sizeText}`)
    load()
  } catch (e) {
    // 错误已在拦截器提示
  }
}

async function copyUrl(row) {
  const data = await fileApi.url(row.id)
  await navigator.clipboard.writeText(location.origin + data.url)
  ElMessage.success('地址已复制(预签名地址有有效期)')
}

async function refreshUrl(row) {
  const data = await fileApi.url(row.id)
  row.url = data.url
  ElMessage.success('访问地址已刷新')
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除「${row.originalName}」吗?对象存储中的文件会一并删除。`, '提示', { type: 'warning' })
  await fileApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  storageType.value = await fileApi.storage()
  load()
})
</script>
