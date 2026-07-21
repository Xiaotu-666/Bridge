<template>
  <main class="login-page" :class="{ 'panel-hidden': !panelVisible }">
    <button
      type="button"
      class="panel-visibility-toggle"
      :class="{ hidden: !panelVisible }"
      :title="panelVisible ? '隐藏登录面板' : '显示登录面板'"
      :aria-label="panelVisible ? '隐藏登录面板' : '显示登录面板'"
      :aria-pressed="!panelVisible"
      @click="panelVisible = !panelVisible"
    >
      <PanelRightClose v-if="panelVisible" :size="20" />
      <PanelRightOpen v-else :size="20" />
    </button>
    <section class="login-visual">
      <video class="login-video" autoplay muted loop playsinline>
        <source src="/huajiang-canyon-bridge.mp4" type="video/mp4" />
      </video>
      <div class="scene-shade" aria-hidden="true" />

      <div class="visual-content">
        <div class="visual-kicker">公路桥梁检查</div>
        <h1>公路桥梁检查信息系统</h1>
        <p>桥梁档案、检查任务、病害记录与技术报告统一管理。</p>
      </div>
    </section>

    <section v-show="panelVisible" class="login-panel">
      <div class="login-shell">
        <div class="product-mark">
          <div><h2>桥梁检查系统登录注册页面</h2></div>
        </div>

        <div class="login-tabs" role="tablist" aria-label="账户操作">
          <button type="button" :class="{ active: mode === 'login' }" @click="mode = 'login'">账号登录</button>
          <button type="button" :class="{ active: mode === 'register' }" @click="mode = 'register'">用户注册</button>
        </div>

        <div class="login-title">
          <h2>{{ mode === 'login' ? '账号登录' : '注册查询账号' }}</h2>
        </div>

        <el-form v-if="mode === 'login'" :model="loginForm" label-position="top" class="login-form" @keyup.enter="submitLogin">
          <el-form-item label="账号">
            <el-input v-model="loginForm.account" size="large" autocomplete="username" placeholder="请输入账号" autofocus>
              <template #prefix><UserRound :size="18" /></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="loginForm.password" size="large" type="password" autocomplete="current-password" show-password placeholder="请输入密码">
              <template #prefix><LockKeyhole :size="18" /></template>
            </el-input>
          </el-form-item>
          <el-button size="large" type="primary" class="login-button" :loading="loading" @click="submitLogin">
            <span>进入系统</span><ArrowRight v-if="!loading" :size="18" />
          </el-button>
        </el-form>

        <el-form v-else :model="registerForm" label-position="top" class="register-form">
          <div class="register-grid">
            <el-form-item label="账号"><el-input v-model="registerForm.account" placeholder="字母开头，4至30位"><template #prefix><UserRound :size="17" /></template></el-input></el-form-item>
            <el-form-item label="姓名"><el-input v-model="registerForm.realName" placeholder="请输入姓名" /></el-form-item>
            <el-form-item label="密码"><el-input v-model="registerForm.password" type="password" show-password placeholder="至少8位，含字母和数字"><template #prefix><LockKeyhole :size="17" /></template></el-input></el-form-item>
            <el-form-item label="确认密码"><el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="再次输入密码" /></el-form-item>
            <el-form-item label="单位或部门"><el-input v-model="registerForm.department" placeholder="选填" /></el-form-item>
            <el-form-item label="联系电话"><el-input v-model="registerForm.phone" placeholder="选填" /></el-form-item>
            <el-form-item label="电子邮箱" class="full-row"><el-input v-model="registerForm.email" placeholder="选填" /></el-form-item>
          </div>
          <el-alert title="注册账号默认为查询人员" type="info" :closable="false" class="register-alert" />
          <el-button size="large" type="primary" class="login-button" :loading="loading" @click="submitRegister">
            <span>完成注册</span><ArrowRight v-if="!loading" :size="18" />
          </el-button>
        </el-form>

        <footer class="login-footer"><span>系统仅展示经授权上传的桥梁资料与现场照片。</span></footer>
      </div>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Landmark, LockKeyhole, PanelRightClose, PanelRightOpen, UserRound } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'
import http from '../api/http'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const mode = ref('login')
const panelVisible = ref(true)
const loginForm = reactive({ account: '', password: '' })
const registerForm = reactive({ account: '', realName: '', password: '', confirmPassword: '', department: '', phone: '', email: '' })

