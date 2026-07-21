<template>
  <section class="workbench-page">
    <div class="workbench-heading">
      <div>
        <span class="eyebrow">{{ isInitial ? '表 B · 初始检查现场填表' : '表 C · 定期检查现场填表' }}</span>
        <h2>{{ isInitial ? '初始检查工作台' : '定期检查工作台' }}</h2>
        <p>{{ isInitial ? '按初检项目字典逐项填写桥梁初始检查结果。' : '按桥型 C 表逐部位、逐部件填写定期检查结果。' }}</p>
      </div>
      <el-tag :type="isInitial ? 'primary' : 'warning'" effect="dark">{{ isInitial ? '初始检查' : '定期检查' }}</el-tag>
    </div>

    <section class="panel task-selector">
      <div class="selector-label"><b>我的检查任务</b><span>只显示分配给当前检查人员的任务</span></div>
      <el-select v-model="selectedTaskId" filterable clearable placeholder="请选择任务" class="task-select" @change="loadTask">
        <el-option v-for="item in tasks" :key="item.task_id" :label="`${item.task_id} · ${item.bridge_code} · ${item.bridge_name || ''}`" :value="item.task_id" />
      </el-select>
      <el-button @click="loadTasks">刷新任务</el-button>
    </section>

    <el-alert v-if="!tasks.length" type="info" :closable="false" title="当前没有可填报的检查任务，请联系工程师分配任务。" />

    <template v-if="task">
      <section class="panel bridge-strip">
        <div><span>桥梁编号</span><strong>{{ bridge.bridge_code }}</strong></div>
        <div><span>桥梁名称</span><strong>{{ bridge.bridge_name || '—' }}</strong></div>
        <div><span>桥型</span><strong>{{ bridge.bridge_type_name || '其他' }}</strong></div>
        <div><span>任务编号</span><strong>{{ task.task_id }}</strong></div>
        <div><span>检查状态</span><el-tag size="small">{{ task.task_status }}</el-tag></div>
      </section>

      <section class="panel record-panel">
        <div class="panel-title"><div><b>{{ isInitial ? 'B 表 · 初始检查基本信息' : `C 表 · ${task.form_table_code || '定期检查'}基本信息` }}</b><span>先完成表头信息，再填写下方检查明细</span></div><el-tag effect="plain">{{ recordStatus }}</el-tag></div>
        <el-form label-position="top" class="record-grid">
          <el-form-item label="检查日期"><el-date-picker v-model="record.inspection_date" value-format="YYYY-MM-DD" type="date" style="width:100%" /></el-form-item>
          <el-form-item v-if="isInitial" label="检查机构"><el-input v-model="record.inspection_org" /></el-form-item>
          <el-form-item v-if="isInitial" label="检查人员"><el-input v-model="record.inspectors" /></el-form-item>
          <el-form-item v-if="isInitial" label="桥梁工程师"><el-input :model-value="bridge.bridge_engineer||'—'" disabled/></el-form-item>
          <el-form-item v-if="isInitial" label="下次定期检查日期"><el-date-picker v-model="record.next_inspection_date" value-format="YYYY-MM-DD" type="date" placeholder="选择首次定期检查日期" style="width:100%" /></el-form-item>
          <el-form-item v-if="!isInitial" label="上次检查日期"><el-date-picker v-model="record.last_inspection_date" value-format="YYYY-MM-DD" type="date" style="width:100%" /></el-form-item>
          <el-form-item v-if="!isInitial" label="技术状况等级"><el-select v-model="record.rating_level_code" clearable style="width:100%"><el-option v-for="item in ratingLevels" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item v-if="!isInitial" label="下次检查日期"><el-date-picker v-model="record.next_inspection_date" value-format="YYYY-MM-DD" type="date" style="width:100%" /></el-form-item>
          <el-form-item v-if="!isInitial" label="记录人"><el-input v-model="record.recorder" /></el-form-item>
          <el-form-item v-if="!isInitial" label="负责人"><el-input v-model="record.principal" /></el-form-item>
          <el-form-item label="天气与环境温度"><el-input v-model="record.weather_temperature" /></el-form-item>
          <el-form-item v-if="!isInitial" label="全桥清洁状况"><el-input v-model="record.cleanliness" /></el-form-item>
          <el-form-item v-if="!isInitial" label="预防及修复状况" class="wide"><el-input v-model="record.maintenance_status" /></el-form-item>
          <el-form-item v-if="isInitial" label="病害描述与建议" class="wide"><el-input v-model="record.defect_advice" type="textarea" :rows="2" /></el-form-item>
        </el-form>
      </section>

      <section class="panel detail-panel">
        <div class="panel-title"><div><b>{{ isInitial ? '初检项目结果明细' : '定检部件检查明细' }}</b><span>{{ isInitial ? `${rows.length} 个适用初检项目` : `${rows.length} 个桥梁部件` }} · 选择病害后将自动写入病害记录</span></div><el-tag type="info" effect="plain">{{ isInitial ? '逐项目' : '逐部件' }}</el-tag></div>
        <el-table v-if="isInitial" :data="rows" border height="560" class="inspection-table initial-table">
          <el-table-column type="index" label="序号" width="60" fixed />
          <el-table-column prop="item_category" label="项目分类" width="140" />
          <el-table-column prop="item_name" label="检测项目" min-width="190" fixed="left" />
          <el-table-column label="要求" width="95"><template #default="{row}">{{ row.requirement_type === 'required' ? '必检' : '条件检' }}</template></el-table-column>
          <el-table-column prop="trigger_condition" label="触发条件" min-width="180" show-overflow-tooltip />
          <el-table-column label="检测结果" min-width="220"><template #default="{row}"><el-input v-model="row.measured_value" placeholder="填写测量值或判定结果" /></template></el-table-column>
          <el-table-column label="病害字典" min-width="180"><template #default="{row}"><el-select v-model="row.defect_definition_code" clearable filterable placeholder="无病害/选择病害" @change="applyDefect(row)"><el-option v-for="item in defectDefinitions" :key="item.defect_definition_code" :label="item.defect_name" :value="item.defect_definition_code" /></el-select></template></el-table-column>
          <el-table-column label="检查说明" min-width="230"><template #default="{row}"><el-input v-model="row.inspection_description" type="textarea" :rows="2" /></template></el-table-column>
        </el-table>
      <el-table v-else :data="rows" border height="560" class="inspection-table periodic-table">
           <el-table-column type="index" label="序号" width="55" fixed />
           <el-table-column prop="part_name" label="部位" width="100" fixed="left" />
           <el-table-column prop="component_name" label="部件" min-width="140" fixed="left" />
           <el-table-column prop="component_serial" label="部件序号" width="100" />
           <el-table-column label="评分" width="85"><template #default="{row}"><el-input-number v-model="row.score" :min="0" :max="100" :precision="1" controls-position="right" size="small"/></template></el-table-column>
           <el-table-column label="病害字典" min-width="160"><template #default="{row}"><el-select v-model="row.defect_definition_code" clearable filterable placeholder="选择病害" size="small" @change="applyDefect(row)"><el-option v-for="item in defectDefinitions" :key="item.defect_definition_code" :label="item.defect_name" :value="item.defect_definition_code" /></el-select></template></el-table-column>
           <el-table-column label="缺损类型" min-width="140"><template #default="{row}"><el-input v-model="row.defect_type" placeholder="裂缝/锈蚀/..." size="small"/></template></el-table-column>
           <el-table-column label="缺损位置" min-width="140"><template #default="{row}"><el-input v-model="row.defect_location" placeholder="如：跨中底板" size="small"/></template></el-table-column>
           <el-table-column label="缺损范围" min-width="120"><template #default="{row}"><el-input v-model="row.defect_range" placeholder="如：3处/2m²" size="small"/></template></el-table-column>
           <el-table-column label="缺损程度" width="100"><template #default="{row}"><el-select v-model="row.defect_degree_code" clearable size="small"><el-option label="轻微" value="slight"/><el-option label="中等" value="medium"/><el-option label="严重" value="serious"/><el-option label="危险" value="danger"/></el-select></template></el-table-column>
           <el-table-column label="最不利构件" min-width="130"><template #default="{row}"><el-input v-model="row.worst_component" placeholder="最不利构件编号" size="small"/></template></el-table-column>
           <el-table-column label="照片" width="130"><template #default="{row}"><div style="display:flex;align-items:center;gap:4px"><input type="file" accept="image/*" style="display:none" :ref="el=>{if(el&&row.component_inspection_id) row._fileInput=el}" @change="e=>uploadPhoto(row,e)"/><el-button size="small" circle @click="row._fileInput?.click()" title="上传照片"><span style="font-size:16px">📷</span></el-button><img v-if="row._photoUrl" :src="row._photoUrl" style="width:40px;height:40px;object-fit:cover;border-radius:4px;cursor:pointer" @click="previewUrl=row._photoUrl" :title="row._photoName"/></div></template></el-table-column>
           <el-table-column label="养护建议" min-width="180"><template #default="{row}"><el-input v-model="row.maintenance_advice" type="textarea" :rows="2" size="small"/></template></el-table-column>
           <el-table-column label="特殊检查" width="85"><template #default="{row}"><el-checkbox v-model="row.special_check_required" :true-value="1" :false-value="0"/></template></el-table-column>
         </el-table>
      </section>

      <div v-if="!taskLocked" class="workbench-actions"><span>填写完成后上传，系统将同步检查记录、病害记录和任务状态。</span><div><el-button @click="save(false)" :loading="saving">保存草稿</el-button><el-button type="primary" @click="save(true)" :loading="saving">上传检查记录</el-button></div></div>
      <el-alert v-else class="locked-alert" type="info" :closable="false" title="任务已完成，检查记录已锁定，不能继续编辑或删除。"/>
    </template>
  </section>
  <el-image-viewer v-if="previewUrl" :url-list="[previewUrl]" @close="previewUrl=''"/>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const props = defineProps({ inspectionType: { type: String, default: 'initial' } })
