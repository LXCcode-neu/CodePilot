import { RouterProvider } from "react-router-dom";
import { router } from "@/routes/router";

/**
 * 应用根组件
 * 作为整个应用的入口，通过 RouterProvider 注入路由配置，
 * 由 react-router-dom 接管页面渲染与导航逻辑。
 */
export default function App() {
  return <RouterProvider router={router} />;
}
