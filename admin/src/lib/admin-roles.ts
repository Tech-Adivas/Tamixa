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
    curatedStories: boolean;
    subscriptions: boolean;
    monitoring: boolean;
    stories: boolean;
    children: boolean;
    voiceLogs: boolean;
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
    curatedStories: true,
    subscriptions: true,
    monitoring: true,
    stories: true,
    children: true,
    voiceLogs: true,
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
    curatedStories: true,
    subscriptions: true,
    monitoring: true,
    stories: true,
    children: true,
    voiceLogs: true,
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
    moderation: false,
    curatedStories: false,
    subscriptions: true,
    monitoring: false,
    stories: false,
    children: false,
    voiceLogs: false,
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
    curatedStories: true,
    subscriptions: false,
    monitoring: false,
    stories: true,
    children: true,
    voiceLogs: true,
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
    curatedStories: false,
    subscriptions: false,
    monitoring: true,
    stories: true,
    children: true,
    voiceLogs: false,
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
