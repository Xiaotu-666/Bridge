<template>
  <section class="review-page">
    <header class="review-heading">
      <div>
        <span class="eyebrow">{{ isInitial ? '表 B' : '表 C' }}</span>
        <h2>{{ isInitial ? '初始检查审核工作台' : '定期检查审核工作台' }}</h2>
      </div>
      <el-tag :type="isInitial ? 'primary' : 'warning'" effect="dark">
        {{ isInitial ? '初始检查' : '定期检查' }}
      </el-tag>
    </header>

    <section class="table-panel review-list">
      <div class="list-toolbar">
        <el-segmented v-model="state" :options="stateOptions" @change="load" />
        <el-button :icon="RefreshCw" :loading="loading" circle title="刷新" @click="load" />
      </div>

      <el-table v-loading="loading" :data="records" border height="620" empty-text="暂无检查表">
        <el-table-column prop="inspection_code" label="检查记录编号" min-width="170" fixed="left" />
        <el-table-column prop="form_code" label="表号" width="120" />
        <el-table-column prop="bridge_code" label="桥梁编号" width="135" />
        <el-table-column prop="bridge_name" label="桥梁名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="bridge_type_name" label="桥型" min-width="130" />
        <el-table-column prop="inspection_date" label="检查日期" width="120" />
        <el-table-column prop="inspector_name" label="检查人员" width="120" />
        <el-table-column label="成果" width="190">
          <template #default="{ row }">
            <div class="result-counts">
              <span>{{ row.detail_count || 0 }} 项明细</span>
              <span>{{ row.defect_count || 0 }} 条病害</span>
              <span>{{ row.report_count || 0 }} 份报告</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="task_status" label="任务状态" width="105" />
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain">{{ statusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewer_name" label="审核人员" width="115" />
        <el-table-column prop="review_time" label="审核时间" width="175" />
        <el-table-column label="操作" width="112" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="ClipboardCheck" @click="openReview(row)">
              {{ row.status === 'pending' ? '审核' : '查看' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="drawer" :title="drawerTitle" size="92%" destroy-on-close>
      <div v-loading="detailLoading" class="review-detail">
        <template v-if="detail.record">
          <section class="record-summary">
            <div><span>检查记录编号</span><strong>{{ detail.record.inspection_code }}</strong></div>
            <div><span>表号</span><strong>{{ detail.record.form_code }}</strong></div>
            <div><span>桥梁</span><strong>{{ detail.record.bridge_name }}（{{ detail.record.bridge_code }}）</strong></div>
            <div><span>桥型</span><strong>{{ detail.record.bridge_type_name }}</strong></div>
            <div><span>检查日期</span><strong>{{ detail.record.inspection_date }}</strong></div>
            <div><span>任务状态</span><strong>{{ detail.record.task_status || '—' }}</strong></div>
            <div v-if="detail.record.archive_id"><span>检查档案编号</span><strong>ARC-{{ detail.record.archive_id }}</strong></div>
          </section>

          <section class="detail-section">
            <div class="section-title">
              <div><span>{{ isInitial ? 'B' : detail.record.form_table_code || 'C-7' }}</span><h3>{{ isInitial ? '初始检查项目结果' : '定期检查部件结果' }}</h3></div>
              <el-tag effect="plain">{{ detail.rows.length }} 项</el-tag>
            </div>
            <el-table v-if="isInitial" :data="detail.rows" border max-height="440">
              <el-table-column type="index" label="序号" width="60" />
              <el-table-column prop="item_category" label="项目分类" width="145" />
              <el-table-column prop="item_name" label="检测项目" min-width="190" />
              <el-table-column prop="measured_value" label="检测结果" min-width="210" show-overflow-tooltip />
              <el-table-column prop="unit" label="单位" width="85" />
              <el-table-column label="适用" width="75"><template #default="{ row }">{{ row.applicable_flag ? '是' : '否' }}</template></el-table-column>
              <el-table-column prop="defect_name" label="病害" width="140" />
              <el-table-column prop="inspection_description" label="检查说明" min-width="240" show-overflow-tooltip />
            </el-table>
            <el-table v-else :data="detail.rows" border max-height="440">
              <el-table-column type="index" label="序号" width="60" />
              <el-table-column prop="part_name" label="部位" width="130" />
              <el-table-column prop="component_name" label="部件" min-width="170" />
              <el-table-column prop="component_serial" label="部件序号" width="120" />
              <el-table-column prop="location_desc" label="所在位置" min-width="160" show-overflow-tooltip />
              <el-table-column prop="score" label="评分" width="85" />
              <el-table-column prop="defect_name" label="病害" width="140" />
              <el-table-column prop="defect_location" label="病害位置" min-width="160" />
              <el-table-column prop="defect_range" label="病害范围" min-width="150" />
              <el-table-column prop="maintenance_advice" label="养护建议" min-width="230" show-overflow-tooltip />
            </el-table>
          </section>

          <section class="detail-grid">
            <div class="detail-section">
              <div class="section-title"><div><span>DEFECTS</span><h3>病害记录</h3></div><el-tag type="danger" effect="plain">{{ detail.defects.length }} 条</el-tag></div>
              <el-table :data="detail.defects" border max-height="300" empty-text="无病害记录">
                <el-table-column prop="part_name" label="部位" width="120" />
                <el-table-column prop="component_name" label="部件" width="130" />
                <el-table-column label="病害类型" min-width="140"><template #default="{ row }">{{ row.dictionary_defect_name || row.defect_type }}</template></el-table-column>
                <el-table-column prop="defect_range" label="范围" min-width="130" />
                <el-table-column prop="defect_degree_code" label="程度" width="95" />
                <el-table-column prop="description" label="说明" min-width="190" show-overflow-tooltip />
              </el-table>
            </div>

            <div class="detail-section">
              <div class="section-title"><div><span>FILES</span><h3>检查报告与附件</h3></div><el-tag type="info" effect="plain">{{ detail.reports.length + detail.attachments.length }} 份</el-tag></div>
              <div class="file-list">
                <button v-for="report in detail.reports" :key="report.report_id" type="button" class="file-row" @click="downloadReport(report)">
                  <FileText :size="18" /><span><b>{{ report.report_id }}</b><small>{{ reportTypeLabel(report.report_type) }} · {{ report.version_no }} · {{ report.report_status }}</small></span><Download :size="17" />
                </button>
                <div v-for="file in detail.attachments" :key="file.file_id" class="file-row attachment-row">
                  <Paperclip :size="18" /><span><b>{{ file.file_name }}</b><small>{{ file.file_description || file.photo_category || '检查附件' }} · {{ formatBytes(file.file_size) }}</small></span>
                </div>
                <el-empty v-if="!detail.reports.length && !detail.attachments.length" description="暂无检查报告或附件" :image-size="58" />
              </div>
            </div>
          </section>

          <section class="review-decision">
            <div class="decision-heading"><div><span>REVIEW</span><h3>审核结论</h3></div><el-tag :type="statusType(detail.record.status)">{{ statusName(detail.record.status) }}</el-tag></div>
            <el-input v-if="detail.record.status === 'pending'" v-model="opinion" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="填写审核意见；打回时必须填写具体原因" />
            <div v-else class="saved-opinion">
              <p>{{ detail.record.review_opinion || '未填写审核意见' }}</p>
              <span>{{ detail.record.reviewer_name || '—' }} · {{ detail.record.review_time || '—' }}</span>
            </div>
          </section>
        </template>
      </div>

      <template #footer>
        <div class="drawer-actions">
          <el-button @click="drawer = false">关闭</el-button>
          <template v-if="detail.record?.status === 'pending'">
            <el-button type="danger" plain :icon="RotateCcw" :loading="acting" @click="reject">打回修改</el-button>
            <el-button type="primary" :icon="Archive" :loading="acting" @click="approve">审核通过并归档</el-button>
          </template>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Archive, ClipboardCheck, Download, FileText, Paperclip, RefreshCw, RotateCcw } from 'lucide-vue-next'
import http from '../api/http'

const props = defineProps({ inspectionType: { type: String, default: 'initial' } })
const isInitial = computed(() => props.inspectionType === 'initial')
const records = ref([])
const counts = reactive({ pending: 0, archived: 0, rejected: 0 })
const state = ref('pending')
const loading = ref(false)
const detailLoading = ref(false)
const acting = ref(false)
const drawer = ref(false)
const opinion = ref('')
const detail = reactive({ record: null, rows: [], defects: [], reports: [], attachments: [] })
const drawerTitle = computed(() => `${isInitial.value ? '初始检查' : '定期检查'} · ${detail.record?.inspection_code || '审核详情'}`)
const stateOptions = computed(() => [
  { label: `待审核 ${counts.pending}`, value: 'pending' },
  { label: `已归档 ${counts.archived}`, value: 'archived' },
  { label: `已打回 ${counts.rejected}`, value: 'rejected' }
])

onMounted(load)
watch(() => props.inspectionType, () => { state.value = 'pending'; drawer.value = false; load() })

async function load() {
  loading.value = true
  try {
    const data = await http.get(`/review-workbench/${props.inspectionType}`, { params: { state: state.value } })
    records.value = data.records || []
    Object.assign(counts, data.counts || {})
  } finally { loading.value = false }
}

async function openReview(row) {
  drawer.value = true
  detailLoading.value = true
  opinion.value = row.status === 'pending' ? '' : (row.review_opinion || '')
  try {
    const data = await http.get(`/review-workbench/${props.inspectionType}/${row.inspection_code}`)
    detail.record = data.record
    detail.rows = data.rows || []
    detail.defects = data.defects || []
    detail.reports = data.reports || []
    detail.attachments = data.attachments || []
  } finally { detailLoading.value = false }
}

async function approve() {
  await ElMessageBox.confirm('审核通过后，该检查表将进入档案且不能再编辑。', '确认归档', { type: 'warning', confirmButtonText: '通过并归档', cancelButtonText: '取消' })
  await act('approve', { opinion: opinion.value }, '检查表已审核通过并归档')
}

async function reject() {
  if (!opinion.value.trim()) { ElMessage.warning('请填写具体打回原因'); return }
  await ElMessageBox.confirm('打回后任务将恢复为进行中，原检查人员可以继续修改。', '确认打回', { type: 'warning', confirmButtonText: '确认打回', cancelButtonText: '取消' })
  await act('reject', { reason: opinion.value.trim() }, '检查表已打回检查人员修改')
}

async function act(action, body, message) {
  acting.value = true
  try {
    await http.post(`/review-workbench/${props.inspectionType}/${detail.record.inspection_code}/${action}`, body)
    ElMessage.success(message)
    drawer.value = false
    await load()
  } finally { acting.value = false }
}

async function downloadReport(report) {
  const response = await fetch(`/api/reports/${report.report_id}/download`, { headers: { Authorization: `Bearer ${localStorage.getItem('bridge_token')}` } })
  if (!response.ok) { ElMessage.error('PDF 下载失败'); return }
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${report.report_id}.pdf`
  link.click()
  URL.revokeObjectURL(url)
}
function reportTypeLabel(type){return{bridge_card:'桥梁基本状况卡片',initial_record:'初始检查记录表',periodic_record:'定期检查记录表',bridge_summary:'检查趋势与对比'}[type]||type}

function statusName(value) { return { pending: '待审核', archived: '已归档', rejected: '已打回', draft: '草稿' }[value] || value || '未提交' }
function statusType(value) { return value === 'archived' ? 'success' : value === 'rejected' ? 'danger' : 'warning' }
function formatBytes(value) { const size = Number(value || 0); if (!size) return '未知大小'; if (size < 1024) return `${size} B`; if (size < 1048576) return `${(size / 1024).toFixed(1)} KB`; return `${(size / 1048576).toFixed(1)} MB` }
</script>

<style scoped>
.review-page{max-width:1800px;margin:auto}.review-heading{display:flex;align-items:flex-start;justify-content:space-between;border-bottom:1px solid #dbe3ec;padding-bottom:16px;margin-bottom:14px}.eyebrow,.section-title span,.decision-heading span{font-size:12px;font-weight:700;color:#0f766e}.review-heading h2{margin:6px 0 0;font-size:24px}.review-list{padding:16px}.list-toolbar{display:flex;align-items:center;justify-content:space-between;margin-bottom:14px}.result-counts{display:flex;gap:8px;flex-wrap:wrap}.result-counts span{font-size:12px;color:#475569}.review-detail{padding:0 4px 24px}.record-summary{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));border:1px solid #dbe3ec;background:#dbe3ec;gap:1px}.record-summary>div{background:#fff;padding:13px 15px;min-width:0}.record-summary span{display:block;color:#64748b;font-size:12px;margin-bottom:5px}.record-summary strong{display:block;font-size:14px;overflow-wrap:anywhere}.detail-section,.review-decision{margin-top:18px;border-top:1px solid #dbe3ec;padding-top:14px}.section-title,.decision-heading{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px}.section-title h3,.decision-heading h3{font-size:16px;margin:3px 0 0}.detail-grid{display:grid;grid-template-columns:1fr 1fr;gap:22px}.file-list{border:1px solid #dbe3ec;min-height:120px}.file-row{appearance:none;width:100%;border:0;border-bottom:1px solid #e2e8f0;background:#fff;padding:11px 13px;display:grid;grid-template-columns:20px minmax(0,1fr) 18px;gap:10px;text-align:left;align-items:center;color:#334155}.file-row:last-child{border-bottom:0}.file-row:not(.attachment-row){cursor:pointer}.file-row:not(.attachment-row):hover{background:#f8fafc}.file-row span{min-width:0}.file-row b,.file-row small{display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.file-row b{font-size:13px}.file-row small{font-size:12px;color:#64748b;margin-top:3px}.saved-opinion{border-left:3px solid #0f766e;padding:9px 13px;background:#f8fafc}.saved-opinion p{margin:0 0 8px;white-space:pre-wrap}.saved-opinion span{font-size:12px;color:#64748b}.drawer-actions{display:flex;justify-content:flex-end;gap:10px}@media(max-width:1200px){.record-summary{grid-template-columns:repeat(3,minmax(0,1fr))}.detail-grid{grid-template-columns:1fr}}@media(max-width:700px){.record-summary{grid-template-columns:repeat(2,minmax(0,1fr))}.list-toolbar{align-items:stretch;gap:10px}.list-toolbar :deep(.el-segmented){max-width:calc(100vw - 110px);overflow:auto}.drawer-actions{flex-wrap:wrap}}
</style>
