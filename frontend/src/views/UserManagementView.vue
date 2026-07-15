<template>
  <section class="table-panel system-page">
    <header class="system-heading">
      <div><span>系统管理</span><h2>用户管理</h2></div>
      <el-button type="primary" :icon="UserPlus" @click="openCreate">新增用户</el-button>
    </header>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="keyword" clearable placeholder="姓名、账号、部门或电话" style="width:300px" @keyup.enter="load" />
        <el-button :icon="Search" @click="load">查询</el-button>
      </div>
      <el-button :icon="RefreshCw" circle title="刷新" @click="load" />
    </div>

    <el-table v-loading="loading" :data="rows" border height="600">
      <el-table-column prop="user_id" label="用户编号" width="105" />
      <el-table-column prop="user_name" label="姓名" width="130" />
      <el-table-column prop="login_account" label="登录账号" width="140" />
      <el-table-column prop="role_name" label="角色" width="130"><template #default="{row}"><el-tag effect="plain">{{ row.role_name }}</el-tag></template></el-table-column>
      <el-table-column prop="department" label="部门" min-width="150" />
      <el-table-column prop="phone" label="联系电话" width="145" />
      <el-table-column prop="email" label="电子邮箱" min-width="190" />
      <el-table-column label="启用状态" width="105"><template #default="{row}"><el-tag :type="row.user_status ? 'success' : 'info'">{{ row.user_status ? '已启用' : '已停用' }}</el-tag></template></el-table-column>
      <el-table-column label="首次改密" width="100"><template #default="{row}">{{ row.force_pwd_change ? '需要' : '不需要' }}</template></el-table-column>
      <el-table-column prop="last_login_time" label="最后登录时间" width="175" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{row}">
          <el-button text type="primary" :icon="Pencil" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm v-if="String(row.user_id) !== String(auth.user?.userId)" title="确认删除该用户？" @confirm="remove(row)">
            <template #reference><el-button text type="danger" :icon="Trash2">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load" /></div>

    <el-drawer v-model="drawer" :title="editing ? `编辑用户 · ${editing.user_id}` : '新增用户'" size="620px">
      <el-form label-position="top" class="user-form">
        <el-form-item label="姓名" required><el-input v-model="form.user_name" /></el-form-item>
        <el-form-item label="登录账号" required><el-input v-model="form.login_account" /></el-form-item>
        <el-form-item label="角色" required><el-select v-model="form.role_id" filterable style="width:100%"><el-option v-for="role in roles" :key="role.role_id" :label="`${role.role_name} · ${role.role_desc}`" :value="role.role_id" /></el-select></el-form-item>
        <el-form-item :label="editing ? '新密码（选填）' : '初始密码（选填）'"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item label="部门"><el-input v-model="form.department" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="电子邮箱" class="full"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="启用状态"><el-select v-model="form.user_status" style="width:100%"><el-option label="启用" :value="1" /><el-option label="停用" :value="0" /></el-select></el-form-item>
        <el-form-item label="首次登录修改密码"><el-switch v-model="form.force_pwd_change" :active-value="1" :inactive-value="0" inline-prompt active-text="是" inactive-text="否" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="drawer=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-drawer>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Pencil, RefreshCw, Search, Trash2, UserPlus } from 'lucide-vue-next'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth=useAuthStore(),rows=ref([]),roles=ref([]),total=ref(0),page=ref(1),size=ref(10),keyword=ref(''),loading=ref(false),saving=ref(false),drawer=ref(false),editing=ref(null)
const form=reactive({user_name:'',login_account:'',password:'',role_id:null,department:'',phone:'',email:'',user_status:1,force_pwd_change:1})

onMounted(async()=>{await loadRoles();await load()})

async function load(){loading.value=true;try{const data=await http.get('/users',{params:{page:page.value,size:size.value,keyword:keyword.value}});rows.value=data.records;total.value=data.total}finally{loading.value=false}}
async function loadRoles(){const data=await http.get('/roles',{params:{page:1,size:100}});roles.value=data.records}
function reset(){Object.assign(form,{user_name:'',login_account:'',password:'',role_id:roles.value[0]?.role_id||null,department:'',phone:'',email:'',user_status:1,force_pwd_change:1})}
function openCreate(){editing.value=null;reset();drawer.value=true}
function openEdit(row){editing.value=row;Object.assign(form,{user_name:row.user_name,login_account:row.login_account,password:'',role_id:row.role_id,department:row.department||'',phone:row.phone||'',email:row.email||'',user_status:Number(row.user_status),force_pwd_change:Number(row.force_pwd_change)});drawer.value=true}
async function save(){if(!form.user_name.trim()||!form.login_account.trim()||!form.role_id){ElMessage.warning('请填写姓名、登录账号并选择角色');return}const payload={...form};if(!payload.password)delete payload.password;saving.value=true;try{if(editing.value)await http.put(`/users/${editing.value.user_id}`,payload);else await http.post('/users',payload);ElMessage.success('用户信息已保存');drawer.value=false;await load()}finally{saving.value=false}}
async function remove(row){await http.delete(`/users/${row.user_id}`);ElMessage.success('用户已删除');await load()}
</script>

<style scoped>
.system-page{max-width:1800px;margin:auto}.system-heading{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:1px solid #dbe3ec;padding-bottom:15px;margin-bottom:14px}.system-heading span{font-size:12px;font-weight:700;color:#0f766e}.system-heading h2{margin:5px 0 0;font-size:23px}.pager{display:flex;justify-content:flex-end;margin-top:14px}.user-form{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 15px}.user-form .full{grid-column:1/-1}@media(max-width:680px){.user-form{grid-template-columns:1fr}.user-form .full{grid-column:auto}.system-heading{align-items:stretch;flex-direction:column;gap:12px}}
</style>
