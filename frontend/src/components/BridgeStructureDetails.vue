<template>
  <div class="structure-details">
    <section><div class="detail-heading"><b>桥梁分孔</b><span>{{ details.spans?.length||0 }} 孔</span></div><div class="dynamic-columns"><article v-for="row in details.spans||[]" :key="row.span_detail_id"><strong>第 {{ row.span_no }} 孔</strong><span>{{ show(row.span_length,' m') }}</span><small>{{ show(row.structure_form) }} · {{ show(row.material_type) }}</small></article></div></section>
    <section v-for="group in structureGroups" :key="group.name"><div class="detail-heading"><b>{{ group.name }}</b><span>{{ group.rows.length }} 项</span></div><div class="dynamic-columns"><article v-for="row in group.rows" :key="row.structure_detail_id"><strong>{{ row.serial_no }} · {{ row.structure_type }}</strong><span>{{ show(row.form) }}</span><small>{{ show(row.material_type) }} · 数量 {{ show(row.quantity) }}</small></article></div></section>
    <section v-if="details.cables?.length"><div class="detail-heading"><b>斜拉索、吊杆与系杆</b><span>{{ details.cables.length }} 根</span></div><div class="dynamic-columns"><article v-for="row in details.cables" :key="row.cable_detail_id"><strong>{{ row.cable_type }} {{ row.serial_no }}</strong><span>索力 {{ show(row.force_value) }}</span><small>{{ show(row.location_desc) }} · {{ show(row.material_type) }}</small></article></div></section>
  </div>
</template>
<script setup>
import { computed } from 'vue'
const props=defineProps({details:{type:Object,default:()=>({spans:[],structures:[],cables:[]})}}),names={system:'结构体系',superstructure:'上部结构形式与材料',deck:'桥面系形式与材料',substructure:'下部结构形式与材料',foundation:'基础形式与材料',bearing:'支座形式与材料',accessory:'附属设施'}
const structureGroups=computed(()=>{const map=new Map();for(const row of props.details.structures||[]){const name=names[row.structure_group]||row.structure_group;if(!map.has(name))map.set(name,[]);map.get(name).push(row)}return[...map].map(([name,rows])=>({name,rows}))}),show=(value,suffix='')=>value===null||value===undefined||value===''?'—':`${value}${suffix}`
</script>
<style scoped>
.structure-details{padding:0 16px}.structure-details>section{padding:14px 0;border-bottom:1px solid #dbe3ec}.detail-heading{display:flex;justify-content:space-between;margin-bottom:9px}.detail-heading span{font-size:11px;color:#64748b}.dynamic-columns{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:8px}.dynamic-columns article{padding:11px;border:1px solid #dbe3ec;background:#f8fafc}.dynamic-columns strong,.dynamic-columns span,.dynamic-columns small{display:block}.dynamic-columns span{margin-top:6px;color:#0f766e}.dynamic-columns small{margin-top:4px;color:#64748b}
</style>
