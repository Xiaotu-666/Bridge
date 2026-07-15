<template>
  <section class="table-panel task-page" :class="inspectionType">
    <div class="task-heading"><div><span>{{ inspectionType==='initial'?'表 B · 初始检查任务':'表 C · 定期检查任务' }}</span><h2>{{ inspectionType==='initial'?'初始检查任务':'定期检查任务' }}</h2><p>{{ inspectionType==='initial'?'编制桥梁首次全面检查的执行任务。':'安排桥梁周期性检查、部件检查和后续复核。' }}</p></div><el-tag :type="inspectionType==='initial'?'primary':'warning'">{{ inspectionType==='initial'?'初始检查':'定期检查' }}</el-tag></div>
    <div class="toolbar"><div class="toolbar-left"><el-input v-model="keyword" clearable placeholder="任务编号、桥梁编号或状态" style="width:280px" @keyup.enter="load"/><el-button @click="load">查询</el-button></div><el-button v-if="canPlan" type="primary" @click="openCreate">创建任务</el-button></div>

    <el-table :data="rows" border height="560">
      <el-table-column prop="task_id" label="任务编号" width="145"/>
      <el-table-column prop="bridge_code" label="桥梁编号" width="125"/>
      <el-table-column prop="inspection_type" label="检查类型" width="115"><template #default="{row}">{{ row.inspection_type==='initial'?'初始检查':'定期检查' }}</template></el-table-column>
      <el-table-column prop="inspection_level" label="等级" width="80"/>
      <el-table-column prop="plan_start_date" label="计划开始" width="120"/>
      <el-table-column prop="plan_end_date" label="计划结束" width="120"/>
      <el-table-column prop="task_status" label="状态" width="110"><template #default="{row}"><span :class="statusDot(row.task_status)"/>{{ row.task_status }}</template></el-table-column>
      <el-table-column prop="remarks" label="备注" min-width="180" show-overflow-tooltip/>
      <el-table-column label="操作" fixed="right" :width="canPlan||canReview?300:180">
        <template #default="{row}">
          <el-button v-if="canPlan" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="canPlan" text @click="assign(row)">分配</el-button>
          <el-button v-if="canExecute&&canAccept(row)" text type="primary" @click="action(row,'accept')">接受</el-button>
          <el-button v-if="canExecute&&canComplete(row)" text type="success" @click="action(row,'complete')">完成</el-button>
          <el-dropdown v-if="canPlan||canReview">
            <el-button text>更多</el-button>
            <template #dropdown><el-dropdown-menu><el-dropdown-item v-if="canReview" @click="action(row,'review')">审核通过</el-dropdown-item><el-dropdown-item v-if="canReview" @click="withReason(row,'reject','驳回原因')">审核驳回</el-dropdown-item><el-dropdown-item v-if="canPlan" @click="withReason(row,'cancel','取消原因')">取消任务</el-dropdown-item></el-dropdown-menu></template>
          </el-dropdown>
          <span v-if="canExecute&&!canAccept(row)&&!canComplete(row)" class="locked-text">任务已锁定</span>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" layout="total, sizes, prev, pager, next" :total="total" :page-sizes="[10,20,50]" @change="load"/></div>

    <el-drawer v-model="drawer" :title="editing?'编辑任务':'创建任务'" size="620px">
      <el-form label-position="top" class="form-grid">
        <el-form-item label="任务编号"><el-input v-model="form.task_id" :disabled="Boolean(editing)" placeholder="留空自动生成"/></el-form-item>
        <el-form-item label="桥梁编号"><el-input v-model="form.bridge_code"/></el-form-item>
        <el-form-item label="检查类型"><el-select v-model="form.inspection_type" style="width:100%"><el-option label="初始检查" value="initial"/><el-option label="定期检查" value="periodic"/></el-select></el-form-item>
        <el-form-item label="检查等级"><el-select v-model="form.inspection_level" style="width:100%"><el-option label="Ⅰ级" value="Ⅰ"/><el-option label="Ⅱ级" value="Ⅱ"/><el-option label="Ⅲ级" value="Ⅲ"/></el-select></el-form-item>
        <el-form-item label="计划开始"><el-date-picker v-model="form.plan_start_date" value-format="YYYY-MM-DD" type="date" style="width:100%"/></el-form-item>
        <el-form-item label="计划结束"><el-date-picker v-model="form.plan_end_date" value-format="YYYY-MM-DD" type="date" style="width:100%"/></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.task_status" style="width:100%"><el-option label="待分配" value="待分配"/><el-option label="进行中" value="进行中"/><el-option label="已完成" value="已完成"/><el-option label="已审核" value="已审核"/><el-option label="已取消" value="已取消"/></el-select></el-form-item>
        <el-form-item label="备注" class="full-row"><el-input v-model="form.remarks" type="textarea" :rows="4"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="drawer=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
