<template>
  <section class="version-page">
    <header class="version-heading">
      <div><span>系统管理</span><h2>版本控制与数据库备份</h2></div>
      <el-button :icon="RefreshCw" circle title="刷新" @click="load"/>
    </header>

    <section class="table-panel github-section">
      <div class="section-heading">
        <div><span>GITHUB VERSION CONTROL</span><h3>GitHub 版本控制</h3><p>以远端仓库更新为准，不展示本地分支和未提交文件。</p></div>
        <div class="section-actions"><a class="repo-link" :href="repositoryUrl" target="_blank" rel="noopener"><Github :size="17"/>Xiaotu-666/Bridge</a><el-button type="primary" :icon="RefreshCw" :loading="checking" @click="checkGithub">检查 GitHub 更新</el-button></div>
      </div>

      <section class="version-strip">
        <div><span>GitHub 仓库</span><strong>Xiaotu-666/Bridge</strong></div>
        <div><span>当前系统版本</span><strong class="version-number">{{ currentVersion }}</strong></div>
        <div><span>GitHub 最新版本</span><strong class="version-number">{{ availableVersion }}</strong></div>
        <div><span>更新状态</span><strong>{{ updateStatus }}</strong></div>
      </section>

      <el-alert v-if="githubUpdate" class="update-alert" :type="githubUpdate.update_available?'warning':'success'" :closable="false" show-icon>
        <template #title>{{ githubUpdate.message }}</template>
        <template #default><span v-if="githubUpdate.remote_message">GitHub 最新提交：{{ githubUpdate.remote_message }} · {{ formatTime(githubUpdate.remote_time) }}</span><el-button v-if="githubUpdate.update_available" type="primary" size="small" :loading="updating" @click="applyUpdate">更新到 {{ githubUpdate.available_version }}</el-button></template>
      </el-alert>

      <div class="history-heading"><div><span>SYSTEM RELEASES</span><h3>系统版本更新记录</h3></div><el-tag effect="plain">{{ versionRows.length }} 个版本</el-tag></div>
      <el-table v-loading="loading" :data="versionRows" border empty-text="暂无系统版本记录">
        <el-table-column prop="version_no" label="版本号" width="120"><template #default="{row}"><strong class="version-number">{{ row.version_no }}</strong></template></el-table-column>
        <el-table-column label="更新说明" min-width="260"><template #default="{row}">{{ versionSummary(row) }}</template></el-table-column>
        <el-table-column label="GitHub 提交" width="145"><template #default="{row}"><code>{{ row.git_commit ? row.git_commit.slice(0,8) : '首次检查后记录' }}</code></template></el-table-column>
        <el-table-column label="更新时间" width="190"><template #default="{row}">{{ formatTime(row.build_time || row.create_time) }}</template></el-table-column>
        <el-table-column label="操作" width="130" fixed="right"><template #default="{row}"><el-tag v-if="row.version_no===currentVersion" type="success" effect="plain">当前版本</el-tag><el-button v-else-if="row.git_commit" text type="warning" :icon="History" :loading="rollingBack===row.version_no" @click="rollback(row)">版本回溯</el-button><span v-else class="muted">暂无恢复点</span></template></el-table-column>
      </el-table>
    </section>

    <section class="table-panel database-section">
      <div class="section-heading">
        <div><span>DATABASE SNAPSHOTS</span><h3>数据库备份</h3><p>数据库备份与 GitHub 版本控制相互独立。</p></div>
        <div class="section-actions"><el-tag type="success" effect="plain">{{ data.backups.length }} 份备份</el-tag><el-button type="success" :icon="DatabaseBackup" @click="backupDialog=true">创建数据库备份</el-button></div>
      </div>
      <el-table :data="data.backups" border empty-text="暂无数据库备份">
        <el-table-column label="备份时间" width="180"><template #default="{row}">{{ formatTime(row.create_time) }}</template></el-table-column>
        <el-table-column prop="backup_id" label="备份编号" width="100"/>
        <el-table-column prop="file_name" label="备份文件" min-width="260" show-overflow-tooltip/>
        <el-table-column prop="version_message" label="备份说明" min-width="240" show-overflow-tooltip/>
        <el-table-column label="文件大小" width="110"><template #default="{row}">{{ formatBytes(row.file_size) }}</template></el-table-column>
        <el-table-column prop="execute_name" label="执行人员" width="120"/>
        <el-table-column label="备份状态" width="110"><template #default="{row}"><el-tag :type="row.backup_status==='成功'?'success':'danger'">{{ row.backup_status }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="155" fixed="right"><template #default="{row}"><el-button v-if="row.backup_status==='成功'" text type="primary" :icon="Download" @click="download(row)">下载</el-button><el-popconfirm title="删除后无法恢复该数据库备份，确认删除？" confirm-button-text="删除" cancel-button-text="取消" @confirm="removeBackup(row)"><template #reference><el-button text type="danger" :icon="Trash2" :loading="deletingBackup===row.backup_id">删除</el-button></template></el-popconfirm></template></el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="backupDialog" title="创建数据库备份" width="520px">
      <el-form label-position="top"><el-form-item label="备份说明"><el-input v-model="backupMessage" type="textarea" :rows="4" maxlength="300" show-word-limit placeholder="例如：定期检查数据导入前备份"/></el-form-item><div class="backup-note"><DatabaseBackup :size="20"/><span>生成独立 SQL 备份文件，不会创建 Git 提交。</span></div></el-form>
      <template #footer><el-button @click="backupDialog=false">取消</el-button><el-button type="success" :loading="backingUp" @click="createBackup">创建备份</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DatabaseBackup, Download, Github, History, RefreshCw, Trash2 } from 'lucide-vue-next'
