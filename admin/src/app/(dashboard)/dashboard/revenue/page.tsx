"use client";

import { useEffect, useState, useCallback } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  DollarSign,
  Users,
  Percent,
  TrendingDown,
  AlertCircle,
  Cpu,
  Search,
  Clock,
  BookOpen,
  Target,
} from "lucide-react";
import {
  MrrLineChart,
  PlanPieChart,
  ChurnBarChart,
  StoryGenerationBarChart,
} from "@/components/charts";
import { RevenueAlertsBanner } from "@/components/revenue/revenue-alerts-banner";
import {
  fetchRevenueDashboard,
  fetchRevenueCharts,
  fetchRevenueAlerts,
  fetchRevenueTable,
  fetchRetentionMetrics,
  fetchCompletionMetrics,
} from "@/lib/revenue-api";
import type { RevenueRow, RevenueAlerts } from "@/types/api";

const METRIC_CARDS = [
  {
    key: "mrr" as const,
    label: "MRR",
    format: (v: number) => `$${v.toLocaleString(undefined, { minimumFractionDigits: 2 })}`,
    icon: DollarSign,
    iconBg: "bg-primary/8 text-primary",
  },
  {
    key: "activeSubscriptions" as const,
    label: "Active subscriptions",
    format: (v: number) => v.toLocaleString(),
    icon: Users,
    iconBg: "bg-primary/8 text-primary",
  },
  {
    key: "trialConversionRate" as const,
    label: "Trial conversion rate",
    format: (v: number) => `${(v * 100).toFixed(1)}%`,
    icon: Percent,
    iconBg: "bg-muted text-muted-foreground",
  },
  {
    key: "churnRate" as const,
    label: "Churn rate",
    format: (v: number) => `${(v * 100).toFixed(2)}%`,
    icon: TrendingDown,
    iconBg: "bg-destructive/8 text-destructive",
  },
  {
    key: "freeLimitHits" as const,
    label: "Free limit hits",
    format: (v: number) => v.toLocaleString(),
    icon: AlertCircle,
    iconBg: "bg-muted text-muted-foreground",
  },
  {
    key: "aiTokenCostMonthly" as const,
    label: "AI token cost (monthly)",
    format: (v: number) => `$${v.toFixed(2)}`,
    icon: Cpu,
    iconBg: "bg-muted text-muted-foreground",
  },
];

const PAGE_SIZE = 10;

