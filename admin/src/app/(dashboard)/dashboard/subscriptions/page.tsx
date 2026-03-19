"use client";

import { useEffect, useState, useCallback } from "react";
import { api } from "@/lib/api";
import { mockApi } from "@/lib/mock-api";
import type {
  SubscriptionStatus,
  PagedResponse,
  AdminInvoice,
} from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/empty-state";
import { CreditCard, Receipt, RotateCcw } from "lucide-react";
import { useActionResult } from "@/contexts/action-result-context";

const PAGE_SIZE = 10;
const INVOICE_PAGE_SIZE = 5;

export default function SubscriptionsPage() {
  const { showSuccess, showError } = useActionResult();
  const [subs, setSubs] = useState<PagedResponse<SubscriptionStatus> | null>(
    null
  );
  const [invoices, setInvoices] = useState<PagedResponse<AdminInvoice> | null>(
    null
  );
  const [page, setPage] = useState(0);
  const [invoicePage, setInvoicePage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refundingId, setRefundingId] = useState<number | null>(null);

  const loadInvoices = useCallback(() => {
    if (mockApi.useMock()) {
      setInvoices(mockApi.getMockInvoices(invoicePage, INVOICE_PAGE_SIZE));
      return;
    }
    api.admin
      .getInvoices(invoicePage, INVOICE_PAGE_SIZE)
      .then(setInvoices)
      .catch(() => setInvoices({ content: [], page: 0, size: INVOICE_PAGE_SIZE, totalElements: 0, totalPages: 0, first: true, last: true }));
  }, [invoicePage]);

  useEffect(() => {
    setError(null);
    if (mockApi.useMock()) {
      setSubs(mockApi.getMockSubscriptions(page, PAGE_SIZE));
      setInvoices(mockApi.getMockInvoices(invoicePage, INVOICE_PAGE_SIZE));
      setLoading(false);
      return;
    }
    setLoading(true);
    Promise.all([
      api.admin.getSubscriptions(page, PAGE_SIZE).catch((e) => {
        setError(e.message);
        return null;
      }),
      api.admin.getInvoices(invoicePage, INVOICE_PAGE_SIZE).catch(() => ({
        content: [],
        page: 0,
        size: INVOICE_PAGE_SIZE,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
      })),
    ]).then(([subsRes, invRes]) => {
      if (subsRes) setSubs(subsRes);
      if (invRes) setInvoices(invRes);
    }).finally(() => setLoading(false));
  }, [page, invoicePage]);

  const handleRefund = async (invoiceId: number) => {
    setRefundingId(invoiceId);
    try {
      if (mockApi.useMock()) {
        await new Promise((r) => setTimeout(r, 600));
        showSuccess("Refund initiated", "Refund has been initiated (mock).");
      } else {
        await api.admin.refundInvoice(invoiceId);
        showSuccess("Refund processed", "The refund has been processed successfully.");
      }
      loadInvoices();
    } catch {
      showError("Refund failed", "Unable to process the refund. Please try again.");
    } finally {
      setRefundingId(null);
    }
  };

  const planBreakdown =
    subs?.content.reduce(
      (acc, row) => {
        const plan = row.plan ?? "Other";
        acc[plan] = (acc[plan] ?? 0) + 1;
        return acc;
      },
      {} as Record<string, number>
    ) ?? {};

  return (
    <div className="space-y-8">
      {/* Page header */}
      <div className="rounded-2xl border-2 border-border bg-gradient-to-r from-primary/5 via-primary/[0.03] to-transparent p-4 sm:p-6">
        <h1 className="page-header">Subscription management</h1>
        <p className="page-subheader">
          View subscription status, invoices, and payment history. Refunds are
          processed via your billing provider.
        </p>
      </div>

      {/* Plan breakdown */}
      <Card className="border-2 border-border overflow-hidden">
        <CardHeader className="border-b border-border/50 bg-muted/20 pb-4">
          <CardTitle className="text-base font-bold flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-tamixa-purple/15 text-tamixa-purple">
              <CreditCard className="h-5 w-5" />
            </div>
            Plan breakdown
          </CardTitle>
        </CardHeader>
        <CardContent className="pt-5">
          {loading && !subs ? (
            <Skeleton className="h-16 w-full rounded-xl" />
          ) : Object.keys(planBreakdown).length === 0 ? (
            <p className="text-sm text-muted-foreground">No plans yet.</p>
          ) : (
            <div className="flex flex-wrap gap-3">
              {Object.entries(planBreakdown).map(([plan, count]) => (
                <div
                  key={plan}
                  className="rounded-xl border-2 border-primary/20 bg-primary/5 px-4 py-3 transition-colors hover:bg-primary/10"
                >
                  <span className="text-sm font-semibold text-foreground">{plan}</span>
                  <span className="ml-2 text-sm text-muted-foreground">
                    {count} subscription{count !== 1 ? "s" : ""}
                  </span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Subscriptions table */}
      <Card className="border-2 border-border overflow-hidden">
        <CardHeader className="border-b border-border/50 bg-muted/20">
          <CardTitle className="text-base font-bold">Subscriptions</CardTitle>
          <p className="text-sm text-muted-foreground mt-0.5">Active and past subscriptions by parent.</p>
        </CardHeader>
        <CardContent className="p-0">
          {error && (
            <p className="px-6 pt-6 pb-2 text-sm font-medium text-destructive">{error}</p>
          )}
          {loading && !subs ? (
            <div className="p-6">
              <Skeleton className="h-48 w-full rounded-xl" />
            </div>
          ) : subs?.content.length === 0 ? (
            <div className="p-6">
              <EmptyState
                icon={CreditCard}
                title="No subscriptions"
                description="Subscription data will appear here."
              />
            </div>
          ) : subs ? (
            <>
              <Table>
                <TableHeader>
                  <TableRow className="hover:bg-transparent">
                    <TableHead className="font-semibold">Parent ID</TableHead>
                    <TableHead className="font-semibold">Email</TableHead>
                    <TableHead className="font-semibold">Status</TableHead>
                    <TableHead className="font-semibold">Plan</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {subs.content.map((row) => (
                    <TableRow key={row.parentId} className="transition-colors hover:bg-muted/30">
                      <TableCell className="font-medium">{row.parentId}</TableCell>
                      <TableCell>{row.email}</TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            row.status === "ACTIVE"
                              ? "default"
                              : "secondary"
                          }
                          className="rounded-lg"
                        >
                          {row.status}
                        </Badge>
                      </TableCell>
                      <TableCell>{row.plan ?? "—"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <div className="flex items-center justify-between border-t border-border bg-muted/10 px-6 py-4">
                <p className="text-sm text-muted-foreground">
                  {subs.totalElements} total · page {page + 1} of{" "}
                  {subs.totalPages || 1}
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={subs.first}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    className="rounded-xl"
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={subs.last}
                    onClick={() => setPage((p) => p + 1)}
                    className="rounded-xl"
                  >
                    Next
                  </Button>
                </div>
              </div>
            </>
          ) : null}
        </CardContent>
      </Card>

      {/* Invoices table */}
      <Card className="border-2 border-border overflow-hidden">
        <CardHeader className="border-b border-border/50 bg-muted/20 flex flex-row items-center justify-between space-y-0">
          <div>
            <CardTitle className="text-base font-bold flex items-center gap-2">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-tamixa-blue/15 text-tamixa-blue">
                <Receipt className="h-5 w-5" />
              </div>
              Invoices
            </CardTitle>
            <p className="text-sm text-muted-foreground mt-0.5">Payment history and refunds.</p>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {!invoices ? (
            <div className="p-6">
              <Skeleton className="h-48 w-full rounded-xl" />
            </div>
          ) : invoices.content.length === 0 ? (
            <div className="p-6">
              <EmptyState
                icon={Receipt}
                title="No invoices"
                description="Invoices will appear here."
              />
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow className="hover:bg-transparent">
                    <TableHead className="font-semibold">Invoice</TableHead>
                    <TableHead className="font-semibold">Email</TableHead>
                    <TableHead className="font-semibold">Plan</TableHead>
                    <TableHead className="font-semibold">Amount</TableHead>
                    <TableHead className="font-semibold">Status</TableHead>
                    <TableHead className="font-semibold">Due / Paid</TableHead>
                    <TableHead className="w-[100px] font-semibold">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {invoices.content.map((row) => (
                    <TableRow key={row.id} className="transition-colors hover:bg-muted/30">
                      <TableCell className="font-mono text-xs">
                        {row.providerInvoiceId ?? String(row.id)}
                      </TableCell>
                      <TableCell>{row.email}</TableCell>
                      <TableCell>{row.plan}</TableCell>
                      <TableCell className="font-medium">
                        {row.currency === "INR" ? `₹${row.amount.toLocaleString("en-IN", { minimumFractionDigits: 2 })}` : `${row.currency} ${row.amount.toFixed(2)}`}
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            row.status === "PAID"
                              ? "default"
                              : row.status === "REFUNDED"
                                ? "secondary"
                                : row.status === "FAILED"
                                  ? "destructive"
                                  : "outline"
                          }
                          className="rounded-lg"
                        >
                          {row.status}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {row.paidAt
                          ? `Paid ${new Date(row.paidAt).toLocaleDateString()}`
                          : `Due ${new Date(row.dueDate).toLocaleDateString()}`}
                      </TableCell>
                      <TableCell>
                        {row.status === "PAID" && (
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={refundingId === row.id}
                            onClick={() => handleRefund(row.id)}
                            className="rounded-xl"
                          >
                            <RotateCcw className="mr-1 h-3 w-3" />
                            {refundingId === row.id ? "Refunding…" : "Refund"}
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <div className="flex items-center justify-between border-t border-border bg-muted/10 px-6 py-4">
                <p className="text-sm text-muted-foreground">
                  {invoices.totalElements} total · page {invoicePage + 1} of{" "}
                  {invoices.totalPages || 1}
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={invoices.first}
                    onClick={() =>
                      setInvoicePage((p) => Math.max(0, p - 1))
                    }
                    className="rounded-xl"
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={invoices.last}
                    onClick={() => setInvoicePage((p) => p + 1)}
                    className="rounded-xl"
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
