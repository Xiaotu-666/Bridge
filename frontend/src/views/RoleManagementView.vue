<template>
  <section class="table-panel system-page">
    <header class="system-heading"><div><span>系统管理</span><h2>角色与权限</h2></div><el-button type="primary" :icon="ShieldPlus" @click="openCreate">新增角色</el-button></header>
    <div class="toolbar"><div class="toolbar-left"><el-input v-model="keyword" clearable placeholder="角色编码或角色名称" style="width:280px" @keyup.enter="load"/><el-button :icon="Search" @click="load">查询</el-button></div><el-button :icon="RefreshCw" circle title="刷新" @click="load"/></div>
    <el-table v-loading="loading" :data="rows" border height="600">
      <el-table-column prop="role_id" label="角色编号" width="100"/>
      <el-table-column prop="role_code" label="角色编码" width="140"/>
      <el-table-column prop="role_name" label="角色名称" width="140"/>
      <el-table-column prop="role_desc" label="角色说明" min-width="220"/>
      <el-table-column label="权限集合" min-width="420"><template #default="{row}"><div class="permission-tags"><el-tag v-for="permission in parsePermissions(row.permission_set).slice(0,5)" :key="permission" size="small" effect="plain">{{ permissionName(permission) }}</el-tag><el-tag v-if="parsePermissions(row.permission_set).length>5" size="small" type="info">+{{ parsePermissions(row.permission_set).length-5 }}</el-tag></div></template></el-table-column>
      <el-table-column prop="user_count" label="用户数" width="90"/>
      <el-table-column label="操作" width="145" fixed="right"><template #default="{row}"><el-button text type="primary" :icon="Pencil" @click="openEdit(row)">编辑</el-button><el-popconfirm title="确认删除该角色？" @confirm="remove(row)"><template #reference><el-button text type="danger" :icon="Trash2">删除</el-button></template></el-popconfirm></template></el-table-column>
    </el-table>
    <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load"/></div>

    <el-drawer v-model="drawer" :title="editing ? `编辑角色 · ${editing.role_id}` : '新增角色'" size="620px">
      <el-form label-position="top" class="role-form">
        <el-form-item label="角色编码" required><el-input v-model="form.role_code" :disabled="Boolean(editing)"/></el-form-item>
        <el-form-item label="角色名称" required><el-input v-model="form.role_name"/></el-form-item>
        <el-form-item label="角色说明" required class="full"><el-select v-model="form.role_desc" style="width:100%" @change="applyTemplate"><el-option v-for="item in templates" :key="item.description" :label="item.description" :value="item.description"/></el-select></el-form-item>
        <el-form-item label="自动生成的权限集合" class="full"><div class="permission-preview"><el-tag v-for="permission in previewPermissions" :key="permission" effect="plain">{{ permissionName(permission) }}</el-tag></div></el-form-item>
      </el-form>
      <template #footer><el-button @click="drawer=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Pencil, RefreshCw, Search, ShieldPlus, Trash2 } from 'lucide-vue-next'
import http from '../api/http'

const rows=ref([]),templates=ref([]),total=ref(0),page=ref(1),size=ref(10),keyword=ref(''),loading=ref(false),saving=ref(false),drawer=ref(false),editing=ref(null)
const form=reactive({role_code:'',role_name:'',role_desc:''})
const previewPermissions=computed(()=>templates.value.find(item=>item.description===form.role_desc)?.permissions||[])
const permissionLabels={'*':'全部权限','bridge-view':'查看桥梁','bridge-create':'新增桥梁','bridge-edit':'编辑桥梁','matrix-view':'查看矩阵','task-view':'查看任务','task-create':'新建任务','task-edit':'编辑任务','task-accept':'接受任务','task-review':'审核任务','initial-view':'查看初检','initial-edit':'填写初检','initial-review':'审核初检','periodic-view':'查看定检','periodic-edit':'填写定检','periodic-review':'审核定检','defect-view':'查看病害','defect-edit':'编辑病害','attachment-upload':'上传附件','report-view':'查看报告','report-create':'生成报告','report-review':'审核报告','report-export':'导出报告','statistics-view':'查看统计'}

onMounted(async()=>{await loadTemplates();await load()})
async function load(){loading.value=true;try{const data=await http.get('/roles',{params:{page:page.value,size:size.value,keyword:keyword.value}});rows.value=data.records;total.value=data.total}finally{loading.value=false}}
async function loadTemplates(){templates.value=await http.get('/system/role-templates')}
function openCreate(){editing.value=null;Object.assign(form,{role_code:'',role_name:'',role_desc:''});drawer.value=true}
function openEdit(row){editing.value=row;Object.assign(form,{role_code:row.role_code,role_name:row.role_name,role_desc:row.role_desc});drawer.value=true}
function applyTemplate(){const selected=templates.value.find(item=>item.description===form.role_desc);if(selected&&!editing.value){form.role_code=form.role_code||selected.code;form.role_name=form.role_name||selected.name}}
async function save(){if(!form.role_code.trim()||!form.role_name.trim()||!form.role_desc){ElMessage.warning('请填写角色编码、角色名称并选择角色说明');return}saving.value=true;try{if(editing.value)await http.put(`/roles/${editing.value.role_id}`,form);else await http.post('/roles',form);ElMessage.success('角色与权限已保存');drawer.value=false;await load()}finally{saving.value=false}}
async function remove(row){if(Number(row.user_count)>0){ElMessage.warning('该角色仍有关联用户，不能删除');return}await http.delete(`/roles/${row.role_id}`);ElMessage.success('角色已删除');await load()}
function parsePermissions(value){if(Array.isArray(value))return value;try{return JSON.parse(value||'[]')}catch{return[]}}
function permissionName(value){return permissionLabels[value]||value}
</script>

<style scoped>
.system-page{max-width:1800px;margin:auto}.system-heading{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:1px solid #dbe3ec;padding-bottom:15px;margin-bottom:14px}.system-heading span{font-size:12px;font-weight:700;color:#0f766e}.system-heading h2{margin:5px 0 0;font-size:23px}.permission-tags,.permission-preview{display:flex;align-items:center;gap:6px;flex-wrap:wrap}.permission-preview{width:100%;min-height:70px;padding:12px;border:1px solid #dbe3ec;background:#f8fafc}.pager{display:flex;justify-content:flex-end;margin-top:14px}.role-form{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 15px}.role-form .full{grid-column:1/-1}@media(max-width:680px){.role-form{grid-template-columns:1fr}.role-form .full{grid-column:auto}.system-heading{align-items:stretch;flex-direction:column;gap:12px}}
</style>
