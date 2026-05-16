export interface SentryProjectMapping {
  id: string;
  projectId: string;
  sentryOrganizationSlug: string;
  sentryProjectSlug: string;
  enabled: boolean;
  autoRunEnabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SaveSentryProjectMappingRequest {
  sentryOrganizationSlug: string;
  sentryProjectSlug: string;
  enabled: boolean;
  autoRunEnabled: boolean;
}
