<template>
  <section class="bridge-map-page">
    <div class="map-toolbar panel">
      <div><h2>重庆桥梁信息查询</h2><p>点击高德地图标注显示桥梁名称，点击名称进入桥梁基础状况卡片。</p></div>
      <div class="map-search"><el-input v-model="keyword" clearable placeholder="桥梁名称、编号或地址" @keyup.enter="load"/><el-button type="primary" @click="load">查询</el-button></div>
    </div>
    <div class="map-layout">
      <aside class="bridge-list panel">
        <div class="list-title"><strong>桥梁点位</strong><span>{{ points.length }} 座</span></div>
        <button v-for="item in points" :key="item.bridge_code" :class="{active:selected?.bridge_code===item.bridge_code}" @click="select(item)">
          <strong>{{ item.bridge_name }}</strong><span>{{ item.bridge_type_name }} · {{ item.route_code }}</span><small>{{ item.location_address }}</small>
        </button>
      </aside>
      <main class="map-shell panel">
        <div ref="mapContainer" class="amap-container"></div>
        <div v-if="mapError" class="map-fallback"><h3>高德地图暂未加载</h3><p>{{ mapError }}</p><p>桥梁列表和详情查询仍可正常使用。</p></div>
        <div class="map-legend"><i></i>桥梁位置标注</div>
      </main>
    </div>
    <el-alert class="amap-note" type="success" :closable="false" title="已接入高德 Web端 JS API。当前以重庆为中心显示桥梁点位，点位坐标使用 GCJ-02。"/>
  </section>
