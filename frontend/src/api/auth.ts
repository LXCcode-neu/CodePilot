import { request } from "@/api/request";
import type { LoginRequest, LoginResponse, RegisterRequest, User } from "@/types/auth";

/**
 * 用户注册
 * @param data - 注册请求参数，包含用户名、密码等注册信息
 * @returns 返回注册成功的用户信息
 */
export function register(data: RegisterRequest) {
  return request.post<User>("/api/auth/register", data);
}

/**
 * 用户登录
 * @param data - 登录请求参数，包含用户名和密码
 * @returns 返回登录响应，包含 token 等认证信息
 */
export function login(data: LoginRequest) {
  return request.post<LoginResponse>("/api/auth/login", data);
}

/**
 * 获取当前登录用户信息
 * @returns 返回当前已认证用户的详细信息
 */
export function getCurrentUser() {
  return request.get<User>("/api/user/me");
}
