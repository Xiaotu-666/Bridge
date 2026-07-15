<template>
  <section class="version-page">
    <header class="version-heading">
      <div><span>系统管理</span><h2>版本控制</h2></div>
      <div class="heading-actions"><el-button :icon="RefreshCw" circle title="刷新" @click="load"/><el-button v-if="!data.initialized" type="primary" :icon="GitBranch" :loading="acting" @click="initialize">初始化仓库</el-button><el-button v-else type="primary" :icon="GitCommitHorizontal" @click="dialog=true">创建系统版本</el-button></div>
    </header>

    <el-alert v-if="!data.initialized" title="Git版本仓库尚未初始化" type="warning" :closable="false" show-icon/>

    <section class="version-strip">
      <div><span>仓库状态</span><strong>{{ data.initialized ? '已初始化' : '未初始化' }}</strong></div>
      <div><span>当前分支</span><strong>{{ data.branch || '—' }}</strong></div>
      <div><span>最新版本</span><strong>{{ data.latest_commit?.short_hash || '暂无提交' }}</strong></div>
      <div><span>待提交文件</span><strong>{{ data.dirty_count || 0 }}</strong></div>
      <div class="path-cell"><span>仓库目录</span><strong>{{ data.repository_path || '—' }}</strong></div>
    </section>

    <section class="table-panel version-section">
      <div class="section-title"><div><span>GIT HISTORY</span><h3>系统版本记录</h3></div><el-tag effect="plain">{{ data.commits.length }} 个版本</el-tag></div>
      <el-table v-loading="loading" :data="data.commits" border height="330" empty-text="尚未创建系统版本">
        <el-table-column prop="short_hash" label="版本号" width="125"><template #default="{row}"><code>{{ row.short_hash }}</code></template></el-table-column>
        <el-table-column prop="message" label="版本说明" min-width="300"/>
        <el-table-column prop="author" label="提交人" width="190"/>
        <el-table-column prop="time" label="提交时间" width="230"/>
        <el-table-column prop="hash" label="完整提交哈希" min-width="330" show-overflow-tooltip/>
      </el-table>
    </section>

    <section class="table-panel version-section">
      <div class="section-title"><div><span>DATABASE SNAPSHOTS</span><h3>数据库备份</h3></div><el-tag type="success" effect="plain">{{ data.backups.length }} 份备份</el-tag></div>
      <el-table :data="data.backups" border height="350" empty-text="暂无数据库备份">
        <el-table-column prop="backup_id" label="备份编号" width="100"/>
        <el-table-column prop="file_name" label="备份文件" min-width="260" show-overflow-tooltip/>
        <el-table-column prop="version_message" label="版本说明" min-width="240" show-overflow-tooltip/>
        <el-table-column label="文件大小" width="110"><template #default="{row}">{{ formatBytes(row.file_size) }}</template></el-table-column>
        <el-table-column prop="git_branch" label="Git分支" width="105"/>
        <el-table-column label="Git版本" width="125"><template #default="{row}"><code>{{ row.git_commit_hash ? row.git_commit_hash.slice(0,8) : '—' }}</code></template></el-table-column>
        <el-table-column prop="execute_name" label="执行人员" width="120"/>
        <el-table-column label="备份状态" width="120"><template #default="{row}"><el-tag :type="row.backup_status==='成功'?'success':'danger'">{{ row.backup_status }}</el-tag></template></el-table-column>
        <el-table-column prop="create_time" label="创建时间" width="175"/>
        <el-table-column label="操作" width="100" fixed="right"><template #default="{row}"><el-button v-if="row.backup_status==='成功'" text type="primary" :icon="Download" @click="download(row)">下载</el-button></template></el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialog" title="创建系统版本" width="540px">
      <el-form label-position="top"><el-form-item label="版本说明"><el-input v-model="message" type="textarea" :rows="4" maxlength="300" show-word-limit/></el-form-item><div class="version-includes"><GitCommitHorizontal :size="18"/><span>项目代码</span><DatabaseBackup :size="18"/><span>数据库 SQL 备份</span></div></el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" :loading="acting" @click="createVersion">生成备份并提交</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DatabaseBackup, Download, GitBranch, GitCommitHorizontal, RefreshCw } from 'lucide-vue-next'
import http from '../api/http'

const data=reactive({initialized:false,repository_path:'',branch:'',dirty_count:0,changed_files:[],latest_commit:null,commits:[],backups:[]}),loading=ref(false),acting=ref(false),dialog=ref(false),message=ref('')
onMounted(load)
async function load(){loading.value=true;try{Object.assign(data,await http.get('/version-control'))}finally{loading.value=false}}
async function initialize(){acting.value=true;try{Object.assign(data,await http.post('/version-control/initialize'));ElMessage.success('Git版本仓库已初始化')}finally{acting.value=false}}
async function createVersion(){acting.value=true;try{const result=await http.post('/version-control/versions',{message:message.value});ElMessage.success(`系统版本 ${result.short_hash} 已创建`);dialog.value=false;message.value='';await load()}finally{acting.value=false}}
async function download(row){const response=await fetch(`/api/version-control/backups/${row.backup_id}/download`,{headers:{Authorization:`Bearer ${localStorage.getItem('bridge_token')}`}});if(!response.ok){ElMessage.error('数据库备份下载失败');return}const blob=await response.blob();const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=row.file_name;link.click();URL.revokeObjectURL(url)}
function formatBytes(value){const size=Number(value||0);if(size<1024)return`${size} B`;if(size<1048576)return`${(size/1024).toFixed(1)} KB`;return`${(size/1048576).toFixed(1)} MB`}
</script>

<style scoped>
.version-page{max-width:1800px;margin:auto}.version-heading{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:1px solid #dbe3ec;padding-bottom:15px;margin-bottom:14px}.version-heading span,.section-title span{font-size:12px;font-weight:700;color:#0f766e}.version-heading h2{margin:5px 0 0;font-size:23px}.heading-actions{display:flex;gap:10px}.version-strip{display:grid;grid-template-columns:repeat(4,minmax(130px,1fr)) minmax(280px,2fr);gap:1px;background:#dbe3ec;border:1px solid #dbe3ec;margin:14px 0}.version-strip>div{background:#fff;padding:14px 16px;min-width:0}.version-strip span{display:block;color:#64748b;font-size:12px;margin-bottom:5px}.version-strip strong{display:block;font-size:15px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.path-cell strong{font-family:Consolas,monospace;font-size:12px}.version-section{margin-top:14px}.section-title{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px}.section-title h3{font-size:16px;margin:3px 0 0}.version-includes{display:flex;align-items:center;gap:8px;padding:12px;border:1px solid #dbe3ec;background:#f8fafc}.version-includes span{margin-right:12px}code{font-family:Consolas,monospace;color:#0f766e}@media(max-width:1000px){.version-strip{grid-template-columns:repeat(2,minmax(0,1fr))}.path-cell{grid-column:1/-1}}@media(max-width:620px){.version-heading{flex-direction:column;gap:12px}.heading-actions{width:100%;justify-content:flex-end}.version-strip{grid-template-columns:1fr}}
</style>