</template>
<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
const router=useRouter(),keyword=ref(''),points=ref([]),selected=ref(null),mapContainer=ref(null),mapError=ref('')
let map=null,infoWindow=null,markers=[]
function loadAmap(){return new Promise((resolve,reject)=>{if(window.AMap)return resolve(window.AMap);const key=import.meta.env.VITE_AMAP_WEB_KEY;if(!key)return reject(new Error('未配置 VITE_AMAP_WEB_KEY'));window._AMapSecurityConfig={securityJsCode:import.meta.env.VITE_AMAP_SECURITY_CODE};const script=document.createElement('script');script.src=`https://webapi.amap.com/maps?v=2.0&key=${key}&plugin=AMap.Scale,AMap.ToolBar`;script.onload=()=>resolve(window.AMap);script.onerror=()=>reject(new Error('高德地图脚本加载失败'));document.head.appendChild(script)})}
async function initMap(){try{const AMap=await loadAmap();await nextTick();map=new AMap.Map(mapContainer.value,{zoom:10,center:[106.551556,29.563009],viewMode:'2D',mapStyle:'amap://styles/normal'});map.addControl(new AMap.Scale());map.addControl(new AMap.ToolBar({position:{right:'16px',top:'90px'}}));infoWindow=new AMap.InfoWindow({offset:new AMap.Pixel(0,-32),isCustom:true});renderMarkers()}catch(error){mapError.value=error.message}}
async function load(){points.value=await http.get('/bridge-profiles/map-points',{params:{keyword:keyword.value}});selected.value=points.value[0]||null;renderMarkers();if(selected.value)focus(selected.value,false)}
function renderMarkers(){if(!map||!window.AMap)return;map.remove(markers);markers=points.value.map(item=>{const marker=new window.AMap.Marker({position:[Number(item.longitude),Number(item.latitude)],title:item.bridge_name,anchor:'bottom-center',content:'<div class="bridge-amap-marker"><span></span></div>'});marker.on('click',()=>select(item));marker.setMap(map);return marker});if(markers.length)map.setFitView(markers,false,[80,80,80,80],11)}
function popupHtml(item){return `<div class="bridge-info-window"><button class="info-close" onclick="window.__closeBridgeInfo()">×</button><b>${escapeHtml(item.bridge_name)}</b><p>${escapeHtml(item.bridge_type_name||'')} · ${escapeHtml(item.route_code||'')}</p><p>${escapeHtml(item.location_address||'重庆市')}</p><p>桥梁全长：${item.bridge_length||'—'} m　桥墩/桥桩：${item.pier_count||0}</p><a onclick="window.__openBridgeProfile('${encodeURIComponent(item.bridge_code)}')">查看桥梁详细信息 →</a></div>`}
function focus(item,pan=true){if(!map||!infoWindow)return;if(pan)map.setZoomAndCenter(14,[Number(item.longitude),Number(item.latitude)]);infoWindow.setContent(popupHtml(item));infoWindow.open(map,[Number(item.longitude),Number(item.latitude)])}
function select(item){selected.value=item;focus(item)}
function openProfile(item){router.push(`/bridges/${encodeURIComponent(item.bridge_code)}`)}
function escapeHtml(value){return String(value??'').replace(/[&<>'"]/g,char=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char]))}
onMounted(async()=>{window.__openBridgeProfile=code=>router.push(`/bridges/${code}`);window.__closeBridgeInfo=()=>infoWindow?.close();await initMap();await load()})
onBeforeUnmount(()=>{delete window.__openBridgeProfile;delete window.__closeBridgeInfo;map?.destroy()})
</script>
<style scoped>
.bridge-map-page{max-width:1600px;margin:auto}.map-toolbar{display:flex;justify-content:space-between;align-items:center;margin-bottom:14px}.map-toolbar h2{margin:0 0 6px}.map-toolbar p{margin:0;color:#64748b}.map-search{display:flex;gap:8px;width:430px}.map-layout{display:grid;grid-template-columns:320px 1fr;gap:14px;height:680px}.bridge-list{padding:10px;overflow:auto}.list-title{display:flex;justify-content:space-between;padding:10px}.list-title span{color:#64748b}.bridge-list button{display:block;width:100%;text-align:left;border:1px solid #dbe3ec;background:#f8fafc;border-radius:8px;padding:12px;margin-bottom:8px;cursor:pointer;color:#172033}.bridge-list button.active{background:#0f766e;color:#fff;border-color:#0f766e}.bridge-list button strong,.bridge-list button span,.bridge-list button small{display:block}.bridge-list button span{margin:5px 0;font-size:12px}.bridge-list button small{opacity:.75}.map-shell{position:relative;padding:0;overflow:hidden}.amap-container{width:100%;height:100%;min-height:620px}.map-fallback{position:absolute;inset:0;display:grid;place-content:center;text-align:center;background:#eef4e9;color:#475569}.map-legend{position:absolute;z-index:5;right:18px;bottom:18px;background:#fff;padding:9px 12px;border-radius:6px;box-shadow:0 6px 20px rgba(15,23,42,.18)}.map-legend i{display:inline-block;width:10px;height:10px;border-radius:50%;background:#dc2626;margin-right:6px}.amap-note{margin-top:14px}:global(.bridge-amap-marker span){display:block;width:24px;height:24px;border:5px solid #fff;border-radius:50% 50% 50% 0;background:#dc2626;transform:rotate(-45deg);box-shadow:0 4px 12px rgba(15,23,42,.35)}:global(.bridge-info-window){position:relative;width:280px;background:#fff;border-radius:9px;padding:16px;box-shadow:0 16px 40px rgba(15,23,42,.28);border:1px solid #cbd5e1}:global(.bridge-info-window b){font-size:17px}:global(.bridge-info-window p){margin:7px 0;color:#64748b;font-size:12px}:global(.bridge-info-window a){color:#0f766e;font-weight:700;cursor:pointer}:global(.info-close){position:absolute;right:8px;top:6px;border:0;background:transparent;font-size:18px;cursor:pointer}@media(max-width:1000px){.map-layout{grid-template-columns:1fr;height:auto}.bridge-list{max-height:260px}.map-shell,.amap-container{height:600px}.map-toolbar{align-items:stretch;flex-direction:column;gap:12px}.map-search{width:100%}}
</style>