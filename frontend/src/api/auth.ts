import { request } from "@/api/request";
import type { LoginRequest, LoginResponse, RegisterRequest, User } from "@/types/auth";

export function register(data: RegisterRequest) {
  return request.post<User>("/api/auth/register", data);
}

export function login(data: LoginRequest) {
  return request.post<LoginResponse>("/api/auth/login", data);
}

export function getCurrentUser() {
  return request.get<User>("/api/user/me");
}