async function submitLogin() {
  if (!loginForm.account.trim() || !loginForm.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const home = await auth.login(loginForm.account.trim(), loginForm.password)
    loginForm.password = ''
    router.push(home)
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  if (!/^[A-Za-z][A-Za-z0-9_]{3,29}$/.test(registerForm.account)) {
    ElMessage.warning('账号须以字母开头，只能包含字母、数字和下划线，长度4至30位')
    return
  }
  if (!registerForm.realName.trim()) {
    ElMessage.warning('请输入真实姓名')
    return
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,64}$/.test(registerForm.password)) {
    ElMessage.warning('密码须为8至64位并同时包含字母和数字')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await http.post('/auth/register', registerForm)
    ElMessage.success('注册成功，请使用新账号登录')
    loginForm.account = registerForm.account
    loginForm.password = ''
    mode.value = 'login'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page{position:relative;min-height:100vh;display:grid;grid-template-columns:minmax(0,1.35fr) minmax(420px,.65fr);background:#edf2f4}.login-page.panel-hidden{grid-template-columns:1fr}.panel-visibility-toggle{position:fixed;z-index:20;top:18px;right:18px;width:40px;height:40px;border:1px solid #cbd5e1;border-radius:8px;display:grid;place-items:center;background:rgba(255,255,255,.92);color:#334155;cursor:pointer;box-shadow:0 5px 16px rgba(15,23,42,.12)}.panel-visibility-toggle:hover{background:#fff;color:#0f766e}.panel-visibility-toggle.hidden{border-color:rgba(255,255,255,.45);background:rgba(6,22,27,.58);color:#fff}.panel-visibility-toggle.hidden:hover{background:rgba(6,22,27,.78)}.login-visual{position:relative;min-height:100vh;overflow:hidden;display:flex;align-items:flex-end;padding:56px;background:radial-gradient(circle at 76% 20%,rgba(45,212,191,.22),transparent 30%),linear-gradient(135deg,#062932 0%,#0b4350 48%,#07161b 100%)}.login-video{position:absolute;inset:0;width:100%;height:100%;object-fit:cover;z-index:0}.scene-shade{position:absolute;inset:0;background:linear-gradient(0deg,rgba(5,17,22,.78),rgba(6,22,27,.22));box-shadow:inset 0 -260px 190px rgba(5,17,22,.38);z-index:1}.visual-content{position:relative;z-index:2;max-width:760px;color:#fff}.visual-kicker{color:#99f6e4;font-size:13px;font-weight:700}.visual-content h1{margin:16px 0 12px;font-size:44px;line-height:1.18;letter-spacing:0}.visual-content p{margin:0;color:#d8e8eb;font-size:17px}.login-panel{min-height:100vh;display:grid;place-items:center;background:#f8fafb;padding:34px 44px;overflow:auto}.login-shell{width:100%;max-width:470px}.product-mark{display:flex;align-items:center;gap:12px;margin-bottom:34px}.mark-icon{width:44px;height:44px;display:grid;place-items:center;background:#0f766e;color:#fff;border-radius:8px}.product-mark strong,.product-mark span{display:block}.product-mark strong{font-size:16px}.product-mark span{font-size:10px;color:#64748b;margin-top:3px}.login-tabs{display:grid;grid-template-columns:1fr 1fr;border:1px solid #cbd5e1;border-radius:8px;padding:3px;margin-bottom:28px;background:#eef2f4}.login-tabs button{border:0;border-radius:6px;padding:10px;background:transparent;color:#64748b;cursor:pointer}.login-tabs button.active{background:#fff;color:#172033;font-weight:700;box-shadow:0 1px 4px rgba(15,23,42,.1)}.login-title{margin-bottom:24px}.login-title span{font-size:11px;font-weight:700;color:#0f766e}.login-title h2{margin:7px 0 0;font-size:28px}.login-form :deep(.el-form-item),.register-form :deep(.el-form-item){margin-bottom:18px}.login-form :deep(.el-input__wrapper),.register-form :deep(.el-input__wrapper){border-radius:6px}.login-button{width:100%;height:46px;margin-top:4px}.login-button :deep(span){display:flex;align-items:center;justify-content:center;gap:10px}.register-grid{display:grid;grid-template-columns:1fr 1fr;gap:0 14px}.full-row{grid-column:1/-1}.register-alert{margin-bottom:16px}.login-footer{display:flex;justify-content:space-between;gap:16px;margin-top:30px;padding-top:16px;border-top:1px solid #dbe3ec;color:#94a3b8;font-size:11px}@media(max-width:980px){.login-page{grid-template-columns:1fr}.login-visual{min-height:310px;padding:34px}.login-panel{min-height:auto;padding:34px 24px}.visual-content h1{font-size:34px}}@media(max-width:560px){.panel-visibility-toggle{top:12px;right:12px}.login-visual{min-height:260px;padding:24px}.visual-content h1{font-size:28px}.visual-content p{font-size:14px}.login-panel{padding:28px 18px}.product-mark{margin-bottom:24px}.register-grid{grid-template-columns:1fr}.full-row{grid-column:auto}.login-footer{flex-direction:column;gap:4px}}
</style>