import http from '../api/http'

const data=reactive({initialized:false,remote_url:'',current_version:'V1.0',system_versions:[],backups:[]})
const loading=ref(false),checking=ref(false),updating=ref(false),rollingBack=ref(''),backupDialog=ref(false),backingUp=ref(false),backupMessage=ref(''),deletingBackup=ref(null),githubUpdate=ref(null)
const repositoryUrl=computed(()=>canonicalRepository(data.remote_url))
const currentVersion=computed(()=>normalizeVersion(data.current_version||'V1.0'))
const availableVersion=computed(()=>normalizeVersion(githubUpdate.value?.available_version||currentVersion.value))
const updateStatus=computed(()=>!githubUpdate.value?'尚未检查':githubUpdate.value.update_available?`发现 ${availableVersion.value} 更新`:'已是最新版本')
const versionRows=computed(()=>{const seen=new Set();return(data.system_versions||[]).map(row=>({...row,version_no:normalizeVersion(row.version_no)})).filter(row=>{if(seen.has(row.version_no))return false;seen.add(row.version_no);return true})})
onMounted(load)
async function load(){loading.value=true;try{Object.assign(data,await http.get('/version-control'))}finally{loading.value=false}}
async function checkGithub(){checking.value=true;try{githubUpdate.value=await http.post('/version-control/github/check');ElMessage[githubUpdate.value.update_available?'warning':'success'](githubUpdate.value.message);await load()}finally{checking.value=false}}
async function applyUpdate(){await ElMessageBox.confirm(`系统将更新到 ${availableVersion.value}。执行前会验证代码目录、创建安全恢复分支并备份数据库。`,'确认系统更新',{type:'warning',confirmButtonText:`更新到 ${availableVersion.value}`,cancelButtonText:'取消'});updating.value=true;try{const result=await http.post('/version-control/github/update');ElMessage.success(result.message);githubUpdate.value=result;await load()}finally{updating.value=false}}
async function rollback(row){await ElMessageBox.confirm(`系统将回溯到 ${row.version_no} 的业务代码。执行前会创建安全恢复分支并备份数据库，GitHub 更新与版本控制功能将保持可用。`,'确认版本回溯',{type:'warning',confirmButtonText:`回溯到 ${row.version_no}`,cancelButtonText:'取消'});rollingBack.value=row.version_no;try{const result=await http.post(`/version-control/versions/${encodeURIComponent(row.version_no)}/rollback`);ElMessage.success(result.message);githubUpdate.value=null;await load()}finally{rollingBack.value=''}}
async function createBackup(){backingUp.value=true;try{await http.post('/version-control/backups',{message:backupMessage.value});ElMessage.success('数据库备份已创建');backupDialog.value=false;backupMessage.value='';await load()}finally{backingUp.value=false}}
async function removeBackup(row){deletingBackup.value=row.backup_id;try{await http.delete(`/version-control/backups/${row.backup_id}`);ElMessage.success('数据库备份已删除');await load()}finally{deletingBackup.value=null}}
async function download(row){const response=await fetch(`/api/version-control/backups/${row.backup_id}/download`,{headers:{Authorization:`Bearer ${localStorage.getItem('bridge_token')}`}});if(!response.ok){ElMessage.error('数据库备份下载失败');return}const blob=await response.blob();const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=row.file_name;link.click();URL.revokeObjectURL(url)}
function canonicalRepository(value){const remote=value||'https://github.com/Xiaotu-666/Bridge';return remote.endsWith('.git')?remote.slice(0,-4):remote}
function normalizeVersion(value){const match=String(value||'').match(/(\d+)\.(\d+)/);return match?`V${match[1]}.${match[2]}`:'V1.0'}
function versionSummary(row){return row.version_no==='V1.0'?'系统初始版本':'通过 GitHub 更新的系统版本'}
function formatTime(value){if(!value)return'—';return String(value).replace('T',' ').replace(/([+-]\d{2}:\d{2}|Z)$/,'').slice(0,19)}
function formatBytes(value){const size=Number(value||0);if(size<1024)return`${size} B`;if(size<1048576)return`${(size/1024).toFixed(1)} KB`;return`${(size/1048576).toFixed(1)} MB`}
</script>

