import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import ResourceView from '../views/ResourceView.vue'
import TaskView from '../views/TaskView.vue'
import ReportView from '../views/ReportView.vue'
import MatrixView from '../views/MatrixView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'
import EngineerDashboardView from '../views/EngineerDashboardView.vue'
import InspectorDashboardView from '../views/InspectorDashboardView.vue'
import ReviewerDashboardView from '../views/ReviewerDashboardView.vue'
import ViewerDashboardView from '../views/ViewerDashboardView.vue'
import ForbiddenView from '../views/ForbiddenView.vue'
import BridgeMapView from '../views/BridgeMapView.vue'
import BridgeProfileView from '../views/BridgeProfileView.vue'
import RouteView from '../views/RouteView.vue'
import BridgeEditView from '../views/BridgeEditView.vue'
import InspectionRecordView from '../views/InspectionRecordView.vue'
import InspectionWorkbenchView from '../views/InspectionWorkbenchView.vue'
import InspectionResultsView from '../views/InspectionResultsView.vue'
import DefectQueryView from '../views/DefectQueryView.vue'
import DefectManagementView from '../views/DefectManagementView.vue'
import InspectionReviewWorkbenchView from '../views/InspectionReviewWorkbenchView.vue'
import UserManagementView from '../views/UserManagementView.vue'
import RoleManagementView from '../views/RoleManagementView.vue'
import VersionControlView from '../views/VersionControlView.vue'

