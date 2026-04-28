export interface PatchRecord {
  id?: string | number | null;
  taskId?: string | number | null;
  analysis?: string | null;
  solution?: string | null;
  patch?: string | null;
  risk?: string | null;
  safetyCheckResult?: string | null;
  rawOutput?: string | null;
  confirmed?: boolean | null;
  confirmedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  raw?: unknown;
}
