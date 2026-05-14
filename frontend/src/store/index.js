import { createStore } from 'vuex'

export default createStore({
  state: {
    user: {
      token: localStorage.getItem('token') || '',
      userId: localStorage.getItem('userId') || '',
      username: localStorage.getItem('username') || '',
      nickname: localStorage.getItem('nickname') || ''
    },
    notification: {
      unreadCount: 0
    }
  },
  getters: {
    isLoggedIn: state => !!state.user.token,
    unreadCount: state => state.notification.unreadCount
  },
  mutations: {
    SET_USER(state, userData) {
      state.user = userData
      localStorage.setItem('token', userData.token)
      localStorage.setItem('userId', userData.userId)
      localStorage.setItem('username', userData.username)
      localStorage.setItem('nickname', userData.nickname)
    },
    LOGOUT(state) {
      state.user = {
        token: '',
        userId: '',
        username: '',
        nickname: ''
      }
      state.notification.unreadCount = 0
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('nickname')
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
