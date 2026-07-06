<template>
    <div class="page">
        <header class="hero">
            <h1> {{ isAuthenticated && user?.name ? `Hallo ${user.name}` : "Welcome" }} </h1>
            <p>Please log in or sign up</p>
        </header>

    <div>
        <section class="panel">
            <div class="library-header">
                <h2>Profile Details</h2>
                <button type="button" class="secondary-button" @click="navigateToHome">Home</button>
            </div>
            <div v-if="!isAuthenticated">
  <form @submit.prevent="handleSubmit">
    <div class="login-container">
      <div class="loginform">
        <h2>{{ isLogin ? "Log In" : "Sign Up" }}</h2>
        <div class="login-fields">
            <!-- Username only for signup -->
            <div class="login-field" v-if="!isLogin">
                <label><b>Username</b></label>
                <input v-model="username" type="text" placeholder="Enter Username" required>
            </div>
            <div class="login-field">
                <label><b>Email</b></label>
                <input v-model="email" type="text" placeholder="Enter Email" required>
            </div>
            <div class="login-field">
                <label><b>Password</b></label>
                <input v-model="password" type="password" placeholder="Enter Password" required>
            </div>
        </div>
      </div>
      <div class="loginbuttons">
        <!-- ONE submit button -->
        <button type="submit">
          {{ isLogin ? "Login" : "Sign Up" }}
        </button>
        <!-- toggle -->
        <p @click="toggleMode" style="cursor:pointer;">
          {{ isLogin
            ? "Don't have an account? Sign up"
            : "Already have an account? Login"
          }}
        </p>
      </div>
    </div>
  </form>
</div>

<div v-else>
  <!-- Profile Content -->
  <h1>{{ user?.name ? `Hallo ${user.name}` : "Welcome" }}</h1>
  <button @click="logout">Logout</button>
</div>
        </section>
      </div>

    </div>




    
</template>

<script setup>
import { ref, computed } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

// form state
const isLogin = ref(true);
const email = ref("");
const password = ref("");
const username = ref("");

// reactivity trigger
const authTrigger = ref(0);

// auth check
const isAuthenticated = computed(() => {
  authTrigger.value;
  return !!localStorage.getItem("authToken");
});

// user data
const user = computed(() => {
  authTrigger.value;
  const u = localStorage.getItem("user");
  return u ? JSON.parse(u) : null;
});

// submit (login/signup)
function handleSubmit() {
  if (isLogin.value) {
    // LOGIN
    localStorage.setItem("authToken", "123456");
  } else {
    // SIGN UP
    localStorage.setItem("authToken", "123456");

    localStorage.setItem("user", JSON.stringify({
      name: username.value
    }));
  }

  authTrigger.value++;

  // optional redirect
  router.push("/");
}

// toggle login/signup
function toggleMode() {
  isLogin.value = !isLogin.value;

  // clear fields (nice UX)
  email.value = "";
  password.value = "";
  username.value = "";
}

// logout
function logout() {
  localStorage.removeItem("authToken");
  localStorage.removeItem("user");
  authTrigger.value++;
}
</script>