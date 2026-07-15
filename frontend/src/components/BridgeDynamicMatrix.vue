<template>
  <div class="dynamic-sections">
    <section v-for="(group,index) in groups" :key="group.key" class="component-section">
      <div class="section-heading">
        <span>D-{{ index + 1 }}</span>
        <div>
          <h3>{{ group.title }}</h3>
          <p>{{ group.subtitle }}</p>
        </div>
        <b>{{ group.instances.length }} 条实例</b>
      </div>
      <div class="instance-table-wrap">
        <table class="instance-table">
          <thead><tr><th>序号</th><th>部件编号</th><th>部件名称</th><th>所在位置</th><th>材料类型</th><th>尺寸规格</th><th>数量</th><th>索力/内力</th><th>高程/变位</th><th>备注</th></tr></thead>
          <tbody><tr v-for="(item,rowIndex) in group.instances" :key="item.bridge_component_id"><td>{{ rowIndex + 1 }}</td><td><strong>{{ item.component_serial || '未编号' }}</strong></td><td>{{ item.component_name || item.component_code }}</td><td>{{ value(item.location_desc) }}</td><td>{{ value(item.material_type) }}</td><td>{{ value(item.dimension_spec) }}</td><td>{{ item.quantity ?? 1 }}</td><td>{{ value(item.force_value) }}</td><td>{{ value(item.elevation_displacement) }}</td><td>{{ value(item.remark) }}</td></tr></tbody>
        </table>
      </div>
    </section>
  </div>
</template>
<script setup>
import { computed } from 'vue'
const props=defineProps({components:{type:Array,default:()=>[]}})
const groups=computed(()=>{
  const result=new Map()
  for(const item of props.components){
    const name=item.component_name||item.component_code||'其他部件'
    const isPierFoundation=item.component_code==='C010'||/桥墩|桥桩|墩柱|承台|桩基|扩大基础|桥梁基础/.test(`${name}${item.component_serial||''}${item.location_desc||''}`)
    const key=isPierFoundation?'pier-foundation':`${item.part_code||'other'}|${item.component_code||name}`
    if(!result.has(key)){
      result.set(key,{key,priority:isPierFoundation?0:1,title:isPierFoundation?'桥墩、桥桩与基础实例':`${item.part_name||'其他'} · ${name}`,subtitle:isPierFoundation?'每个实际桥墩、桥桩或基础作为一条独立记录管理':`该桥梁实际存在的${name}逐条记录，不使用横向动态列`,instances:[]})
    }
    result.get(key).instances.push(item)
  }
  return[...result.values()].sort((left,right)=>left.priority-right.priority||left.key.localeCompare(right.key,'zh-CN'))
})
const value=value=>value===null||value===undefined||value===''?'—':value
</script>
<style scoped>
.dynamic-sections{padding:0 16px 18px}.component-section{margin-top:16px;border:1px solid #d4dde7;border-radius:8px;overflow:hidden;background:#fff}.section-heading{display:grid;grid-template-columns:58px 1fr auto;align-items:center;gap:12px;padding:13px 15px;background:linear-gradient(90deg,#f0fdfa,#f8fafc);border-bottom:1px solid #cbd5e1}.section-heading>span{display:grid;place-items:center;min-height:36px;background:#0f766e;color:#fff;border-radius:6px;font-weight:800}.section-heading h3{margin:0;color:#173f3a;font-size:16px}.section-heading p{margin:4px 0 0;color:#64748b;font-size:12px}.section-heading>b{color:#0f766e;font-size:12px;background:#ccfbf1;padding:6px 9px;border-radius:999px}.instance-table-wrap{overflow:auto}.instance-table{width:100%;min-width:1120px;border-collapse:collapse}.instance-table th,.instance-table td{padding:10px 11px;border-right:1px solid #dbe3ec;border-bottom:1px solid #dbe3ec;text-align:center}.instance-table th{background:#f8fafc;color:#334155;font-size:12px;white-space:nowrap}.instance-table td{color:#475569}.instance-table td:nth-child(2) strong{color:#0f766e}.instance-table tbody tr:hover{background:#f0fdfa}@media(max-width:760px){.section-heading{grid-template-columns:48px 1fr}.section-heading>b{display:none}}
</style>
