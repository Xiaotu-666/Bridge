<template>
  <section class="defect-page">
    <div class="defect-heading"><div><span>{{ isInitial?'INITIAL DEFECTS':'PERIODIC DEFECTS' }}</span><h2>{{ isInitial?'初始病害':'定期病害' }}</h2><p>按桥梁卡片集中管理病害，并保留检查记录来源。</p></div><el-tag :type="isInitial?'primary':'warning'">{{ total }} 条病害</el-tag></div>
    <div class="panel filters"><el-input v-model="filters.bridgeCode" clearable placeholder="桥梁编号" @keyup.enter="load"/><el-input v-model="filters.keyword" clearable placeholder="桥梁名称、病害名称或说明" @keyup.enter="load"/><el-button type="primary" @click="load">查询</el-button></div>
    <el-empty v-if="!groups.length" description="暂无符合条件的病害记录"/>
    <div class="bridge-card-grid">
      <article v-for="group in groups" :key="group.bridge_code" class="bridge-defect-card">
        <header><div><span>{{ group.bridge_type_name||'其他桥型' }}</span><h3>{{ group.bridge_name||group.bridge_code }}</h3><p>{{ group.bridge_code }} · {{ group.rows.length }} 条病害</p></div><el-button text type="primary" @click="router.push(`/bridges/${group.bridge_code}`)">查看桥梁卡片</el-button></header>
        <el-table :data="group.rows" border>
          <el-table-column prop="inspection_code" label="检查编号" width="175"/>
          <el-table-column prop="dictionary_defect_name" label="病害" min-width="130"><template #default="{row}">{{ row.dictionary_defect_name||row.defect_type||'未选择字典' }}</template></el-table-column>
          <el-table-column prop="part_name" label="部位" width="110"/>
          <el-table-column prop="defect_range" label="范围" min-width="115"/>
          <el-table-column prop="defect_degree_code" label="程度" width="85"><template #default="{row}">{{ degreeName(row.defect_degree_code) }}</template></el-table-column>
          <el-table-column prop="task_status" label="任务状态" width="105"><template #default="{row}"><el-tag size="small" :type="isLocked(row)?'info':'success'">{{ row.task_status||'未关联' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="150" fixed="right"><template #default="{row}"><el-button v-if="!isLocked(row)" text type="primary" @click="openEdit(row)">编辑</el-button><el-popconfirm v-if="!isLocked(row)" title="确认删除该病害记录？" @confirm="remove(row)"><template #reference><el-button text type="danger">删除</el-button></template></el-popconfirm><span v-if="isLocked(row)" class="locked-text">已锁定</span></template></el-table-column>
        </el-table>
      </article>
    </div>
    <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" layout="total, sizes, prev, pager, next" :total="total" :page-sizes="[10,20,30,50]" @change="load"/></div>

    <el-drawer v-model="drawer" title="编辑病害记录" size="620px">
      <div class="source-strip"><span>{{ editing?.bridge_code }} · {{ editing?.bridge_name }}</span><b>{{ editing?.inspection_code }}</b></div>
      <el-form label-position="top" class="form-grid">
        <el-form-item label="病害字典"><el-select v-model="form.defect_definition_code" clearable filterable style="width:100%" @change="applyDefinition"><el-option v-for="item in definitions" :key="item.defect_definition_code" :label="item.defect_name" :value="item.defect_definition_code"/></el-select></el-form-item>
        <el-form-item label="病害类型"><el-input v-model="form.defect_type"/></el-form-item>
        <el-form-item label="病害性质"><el-input v-model="form.defect_nature"/></el-form-item>
        <el-form-item label="病害程度"><el-select v-model="form.defect_degree_code" clearable style="width:100%"><el-option label="轻微" value="slight"/><el-option label="中等" value="medium"/><el-option label="严重" value="serious"/><el-option label="危险" value="danger"/></el-select></el-form-item>
        <el-form-item label="病害范围"><el-input v-model="form.defect_range"/></el-form-item>
        <el-form-item label="病害数量"><el-input v-model="form.defect_quantity"/></el-form-item>
        <el-form-item label="说明" class="full-row"><el-input v-model="form.description" type="textarea" :rows="4"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="drawer=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-drawer>
  </section>