const route = useRoute()
const isInitial = computed(() => props.inspectionType === 'initial')
const tasks = ref([])
const selectedTaskId = ref('')
const task = ref(null)
const bridge = reactive({})
const record = reactive({})
const rows = ref([])
const defectDefinitions = ref([])
const saving = ref(false)
const previewUrl = ref('')
const ratingLevels = [{ value: '1', label: '1 类' }, { value: '2', label: '2 类' }, { value: '3', label: '3 类' }, { value: '4', label: '4 类' }, { value: '5', label: '5 类' }]
const recordStatus = computed(() => record.status === 'pending' ? '待审核' : record.status === 'draft' ? '草稿' : (record.status || '未上传'))

const taskLocked = computed(() => ['已完成','已审核','已取消'].includes(task.value?.task_status))

onMounted(loadTasks)
watch(() => props.inspectionType, () => {
  selectedTaskId.value = ''
  task.value = null
  rows.value = []
  Object.keys(bridge).forEach(key => delete bridge[key])
  Object.keys(record).forEach(key => delete record[key])
  loadTasks()
})

async function loadTasks() {
  const type = props.inspectionType
  const loadedTasks = await http.get(`/inspection-workbench/${type}/tasks`)
  if (type !== props.inspectionType) return
  tasks.value = loadedTasks
  const requestedTaskId = String(route.query.taskId || '')
  if (requestedTaskId && tasks.value.some(item => item.task_id === requestedTaskId)) selectedTaskId.value = requestedTaskId
  else if (!tasks.value.some(item => item.task_id === selectedTaskId.value)) selectedTaskId.value = tasks.value[0]?.task_id || ''
  if (selectedTaskId.value) await loadTask(selectedTaskId.value)
  else { task.value = null; rows.value = [] }
}

