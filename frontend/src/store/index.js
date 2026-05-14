import { createStore } from 'vuex'

export default createStore({
  state: {
    user: {
      token: localStorage.getItem('token') || '',
      userId: localStorage.getItem('userId') || '',
      username: localStorage.getItem('username') || '',
      nickname: localStorage.getItem('nickname') || ''
    }
  },
  getters: {
    isLoggedIn: state => !!state.user.token
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
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('nickname')
    }
  },
  actions: {
    login({ commit }, userData) {
      commit('SET_USER', userData)
    },
    logout({ commit }) {
      commit('LOGOUT')
    }
  },
  modules: {
  }
})
