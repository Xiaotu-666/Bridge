<template>
  <div class="table-wrap">
    <table class="basic-table"><tbody>
      <tr><th>34</th><th class="item-label">桥面高程(m)</th><td><DynamicCells :rows="details.measurementPoints" title-key="point_no" :detail="row=>`${row.point_name||'测点'}：${show(row.benchmark_elevation,' m')}`" empty="按测点设置列数"/></td></tr>
      <tr><th>35</th><th class="item-label">桥梁分孔（m）</th><td><DynamicCells :rows="details.spans" title-key="span_no" :detail="row=>`${show(row.span_length,' m')} · ${show(row.structure_form)} · ${show(row.material_type)}`" :title="row=>`第${row.span_no}孔`" empty="按孔数（号）设置列数"/></td></tr>
      <tr><th>36</th><th class="item-label">结构体系</th><td><DynamicCells :rows="systemRows" title-key="serial_no" :detail="structureText" empty="按种类设置列数"/></td></tr>
    </tbody></table>

    <div class="section-head"><span>A表37-44</span>上部结构形式与材料</div>
    <table class="basic-table"><tbody>
      <tr v-for="row in upperRows" :key="row.no"><th>{{ row.no }}</th><th class="item-label">{{ row.label }}</th><td><DynamicCells :rows="rowsFor(row)" title-key="serial_no" :detail="structureText" :empty="row.dynamic"/></td></tr>
    </tbody></table>

    <div class="section-head"><span>A表45-49</span>桥面系形式与材料</div>
    <table class="basic-table"><tbody>
      <tr v-for="row in deckRows" :key="row.no"><th>{{ row.no }}</th><th class="item-label">{{ row.label }}</th><td><DynamicCells :rows="rowsFor(row)" title-key="serial_no" :detail="structureText" :empty="row.dynamic"/></td></tr>
    </tbody></table>

    <div class="section-head"><span>A表50-53</span>下部结构形式与材料</div>
    <table class="basic-table"><tbody>
      <tr v-for="row in lowerRows" :key="row.no"><th>{{ row.no }}</th><th class="item-label">{{ row.label }}</th><td><DynamicCells :rows="rowsFor(row)" title-key="serial_no" :detail="structureText" :empty="row.dynamic"/></td></tr>
    </tbody></table>

    <div class="section-head"><span>A表54-55</span>基础形式与材料</div>
    <table class="basic-table"><tbody>
      <tr v-for="row in foundationRows" :key="row.no"><th>{{ row.no }}</th><th class="item-label">{{ row.label }}</th><td><DynamicCells :rows="rowsFor(row)" title-key="serial_no" :detail="structureText" :empty="row.dynamic"/></td></tr>
    </tbody></table>

    <div class="section-head"><span>A表56-59</span>支座形式、材料与附属设施</div>
    <table class="basic-table"><tbody>
      <tr v-for="row in bearingRows" :key="row.no"><th>{{ row.no }}</th><th class="item-label">{{ row.label }}</th><td><DynamicCells :rows="rowsFor(row)" title-key="serial_no" :detail="structureText" :empty="row.dynamic"/></td></tr>
    </tbody></table>
  </div>
</template>

<script setup>
import { computed, defineComponent, h } from 'vue'

