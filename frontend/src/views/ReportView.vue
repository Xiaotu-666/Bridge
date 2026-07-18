<template>
  <section class="table-panel">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="bridgeCode" clearable placeholder="桥梁编号" style="width: 170px" @keyup.enter="load" />
        <el-input v-model="keyword" clearable placeholder="报告编号、任务编号或版本" style="width: 280px" @keyup.enter="load" />
        <el-button @click="load">查询</el-button>
      </div>
      <el-button v-if="canGenerate" type="primary" @click="openGenerate">生成 PDF 报告</el-button>
    </div>

    <el-table :data="rows" border height="560">
      <el-table-column prop="bridge_code" label="桥梁编号" width="130" />
      <el-table-column prop="bridge_name" label="桥梁名称" min-width="170" />
      <el-table-column prop="report_id" label="报告编号" width="150" />
      <el-table-column prop="task_id" label="任务编号" width="130" />
      <el-table-column prop="report_type" label="报告类型" width="160" />
      <el-table-column prop="version_no" label="版本" width="100" />
      <el-table-column prop="file_format" label="格式" width="90" />
      <el-table-column prop="report_status" label="状态" width="110" />
      <el-table-column prop="generation_time" label="生成时间" width="180" />
      <el-table-column prop="change_summary" label="变更摘要" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.file_path" text type="primary" @click="download(row)">下载</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display:flex;justify-content:flex-end;margin-top:14px">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @change="load"
      />
    </div>

    <el-dialog v-model="dialog" title="生成检查报告" width="520px">
      <el-form label-position="top">
        <el-form-item label="任务编号">
          <el-select v-model="form.taskId" filterable style="width:100%" placeholder="请选择检查任务" @change="syncReportType"><el-option v-for="task in taskOptions" :key="task.task_id" :label="`${task.task_id} · ${task.bridge_code} · ${task.inspection_type==='periodic'?'定期检查':'初始检查'} · ${task.task_status}`" :value="task.task_id"/></el-select>
        </el-form-item>
        <el-form-item label="报告类型">
          <el-select v-model="form.reportType" style="width:100%" disabled>
            <el-option label="初始检查记录表" value="initial_record" />
            <el-option label="定期检查记录表" value="periodic_record" />
          </el-select>
          <div class="form-tip">报告类型由所选检查任务决定，不能跨检查类型生成。</div>
        </el-form-item>
        <el-form-item label="变更摘要">
          <el-input v-model="form.changeSummary" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="generate">生成</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const canGenerate = computed(() => ['admin', 'engineer'].includes(auth.role))
const inspectionType=computed(()=>route.path.startsWith('/initial/')?'initial':route.path.startsWith('/periodic/')?'periodic':'')
const reportTypeFilter=computed(()=>inspectionType.value==='initial'?'initial_record':inspectionType.value==='periodic'?'periodic_record':'')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const bridgeCode = ref('')
const dialog = ref(false)
const taskOptions=ref([])
const generating = ref(false)
const selectedTask=computed(()=>taskOptions.value.find(task=>task.task_id===form.taskId))
const form = reactive({
  taskId: '',
  reportType: 'initial_record',
  changeSummary: '生成桥梁检查记录表 PDF'
})

onMounted(load)
watch(reportTypeFilter, () => { page.value=1; load() })

async function load() {
  const reportType = reportTypeFilter.value
  const data = await http.get('/reports', { params: { page: page.value, size: size.value, keyword: keyword.value, bridgeCode: bridgeCode.value, report_type: reportType || undefined } })
  if (reportType !== reportTypeFilter.value) return
  rows.value = data.records
  total.value = data.total
}

async function openGenerate() {
  form.taskId=''
  form.reportType=inspectionType.value==='periodic'?'periodic_record':'initial_record'
  dialog.value = true
  await loadTasks()
}

function syncReportType(){form.reportType=selectedTask.value?.inspection_type==='periodic'?'periodic_record':'initial_record'}
async function loadTasks(){const data=await http.get('/tasks',{params:{page:1,size:100,inspection_type:inspectionType.value||undefined}});taskOptions.value=data.records;if(!form.taskId&&data.records.length)form.taskId=data.records[0].task_id;syncReportType()}

async function generate() {
  if(!form.taskId){ElMessage.warning('请选择检查任务');return}
  generating.value = true
  try {
    await http.post(`/reports/generate/${form.taskId}`, {
      reportType: form.reportType,
      changeSummary: form.changeSummary
    })
    ElMessage.success('报告生成成功')
    dialog.value = false
    await load()
  } finally {
    generating.value = false
  }
}

async function download(row) {
  const response = await fetch(`/api/reports/${row.report_id}/download`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('bridge_token')}` }
  })
  if(!response.ok){ElMessage.error('PDF 下载失败');return}
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${row.report_id}.pdf`
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.form-tip{margin-top:6px;color:#64748b;font-size:12px;line-height:1.5}
</style>