export default function RevenuePage() {
  const [dashboard, setDashboard] = useState<Awaited<
    ReturnType<typeof fetchRevenueDashboard>
  > | null>(null);
  const [charts, setCharts] = useState<Awaited<
    ReturnType<typeof fetchRevenueCharts>
  > | null>(null);
  const [retention, setRetention] = useState<Awaited<
    ReturnType<typeof fetchRetentionMetrics>
  > | null>(null);
  const [completion, setCompletion] = useState<Awaited<
    ReturnType<typeof fetchCompletionMetrics>
  > | null>(null);
  const [alerts, setAlerts] = useState<RevenueAlerts | null>(null);
  const [tableData, setTableData] = useState<{
    content: RevenueRow[];
    totalElements: number;
    totalPages: number;
    page: number;
    first: boolean;
    last: boolean;
  } | null>(null);
  const [loading, setLoading] = useState(true);
  const [tableLoading, setTableLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [planFilter, setPlanFilter] = useState<string>("ALL");
  const [searchInput, setSearchInput] = useState("");

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    try {
      const [d, c, a, ret, compl] = await Promise.all([
        fetchRevenueDashboard(),
        fetchRevenueCharts(),
        fetchRevenueAlerts(),
        fetchRetentionMetrics(30),
        fetchCompletionMetrics(30),
      ]);
      setDashboard(d);
      setCharts(c);
      setAlerts(a);
      setRetention(ret);
      setCompletion(compl);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadTable = useCallback(async () => {
    setTableLoading(true);
    try {
      const res = await fetchRevenueTable(page, PAGE_SIZE, search || undefined, planFilter === "ALL" ? undefined : planFilter);
      setTableData(res);
    } finally {
      setTableLoading(false);
    }
  }, [page, search, planFilter]);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  useEffect(() => {
    loadTable();
  }, [loadTable]);

  const handleSearch = () => {
    setSearch(searchInput);
    setPage(0);
  };

  const handlePlanFilterChange = (v: string) => {
    setPlanFilter(v);
    setPage(0);
  };

  const dashboardData = dashboard ?? {
    mrr: 0,
    activeSubscriptions: 0,
    trialConversionRate: 0,
    churnRate: 0,
    freeLimitHits: 0,
    aiTokenCostMonthly: 0,
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">
          Revenue Analytics
        </h1>
        <p className="text-muted-foreground">
          MRR, subscriptions, churn, and AI cost metrics.
        </p>
      </div>

      <RevenueAlertsBanner alerts={alerts} loading={loading} />

      {/* Metric cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
        {METRIC_CARDS.map(({ key, label, format, icon: Icon, iconBg }) => (
          <Card key={key} className="border-border/80">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                {label}
              </CardTitle>
              <div className={`flex h-9 w-9 items-center justify-center rounded-md ${iconBg}`}>
                <Icon className="h-4 w-4" />
              </div>
            </CardHeader>
            <CardContent>
              {loading ? (
                <Skeleton className="h-8 w-20" />
              ) : (
                <div className="text-2xl font-semibold text-foreground">
                  {format(dashboardData[key] ?? 0)}
                </div>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Story engagement metrics (for revenue dashboard) */}
      <div>
        <h2 className="mb-3 text-lg font-medium">Story engagement</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Card className="border-border/80">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Completion rate
              </CardTitle>
              <div className="flex h-9 w-9 items-center justify-center rounded-md bg-muted">
                <Target className="h-4 w-4 text-muted-foreground" />
              </div>
            </CardHeader>
            <CardContent>
              {loading ? (
                <Skeleton className="h-8 w-20" />
              ) : (
                <div className="text-2xl font-semibold text-foreground">
                  {completion
                    ? `${(completion.overallCompletionRate * 100).toFixed(1)}%`
                    : "-"}
                </div>
              )}
            </CardContent>
          </Card>
          <Card className="border-border/80">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Avg listen time
              </CardTitle>
              <div className="flex h-9 w-9 items-center justify-center rounded-md bg-muted">
                <Clock className="h-4 w-4 text-muted-foreground" />
              </div>
            </CardHeader>
            <CardContent>
              {loading ? (
                <Skeleton className="h-8 w-20" />
              ) : (
                <div className="text-2xl font-semibold text-foreground">
                  {retention
                    ? `${Math.round(retention.averageListenTimeSeconds / 60)}m`
                    : "-"}
                </div>
              )}
            </CardContent>
          </Card>
          <Card className="border-border/80">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Most popular category
              </CardTitle>
              <div className="flex h-9 w-9 items-center justify-center rounded-md bg-muted">
                <BookOpen className="h-4 w-4 text-muted-foreground" />
              </div>
            </CardHeader>
            <CardContent>
              {loading ? (
                <Skeleton className="h-8 w-20" />
              ) : (
                <div className="text-2xl font-semibold text-foreground">
                  {retention?.mostPopularCategory || "-"}
                </div>
              )}
            </CardContent>
          </Card>
          <Card className="border-border/80">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                Retention by language
              </CardTitle>
              <div className="flex h-9 w-9 items-center justify-center rounded-md bg-muted">
                <Percent className="h-4 w-4 text-muted-foreground" />
              </div>
            </CardHeader>
            <CardContent>
              {loading ? (
                <Skeleton className="h-8 w-20" />
              ) : (
                <div className="text-sm font-medium text-foreground">
                  {retention?.retentionByLanguage &&
                  Object.keys(retention.retentionByLanguage).length > 0 ? (
                    <span className="truncate">
                      {Object.entries(retention.retentionByLanguage)
                        .map(([lang, rate]) => `${lang}: ${(rate * 100).toFixed(0)}%`)
                        .join(", ")}
                    </span>
                  ) : (
                    "-"
                  )}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Charts */}
      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary/8">
                <DollarSign className="h-4 w-4 text-primary" />
              </div>
              MRR over time
            </CardTitle>
            <p className="text-sm text-muted-foreground">
              Monthly recurring revenue trend
            </p>
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-64 w-full" />
            ) : (
              <MrrLineChart data={charts?.mrrOverTime ?? []} />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary/8">
                <Users className="h-4 w-4 text-primary" />
              </div>
              Plan distribution
            </CardTitle>
            <p className="text-sm text-muted-foreground">
              Subscriptions by plan
            </p>
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-64 w-full" />
            ) : (
              <PlanPieChart data={charts?.planDistribution ?? []} />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <div className="flex h-8 w-8 items-center justify-center rounded-md bg-destructive/8">
                <TrendingDown className="h-4 w-4 text-destructive" />
              </div>
              Churn trend
            </CardTitle>
            <p className="text-sm text-muted-foreground">
              Monthly churn rate
            </p>
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-64 w-full" />
            ) : (
              <ChurnBarChart data={charts?.churnTrend ?? []} />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary/8">
                <AlertCircle className="h-4 w-4 text-primary" />
              </div>
              Story generation per plan
            </CardTitle>
            <p className="text-sm text-muted-foreground">
              Stories generated by plan type
            </p>
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-64 w-full" />
            ) : (
              <StoryGenerationBarChart
                data={charts?.storyGenerationByPlan ?? []}
              />
            )}
          </CardContent>
        </Card>
      </div>

      {/* Revenue table */}
      <Card>
        <CardHeader>
          <CardTitle>Revenue table</CardTitle>
          <p className="text-sm text-muted-foreground">
            Parent subscriptions with plan and payment details
          </p>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search by parent ID or email..."
                className="pl-9"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSearch()}
              />
            </div>
            <Button variant="secondary" size="sm" onClick={handleSearch}>
              Search
            </Button>
            <Select value={planFilter} onValueChange={handlePlanFilterChange}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="Filter by plan" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All plans</SelectItem>
                <SelectItem value="FREE">Free</SelectItem>
                <SelectItem value="PREMIUM_MONTHLY">Monthly</SelectItem>
                <SelectItem value="PREMIUM_YEARLY">Yearly</SelectItem>
                <SelectItem value="FAMILY">Family</SelectItem>
                <SelectItem value="VOICE_PREMIUM">Voice Premium</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {tableLoading ? (
            <Skeleton className="h-48 w-full" />
          ) : tableData?.content.length === 0 ? (
            <p className="py-8 text-center text-sm text-muted-foreground">
              No revenue records found.
            </p>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Parent ID</TableHead>
                    <TableHead>Plan</TableHead>
                    <TableHead>Subscription state</TableHead>
                    <TableHead>Monthly payment</TableHead>
                    <TableHead>Created date</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {(tableData?.content ?? []).map((row) => (
                    <TableRow key={row.parentId}>
                      <TableCell className="font-mono text-sm">
                        {row.parentId}
                      </TableCell>
                      <TableCell>{row.plan.replace(/_/g, " ")}</TableCell>
                      <TableCell>{row.subscriptionState}</TableCell>
                      <TableCell>
                        ${row.monthlyPayment.toFixed(2)}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {new Date(row.createdAt).toLocaleDateString()}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <div className="flex items-center justify-between">
                <p className="text-sm text-muted-foreground">
                  {tableData?.totalElements ?? 0} total · page {page + 1} of{" "}
                  {tableData?.totalPages ?? 1}
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={tableData?.first ?? true}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={tableData?.last ?? true}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
