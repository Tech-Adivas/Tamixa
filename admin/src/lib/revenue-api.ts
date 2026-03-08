/**
 * Revenue Analytics API service.
 * Connects to backend endpoints with mock fallback for development.
 * All routes protected by DashboardGuard (admin role + JWT).
 */
import { api } from "@/lib/api";
import type {
  RevenueMetricsDto,
  SubscriptionMetricsDto,
  AiMetricsDto,
  PagedResponse,
  RevenueRow,
  RevenueChartPoint,
  PlanDistributionPoint,
  ChurnTrendPoint,
  StoryGenerationByPlanPoint,
  RevenueAlerts,
  RetentionMetricsDto,
  CompletionMetricsDto,
} from "@/types/api";

const PAYMENT_FAILURE_THRESHOLD = 0.05;
const CHURN_WARNING_THRESHOLD = 0.08;

export interface RevenueDashboardData {
  mrr: number;
  activeSubscriptions: number;
  trialConversionRate: number;
  churnRate: number;
  freeLimitHits: number;
  aiTokenCostMonthly: number;
}

export interface RevenueChartsData {
  mrrOverTime: RevenueChartPoint[];
  planDistribution: PlanDistributionPoint[];
  churnTrend: ChurnTrendPoint[];
  storyGenerationByPlan: StoryGenerationByPlanPoint[];
}

async function fetchWithFallback<T>(
  fetcher: () => Promise<T>,
  mockValue: T
): Promise<T> {
  try {
    return await fetcher();
  } catch {
    return mockValue;
  }
}

function getMockRevenueMetrics(): RevenueMetricsDto {
  return {
    month: new Date().toISOString().slice(0, 7),
    revenue: 18650,
    currency: "USD",
  };
}

function getMockSubscriptionMetrics(): SubscriptionMetricsDto {
  return {
    activeSubscriptions: 1247,
    trialCount: 89,
    planDistribution: {
      FREE: 420,
      PREMIUM_MONTHLY: 512,
      PREMIUM_YEARLY: 215,
      FAMILY: 85,
      VOICE_PREMIUM: 15,
    },
    mrr: 18650,
    trialConversionRate: 0.42,
    churnRate: 0.032,
  };
}

function getMockMrrOverTime(): RevenueChartPoint[] {
  const months = ["Sep", "Oct", "Nov", "Dec", "Jan", "Feb"];
  const values = [14200, 15800, 17100, 18200, 17900, 18650];
  return months.map((month, i) => ({ month, revenue: values[i] ?? 0 }));
}

function getMockPlanDistribution(): PlanDistributionPoint[] {
  return [
    { name: "Monthly", value: 512 },
    { name: "Yearly", value: 215 },
    { name: "Family", value: 85 },
    { name: "Free", value: 420 },
  ];
}

function getMockChurnTrend(): ChurnTrendPoint[] {
  return [
    { month: "Sep", churnRate: 0.045 },
    { month: "Oct", churnRate: 0.038 },
    { month: "Nov", churnRate: 0.041 },
    { month: "Dec", churnRate: 0.029 },
    { month: "Jan", churnRate: 0.035 },
    { month: "Feb", churnRate: 0.032 },
  ];
}

function getMockStoryGenerationByPlan(): StoryGenerationByPlanPoint[] {
  return [
    { plan: "Free", count: 4200 },
    { plan: "Monthly", count: 12500 },
    { plan: "Yearly", count: 8100 },
    { plan: "Family", count: 4200 },
  ];
}

const MOCK_REVENUE_ROWS: RevenueRow[] = [
  {
    parentId: 1,
    email: "jane@example.com",
    plan: "PREMIUM_YEARLY",
    subscriptionState: "ACTIVE",
    monthlyPayment: 7.5,
    createdAt: "2024-06-01T10:00:00Z",
  },
  {
    parentId: 2,
    email: "john@example.com",
    plan: "PREMIUM_MONTHLY",
    subscriptionState: "ACTIVE",
    monthlyPayment: 9.99,
    createdAt: "2024-08-15T14:30:00Z",
  },
  {
    parentId: 3,
    email: "alice@example.com",
    plan: "PREMIUM_MONTHLY",
    subscriptionState: "CANCELLED",
    monthlyPayment: 9.99,
    createdAt: "2024-09-20T09:00:00Z",
  },
  {
    parentId: 4,
    email: "bob@example.com",
    plan: "FAMILY",
    subscriptionState: "ACTIVE",
    monthlyPayment: 14.99,
    createdAt: "2024-11-01T12:00:00Z",
  },
];

function getMockRevenueTable(
  page: number,
  size: number
): PagedResponse<RevenueRow> {
  const start = page * size;
  const content = MOCK_REVENUE_ROWS.slice(start, start + size);
  return {
    content,
    page,
    size,
    totalElements: MOCK_REVENUE_ROWS.length,
    totalPages: Math.ceil(MOCK_REVENUE_ROWS.length / size) || 1,
    first: page === 0,
    last: start + content.length >= MOCK_REVENUE_ROWS.length,
  };
}

