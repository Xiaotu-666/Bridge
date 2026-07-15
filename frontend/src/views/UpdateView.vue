<template>
  <div>
    <el-row :gutter="14">
      <el-col :xs="24" :lg="9">
        <section class="panel">
          <div class="toolbar">
            <div>
              <strong>当前版本</strong>
              <div class="muted">系统运行版本与 GitHub 仓库</div>
            </div>
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="版本号">{{ version.version_no || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Git Commit">{{ version.git_commit || '-' }}</el-descriptions-item>
            <el-descriptions-item label="仓库">{{ version.repository_url || '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-button type="primary" :loading="checking" style="margin-top:14px" @click="checkUpdate">
            检测更新
          </el-button>
        </section>
      </el-col>
      <el-col :xs="24" :lg="15">
        <section class="panel">
          <div class="toolbar">
            <div>
              <strong>检测结果</strong>
              <div class="muted">检测 GitHub Release 或发布清单，不自动覆盖当前系统</div>
            </div>
          </div>
          <el-empty v-if="!result" description="尚未检测更新" />
          <div v-else-if="!result.hasUpdate">
            <el-alert :title="result.message || '暂无可用更新'" type="info" show-icon :closable="false" />
          </div>
          <div v-else>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="版本号">{{ result.record.version_no }}</el-descriptions-item>
              <el-descriptions-item label="发布日期">{{ result.record.release_date || '-' }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ result.record.download_status }}</el-descriptions-item>
              <el-descriptions-item label="下载地址">{{ result.record.download_url }}</el-descriptions-item>
              <el-descriptions-item label="说明">{{ result.record.release_notes || '-' }}</el-descriptions-item>
            </el-descriptions>
            <el-button type="primary" :loading="downloading" style="margin-top:14px" @click="download(result.record)">
              下载发布包
            </el-button>
          </div>
        </section>
      </el-col>
    </el-row>

    <section class="table-panel" style="margin-top:14px">
      <div class="toolbar">
        <strong>更新记录</strong>
        <el-button @click="loadRecords">刷新</el-button>
      </div>
      <el-table :data="records" border>
        <el-table-column prop="update_id" label="记录ID" width="150" />
        <el-table-column prop="version_no" label="版本" width="120" />
        <el-table-column prop="download_status" label="状态" width="110" />
        <el-table-column prop="package_size" label="大小" width="110" />
        <el-table-column prop="sha256" label="SHA-256" min-width="260" show-overflow-tooltip />
        <el-table-column prop="local_path" label="本地路径" min-width="220" show-overflow-tooltip />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const version = ref({})
const result = ref(null)
const records = ref([])
const checking = ref(false)
const downloading = ref(false)

onMounted(async () => {
  version.value = await http.get('/system/version')
  await loadRecords()
})

async function checkUpdate() {
  checking.value = true
  try {
    result.value = await http.post('/system/updates/check')
    if (result.value.hasUpdate) {
      ElMessage.success('发现发布版本')
      await loadRecords()
    } else {
      ElMessage.info(result.value.message || '暂无更新')
    }
  } finally {
    checking.value = false
  }
}

async function download(row) {
  downloading.value = true
  try {
    await http.post(`/system/updates/${row.update_id}/download`)
    ElMessage.success('发布包下载并校验完成')
    await loadRecords()
  } finally {
    downloading.value = false
  }
}

async function loadRecords() {
  const data = await http.get('/updates', { params: { size: 20 } })
  records.value = data.records
}
</script>
