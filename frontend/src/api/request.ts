import axios, { AxiosError, type AxiosRequestConfig } from "axios";
import { clearToken, getToken } from "@/lib/token";
import type { Result } from "@/types/common";

const client = axios.create({
  baseURL: "",
  timeout: 20000,
  headers: {
    "Content-Type": "application/json",
  },
});

function redirectToLogin() {
  clearToken();
  if (window.location.pathname !== "/login") {
    window.location.assign("/login");
  }
}

client.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => {
    const payload = response.data as Result<unknown>;
    if (payload && typeof payload.code === "number") {
      if (payload.code === 0) {
        return payload.data;
      }
      if (payload.code === 401) {
        redirectToLogin();
      }
      return Promise.reject(new Error(payload.message || "请求失败"));
    }
    return response.data;
  },
  (error: AxiosError<Result<never>>) => {
    if (error.response?.status === 401 || error.response?.data?.code === 401) {
      redirectToLogin();
    }
    return Promise.reject(
      new Error(error.response?.data?.message || error.message || "网络异常")
    );
  }
);

export const request = {
  get<T>(url: string, config?: AxiosRequestConfig) {
    return client.get(url, config) as Promise<T>;
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return client.post(url, data, config) as Promise<T>;
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return client.put(url, data, config) as Promise<T>;
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return client.delete(url, config) as Promise<T>;
  },
};
