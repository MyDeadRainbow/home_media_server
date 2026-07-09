import { createRouter, createWebHistory } from 'vue-router'
import Main from '../views/Main.vue'
import SearchPage from '../views/SearchPage.vue'
import SeriesView from '../views/SeriesView.vue'
import MovieView from '../views/MovieView.vue'
import Profile from '../views/Profile.vue'

const routes = [
  {
    path: '/',
    component: Main
  },
  {
    path: '/search',
    component: SearchPage
  },
  {
    path: '/series/:seriesId',
    component: SeriesView
  },
  {
    path: '/movie/:movieId',
    component: MovieView
  },
  {
    path: '/profile',
    component: Profile
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router