async function loadTask(taskId) {
  if (!taskId) { task.value = null; rows.value = []; return }
  const type = props.inspectionType
  const data = await http.get(`/inspection-workbench/${type}/tasks/${taskId}`)
  if (type !== props.inspectionType) return
  task.value = data.task
  Object.keys(bridge).forEach(key => delete bridge[key]); Object.assign(bridge, data.bridge || {})
  Object.keys(record).forEach(key => delete record[key]); Object.assign(record, data.record || {})
  rows.value = data.rows || []
  defectDefinitions.value = data.defectDefinitions || []
  if (!isInitial.value) await loadExistingPhotos()
}

async function loadExistingPhotos() {
  const ids = rows.value.filter(r => r.component_inspection_id).map(r => r.component_inspection_id)
  if (!ids.length) return
  try {
    const photos = await http.get('/attachments', { params: { size: 500 } })
    const photoList = photos.records || []
    rows.value.forEach(row => {
      const p = photoList.find(ph => ph.component_inspection_id === row.component_inspection_id)
      if (p) { row._photoUrl = `/api/files/view?path=${encodeURIComponent(p.storage_path)}`; row._photoName = p.file_name }
    })
  } catch {}
}

function applyDefect(row) {
  const selected = defectDefinitions.value.find(item => item.defect_definition_code === row.defect_definition_code)
  if (!selected) return
  row.defect_type = selected.defect_name
  row.defect_nature = selected.defect_nature
  row.defect_degree_code = selected.default_degree_code
  row.defect_range = row.defect_range || selected.default_range
  row.maintenance_advice = row.maintenance_advice || selected.default_advice
}

