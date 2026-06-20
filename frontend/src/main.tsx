import React from "react";
import ReactDOM from "react-dom/client";
import App from "@/App";
/** 全局样式入口，引入 Tailwind CSS 及自定义基础样式 */
import "@/index.css";

/**
 * 应用入口文件
 * 使用 React 18 的 createRoot API 挂载根组件到 DOM 节点 #root。
 * StrictMode 会在开发模式下启用额外的检查与警告，帮助发现潜在问题。
 */
ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