</template>
<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
const props=defineProps({inspectionType:{type:String,default:'initial'}}),router=useRouter(),isInitial=computed(()=>props.inspectionType==='initial'),rows=ref([]),total=ref(0),page=ref(1),size=ref(20),drawer=ref(false),saving=ref(false),editing=ref(null),definitions=ref([]),form=reactive({}),filters=reactive({bridgeCode:'',keyword:''})
const groups=computed(()=>{const map=new Map();for(const row of rows.value){if(!map.has(row.bridge_code))map.set(row.bridge_code,{bridge_code:row.bridge_code,bridge_name:row.bridge_name,bridge_type_name:row.bridge_type_name,rows:[]});map.get(row.bridge_code).rows.push(row)}return [...map.values()]})
function isLocked(row){return ['已完成','已审核','已取消'].includes(row.task_status)}
function degreeName(value){return {slight:'轻微',medium:'中等',serious:'严重',danger:'危险'}[value]||value||'未填写'}
async function load(){const data=await http.get('/defect-results',{params:{page:page.value,size:size.value,inspectionType:props.inspectionType,scope:'managed',...filters}});rows.value=data.records;total.value=data.total}
async function loadDefinitions(){const data=await http.get('/defect-definitions',{params:{page:1,size:100}});definitions.value=data.records.filter(item=>item.inspection_scope==='both'||item.inspection_scope===props.inspectionType)}
function openEdit(row){editing.value=row;Object.keys(form).forEach(key=>delete form[key]);for(const key of ['defect_definition_code','defect_type','defect_nature','defect_range','defect_quantity','defect_degree_code','description'])form[key]=row[key]??'';drawer.value=true}
function applyDefinition(code){const item=definitions.value.find(x=>x.defect_definition_code===code);if(!item)return;form.defect_type=item.defect_name;form.defect_nature=item.defect_nature;form.defect_degree_code=item.default_degree_code;form.defect_range=form.defect_range||item.default_range;form.description=form.description||item.default_advice}
async function save(){saving.value=true;try{await http.put(`/defects/${editing.value.defect_id}`,form);ElMessage.success('病害记录已更新');drawer.value=false;await load()}finally{saving.value=false}}
async function remove(row){await http.delete(`/defects/${row.defect_id}`);ElMessage.success('病害记录已删除');await load()}
onMounted(async()=>{await Promise.all([loadDefinitions(),load()])});watch(()=>props.inspectionType,async()=>{page.value=1;await Promise.all([loadDefinitions(),load()])})
</script>
<style scoped>
.defect-page{max-width:1700px;margin:auto}.defect-heading{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:1px solid #dbe3ec;padding-bottom:15px;margin-bottom:14px}.defect-heading span{font-size:11px;font-weight:700;color:#0f766e}.defect-heading h2{margin:5px 0;font-size:24px}.defect-heading p{margin:0;color:#64748b}.filters{display:grid;grid-template-columns:180px minmax(260px,1fr) 80px;gap:10px;margin-bottom:14px}.bridge-card-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.bridge-defect-card{border:1px solid #dbe3ec;border-radius:6px;background:#fff;overflow:hidden}.bridge-defect-card header{display:flex;justify-content:space-between;align-items:flex-start;padding:15px 16px;border-bottom:1px solid #e2e8f0}.bridge-defect-card header span{font-size:11px;color:#0f766e;font-weight:700}.bridge-defect-card h3{margin:4px 0;font-size:17px}.bridge-defect-card p{margin:0;color:#64748b;font-size:12px}.locked-text{font-size:12px;color:#94a3b8}.pager{display:flex;justify-content:flex-end;margin-top:14px}.source-strip{display:flex;justify-content:space-between;padding:12px;background:#f8fafc;border:1px solid #e2e8f0;margin-bottom:15px}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:4px 16px}.full-row{grid-column:1/-1}@media(max-width:1100px){.bridge-card-grid{grid-template-columns:1fr}}@media(max-width:680px){.filters,.form-grid{grid-template-columns:1fr}.full-row{grid-column:auto}}
</style>