<style scoped>
.version-page{max-width:1800px;margin:auto}.version-heading{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:1px solid #dbe3ec;padding-bottom:15px;margin-bottom:14px}.version-heading span,.section-heading span,.history-heading span{font-size:12px;font-weight:700;color:#0f766e;letter-spacing:.04em}.version-heading h2{margin:5px 0 0;font-size:23px}.github-section,.database-section{padding:18px;border-radius:12px}.database-section{margin-top:18px}.section-heading,.history-heading{display:flex;align-items:flex-start;justify-content:space-between;gap:18px}.section-heading h3,.history-heading h3{margin:4px 0 0;font-size:18px}.section-heading p{margin:6px 0 0;color:#64748b;font-size:13px}.section-actions{display:flex;align-items:center;justify-content:flex-end;gap:9px;flex-wrap:wrap}.repo-link{display:inline-flex;align-items:center;gap:7px;padding:7px 11px;color:#0f766e;background:#f0fdfa;border:1px solid #99f6e4;border-radius:7px;text-decoration:none;font-weight:700;font-size:13px}.repo-link:hover{background:#ccfbf1}.version-strip{display:grid;grid-template-columns:1.4fr repeat(3,minmax(130px,1fr));gap:1px;margin:16px 0;background:#dbe3ec;border:1px solid #dbe3ec}.version-strip>div{min-width:0;padding:14px 16px;background:#fff}.version-strip span{display:block;margin-bottom:5px;color:#64748b;font-size:12px}.version-strip strong{display:block;overflow:hidden;color:#1e293b;font-size:15px;text-overflow:ellipsis;white-space:nowrap}.version-number{color:#0f766e!important;font-weight:800}.update-alert{margin:14px 0}.update-alert :deep(.el-alert__content){width:100%}.update-alert :deep(.el-alert__description){display:flex;align-items:center;justify-content:space-between;gap:12px}.history-heading{align-items:center;margin:18px 0 11px;padding-top:16px;border-top:1px solid #e2e8f0}.database-section :deep(.el-table){margin-top:14px}.database-section :deep(.el-table__cell){padding:10px 0}.backup-note{display:flex;align-items:center;gap:9px;padding:12px;color:#0f766e;background:#f0fdfa;border:1px solid #99f6e4;border-radius:8px}.muted{color:#94a3b8;font-size:12px}code{font-family:Consolas,monospace;color:#0f766e}@media(max-width:1000px){.section-heading{flex-direction:column}.section-actions{width:100%;justify-content:flex-start}.version-strip{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:620px){.version-heading{flex-direction:column;gap:12px}.version-strip{grid-template-columns:1fr}.section-actions{align-items:stretch;flex-direction:column}.repo-link{justify-content:center}.update-alert :deep(.el-alert__description){align-items:flex-start;flex-direction:column}}
</style>
