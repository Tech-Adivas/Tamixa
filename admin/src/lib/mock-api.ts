import type {
  AdminInvoice,
  DashboardKpis,
  ReferralCode,
  RevenueChartPoint,
  StoryUsageChartPoint,
  SystemMonitoringMetrics,
  PagedResponse,
  ParentSummary,
  StorySummary,
  SubscriptionStatus,
} from "@/types/api";

const useMock = () =>
  typeof process !== "undefined" &&
  process.env.NEXT_PUBLIC_USE_MOCK_API === "true";

// Dashboard KPIs
export function getMockDashboardKpis(): DashboardKpis {
  return {
    activeSubscriptions: 1247,
    monthlyRevenue: 18650,
    storyGenerationsToday: 342,
    aiTokenUsage: 1284000,
    moderationFlags: 12,
  };
}

// Revenue chart - last 6 months
export function getMockRevenueChart(): RevenueChartPoint[] {
  return [
    { month: "Sep", revenue: 14200 },
    { month: "Oct", revenue: 15800 },
    { month: "Nov", revenue: 17100 },
    { month: "Dec", revenue: 18200 },
    { month: "Jan", revenue: 17900 },
    { month: "Feb", revenue: 18650 },
  ];
}

// Story usage - last 7 days
export function getMockStoryUsageChart(): StoryUsageChartPoint[] {
  return [
    { date: "Mon", count: 280 },
    { date: "Tue", count: 310 },
    { date: "Wed", count: 295 },
    { date: "Thu", count: 340 },
    { date: "Fri", count: 368 },
    { date: "Sat", count: 420 },
    { date: "Sun", count: 342 },
  ];
}

// Invoices for subscription management
export function getMockInvoices(
  page: number,
  size: number
): PagedResponse<AdminInvoice> {
  const all: AdminInvoice[] = [
    {
      id: 1,
      providerInvoiceId: "inv_001",
      parentId: 1,
      email: "parent1@example.com",
      amount: 9.99,
      currency: "INR",
      status: "PAID",
      plan: "Monthly",
      dueDate: "2025-02-01",
      paidAt: "2025-02-01",
    },
    {
      id: 2,
      providerInvoiceId: "inv_002",
      parentId: 2,
      email: "parent2@example.com",
      amount: 89.99,
      currency: "INR",
      status: "PAID",
      plan: "Annual",
      dueDate: "2025-01-15",
      paidAt: "2025-01-15",
    },
    {
      id: 3,
      providerInvoiceId: "inv_003",
      parentId: 3,
      email: "parent3@example.com",
      amount: 9.99,
      currency: "INR",
      status: "PENDING",
      plan: "Monthly",
      dueDate: "2025-02-26",
      paidAt: null,
    },
    {
      id: 4,
      providerInvoiceId: "inv_004",
      parentId: 4,
      email: "parent4@example.com",
      amount: 9.99,
      currency: "INR",
      status: "REFUNDED",
      plan: "Monthly",
      dueDate: "2025-01-10",
      paidAt: "2025-01-10",
    },
  ];
  const start = page * size;
  const content = all.slice(start, start + size);
  return {
    content,
    page,
    size,
    totalElements: all.length,
    totalPages: Math.ceil(all.length / size) || 1,
    first: page === 0,
    last: start + content.length >= all.length,
  };
}

// System monitoring metrics
export function getMockSystemMetrics(): SystemMonitoringMetrics {
  return {
    apiLatencyMs: 42,
    kafkaLag: 12,
    redisHitRatio: 0.94,
    errorRate: 0.002,
    aiCostUsd: 124.5,
  };
}

// Mock parents with name, plan, status (for when API is unavailable or for demo)
export function getMockParents(
  page: number,
  size: number,
  search?: string,
  statusFilter?: string
): PagedResponse<ParentSummary> {
  const all: ParentSummary[] = [
    {
      id: 1,
      name: "Jane Doe",
      email: "jane@example.com",
      role: "USER",
      plan: "Annual",
      status: "ACTIVE",
      createdAt: "2024-06-01T10:00:00Z",
    },
    {
      id: 2,
      name: "John Smith",
      email: "john@example.com",
      role: "USER",
      plan: "Monthly",
      status: "ACTIVE",
      createdAt: "2024-08-15T14:30:00Z",
    },
    {
      id: 3,
      name: "Alice Brown",
      email: "alice@example.com",
      role: "USER",
      plan: "Monthly",
      status: "SUSPENDED",
      createdAt: "2024-09-20T09:00:00Z",
    },
  ];
  let filtered = all;
  if (search) {
    const q = search.toLowerCase();
    filtered = filtered.filter(
      (p) =>
        p.email.toLowerCase().includes(q) ||
        (p.name && p.name.toLowerCase().includes(q))
    );
  }
  if (statusFilter && statusFilter !== "ALL") {
    filtered = filtered.filter((p) => p.status === statusFilter);
  }
  const start = page * size;
  const content = filtered.slice(start, start + size);
  return {
    content,
    page,
    size,
    totalElements: filtered.length,
    totalPages: Math.ceil(filtered.length / size) || 1,
    first: page === 0,
    last: start + content.length >= filtered.length,
  };
}

