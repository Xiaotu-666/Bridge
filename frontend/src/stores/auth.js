import { defineStore } from 'pinia'
import http from '../api/http'
const homes={admin:'/dashboard/admin',engineer:'/dashboard/engineer',inspector:'/dashboard/inspector',reviewer:'/dashboard/reviewer',viewer:'/dashboard/viewer'}
export const useAuthStore = defineStore('auth', {
  state: () => ({ token: localStorage.getItem('bridge_token') || '', user: JSON.parse(localStorage.getItem('bridge_user') || 'null') }),
  getters: {
    isAuthed: (state) => Boolean(state.token), permissions: (state) => state.user?.permissions || [], roles: (state) => state.user?.roles || [],
    role: (state) => state.user?.roles?.[0] || '', homePath: (state) => homes[state.user?.roles?.[0]] || '/login'
  },
  actions: {
    async login(account, password) { const data=await http.post('/auth/login',{account,password});this.token=data.token;this.user=data.user;localStorage.setItem('bridge_token',data.token);localStorage.setItem('bridge_user',JSON.stringify(data.user));return data.homePath },
    logout(){this.token='';this.user=null;localStorage.removeItem('bridge_token');localStorage.removeItem('bridge_user')},
    can(permission){return this.role==='admin'||this.permissions.includes('*')||this.permissions.includes(permission)}
  }
})
