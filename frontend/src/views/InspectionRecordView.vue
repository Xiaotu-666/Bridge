<template>
  <section class="record-page table-panel">
    <div class="record-heading"><div><span>{{ isInitial?'表 B · 初始检查':'表 C · 定期检查' }}</span><h2>{{ isInitial?'桥梁初始检查记录':'桥梁定期检查记录' }}</h2><p>{{ isInitial?'按任务保存初检表头和逐项目检测结果。':'按任务保存定检表头、部件评分和病害。' }}</p></div><el-button v-if="!readonly" type="primary" @click="startCreate">{{ auth.role==='inspector'?'进入检查工作台':`新增${isInitial?'初检':'定检'}记录` }}</el-button></div>
    <div class="record-filters"><el-input v-model="filters.keyword" clearable placeholder="检查编号、桥梁编号或人员" @keyup.enter="load"/><el-input v-model="filters.bridge_code" clearable placeholder="桥梁编号" @keyup.enter="load"/><el-button @click="load">查询</el-button></div>
    <el-table :data="rows" border height="580">
      <template v-if="isInitial">
        <el-table-column prop="initial_inspection_code" label="初始检查编号" width="175"/><el-table-column prop="task_id" label="任务编号" width="150"/><el-table-column prop="bridge_code" label="桥梁编号" width="125"/><el-table-column prop="bridge_name" label="桥梁名称" min-width="170"/><el-table-column prop="inspection_date" label="检查日期" width="120"/><el-table-column prop="inspection_org" label="检查机构" min-width="160"/><el-table-column prop="inspectors" label="检查人员" min-width="140"/>
      </template>
      <template v-else>
        <el-table-column prop="periodic_inspection_code" label="定期检查编号" width="175"/><el-table-column prop="task_id" label="任务编号" width="150"/><el-table-column prop="form_table_code" label="定检表号" width="90"/><el-table-column prop="bridge_code" label="桥梁编号" width="125"/><el-table-column prop="bridge_name" label="桥梁名称" min-width="170"/><el-table-column prop="inspection_date" label="检查日期" width="120"/><el-table-column prop="rating_level_code" label="技术状况等级" width="120"/><el-table-column prop="recorder" label="记录人" width="120"/>
      </template>
      <el-table-column prop="status" label="记录状态" width="105"><template #default="{row}">{{ statusName(row.status) }}</template></el-table-column>
      <el-table-column prop="task_status" label="任务状态" width="105"><template #default="{row}"><el-tag :type="isLocked(row)?'info':'success'" size="small">{{ row.task_status||'未关联' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="190" fixed="right"><template #default="{row}"><el-button v-if="!readonly&&!isLocked(row)" text type="primary" @click="openEdit(row)">编辑{{ isInitial?'初检':'定检' }}</el-button><el-popconfirm v-if="canDelete&&!isLocked(row)" title="确认删除这份检查记录？" @confirm="remove(row)"><template #reference><el-button text type="danger">删除</el-button></template></el-popconfirm><span v-if="isLocked(row)" class="locked-text">已完成，禁止修改</span></template></el-table-column>
    </el-table>
    <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" layout="total, sizes, prev, pager, next" :total="total" :page-sizes="[10,20,50]" @change="load"/></div>

    <el-drawer v-model="drawer" :title="editing?'编辑记录':`新增${isInitial?'初始':'定期'}检查记录`" size="680px">
      <el-form label-position="top" class="form-grid">
        <template v-if="isInitial"><el-form-item label="初始检查编号"><el-input v-model="form.initial_inspection_code" :disabled="Boolean(editing)" placeholder="留空自动生成"/></el-form-item><el-form-item label="桥梁编号" required><el-input v-model="form.bridge_code"/></el-form-item><el-form-item label="检查日期" required><el-date-picker v-model="form.inspection_date" value-format="YYYY-MM-DD" type="date" style="width:100%"/></el-form-item><el-form-item label="检查机构"><el-input v-model="form.inspection_org"/></el-form-item><el-form-item label="检查人员"><el-input v-model="form.inspectors"/></el-form-item><el-form-item label="桥梁工程师"><el-input v-model="form.bridge_engineer"/></el-form-item><el-form-item label="天气与环境温度"><el-input v-model="form.weather_temperature"/></el-form-item><el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option label="草稿" value="draft"/><el-option label="待审核" value="pending"/><el-option label="已审核" value="approved"/></el-select></el-form-item></template>
        <template v-else><el-form-item label="定期检查编号"><el-input v-model="form.periodic_inspection_code" :disabled="Boolean(editing)" placeholder="留空自动生成"/></el-form-item><el-form-item label="桥梁编号" required><el-input v-model="form.bridge_code"/></el-form-item><el-form-item label="检查日期" required><el-date-picker v-model="form.inspection_date" value-format="YYYY-MM-DD" type="date" style="width:100%"/></el-form-item><el-form-item label="上次检查日期"><el-date-picker v-model="form.last_inspection_date" value-format="YYYY-MM-DD" type="date" style="width:100%"/></el-form-item><el-form-item label="技术状况等级"><el-select v-model="form.rating_level_code" style="width:100%"><el-option v-for="item in ratingLevels" :key="item.value" :label="item.label" :value="item.value"/></el-select></el-form-item><el-form-item label="下次检查日期"><el-date-picker v-model="form.next_inspection_date" value-format="YYYY-MM-DD" type="date" style="width:100%"/></el-form-item><el-form-item label="记录人"><el-input v-model="form.recorder"/></el-form-item><el-form-item label="负责人"><el-input v-model="form.principal"/></el-form-item><el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option label="草稿" value="draft"/><el-option label="待审核" value="pending"/><el-option label="已审核" value="approved"/></el-select></el-form-item></template>
      </el-form>
      <template #footer><el-button @click="drawer=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存记录</el-button></template>
    </el-drawer>
  </section>
