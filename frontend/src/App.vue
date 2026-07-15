<template>
  <router-view v-if="$route.path === '/login'" />
  <el-container v-else class="app-shell">
    <el-aside class="sidebar" width="252px">
      <div class="brand"><div class="brand-mark">B</div><div><strong>Bridge Inspect</strong><span>{{ roleName }}门户</span></div></div>
      <el-scrollbar>
        <el-menu router :default-active="$route.path" class="nav-menu">
          <template v-for="group in menus" :key="group.key">
            <el-menu-item v-if="!group.children" :index="group.path"><component :is="group.icon" :size="18"/><span>{{ group.label }}</span></el-menu-item>
            <el-sub-menu v-else :index="`group-${group.key}`">
              <template #title><component :is="group.icon" :size="18"/><span>{{ group.label }}</span></template>
              <el-menu-item v-for="item in group.children" :key="item.path" :index="item.path">{{ item.label }}</el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>
    <el-container>
      <el-header class="topbar"><div><h1>{{ currentLabel }}</h1><p>{{ roleName }} · 每个菜单使用唯一地址</p></div><div class="user-box"><el-tag effect="plain">{{ auth.user?.userName }}</el-tag><el-button text @click="logout">退出</el-button></div></el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { ClipboardList, Database, FileText, Landmark, LayoutDashboard, MapPinned, Settings, ShieldCheck } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const roleNames = { admin: '系统管理员', engineer: '桥梁工程师', inspector: '检查人员', reviewer: '审核人员', viewer: '查询人员' }
const roleName = computed(() => roleNames[auth.role] || '用户')
const definitions = {
  admin: [home(), workbench(), base(), archive(), initial(), periodic(), review(), query(), system()],
  engineer: [home(), base(), archive(), initial(), periodic(), query()],
  inspector: [home(), workbench(), initial(), periodic(), query()],
  reviewer: [home(), review(), query()],
  viewer: [home(), query()]
}
const menus = computed(() => definitions[auth.role] || [home()])
const currentLabel = computed(() => {
  for (const group of menus.value) {
    if (group.path === route.path) return group.label
    const item = group.children?.find(child => child.path === route.path)
    if (item) return item.label
  }
  return '公路桥梁初始检查信息系统'
})
function home() { return { key: 'home', label: '角色工作台', path: '/dashboard', icon: LayoutDashboard } }
function workbench() { return { key: 'workbench', label: '检查人员工作台', icon: ClipboardList, children: [i('/inspector/initial-workbench', '初始检查工作台'), i('/inspector/periodic-workbench', '定期检查工作台')] } }
function base() { return { key: 'base', label: '基础数据与矩阵', icon: Database, children: [i('/base/routes', '路线'), i('/base/bridge-types', '桥梁类型'), i('/base/parts', '部位字典'), i('/base/components', '部件字典'), i('/base/initial-items', '初检项目字典'), i('/base/defect-definitions', '病害字典'), i('/base/component-matrix', '桥型-部位-部件矩阵'), i('/base/initial-matrix', '桥型-初检项目矩阵')] } }
function archive() { return { key: 'archive', label: '桥梁档案', icon: Landmark, children: [i('/archive/bridges', '基础状况卡片'), i('/archive/components', '桥梁具体部件'), i('/archive/files', '档案资料记录')] } }
function initial() { return { key: 'initial', label: '初始检查', icon: ClipboardList, children: [i('/initial/tasks', '初始检查任务'), i('/initial/records', '初始检查记录'), i('/initial/items', '检测项目结果'), i('/initial/defects', '初始病害'), i('/initial/reports', '初始检查报告')] } }
function periodic() { return { key: 'periodic', label: '定期检查', icon: MapPinned, children: [i('/periodic/tasks', '定期检查任务'), i('/periodic/records', '定期检查记录'), i('/periodic/components', '部件检查记录'), i('/periodic/defects', '定期病害'), i('/periodic/reports', '定期检查报告')] } }
function review() { return { key: 'review', label: '审核归档', icon: ShieldCheck, children: [i('/review/initial', '初始检查审核'), i('/review/periodic', '定期检查审核'), i('/review/reports', '报告审核')] } }
function query() { return { key: 'query', label: '查询统计', icon: FileText, children: [i('/query/bridge-map', '桥梁地图'), i('/query/bridges', '桥梁综合查询'), i('/query/inspections', '检查结果查询'), i('/query/defects', '病害查询'), i('/query/reports', '报告查询')] } }
function system() { return { key: 'system', label: '系统管理', icon: Settings, children: [i('/system/users', '用户管理'), i('/system/roles', '角色与权限'), i('/system/logs', '操作日志'), i('/system/backups', '版本控制')] } }
function i(path, label) { return { path, label } }
function logout() { auth.logout(); router.push('/login') }
</script>
