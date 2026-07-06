<template>
<nav class="navbar">
        <div class="nav-logo">
            <a href="#">MyBrand</a>
        </div>
        <ul class="nav-links">
            <li><a @click="navigateToHome">Home</a></li>
            <li><a @click="">Series</a></li>
            <li><a @click="">Movies</a></li>
            <li><a @click="navigateToProfile">Profile</a></li>
        </ul>
    </nav>

  <router-view />
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()

function navigateToHome (){
   router.push({
    path: `/`
  })
}

function navigateToSeries (){
   router.push({
    path: `/`
  })
}

function navigateToMovies (){
   router.push({
    path: `/`
  })
}

function navigateToProfile (){
   router.push({
    path: `/profile`
  })
}
</script>