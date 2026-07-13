import { createRouter, createWebHistory } from 'vue-router'
import Main from './views/Main.vue'
import SearchPage from './views/SearchPage.vue'
import MovieView from './views/MovieView.vue'
import SeriesView from './views/SeriesView.vue'
import TorrentInfoView from './views/TorrentInfoView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: Main
  },
  {
    path: '/search',
    name: 'search',
    component: SearchPage
  },
  {
    path: '/movie/:movieId',
    name: 'movie',
    component: MovieView
  },
  {
    path: '/series/:seriesId',
    name: 'series',
    component: SeriesView
  },
  {
    path: '/torrents',
    name: 'torrents',
    component: TorrentInfoView
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