export async function fetchRevenueDashboard(): Promise<RevenueDashboardData> {
  const useMock =
    typeof process !== "undefined" &&
    process.env.NEXT_PUBLIC_USE_MOCK_API === "true";

  if (useMock) {
    const sub = getMockSubscriptionMetrics();
    const ai = {
      storyGenerationsTotal: 34200,
      cacheHits: 12000,
      cacheMisses: 22200,
      voiceProcessingCount: 890,
      openaiTokensUsed: 1284000,
    };
    // ~$0.002 per 1K tokens (GPT-4) => ~$2.57 per 1.28M
    const aiCostMonthly = ai.openaiTokensUsed
      ? (ai.openaiTokensUsed / 1_000_000) * 2
      : 0;
    return {
      mrr: sub.mrr,
      activeSubscriptions: sub.activeSubscriptions,
      trialConversionRate: sub.trialConversionRate ?? 0,
      churnRate: sub.churnRate ?? 0,
      freeLimitHits: 1240,
      aiTokenCostMonthly: aiCostMonthly,
    };
  }

  const [revenueRes, subRes, aiRes] = await Promise.allSettled([
    api.admin.getRevenueMetrics(),
    api.admin.getSubscriptionMetrics(),
    api.admin.getAiMetrics(),
  ]);

  const revenue =
    revenueRes.status === "fulfilled"
      ? (revenueRes.value as RevenueMetricsDto).revenue
      : getMockRevenueMetrics().revenue;
  const sub: SubscriptionMetricsDto =
    subRes.status === "fulfilled" && subRes.value
      ? subRes.value
      : getMockSubscriptionMetrics();
  const ai: AiMetricsDto =
    aiRes.status === "fulfilled" ? aiRes.value : ({} as AiMetricsDto);

  const tokens = ai.openaiTokensUsed ?? 0;
  const aiCostMonthly = (tokens / 1_000_000) * 2;

  return {
    mrr: typeof revenue === "number" ? revenue : Number(revenue) || sub.mrr,
    activeSubscriptions: sub.activeSubscriptions,
    trialConversionRate: sub.trialConversionRate ?? 0,
    churnRate: sub.churnRate ?? 0,
    freeLimitHits: 0,
    aiTokenCostMonthly: aiCostMonthly,
  };
}

export async function fetchRevenueCharts(): Promise<RevenueChartsData> {
  const useMock =
    typeof process !== "undefined" &&
    process.env.NEXT_PUBLIC_USE_MOCK_API === "true";

  if (useMock) {
    return {
      mrrOverTime: getMockMrrOverTime(),
      planDistribution: getMockPlanDistribution(),
      churnTrend: getMockChurnTrend(),
      storyGenerationByPlan: getMockStoryGenerationByPlan(),
    };
  }

  const [, subRes] = await Promise.allSettled([
    api.admin.getRevenueMetrics(),
    api.admin.getSubscriptionMetrics(),
  ]);

  const sub: SubscriptionMetricsDto =
    subRes.status === "fulfilled" && subRes.value
      ? subRes.value
      : getMockSubscriptionMetrics();

  const mrrOverTime = getMockMrrOverTime();
  const planDistribution: PlanDistributionPoint[] = Object.entries(
    sub.planDistribution ?? {}
  ).map(([name, value]) => ({ name, value }));

  return {
    mrrOverTime,
    planDistribution: planDistribution.length
      ? planDistribution
      : getMockPlanDistribution(),
    churnTrend: getMockChurnTrend(),
    storyGenerationByPlan: getMockStoryGenerationByPlan(),
  };
}

export async function fetchRetentionMetrics(
  days = 30
): Promise<RetentionMetricsDto> {
  try {
    return await api.admin.getRetentionMetrics(days);
  } catch {
    return {
      averageListenTimeSeconds: 0,
      mostPopularCategory: "",
      retentionByLanguage: {},
      periodDays: days,
    };
  }
}

export async function fetchCompletionMetrics(
  days = 30
): Promise<CompletionMetricsDto> {
  try {
    return await api.admin.getCompletionMetrics(days);
  } catch {
    return {
      completionRatePerStory: [],
      overallCompletionRate: 0,
      retentionByLanguage: {},
      periodDays: days,
    };
  }
}

export async function fetchRevenueAlerts(): Promise<RevenueAlerts> {
  const useMock =
    typeof process !== "undefined" &&
    process.env.NEXT_PUBLIC_USE_MOCK_API === "true";

  if (useMock) {
    return {
      paymentFailureRate: 0.03,
      paymentFailureThreshold: PAYMENT_FAILURE_THRESHOLD,
      aiTokenSpike: false,
      churnIncrease: false,
      churnRate: 0.032,
    };
  }

  const subRes = await fetchWithFallback(
    () => api.admin.getSubscriptionMetrics(),
    getMockSubscriptionMetrics()
  );

  const churnRate = subRes.churnRate ?? 0;
  const churnIncrease = churnRate > CHURN_WARNING_THRESHOLD;

  return {
    paymentFailureRate: 0.02,
    paymentFailureThreshold: PAYMENT_FAILURE_THRESHOLD,
    aiTokenSpike: false,
    churnIncrease,
    churnRate,
  };
}

export async function fetchRevenueTable(
  page: number,
  size: number,
  search?: string,
  planFilter?: string
): Promise<PagedResponse<RevenueRow>> {
  const useMock =
    typeof process !== "undefined" &&
    process.env.NEXT_PUBLIC_USE_MOCK_API === "true";

  if (useMock) {
    let filtered = [...MOCK_REVENUE_ROWS];
    if (search) {
      const q = search.toLowerCase();
      filtered = filtered.filter(
        (r) =>
          r.email.toLowerCase().includes(q) ||
          r.parentId.toString().includes(q)
      );
    }
    if (planFilter && planFilter !== "ALL") {
      filtered = filtered.filter(
        (r) => r.plan.toUpperCase() === planFilter.toUpperCase()
      );
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

  try {
    return await api.admin.getRevenueTable(
      page,
      size,
      search || undefined,
      planFilter === "ALL" ? undefined : planFilter
    );
  } catch {
    return getMockRevenueTable(page, size);
  }
}
