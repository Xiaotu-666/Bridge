<template>
  <div v-if="hasDetails" class="structure-details">
    <section v-if="details.measurementPoints?.length" class="detail-section">
      <div class="detail-heading"><div><b>桥面高程测点</b><small>按测点数量生成，作为初始检查测值的动态列</small></div><span>{{ details.measurementPoints.length }} 点</span></div>
      <div class="detail-grid"><article v-for="row in details.measurementPoints" :key="row.measurement_point_id"><strong>{{ row.point_no }}</strong><span>{{ show(row.point_name) }}</span><small>基准高程 {{ show(row.benchmark_elevation,' m') }}</small></article></div>
    </section>
    <section v-if="details.spans?.length" class="detail-section">
      <div class="detail-heading"><div><b>桥梁分孔</b><small>按实际跨径逐孔展示</small></div><span>{{ details.spans.length }} 孔</span></div>
      <div class="span-grid"><article v-for="row in details.spans" :key="row.span_detail_id"><strong>第 {{ row.span_no }} 孔</strong><b>{{ show(row.span_length,' m') }}</b><small>{{ show(row.structure_form) }} · {{ show(row.material_type) }}</small></article></div>
    </section>
    <section v-for="group in structureGroups" :key="group.code" class="detail-section">
      <div class="detail-heading"><div><b>{{ group.name }}</b><small>结构明细</small></div><span>{{ group.rows.length }} 项</span></div>
      <div class="detail-grid"><article v-for="row in group.rows" :key="row.structure_detail_id"><strong>{{ row.serial_no || '—' }} · {{ row.structure_type || '未命名结构' }}</strong><span>{{ show(row.form) }}</span><small>{{ show(row.material_type) }} · 数量 {{ show(row.quantity) }}</small></article></div>
    </section>
    <section v-if="details.cables?.length" class="detail-section">
      <div class="detail-heading"><div><b>斜拉索、吊杆与系杆</b><small>索力与位置明细</small></div><span>{{ details.cables.length }} 根</span></div>
      <div class="detail-grid"><article v-for="row in details.cables" :key="row.cable_detail_id"><strong>{{ row.cable_type }} {{ row.serial_no }}</strong><span>索力 {{ show(row.force_value) }}</span><small>{{ show(row.location_desc) }} · {{ show(row.material_type) }}</small></article></div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props=defineProps({details:{type:Object,default:()=>({spans:[],structures:[],cables:[],measurementPoints:[]})}})
const names={superstructure:'上部结构形式与材料',deck:'桥面系形式与材料',substructure:'下部结构形式与材料',foundation:'基础形式与材料',bearing_facility:'支座形式、材料与附属设施'}
const hasDetails=computed(()=>Boolean(props.details.spans?.length||props.details.structures?.length||props.details.cables?.length||props.details.measurementPoints?.length))
const structureGroups=computed(()=>{
  const map=new Map()
  for(const row of props.details.structures||[]){
    const code=row.structure_group||'other'
    if(!map.has(code)) map.set(code,{code,name:names[code]||code,rows:[]})
    map.get(code).rows.push(row)
  }
  return [...map.values()].sort((a,b)=>Object.keys(names).indexOf(a.code)-Object.keys(names).indexOf(b.code))
})
const show=(value,suffix='')=>value===null||value===undefined||value===''?'—':`${value}${suffix}`
</script>

<style scoped>
.structure-details{padding:4px 16px 0}.detail-section{padding:16px 0;border-bottom:1px solid #e2e8f0}.detail-section:last-child{border-bottom:0}.detail-heading{display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:11px}.detail-heading b,.detail-heading small{display:block}.detail-heading b{color:#0f3e38;font-size:14px}.detail-heading small{margin-top:3px;color:#64748b;font-size:11px}.detail-heading>span{padding:4px 8px;border-radius:999px;background:#ecfdf5;color:#0f766e;font-size:11px;font-weight:700}.span-grid,.detail-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:10px}.span-grid article,.detail-grid article{padding:12px;border:1px solid #dce7e8;border-radius:8px;background:linear-gradient(145deg,#fff,#f8fafc)}.span-grid strong,.span-grid b,.span-grid small,.detail-grid strong,.detail-grid span,.detail-grid small{display:block}.span-grid b,.detail-grid span{margin-top:6px;color:#0f766e}.span-grid small,.detail-grid small{margin-top:5px;color:#64748b;font-size:12px;line-height:1.45}
</style>