const route=useRoute(),router=useRouter(),props=defineProps({inspectionType:{type:String,default:''}}),auth=useAuthStore()
const canPlan=computed(()=>['admin','engineer'].includes(auth.role)),canExecute=computed(()=>['admin','inspector'].includes(auth.role)),canReview=computed(()=>['admin','reviewer'].includes(auth.role)),inspectionType=computed(()=>props.inspectionType||(route.path.startsWith('/periodic/')?'periodic':'initial'))
const rows=ref([]),total=ref(0),page=ref(1),size=ref(10),keyword=ref(''),drawer=ref(false),saving=ref(false),editing=ref(null),form=reactive({})
async function load(){const data=await http.get('/tasks',{params:{page:page.value,size:size.value,keyword:keyword.value,inspection_type:inspectionType.value}});rows.value=data.records;total.value=data.total}
function openCreate(){editing.value=null;Object.assign(form,{task_id:'',bridge_code:'',inspection_type:inspectionType.value,inspection_level:'Ⅰ',task_status:'待分配',plan_start_date:'',plan_end_date:'',remarks:''});drawer.value=true}
function openEdit(row){editing.value=row;Object.assign(form,row);drawer.value=true}
async function save(){saving.value=true;try{if(editing.value)await http.put(`/tasks/${editing.value.task_id}`,form);else await http.post('/tasks',form);ElMessage.success('保存成功');drawer.value=false;await load()}finally{saving.value=false}}
async function assign(row){const {value}=await ElMessageBox.prompt('请输入检查员用户ID，多个用逗号分隔','分配检查人员',{inputValue:'3'});await http.post(`/tasks/${row.task_id}/assign`,{userIds:value.split(',').map(x=>x.trim()).filter(Boolean)});ElMessage.success('分配成功');await load()}
async function action(row,name){await http.post(`/tasks/${row.task_id}/${name}`,{});ElMessage.success(name==='accept'?'任务已接受':'操作成功');if(name==='accept'&&auth.role==='inspector'){await router.push(`/inspector/${inspectionType.value}-workbench?taskId=${row.task_id}`);return}await load()}
async function withReason(row,name,title){const {value}=await ElMessageBox.prompt(`请输入${title}`,title);await http.post(`/tasks/${row.task_id}/${name}`,{reason:value,opinion:value});ElMessage.success('操作成功');await load()}
function canAccept(row){return !['已完成','已审核','已取消'].includes(row.task_status)&&row.task_status!=='进行中'}
function canComplete(row){return row.task_status==='进行中'}
function statusDot(status){if(status==='已审核'||status==='已完成')return'ok-dot';if(status==='已取消')return'danger-dot';return'warn-dot'}
onMounted(load)
</script>
<style scoped>
.task-heading{display:flex;justify-content:space-between;align-items:center;padding:4px 0 16px;margin-bottom:14px;border-bottom:1px solid #dbe3ec}.task-heading span{font-size:11px;font-weight:700;color:#0f766e}.task-heading h2{margin:5px 0;font-size:23px}.task-heading p{margin:0;color:#64748b}.task-page.periodic .task-heading span{color:#c2410c}.task-page.periodic :deep(.el-table th){--el-table-header-bg-color:#fff7ed}.task-page.initial :deep(.el-table th){--el-table-header-bg-color:#eff6ff}.pager{display:flex;justify-content:flex-end;margin-top:14px}.locked-text{font-size:12px;color:#94a3b8}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:5px 16px}.full-row{grid-column:1/-1}@media(max-width:680px){.task-heading{align-items:flex-start;flex-direction:column;gap:10px}.form-grid{grid-template-columns:1fr}}
</style>
