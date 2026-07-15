<template>
  <div>
    <div class="page-grid">
      <div class="metric-card">
        <span>桥梁档案</span>
        <strong>{{ metrics.bridges }}</strong>
      </div>
      <div class="metric-card">
        <span>检查任务</span>
        <strong>{{ metrics.tasks }}</strong>
      </div>
      <div class="metric-card">
        <span>检测数据</span>
        <strong>{{ metrics.data }}</strong>
      </div>
      <div class="metric-card">
        <span>缺损记录</span>
        <strong>{{ metrics.defects }}</strong>
      </div>
    </div>

    <el-row :gutter="14" style="margin-top: 14px">
      <el-col :xs="24" :lg="14">
        <section class="panel">
          <div class="toolbar">
            <div>
              <strong>近期检查任务</strong>
              <div class="muted">跟踪待分配、进行中、已完成和已审核状态</div>
            </div>
            <el-button type="primary" @click="$router.push('/tasks')">进入任务</el-button>
          </div>
          <el-table :data="tasks" height="320">
            <el-table-column prop="task_id" label="任务编号" width="130" />
            <el-table-column prop="bridge_code" label="桥梁编码" width="120" />
            <el-table-column prop="inspection_type" label="检查类型" />
            <el-table-column prop="task_status" label="状态">
              <template #default="{ row }">
                <span :class="statusDot(row.task_status)" />{{ row.task_status }}
              </template>
            </el-table-column>
          </el-table>
        </section>
      </el-col>
      <el-col :xs="24" :lg="10">
        <section class="panel">
          <div class="toolbar">
            <div>
              <strong>系统运行</strong>
              <div class="muted">后端服务与版本信息</div>
            </div>
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="服务状态">运行中</el-descriptions-item>
            <el-descriptions-item label="当前版本">{{ version.version_no || '1.0.0' }}</el-descriptions-item>
            <el-descriptions-item label="远程仓库">{{ version.repository_url || '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-button style="margin-top: 14px" @click="$router.push('/updates')">检查版本更新</el-button>
        </section>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '../api/http'

const metrics = reactive({ bridges: 0, tasks: 0, data: 0, defects: 0 })
const tasks = ref([])
const version = ref({})

onMounted(load)

async function load() {
  const [bridges, taskPage, dataPage, defectPage, versionData] = await Promise.all([
    http.get('/bridges', { params: { size: 1 } }),
    http.get('/tasks', { params: { size: 8 } }),
    http.get('/inspection-data', { params: { size: 1 } }),
    http.get('/defects', { params: { size: 1 } }),
    http.get('/system/version')
  ])
  metrics.bridges = bridges.total
  metrics.tasks = taskPage.total
  metrics.data = dataPage.total
  metrics.defects = defectPage.total
  tasks.value = taskPage.records
  version.value = versionData
}

function statusDot(status) {
  if (status === '已审核') return 'ok-dot'
  if (status === '进行中' || status === '已完成') return 'warn-dot'
  if (status === '已取消') return 'danger-dot'
  return 'warn-dot'
}
</script>