const props=defineProps({details:{type:Object,default:()=>({spans:[],structures:[],cables:[],measurementPoints:[]})}})
const show=(value,suffix='')=>value===null||value===undefined||value===''?'未录入':`${value}${suffix}`
const upperRows=[{no:37,label:'主梁',terms:['主梁']},{no:38,label:'主拱圈',terms:['主拱','拱圈']},{no:39,label:'桥（索）塔',terms:['桥塔','索塔']},{no:40,label:'拱上建筑',terms:['拱上']},{no:41,label:'主缆',terms:['主缆']},{no:42,label:'斜拉索（含索力）',cable:'斜拉索',dynamic:'按索数设置列数'},{no:43,label:'吊杆（含索力）',cable:'吊杆',dynamic:'按吊杆数设置列数'},{no:44,label:'系杆（含索力）',cable:'系杆',dynamic:'按系杆数设置列数'}]
const deckRows=[{no:45,label:'桥面铺装',terms:['桥面铺装']},{no:46,label:'伸缩缝',terms:['伸缩缝'],dynamic:'按孔数设置列数'},{no:47,label:'人行道、路缘',terms:['人行道','路缘']},{no:48,label:'栏杆、护栏',terms:['栏杆','护栏'],dynamic:'按部位不同设置列数'},{no:49,label:'照明、标志',terms:['照明','标志']}]
const lowerRows=[{no:50,label:'桥台',terms:['桥台'],dynamic:'按桥台数设置列数'},{no:51,label:'桥墩',terms:['桥墩'],dynamic:'按桥墩数设置列数'},{no:52,label:'锥坡、护坡',terms:['锥坡','护坡']},{no:53,label:'翼墙、耳墙',terms:['翼墙','耳墙']}]
const foundationRows=[{no:54,label:'基础',terms:['基础']},{no:55,label:'锚碇',terms:['锚碇'],dynamic:'按锚碇数设置列数'}]
const bearingRows=[{no:56,label:'支座',terms:['支座'],dynamic:'按支座数设置列数'},{no:57,label:'桥梁防撞设施',terms:['防撞']},{no:58,label:'航标及排水系统',terms:['航标','排水']},{no:59,label:'其他附属设施',terms:['附属','检修','监测']}]
const systemRows=computed(()=>props.details.structures?.filter(row=>row.structure_type==='结构体系')||[])
function rowsFor(config){if(config.cable)return props.details.cables?.filter(row=>row.cable_type===config.cable)||[];return props.details.structures?.filter(row=>config.terms?.some(term=>String(row.structure_type||'').includes(term)))||[]}
function structureText(row){return `${show(row.form)} · ${show(row.material_type)}${row.quantity!==null&&row.quantity!==undefined?` · 数量${row.quantity}`:''}`}
const DynamicCells=defineComponent({props:{rows:{type:Array,default:()=>[]},titleKey:String,title:Function,detail:Function,empty:String},setup(p){return()=>p.rows.length?h('div',{class:'cells'},p.rows.map((row,index)=>h('article',{key:row[p.titleKey]||index},[h('b',p.title?p.title(row):row[p.titleKey]),h('span',p.detail(row))]))):h('span',{class:'empty'},p.empty||'未录入')}})
</script>

<style scoped>
.table-wrap{overflow:auto;padding:16px}.basic-table{width:100%;min-width:700px;border-collapse:collapse;table-layout:fixed;font-family:SimSun,serif;margin-bottom:10px}.basic-table th,.basic-table td{border:1px solid #334155;padding:7px 8px;vertical-align:middle}.basic-table th{background:#f8fafc;text-align:center;font-weight:700}.basic-table th:nth-child(1){width:40px}.basic-table th:nth-child(2){width:132px;text-align:left}.section-head{display:flex;align-items:center;gap:8px;padding:8px 12px;margin:14px 0 0;background:#f0fdfa;border-left:4px solid #0f766e;font-size:14px;font-weight:700;color:#115e59}.section-head span{font-size:11px;color:#0f766e;background:#ccfbf1;padding:2px 8px;border-radius:4px}.cells{display:flex;flex-wrap:wrap;gap:6px}.cells article{min-width:142px;flex:1 1 142px;padding:6px 8px;border:1px solid #cbd5e1;background:#fff}.cells b,.cells span{display:block}.cells b{color:#0f766e;font-size:12px}.cells span{margin-top:3px;color:#334155;font-size:11px;line-height:1.4}.empty{color:#94a3b8;font-size:12px}@media print{.table-wrap{padding:0}.basic-table{font-size:10px;margin-bottom:8px}.cells{gap:3px}.cells article{min-width:85px;padding:3px}.section-head{background:#fff;border-left-color:#000}}
.basic-table .item-label{width:132px!important;text-align:center!important;vertical-align:middle!important}
</style>
