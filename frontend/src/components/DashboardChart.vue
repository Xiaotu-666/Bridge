<template><div ref="container" class="dashboard-chart" /></template>
<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
const props=defineProps({option:{type:Object,required:true}}),container=ref(),chart=ref(),observer=ref();let frame=0
function render(){if(!container.value)return;const source=props.option||{},series=Array.isArray(source.series)?source.series:[];if(series.some(item=>item.type==='line')&&!series.some(item=>Array.isArray(item.data)&&item.data.length))return;if(!chart.value)chart.value=echarts.init(container.value);const option={...source,animation:false};if(Array.isArray(option.series))option.series=option.series.map(item=>Array.isArray(item.data)&&!item.data.length?{...item,data:[{name:'暂无数据',value:0}]}:{...item});chart.value.clear();chart.value.setOption(option,{notMerge:true,lazyUpdate:false,silent:true})}
function schedule(){cancelAnimationFrame(frame);frame=requestAnimationFrame(render)}
onMounted(()=>{schedule();observer.value=new ResizeObserver(()=>chart.value?.resize());observer.value.observe(container.value)})
watch(()=>props.option,schedule,{deep:true,flush:'post'})
onBeforeUnmount(()=>{cancelAnimationFrame(frame);observer.value?.disconnect();chart.value?.dispose()})
</script>
<style scoped>.dashboard-chart{width:100%;height:320px;min-height:320px}</style>
