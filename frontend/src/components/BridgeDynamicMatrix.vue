<template>
  <div v-if="components.length" class="component-browser">
    <section v-if="pierItems.length" class="pier-section">
      <div class="pier-heading"><div><span>D · 下部结构重点</span><h3>桥墩实例</h3><p>按桥墩编号展示，避免与基础、台帽和系统默认部件混在一张宽表中。</p></div><b>{{ pierItems.length }} 座</b></div>
      <div class="pier-grid"><article v-for="item in pierItems" :key="item.bridge_component_id" class="pier-card"><div class="pier-number">{{ item.component_serial || '未编号' }}</div><div class="pier-name">{{ item.component_name || '桥墩' }}</div><dl><div><dt>所在位置</dt><dd>{{ value(item.location_desc) }}</dd></div><div><dt>材料</dt><dd>{{ value(item.material_type) }}</dd></div><div><dt>尺寸规格</dt><dd>{{ value(item.dimension_spec) }}</dd></div><div><dt>数量</dt><dd>{{ item.quantity ?? 1 }}</dd></div></dl></article></div>
    </section>

    <details v-if="lowerItems.length" class="component-group" open><summary><div><span>下部结构与基础</span><small>桥台、基础、翼墙、护坡等</small></div><b>{{ lowerItems.length }} 条</b></summary><div class="component-grid"><article v-for="item in lowerItems" :key="item.bridge_component_id"><strong>{{ item.component_name || item.component_code }}</strong><span>{{ item.component_serial || '未编号' }}</span><small>{{ value(item.location_desc) }}</small><footer>{{ value(item.material_type) }} · {{ value(item.dimension_spec) }}</footer></article></div></details>
    <details v-for="group in otherGroups" :key="group.key" class="component-group" :open="group.key===otherGroups[0]?.key"><summary><div><span>{{ group.title }}</span><small>点击展开查看具体部件</small></div><b>{{ group.instances.length }} 条</b></summary><div class="component-grid"><article v-for="item in group.instances" :key="item.bridge_component_id"><strong>{{ item.component_name || item.component_code }}</strong><span>{{ item.component_serial || '未编号' }}</span><small>{{ value(item.location_desc) }}</small><footer>{{ value(item.material_type) }} · {{ value(item.dimension_spec) }}</footer></article></div></details>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props=defineProps({components:{type:Array,default:()=>[]}})
const itemText=item=>`${item.part_name||''} ${item.component_name||''} ${item.component_serial||''} ${item.location_desc||''}`
const isExplicitPier=item=>/\d+\s*(?:#|号)?(?:墩|台|桩)/.test(itemText(item))
const isLower=item=>/下部结构|桥墩|桥桩|墩柱|基础|桥台|锚碇|翼墙|耳墙|护坡/.test(itemText(item))
const pierItems=computed(()=>props.components.filter(isExplicitPier))
const lowerItems=computed(()=>props.components.filter(item=>isLower(item)&&!isExplicitPier(item)))
const otherGroups=computed(()=>{
  const map=new Map()
  for(const item of props.components.filter(item=>!isLower(item))){
    const key=item.part_code||item.part_name||'other'
    if(!map.has(key)) map.set(key,{key,title:item.part_name||'其他部件',instances:[]})
    map.get(key).instances.push(item)
  }
  return [...map.values()].sort((a,b)=>a.title.localeCompare(b.title,'zh-CN'))
})
const value=value=>value===null||value===undefined||value===''?'—':value
</script>

<style scoped>
.component-browser{padding:2px 16px 20px}.pier-section{margin:16px 0;padding:18px;border:1px solid #99d8ca;border-radius:12px;background:linear-gradient(145deg,#f0fdfa,#f8fafc)}.pier-heading{display:flex;align-items:flex-start;justify-content:space-between;gap:18px;margin-bottom:14px}.pier-heading span{color:#0f766e;font-size:11px;font-weight:800;letter-spacing:.06em}.pier-heading h3{margin:5px 0;color:#134e4a;font-size:20px}.pier-heading p{max-width:680px;margin:0;color:#64748b;font-size:12px;line-height:1.55}.pier-heading>b{padding:7px 11px;border-radius:999px;background:#0f766e;color:#fff;font-size:12px;white-space:nowrap}.pier-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:12px}.pier-card{overflow:hidden;border:1px solid #cde5df;border-radius:10px;background:#fff;box-shadow:0 5px 14px rgba(15,118,110,.07)}.pier-number{padding:9px 12px;background:#134e4a;color:#fff;font-size:17px;font-weight:800}.pier-name{padding:10px 12px 0;color:#0f766e;font-size:13px;font-weight:700}.pier-card dl{display:grid;grid-template-columns:1fr 1fr;margin:10px 12px 12px;border-top:1px solid #e2e8f0;border-left:1px solid #e2e8f0}.pier-card dl div{min-width:0;padding:8px;border-right:1px solid #e2e8f0;border-bottom:1px solid #e2e8f0}.pier-card dt{color:#94a3b8;font-size:10px}.pier-card dd{margin:3px 0 0;color:#334155;font-size:12px;line-height:1.4;word-break:break-word}.component-group{margin-top:11px;border:1px solid #dbe5ea;border-radius:9px;background:#fff}.component-group summary{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:13px 15px;cursor:pointer;list-style:none}.component-group summary::-webkit-details-marker{display:none}.component-group summary:after{content:'⌄';color:#0f766e;font-size:18px}.component-group[open] summary{border-bottom:1px solid #dbe5ea;background:#f8fafc}.component-group[open] summary:after{transform:rotate(180deg)}.component-group summary div{margin-right:auto}.component-group summary span,.component-group summary small{display:block}.component-group summary span{color:#1e3a3a;font-size:14px;font-weight:700}.component-group summary small{margin-top:3px;color:#64748b;font-size:11px}.component-group summary>b{padding:4px 8px;border-radius:999px;background:#ecfdf5;color:#0f766e;font-size:11px}.component-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:10px;padding:13px}.component-grid article{min-width:0;padding:11px;border:1px solid #e2e8f0;border-radius:7px;background:#fff}.component-grid strong,.component-grid span,.component-grid small,.component-grid footer{display:block}.component-grid strong{color:#334155;font-size:13px}.component-grid span{margin-top:5px;color:#0f766e;font-size:12px;font-weight:700}.component-grid small{min-height:20px;margin-top:5px;color:#64748b;font-size:11px}.component-grid footer{margin-top:8px;padding-top:8px;border-top:1px dashed #e2e8f0;color:#64748b;font-size:11px;line-height:1.4}@media(max-width:700px){.pier-heading{flex-direction:column}.pier-grid{grid-template-columns:1fr}.component-grid{grid-template-columns:1fr}}
</style>