// Mock stories for moderation (READY / FLAGGED / FAILED)
export function getMockStories(
  page: number,
  size: number,
  status?: string,
  theme?: string
): PagedResponse<StorySummary> {
  const all: StorySummary[] = [
    {
      id: 1,
      parentId: 1,
      childId: 10,
      theme: "Space adventure",
      language: "en",
      age: 5,
      childName: "Emma",
      wordCount: 320,
      status: "READY",
      safetyScore: 92,
      createdAt: "2025-02-25T10:00:00Z",
    },
    {
      id: 2,
      parentId: 2,
      childId: 20,
      theme: "Under the sea",
      language: "en",
      age: 4,
      childName: "Leo",
      wordCount: 280,
      status: "FLAGGED",
      safetyScore: 45,
      createdAt: "2025-02-24T15:30:00Z",
    },
    {
      id: 3,
      parentId: 1,
      childId: 10,
      theme: "Dinosaurs",
      language: "en",
      age: 5,
      childName: "Emma",
      wordCount: 0,
      status: "FAILED",
      safetyScore: null,
      createdAt: "2025-02-23T09:00:00Z",
    },
  ];
  let filtered = all;
  if (status && status !== "ALL") {
    filtered = filtered.filter((s) => s.status === status);
  }
  if (theme && theme.trim()) {
    const q = theme.trim().toLowerCase();
    filtered = filtered.filter((s) => s.theme.toLowerCase().includes(q));
  }
  const start = page * size;
  const content = filtered.slice(start, start + size);
  return {
    content,
    page,
    size,
    totalElements: filtered.length,
    totalPages: Math.ceil(filtered.length / size) || 1,
    first: page === 0,
    last: start + content.length >= filtered.length,
  };
}

// Mock story body for preview
export function getMockStoryBody(storyId: number): string {
  if (storyId === 1) {
    return `Once upon a time, Emma put on her astronaut helmet and climbed into her cardboard rocket. "Three, two, one, blast off!" she shouted. The rocket zoomed past the moon and the stars. She waved at the friendly aliens on Mars and floated in zero gravity. When she landed back in her garden, she told her mom all about the adventure. The end.`;
  }
  if (storyId === 2) {
    return `Leo dreamed of swimming with fish. One day he found a magic snorkel. Under the sea he met a kind octopus and a singing dolphin. They showed him a treasure chest full of shiny shells. Leo shared the shells with his friends. The end.`;
  }
  return `Story content for #${storyId} would appear here.`;
}

// Mock subscriptions
export function getMockSubscriptions(
  page: number,
  size: number
): PagedResponse<SubscriptionStatus> {
  const all: SubscriptionStatus[] = [
    { parentId: 1, email: "jane@example.com", status: "ACTIVE", plan: "Annual" },
    { parentId: 2, email: "john@example.com", status: "ACTIVE", plan: "Monthly" },
    { parentId: 3, email: "alice@example.com", status: "CANCELLED", plan: "Monthly" },
  ];
  const start = page * size;
  const content = all.slice(start, start + size);
  return {
    content,
    page,
    size,
    totalElements: all.length,
    totalPages: Math.ceil(all.length / size) || 1,
    first: page === 0,
    last: start + content.length >= all.length,
  };
}

// Referral codes (mock)
export function getMockReferralCodes(): ReferralCode[] {
  const now = new Date().toISOString();
  const nextYear = new Date();
  nextYear.setFullYear(nextYear.getFullYear() + 1);
  return [
    {
      id: 1,
      shortcode: "AMAZ5",
      shopName: "Amazon",
      offerPercent: 5,
      expiresAt: nextYear.toISOString(),
      active: true,
      stripeCouponId: null,
      createdAt: now,
      updatedAt: now,
    },
    {
      id: 2,
      shortcode: "SHOPSTOP10",
      shopName: "ShopStop",
      offerPercent: 10,
      expiresAt: nextYear.toISOString(),
      active: true,
      stripeCouponId: null,
      createdAt: now,
      updatedAt: now,
    },
  ];
}

export const mockApi = {
  useMock,
  getMockDashboardKpis,
  getMockRevenueChart,
  getMockStoryUsageChart,
  getMockInvoices,
  getMockSystemMetrics,
  getMockParents,
  getMockStories,
  getMockStoryBody,
  getMockSubscriptions,
  getMockReferralCodes,
};
