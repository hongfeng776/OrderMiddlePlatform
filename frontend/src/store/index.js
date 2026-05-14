import { createStore } from 'vuex'

const getUserFromStorage = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      return JSON.parse(userStr)
    } catch (e) {
      console.error('解析用户信息失败', e)
    }
  }
  return {
    token: '',
    userId: '',
    username: '',
    nickname: '',
    role: '',
    permissions: ''
  }
}

export default createStore({
  state: {
    user: getUserFromStorage(),
    notification: {
      unreadCount: 0
    }
  },
  getters: {
    isLoggedIn: state => !!state.user.token,
    unreadCount: state => state.notification.unreadCount,
    isAdmin: state => state.user.role === 'ADMIN'
  },
  mutations: {
    SET_USER(state, userData) {
      state.user = {
        token: userData.token,
        userId: userData.userId,
        username: userData.username,
        nickname: userData.nickname,
        role: userData.role || '',
        permissions: userData.permissions || ''
      }
      localStorage.setItem('user', JSON.stringify(state.user))
    },
    LOGOUT(state) {
      state.user = {
        token: '',
        userId: '',
        username: '',
        nickname: '',
        role: '',
        permissions: ''
      }
      state.notification.unreadCount = 0
      localStorage.removeItem('user')
    },
    SET_UNREAD_COUNT(state, count) {
      state.notification.unreadCount = count
    },
    DECREMENT_UNREAD_COUNT(state) {
      if (state.notification.unreadCount > 0) {
        state.notification.unreadCount--
      }
    },
    CLEAR_UNREAD_COUNT(state) {
      state.notification.unreadCount = 0
    }
  },
  actions: {
    login({ commit }, userData) {
      commit('SET_USER', userData)
    },
    logout({ commit }) {
      commit('LOGOUT')
    },
    setUnreadCount({ commit }, count) {
      commit('SET_UNREAD_COUNT', count)
    },
    decrementUnreadCount({ commit }) {
      commit('DECREMENT_UNREAD_COUNT')
    },
    clearUnreadCount({ commit }) {
      commit('CLEAR_UNREAD_COUNT')
    }
  },
  modules: {
  }
})
