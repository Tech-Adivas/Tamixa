/**
 * Admin roles that can access the dashboard.
 * Maps to backend Role enum.
 */
export const ADMIN_ROLES = [
  "ADMIN",
  "SUPER_ADMIN",
  "REVENUE_ANALYST",
  "CONTENT_MANAGER",
  "SUPPORT",
] as const;

export type AdminRole = (typeof ADMIN_ROLES)[number];

/** Permissions per role. Revenue Analytics visible only to SUPER_ADMIN, ADMIN, REVENUE_ANALYST. */
export const ROLE_NAV_PERMISSIONS: Record<
  string,
  {
    dashboard: boolean;
    revenue: boolean;
    parents: boolean;
    moderation: boolean;
    storyLibrary: boolean;
    storyForReview: boolean;
    pipelineTriage: boolean;
    subscriptions: boolean;
    referralCodes: boolean;
    monitoring: boolean;
    stories: boolean;
    voiceLogs: boolean;
    voiceTest: boolean;
    health: boolean;
    aiMetrics: boolean;
    kafka: boolean;
    audit: boolean;
    users: boolean;
  }
> = {
  ADMIN: {
    dashboard: true,
    revenue: true,
    parents: true,
    moderation: true,
    storyLibrary: true,
    storyForReview: true,
    pipelineTriage: true,
    subscriptions: true,
    referralCodes: true,
    monitoring: true,
    stories: true,
    voiceLogs: true,
    voiceTest: true,
    health: true,
    aiMetrics: true,
    kafka: true,
    audit: true,
    users: true,
  },
  SUPER_ADMIN: {
    dashboard: true,
    revenue: true,
    parents: true,
    moderation: true,
    storyLibrary: true,
    storyForReview: true,
    pipelineTriage: true,
    subscriptions: true,
    referralCodes: true,
    monitoring: true,
    stories: true,
    voiceLogs: true,
    voiceTest: true,
    health: true,
    aiMetrics: true,
    kafka: true,
    audit: true,
    users: true,
  },
  REVENUE_ANALYST: {
    dashboard: true,
    revenue: true,
    parents: false,
    referralCodes: true,
    moderation: false,
    storyLibrary: false,
    storyForReview: false,
    pipelineTriage: false,
    subscriptions: true,
    monitoring: false,
    stories: false,
    voiceLogs: false,
    voiceTest: false,
    health: false,
    aiMetrics: true,
    kafka: false,
    audit: false,
    users: false,
  },
  CONTENT_MANAGER: {
    dashboard: true,
    revenue: false,
    parents: false,
    moderation: true,
    storyLibrary: true,
    storyForReview: true,
    pipelineTriage: true,
    subscriptions: false,
    referralCodes: false,
    monitoring: false,
    stories: true,
    voiceLogs: true,
    voiceTest: true,
    health: false,
    aiMetrics: false,
    kafka: false,
    audit: false,
    users: false,
  },
  SUPPORT: {
    dashboard: true,
    revenue: false,
    parents: true,
    moderation: false,
    storyLibrary: false,
    storyForReview: false,
    pipelineTriage: false,
    subscriptions: false,
    referralCodes: false,
    monitoring: true,
    stories: true,
    voiceLogs: false,
    voiceTest: false,
    health: true,
    aiMetrics: false,
    kafka: false,
    audit: false,
    users: false,
  },
};

export function isAdminRole(role: string): role is AdminRole {
  return ADMIN_ROLES.includes(role as AdminRole);
}

export function canAccessNav(role: string, key: keyof (typeof ROLE_NAV_PERMISSIONS)["SUPER_ADMIN"]): boolean {
  const perms = ROLE_NAV_PERMISSIONS[role];
  return perms?.[key] ?? false;
}

/** True only for SUPER_ADMIN. Use to gate destructive actions (e.g. delete stories). */
export function isSuperAdmin(role: string): boolean {
  return role === "SUPER_ADMIN";
}

/** True if user can manage story library (create, edit, trigger pipeline, regenerate). Uses permissions from /me when available. */
export function canManageStories(user: { role?: string; permissions?: string[] } | null): boolean {
  if (!user) return false;
  if (user.permissions?.includes("MANAGE_STORIES")) return true;
  return canAccessNav(user.role ?? "", "storyLibrary");
}

/** True if user can moderate (approve for delivery, flag stories). Uses permissions from /me when available. */
export function canModerateStories(user: { role?: string; permissions?: string[] } | null): boolean {
  if (!user) return false;
  if (user.permissions?.includes("MODERATE_STORIES")) return true;
  return canAccessNav(user.role ?? "", "storyForReview");
}
