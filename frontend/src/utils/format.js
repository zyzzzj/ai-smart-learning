/**
 * 通用格式化与轻量 Markdown 渲染。
 *
 * AI 学习建议是结构化程度很高的 Markdown(标题 / 列表 / 加粗),
 * 这里用一个不依赖第三方库的极简渲染器:先转义 HTML,再逐行处理,
 * 既避免 XSS 风险,也省掉一个运行时依赖。
 */

function escapeHtml(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function inline(text) {
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
}

export function renderMarkdown(text) {
  if (!text) return ''
  const lines = escapeHtml(text).split(/\r?\n/)
  const html = []
  let listType = null

  const closeList = () => {
    if (listType) {
      html.push(listType === 'ol' ? '</ol>' : '</ul>')
      listType = null
    }
  }

  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (!line) {
      closeList()
      continue
    }
    const heading = line.match(/^(#{1,4})\s+(.*)$/)
    if (heading) {
      closeList()
      const level = Math.min(heading[1].length + 1, 5)
      html.push(`<h${level}>${inline(heading[2])}</h${level}>`)
      continue
    }
    const ordered = line.match(/^\d+[.)]\s+(.*)$/)
    if (ordered) {
      if (listType !== 'ol') {
        closeList()
        html.push('<ol>')
        listType = 'ol'
      }
      html.push(`<li>${inline(ordered[1])}</li>`)
      continue
    }
    const bullet = line.match(/^[-*+]\s+(.*)$/)
    if (bullet) {
      if (listType !== 'ul') {
        closeList()
        html.push('<ul>')
        listType = 'ul'
      }
      html.push(`<li>${inline(bullet[1])}</li>`)
      continue
    }
    closeList()
    html.push(`<p>${inline(line)}</p>`)
  }
  closeList()
  return html.join('')
}

const TYPE_TEXT = {
  SINGLE: '单选题',
  MULTIPLE: '多选题',
  JUDGE: '判断题',
  FILL: '填空题',
  SUBJECTIVE: '主观题'
}

export function typeText(type) {
  return TYPE_TEXT[type] || type
}

const MODE_TEXT = {
  RANDOM: '随机练习',
  KNOWLEDGE: '知识点专项',
  DIFFICULTY: '难度专项',
  WRONG: '错题重做'
}

export function modeText(mode) {
  return MODE_TEXT[mode] || mode
}

const STATUS_TEXT = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  CLOSED: '已关闭',
  ONGOING: '进行中',
  SUBMITTED: '已交卷',
  GRADED: '已判分',
  FINISHED: '已完成'
}

export function statusText(status) {
  return STATUS_TEXT[status] || status
}

export function statusType(status) {
  return {
    DRAFT: 'info',
    PUBLISHED: 'success',
    CLOSED: 'info',
    ONGOING: 'warning',
    SUBMITTED: 'primary',
    GRADED: 'success',
    FINISHED: 'success'
  }[status] || 'info'
}

export function secondsToClock(seconds) {
  const s = Math.max(0, Number(seconds) || 0)
  const h = String(Math.floor(s / 3600)).padStart(2, '0')
  const m = String(Math.floor((s % 3600) / 60)).padStart(2, '0')
  const sec = String(s % 60).padStart(2, '0')
  return `${h}:${m}:${sec}`
}

export function difficultyText(level) {
  return ['', '入门', '简单', '中等', '较难', '困难'][level] || '中等'
}
