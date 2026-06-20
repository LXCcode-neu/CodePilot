/** localStorage 中存储 JWT Token 所使用的键名 */
const TOKEN_KEY = "codepilot_token";

/**
 * 获取本地存储的 Token
 * @returns Token 字符串，若不存在则返回 null
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

/**
 * 将 Token 存入 localStorage
 * @param token - 需要存储的 JWT Token 字符串
 */
export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

/**
 * 清除本地存储的 Token（退出登录时调用）
 */
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

/**
 * 判断本地是否存在有效的 Token
 * @returns 存在 Token 返回 true，否则返回 false
 */
export function hasToken() {
  return Boolean(getToken());
}
