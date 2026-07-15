<template>
  <div>
    <section class="role-hero"><div><span>{{ eyebrow }}</span><h2>{{ heading }}</h2><p>{{ description }}</p></div><el-button type="primary" @click="$router.push(primaryPath)">{{ primaryLabel }}</el-button></section>
    <div class="page-grid"><div v-for="metric in metrics" :key="metric.key" class="metric-card"><span>{{ metric.label }}</span><strong>{{ data[metric.key] || 0 }}</strong></div></div>
    <div v-if="roleCode==='inspector'" class="workbench-links"><el-button type="primary" @click="$router.push('/inspector/initial-workbench')">进入初始检查工作台</el-button><el-button type="warning" plain @click="$router.push('/inspector/periodic-workbench')">进入定期检查工作台</el-button></div>
    <template v-if="roleCode==='admin'">
      <div class="chart-grid">
        <section class="panel"><div class="panel-heading"><strong>启用用户角色分布</strong><span>{{ data.users || 0 }} 人</span></div><DashboardChart :option="roleOption"/></section>
        <section class="panel"><div class="panel-heading"><strong>在册桥梁类型分布</strong><span>{{ data.bridges || 0 }} 座</span></div><DashboardChart :option="bridgeOption"/></section>
      </div>
      <section class="panel trend-panel"><div class="panel-heading"><strong>定期检查完成与待检趋势</strong><el-segmented v-model="months" :options="monthOptions" @change="load"/></div><DashboardChart :option="trendOption"/></section>
    </template>
    <div class="task-grid">
      <section v-for="group in taskGroups" :key="group.title" class="panel task-panel"><div class="panel-heading"><strong>{{ group.title }}</strong><span>{{ group.rows.length }} 条</span></div><el-table :data="group.rows" height="265"><el-table-column prop="task_id" label="任务编号" width="145"/><el-table-column prop="bridge_code" label="桥梁编号" width="125"/><el-table-column prop="task_status" label="状态"/><el-table-column prop="plan_end_date" label="计划完成" width="120"/></el-table></section>
    </div>
    <section class="panel recent-panel"><div class="panel-heading"><strong>{{ tableTitle }}</strong><span>全系统近期任务</span></div><el-table :data="data.recentTasks || []" height="300"><el-table-column prop="task_id" label="任务编号" width="150"/><el-table-column prop="bridge_code" label="桥梁编号" width="140"/><el-table-column prop="inspection_type" label="检查类型"><template #default="{row}">{{ typeName(row.inspection_type) }}</template></el-table-column><el-table-column prop="task_status" label="任务状态"/><el-table-column prop="plan_end_date" label="计划完成" width="140"/></el-table></section>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from '../api/http'
import DashboardChart from '../components/DashboardChart.vue'
const props=defineProps({roleCode:String,eyebrow:String,heading:String,description:String,primaryPath:String,primaryLabel:String,tableTitle:String,metrics:Array})
const data=reactive({}),months=ref(12),monthOptions=[{label:'6个月',value:6},{label:'12个月',value:12},{label:'24个月',value:24}]
const colors=['#0f766e','#2563eb','#d97706','#dc2626','#7c3aed','#0891b2','#64748b']
const pie=source=>({color:colors,tooltip:{trigger:'item'},legend:{bottom:0,type:'scroll'},series:[{type:'pie',radius:['42%','70%'],center:['50%','44%'],avoidLabelOverlap:true,label:{formatter:'{b}\n{c} ({d}%)'},data:source||[]}]})
const roleOption=computed(()=>pie(data.roleDistribution)),bridgeOption=computed(()=>pie(data.bridgeTypeDistribution))
const taskGroups=computed(()=>[{title:'近期初始检查任务',rows:data.recentInitialTasks||[]},{title:'近期定期检查任务',rows:data.recentPeriodicTasks||[]}])
const trendOption=computed(()=>{const rows=data.periodicTrend||[];return{color:['#0f766e','#dc2626'],tooltip:{trigger:'axis'},legend:{top:4,data:['已完成定检','到期未完成']},grid:{left:46,right:24,top:45,bottom:34},xAxis:{type:'category',data:rows.map(x=>x.month),axisLabel:{rotate:rows.length>12?40:0}},yAxis:{type:'value',name:'检查次数',minInterval:1},series:[{name:'已完成定检',type:'line',smooth:true,symbolSize:8,data:rows.map(x=>x.completed),areaStyle:{opacity:.06}},{name:'到期未完成',type:'line',smooth:true,symbolSize:8,data:rows.map(x=>x.pending)}]}})
const typeName=value=>value==='initial'?'初始检查':'定期检查'
async function load(){Object.assign(data,await http.get('/dashboard/current',{params:{months:months.value}}))}
onMounted(load)
</script>
<style scoped>
.chart-grid,.task-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px;margin-top:14px}.workbench-links{display:flex;gap:12px;margin-top:14px}.panel-heading{display:flex;align-items:center;justify-content:space-between;min-height:34px}.panel-heading span{font-size:12px;color:#64748b}.trend-panel,.recent-panel{margin-top:14px}.task-panel{min-height:330px}.task-panel :deep(.el-table){margin-top:10px}@media(max-width:1000px){.chart-grid,.task-grid{grid-template-columns:1fr}}@media(max-width:680px){.workbench-links{flex-direction:column;align-items:stretch}}
</style>
