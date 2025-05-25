import { createRouter, createWebHashHistory } from "vue-router";

const routes = [
  {
    path: "/",
    name: "Master",
    component: () => import("@/viewer/Master.vue"),  // 移除async/await
    children: [
      {
        path: "/:channel/center",
        name: "Center",
        component: () => import("@/viewer/Home.vue")  // 移除async/await
      },
      {
        path: "/:channel/login",
        name: "Login",
        component: () => import("@/viewer/Login.vue")  // 移除async/await
      }
    ]
  }
];

const router = createRouter({
  routes,
  history: createWebHashHistory()
});

export default router;