</template>
<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
const props=defineProps({inspectionType:{type:String,default:'initial'}}),route=useRoute(),router=useRouter(),auth=useAuthStore(),isInitial=computed(()=>props.inspectionType==='initial'),resource=computed(()=>isInitial.value?'initial-inspections':'periodic-inspections'),idField=computed(()=>isInitial.value?'initial_inspection_code':'periodic_inspection_code'),readonly=computed(()=>['viewer','reviewer'].includes(auth.role)||route.path.startsWith('/query/')||route.path.startsWith('/review/')),canDelete=computed(()=>auth.role==='admin'),rows=ref([]),total=ref(0),page=ref(1),size=ref(10),drawer=ref(false),saving=ref(false),editing=ref(null),form=reactive({}),filters=reactive({keyword:'',bridge_code:''})
const ratingLevels=[1,2,3,4,5].map(value=>({value:String(value),label:`${value} 类`}))
function statusName(value){return {draft:'草稿',pending:'待审核',approved:'已审核',completed:'已完成',archived:'已归档'}[value]||value||'未填写'}
function isLocked(row){return ['已完成','已审核','已取消'].includes(row.task_status)}
async function load(){const data=await http.get(`/${resource.value}`,{params:{page:page.value,size:size.value,keyword:filters.keyword,bridge_code:filters.bridge_code}});rows.value=data.records;total.value=data.total}
function startCreate(){if(auth.role==='inspector'){router.push(`/inspector/${props.inspectionType}-workbench`);return}openCreate()}
function openCreate(){editing.value=null;Object.keys(form).forEach(key=>delete form[key]);if(isInitial.value)Object.assign(form,{initial_inspection_code:'',bridge_code:'',inspection_date:'',inspection_org:'',inspectors:'',bridge_engineer:'',weather_temperature:'',status:'draft',effective_flag:1});else Object.assign(form,{periodic_inspection_code:'',bridge_code:'',inspection_date:'',last_inspection_date:'',rating_level_code:'',next_inspection_date:'',recorder:'',principal:'',status:'draft'});drawer.value=true}
function openEdit(row){if(auth.role==='inspector'&&row.task_id){router.push(`/inspector/${props.inspectionType}-workbench?taskId=${row.task_id}`);return}editing.value=row;Object.keys(form).forEach(key=>delete form[key]);Object.assign(form,row);drawer.value=true}
async function save(){saving.value=true;try{if(editing.value)await http.put(`/${resource.value}/${editing.value[idField.value]}`,form);else await http.post(`/${resource.value}`,form);ElMessage.success('检查记录保存成功');drawer.value=false;await load()}finally{saving.value=false}}
async function remove(row){await http.delete(`/${resource.value}/${row[idField.value]}`);ElMessage.success('检查记录已删除');await load()}
onMounted(load);watch([()=>props.inspectionType,()=>route.path],()=>{page.value=1;load()})
</script>
<style scoped>
.record-page{max-width:1600px;margin:auto}.record-heading{display:flex;justify-content:space-between;align-items:center;margin-bottom:18px}.record-heading span{color:#0f766e;font-size:12px;font-weight:700}.record-heading h2{margin:6px 0;font-size:24px}.record-heading p{margin:0;color:#64748b}.record-filters{display:flex;gap:10px;margin-bottom:14px}.record-filters .el-input{max-width:290px}.pager{display:flex;justify-content:flex-end;margin-top:14px}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:5px 16px}.locked-text{font-size:12px;color:#94a3b8}@media(max-width:700px){.record-heading{align-items:flex-start;flex-direction:column;gap:12px}.record-filters{flex-direction:column}.record-filters .el-input{max-width:none}.form-grid{grid-template-columns:1fr}}
</style>
