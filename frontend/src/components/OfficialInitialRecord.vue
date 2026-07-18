<template>
  <article class="official-record initial-record">
    <div class="record-title"><p>（{{ bridge?.road_management_org || '公路管理机构名称未录入' }}）</p><h1>桥梁初始检查记录表</h1><span>表 B · 首次建档基准记录</span></div>
    <el-empty v-if="!record" description="该桥梁尚未建立初始检查记录表"/>
    <template v-else>
      <section class="record-page">
        <table class="official-table identity-table"><tbody>
          <tr v-for="row in identityRows" :key="row[0].no"><template v-for="field in row" :key="field.no"><th>{{ field.no }} {{ field.label }}</th><td>{{ field.value }}</td></template></tr>
          <tr v-for="field in narrativeRows" :key="field.no"><th>{{ field.no }} {{ field.label }}</th><td colspan="5" class="narrative">{{ field.value }}</td></tr>
          <tr><th>16 设计单位名称</th><td>{{ value('design_unit') }}</td><th>17 施工单位名称</th><td>{{ value('construction_unit') }}</td><th>18 监理单位名称</th><td>{{ value('supervision_unit') }}</td></tr>
          <tr><th>19 交工时间（年 月 日）</th><td colspan="3">{{ value('completion_date') }}</td><th>20 初始检查（年 月 日）</th><td>{{ value('inspection_date') }}</td></tr>
          <tr><th>21 初始检查时的气候及环境温度</th><td colspan="5">{{ value('weather_temperature') }}</td></tr>
          <tr><th>22 桥面高程</th><td colspan="5">{{ itemValue('桥面高程') }}</td></tr>
          <tr><th>23 拱轴线</th><td colspan="5">{{ itemValue('拱轴线') }}</td></tr>
        </tbody></table>
      </section>
      <section class="record-page page-break">
        <table class="official-table measurement-table"><tbody>
          <tr v-for="field in measurementRows" :key="field.no"><th>{{ field.no }} {{ field.label }}</th><td>{{ itemValue(field.label) }}</td></tr>
          <tr class="test-row"><th>36 静载试验结果</th><td>{{ itemValue('静载试验') }}</td></tr>
          <tr class="test-row"><th>37 动载试验结果</th><td>{{ itemValue('动载试验') }}</td></tr>
          <tr><th>38 记录人</th><td>{{ value('recorder') }}</td><th>39 桥梁工程师</th><td>{{ value('bridge_engineer') }}</td></tr>
          <tr><th>40 桥梁初始检查机构</th><td colspan="3">{{ value('inspection_org') }}</td></tr>
        </tbody></table>
      </section>
    </template>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props=defineProps({bridge:Object,record:Object,items:{type:Array,default:()=>[]}})
const empty=value=>value===null||value===undefined||value===''?'未录入':String(value)
const value=key=>empty(props.record?.[key] ?? props.bridge?.[key])
const identityRows=computed(()=>[
  [{no:1,label:'路线编号',value:value('route_code')},{no:2,label:'路线名称',value:value('route_name')},{no:3,label:'桥位桩号',value:value('pile_number')}],
  [{no:4,label:'桥梁编号',value:value('bridge_code')},{no:5,label:'桥梁名称',value:value('bridge_name')},{no:6,label:'被跨越道路（通道）名称',value:value('crossed_road_name')}],
  [{no:7,label:'被跨越道路（通道）桩号',value:value('crossed_road_pile')},{no:8,label:'桥梁全长(m)',value:value('bridge_length')},{no:9,label:'最大跨径(m)',value:value('maximum_span')}]
])
const narrativeRows=computed(()=>[
  {no:10,label:'上、下部结构形式',value:value('structure_form')},
  {no:11,label:'桥梁分联及跨径组合',value:value('span_combination')},
  {no:12,label:'桥梁施工方法',value:value('construction_method')},
  {no:13,label:'新建桥梁在施工过程中的返工、维修或加固情况',value:value('construction_issue')},
  {no:14,label:'加固改造后的桥梁、加固改造情况',value:value('reinforcement_note')},
  {no:15,label:'档案资料不齐全的桥梁、维修加固情况',value:value('archive_note')}
])
const measurementRows=[
  {no:24,label:'主缆线形'},{no:25,label:'墩、台身、锚碇的高程'},{no:26,label:'墩、台身、索塔倾斜度'},
  {no:27,label:'索塔水平变位、高程'},{no:28,label:'拱桥桥台、悬索桥锚碇水平位移'},
  {no:29,label:'悬索桥索夹螺栓紧固力'},{no:30,label:'水中基础'},{no:31,label:'斜拉索或吊杆索力'},
  {no:32,label:'主要承重构件尺寸'},{no:33,label:'材质强度'},{no:34,label:'保护层厚度'},
  {no:35,label:'钢管混凝土管内混凝土密实度'}
]
function itemValue(label){const item=props.items.find(row=>String(row.item_name||'').includes(label)||label.includes(String(row.item_name||'')));return empty(item?.measured_value)}
</script>

<style scoped>
.official-record{background:#fff;color:#111;font-family:SimSun,serif}.record-title{text-align:center;border-top:2px solid #111;border-bottom:1px solid #111;padding:7px 0 9px}.record-title p,.record-title h1,.record-title span{margin:0}.record-title p{font-weight:700}.record-title h1{font-size:24px;letter-spacing:2px;margin-top:3px}.record-title span{font-size:12px;color:#475569}.record-page{padding-top:14px}.official-table{width:100%;border-collapse:collapse;table-layout:fixed;font-size:13px}.official-table th,.official-table td{border:1px solid #111;padding:6px 8px;vertical-align:middle}.official-table th{width:15%;font-weight:700;text-align:left;background:#fafafa}.official-table td{min-height:30px;word-break:break-word}.identity-table tr:nth-child(-n+3) th{width:11.5%}.identity-table tr:nth-child(-n+3) td{width:21.8%}.narrative{height:38px;white-space:pre-wrap}.measurement-table th{width:28%}.measurement-table td{height:33px}.test-row th,.test-row td{height:130px}.test-row th{vertical-align:middle}.page-break{margin-top:20px}@media print{.page-break{break-before:page}.official-record{box-shadow:none}.record-title{margin-top:0}}@media(max-width:720px){.official-table{font-size:11px}.official-table th,.official-table td{padding:5px 4px}.record-title h1{font-size:19px}}
</style>
