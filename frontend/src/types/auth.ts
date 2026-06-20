/** 用户信息 */
export interface User {
  /** 用户唯一标识 */
  id: string;
  /** 用户名 */
  username: string;
  /** 邮箱地址 */
  email: string;
  /** 创建时间 */
  createdAt: string;
  /** 更新时间 */
  updatedAt: string;
}

/** 登录请求参数 */
export interface LoginRequest {
  /** 用户名 */
  username: string;
  /** 密码 */
  password: string;
}

/** 注册请求参数 */
export interface RegisterRequest {
  /** 用户名 */
  username: string;
  /** 邮箱地址 */
  email: string;
  /** 密码 */
  password: string;
}

/** 登录响应结果 */
export interface LoginResponse {
  /** 认证令牌 */
  token: string;
  /** 令牌类型，如 Bearer */
  tokenType: string;
  /** 登录用户信息 */
  user: User;
}