const businessRoles = ['admin', 'engineer', 'inspector', 'reviewer', 'viewer']
const routes = [
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/', redirect: () => useAuthStore().homePath },
  { path: '/dashboard', redirect: () => useAuthStore().homePath },
  { path: '/403', component: ForbiddenView },
  { path: '/bridges/:bridgeCode', component: BridgeProfileView, props: { mode: 'summary' }, meta: { roles: businessRoles } },
  { path: '/bridges/:bridgeCode/A', component: BridgeProfileView, props: { mode: 'A' }, meta: { roles: businessRoles } },
  { path: '/bridges/:bridgeCode/B', component: BridgeProfileView, props: { mode: 'B' }, meta: { roles: businessRoles } },
  { path: '/bridges/:bridgeCode/C', component: BridgeProfileView, props: { mode: 'C' }, meta: { roles: businessRoles } },
  { path: '/dashboard/admin', component: AdminDashboardView, meta: { roles: ['admin'] } },
  { path: '/dashboard/engineer', component: EngineerDashboardView, meta: { roles: ['engineer'] } },
  { path: '/dashboard/inspector', component: InspectorDashboardView, meta: { roles: ['inspector'] } },
  { path: '/dashboard/reviewer', component: ReviewerDashboardView, meta: { roles: ['reviewer'] } },
  { path: '/dashboard/viewer', component: ViewerDashboardView, meta: { roles: ['viewer'] } },
  { path: '/base/routes', component: RouteView, meta: { roles: ['engineer'] } },
  { path: '/base/bridge-types', component: ResourceView, props: { resource: 'bridge-types' }, meta: { roles: ['engineer'] } },
  { path: '/base/initial-items', component: ResourceView, props: { resource: 'initial-item-definitions' }, meta: { roles: ['engineer'] } },
  { path: '/base/parts', component: ResourceView, props: { resource: 'bridge-positions' }, meta: { roles: ['engineer'] } },
  { path: '/base/components', component: ResourceView, props: { resource: 'bridge-components' }, meta: { roles: ['engineer'] } },
  { path: '/base/defect-definitions', component: ResourceView, props: { resource: 'defect-definitions' }, meta: { roles: ['engineer'] } },
  { path: '/base/component-matrix', component: MatrixView, props: { mode: 'components' }, meta: { roles: ['engineer'] } },
  { path: '/base/component-matrix/:bridgeTypeCode', component: MatrixView, props: { mode: 'components' }, meta: { roles: ['engineer'] } },
  { path: '/base/initial-matrix', component: MatrixView, props: { mode: 'initial-items' }, meta: { roles: ['engineer'] } },
  { path: '/base/initial-matrix/:bridgeTypeCode', component: MatrixView, props: { mode: 'initial-items' }, meta: { roles: ['engineer'] } },
  { path: '/archive/bridges', component: ResourceView, props: { resource: 'bridges' }, meta: { roles: ['engineer'] } },
  { path: '/archive/bridges/new', component: BridgeEditView, meta: { roles: ['engineer'] } },
  { path: '/archive/bridges/:bridgeCode/edit', component: BridgeEditView, meta: { roles: ['engineer'] } },
  { path: '/archive/components', component: ResourceView, props: { resource: 'bridge-instance-components' }, meta: { roles: ['engineer'] } },
  { path: '/archive/files', component: ResourceView, props: { resource: 'archive-records' }, meta: { roles: ['engineer'] } },
  { path: '/initial/tasks', component: TaskView, props: { inspectionType: 'initial' }, meta: { roles: ['engineer'] } },
  { path: '/inspector/initial-workbench', component: InspectionWorkbenchView, props: { inspectionType: 'initial' }, meta: { roles: ['inspector'] } },
  { path: '/initial/records', component: InspectionRecordView, props: { inspectionType: 'initial' }, meta: { roles: ['engineer','inspector','reviewer','viewer'] } },
  { path: '/initial/items', component: ResourceView, props: { resource: 'initial-inspection-items' }, meta: { roles: ['inspector'] } },
  { path: '/initial/defects', component: DefectManagementView, props: { inspectionType: 'initial' }, meta: { roles: ['inspector'] } },
  { path: '/initial/reports', component: ReportView, meta: { roles: ['engineer','inspector','reviewer','viewer'] } },
  { path: '/periodic/tasks', component: TaskView, props: { inspectionType: 'periodic' }, meta: { roles: ['engineer'] } },
  { path: '/inspector/periodic-workbench', component: InspectionWorkbenchView, props: { inspectionType: 'periodic' }, meta: { roles: ['inspector'] } },
  { path: '/periodic/records', component: InspectionRecordView, props: { inspectionType: 'periodic' }, meta: { roles: ['engineer','inspector','reviewer','viewer'] } },
  { path: '/periodic/components', component: ResourceView, props: { resource: 'component-inspection-records' }, meta: { roles: ['inspector'] } },
  { path: '/periodic/defects', component: DefectManagementView, props: { inspectionType: 'periodic' }, meta: { roles: ['inspector'] } },
  { path: '/periodic/reports', component: ReportView, meta: { roles: ['engineer','inspector','reviewer','viewer'] } },
  { path: '/review/initial', component: InspectionReviewWorkbenchView, props: { inspectionType: 'initial' }, meta: { roles: ['reviewer'] } },
  { path: '/review/periodic', component: InspectionReviewWorkbenchView, props: { inspectionType: 'periodic' }, meta: { roles: ['reviewer'] } },
  { path: '/review/reports', component: ReportView, meta: { roles: ['reviewer'] } },
  { path: '/query/bridges', component: ResourceView, props: { resource: 'bridges' }, meta: { roles: businessRoles } },
  { path: '/query/bridge-map', component: BridgeMapView, meta: { roles: businessRoles } },
  { path: '/query/inspections', component: InspectionResultsView, meta: { roles: businessRoles } },
  { path: '/query/defects', component: DefectQueryView, meta: { roles: businessRoles } },
  { path: '/query/reports', component: ReportView, meta: { roles: businessRoles } },
  { path: '/system/users', component: UserManagementView, meta: { roles: ['admin'] } },
  { path: '/system/roles', component: RoleManagementView, meta: { roles: ['admin'] } },
  { path: '/system/logs', component: ResourceView, props: { resource: 'logs' }, meta: { roles: ['admin'] } },
  { path: '/system/backups', component: VersionControlView, meta: { roles: ['admin'] } }
]
const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthed) return '/login'
  if (to.path === '/login' && auth.isAuthed) return auth.homePath
  // 系统管理员拥有全部工作台入口；其他角色仍严格遵循路由角色范围。
  if (to.meta.roles && auth.role !== 'admin' && !to.meta.roles.includes(auth.role)) {
    if (to.path.startsWith('/dashboard/')) return auth.homePath
    return '/403'
  }
})
export default router
