<template>
  <section class="table-panel query-page">
    <div class="query-heading"><div><span>BRIDGE-LINKED QUERY</span><h2>检查结果查询</h2><p>初始检查和定期检查统一展示，并始终绑定到桥梁档案。</p></div><el-tag effect="plain">{{ total }} 条检查记录</el-tag></div>
    <div class="query-filters">
      <el-select v-model="filters.inspectionType" clearable placeholder="检查类型" @change="load"><el-option label="初始检查" value="initial"/><el-option label="定期检查" value="periodic"/></el-select>
      <el-input v-model="filters.bridgeCode" clearable placeholder="桥梁编号" @keyup.enter="load"/>
      <el-input v-model="filters.bridgeName" clearable placeholder="桥梁名称" @keyup.enter="load"/>
      <el-input v-model="filters.keyword" clearable placeholder="编号、任务或桥梁关键词" @keyup.enter="load"/>
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <el-table :data="rows" border height="590">
      <el-table-column prop="inspection_type" label="检查类型" width="110"><template #default="{row}"><el-tag :type="row.inspection_type==='initial'?'primary':'warning'">{{ row.inspection_type==='initial'?'初始检查':'定期检查' }}</el-tag></template></el-table-column>
      <el-table-column prop="inspection_code" label="检查编号" width="180"/>
      <el-table-column prop="task_id" label="任务编号" width="150"/>
      <el-table-column prop="bridge_code" label="桥梁编号" width="130"/>
      <el-table-column prop="bridge_name" label="桥梁名称" min-width="180"/>
      <el-table-column prop="bridge_type_name" label="桥型" min-width="150"/>
      <el-table-column prop="form_table_code" label="表号" width="90"/>
      <el-table-column prop="inspection_date" label="检查日期" width="125"/>
      <el-table-column prop="result_count" label="明细数" width="85"/>
      <el-table-column prop="defect_count" label="病害数" width="85"/>
      <el-table-column prop="status" label="状态" width="110"><template #default="{row}">{{ statusName(row.status) }}</template></el-table-column>
      <el-table-column label="操作" width="120" fixed="right"><template #default="{row}"><el-button text type="primary" @click="openBridge(row)">查看桥梁</el-button></template></el-table-column>
    </el-table>
    <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" layout="total, sizes, prev, pager, next" :total="total" :page-sizes="[10,20,50]" @change="load"/></div>
  </section>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
const router=useRouter(),rows=ref([]),total=ref(0),page=ref(1),size=ref(10),filters=reactive({inspectionType:'',bridgeCode:'',bridgeName:'',keyword:''})
async function load(){const data=await http.get('/inspection-results',{params:{page:page.value,size:size.value,...filters}});rows.value=data.records;total.value=data.total}
function openBridge(row){router.push(`/bridges/${row.bridge_code}`)}
function statusName(value){return {draft:'草稿',pending:'待审核',approved:'已审核',archived:'已归档',completed:'已完成'}[value]||value||'未填写'}
onMounted(load)
</script>
<style scoped>
.query-page{max-width:1800px;margin:auto}.query-heading{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:1px solid #dbe3ec;padding-bottom:15px;margin-bottom:14px}.query-heading span{font-size:12px;font-weight:700;color:#0f766e}.query-heading h2{margin:5px 0;font-size:23px}.query-heading p{margin:0;color:#64748b}.query-filters{display:grid;grid-template-columns:160px 170px 190px minmax(220px,1fr) 80px;gap:10px;margin-bottom:14px}.pager{display:flex;justify-content:flex-end;margin-top:14px}@media(max-width:900px){.query-filters{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:560px){.query-filters{grid-template-columns:1fr}}
</style>
