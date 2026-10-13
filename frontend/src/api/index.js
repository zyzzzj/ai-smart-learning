import request from './request'

/* ---------------- 认证 ---------------- */
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  me: () => request.get('/auth/me'),
  changePassword: (data) => request.put('/auth/password', data)
}

/* ---------------- 科目与知识点 ---------------- */
export const subjectApi = {
  list: () => request.get('/subjects'),
  listAll: () => request.get('/subjects/all'),
  detail: (id) => request.get(`/subjects/${id}`),
  save: (data) => request.post('/subjects', data),
  update: (data) => request.put('/subjects', data),
  remove: (id) => request.delete(`/subjects/${id}`)
}

export const knowledgeApi = {
  tree: (subjectId) => request.get('/knowledge-points/tree', { params: { subjectId } }),
  list: (subjectId) => request.get('/knowledge-points', { params: { subjectId } }),
  save: (data) => request.post('/knowledge-points', data),
  update: (data) => request.put('/knowledge-points', data),
  remove: (id) => request.delete(`/knowledge-points/${id}`)
}

/* ---------------- 题库 ---------------- */
export const questionApi = {
  page: (params) => request.get('/questions', { params }),
  detail: (id) => request.get(`/questions/${id}`),
  hot: (subjectId, limit = 10) => request.get('/questions/hot', { params: { subjectId, limit } }),
  create: (data) => request.post('/questions', data),
  update: (data) => request.put('/questions', data),
  remove: (id) => request.delete(`/questions/${id}`),
  removeBatch: (ids) => request.delete('/questions', { params: { ids: ids.join(',') } })
}

/* ---------------- AI 能力 ---------------- */
export const aiApi = {
  status: () => request.get('/ai/status'),
  generate: (data) => request.post('/ai/questions/generate', data),
  preview: (data) => request.post('/ai/questions/preview', data),
  grade: (data) => request.post('/ai/grade', data),
  advice: (subjectId) => request.post('/ai/advice', null, { params: { subjectId } }),
  latestAdvice: () => request.get('/ai/advice/latest'),
  tasks: (params) => request.get('/ai/tasks', { params }),
  task: (id) => request.get(`/ai/task/${id}`)
}

/* ---------------- 智能刷题 ---------------- */
export const practiceApi = {
  start: (data) => request.post('/practice/start', data),
  submit: (sessionId, data) => request.post(`/practice/${sessionId}/submit`, data),
  finish: (sessionId) => request.post(`/practice/${sessionId}/finish`),
  sessions: (params) => request.get('/practice/sessions', { params }),
  sessionDetail: (id) => request.get(`/practice/sessions/${id}`),
  wrongBook: (params) => request.get('/practice/wrong-book', { params }),
  removeWrong: (questionId) => request.delete(`/practice/wrong-book/${questionId}`),
  stats: () => request.get('/practice/stats')
}

/* ---------------- 智能考试 ---------------- */
export const examApi = {
  list: (params) => request.get('/exams', { params }),
  manageList: (params) => request.get('/exams/manage', { params }),
  start: (examId) => request.post(`/exams/${examId}/start`),
  save: (examId, questionId, userAnswer) =>
    request.post(`/exams/${examId}/save`, null, { params: { questionId, userAnswer } }),
  submit: (examId, data) => request.post(`/exams/${examId}/submit`, data || {}),
  result: (examId) => request.get(`/exams/${examId}/result`),
  rank: (examId, limit = 20) => request.get(`/exams/${examId}/rank`, { params: { limit } }),
  create: (data) => request.post('/exams', data),
  autoCompose: (data) => request.post('/exams/auto-compose', data),
  update: (data) => request.put('/exams', data),
  changeStatus: (examId, status) => request.put(`/exams/${examId}/status/${status}`),
  remove: (examId) => request.delete(`/exams/${examId}`)
}

/* ---------------- 资源管理 ---------------- */
export const fileApi = {
  storage: () => request.get('/files/storage'),
  list: (params) => request.get('/files', { params }),
  upload: (file, bizType, bizId) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/files/upload', form, {
      params: { bizType, bizId },
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  presign: (params) => request.post('/files/presign', null, { params }),
  url: (id) => request.get(`/files/${id}/url`),
  remove: (id) => request.delete(`/files/${id}`)
}
