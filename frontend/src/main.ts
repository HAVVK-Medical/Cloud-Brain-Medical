import { createApp } from 'vue';

import App from './App.vue';
import router from './router';
import { useAuthStore } from './stores/auth';
import { pinia } from './stores/pinia';
import './styles/tailwind.css';

const app = createApp(App);
app.use(pinia);
app.use(router);
useAuthStore(pinia).hydrateFromStorage();
app.mount('#app');