async function uploadPhoto(row, event) {
      const file = event.target.files[0]; if (!file || !row.component_inspection_id) return
      const form = new FormData(); form.append('file', file); form.append('componentInspectionId', row.component_inspection_id)
      try {
        const data = await http.post('/inspection-workbench/periodic/photos', form)
        row._photoUrl = `/api/files/view?path=${encodeURIComponent(data.storage_path)}`
        row._photoName = file.name; ElMessage.success('照片已上传')
      } catch (err) { ElMessage.error(err.message || '照片上传失败') }
      event.target.value = ''
    }

async function save(finalize) {
      if (!selectedTaskId.value) return
      saving.value = true
      try {
        if (isInitial.value) record.bridge_engineer = bridge.bridge_engineer || ''
        const data = await http.post(`/inspection-workbench/${props.inspectionType}/tasks/${selectedTaskId.value}/${finalize ? 'submit' : 'draft'}`, { record, rows: rows.value })
    ElMessage.success(finalize ? '检查记录已上传，病害记录已自动生成' : '检查填表草稿已保存')
    task.value = data.task; Object.keys(record).forEach(key => delete record[key]); Object.assign(record, data.record || {})
    rows.value = data.rows || rows.value
  } finally { saving.value = false }
}
</script>
<style scoped>
.workbench-page{max-width:1800px;margin:auto}.workbench-heading{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:1px solid #dbe3ec;padding-bottom:16px;margin-bottom:14px}.eyebrow{font-size:12px;font-weight:700;color:#0f766e}.workbench-heading h2{margin:6px 0;font-size:24px}.workbench-heading p{margin:0;color:#64748b}.task-selector{display:flex;align-items:center;gap:12px;margin-bottom:14px}.selector-label{display:flex;flex-direction:column;min-width:210px}.selector-label span,.panel-title span{font-size:12px;color:#64748b;margin-top:3px}.task-select{max-width:560px;flex:1}.bridge-strip{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:1px;background:#e2e8f0;padding:1px;margin-top:14px}.bridge-strip>div{background:#fff;padding:13px 15px;display:flex;flex-direction:column;gap:4px}.bridge-strip span{font-size:12px;color:#64748b}.record-panel,.detail-panel{margin-top:14px}.panel-title{display:flex;align-items:center;justify-content:space-between;margin-bottom:13px}.panel-title b{display:block;font-size:16px}.record-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:0 16px}.record-grid .wide{grid-column:span 2}.inspection-table :deep(.el-input-number){width:100%}.inspection-table :deep(.el-select){width:100%}.inspection-table :deep(.el-textarea__inner){min-height:52px}.workbench-actions{display:flex;justify-content:space-between;align-items:center;padding:16px 2px;color:#64748b;font-size:13px}.workbench-actions div{display:flex;gap:10px}@media(max-width:1100px){.bridge-strip{grid-template-columns:repeat(3,minmax(0,1fr))}.record-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:680px){.task-selector,.workbench-actions{align-items:stretch;flex-direction:column}.task-select{max-width:none;width:100%}.bridge-strip{grid-template-columns:repeat(2,minmax(0,1fr))}.record-grid{grid-template-columns:1fr}.record-grid .wide{grid-column:auto}.workbench-actions{gap:12px}}
</style>
