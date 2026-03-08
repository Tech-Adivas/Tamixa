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

  const loadSubs = useCallback(() => {
    if (mockApi.useMock()) {
      setSubs(mockApi.getMockSubscriptions(page, PAGE_SIZE));
      return;
    }
    api.admin
      .getSubscriptions(page, PAGE_SIZE)
      .then(setSubs)
      .catch((e) => setError(e.message));
  }, [page]);

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
    setLoading(true);
    setError(null);
    loadSubs();
    loadInvoices();
    setLoading(false);
  }, [loadSubs, loadInvoices]);

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
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">
          Subscription management
        </h1>
        <p className="text-muted-foreground">
          View subscription status, invoices, and payment history. Refunds are
          processed via your billing provider.
        </p>
      </div>

      {/* Plan breakdown */}
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-base flex items-center gap-2">
            <CreditCard className="h-4 w-4" />
            Plan breakdown
          </CardTitle>
        </CardHeader>
        <CardContent>
          {loading && !subs ? (
            <Skeleton className="h-16 w-full" />
          ) : Object.keys(planBreakdown).length === 0 ? (
            <p className="text-sm text-muted-foreground">No plans yet.</p>
          ) : (
            <div className="flex flex-wrap gap-4">
              {Object.entries(planBreakdown).map(([plan, count]) => (
                <div
                  key={plan}
                  className="rounded-lg border border-border bg-muted/30 px-4 py-2"
                >
                  <span className="text-sm font-medium">{plan}</span>
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
      <Card>
        <CardHeader>
          <CardTitle>Subscriptions</CardTitle>
        </CardHeader>
        <CardContent>
          {error && (
            <p className="mb-4 text-sm text-destructive">{error}</p>
          )}
          {loading && !subs ? (
            <Skeleton className="h-48 w-full" />
          ) : subs?.content.length === 0 ? (
            <EmptyState
              icon={CreditCard}
              title="No subscriptions"
              description="Subscription data will appear here."
            />
          ) : subs ? (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Parent ID</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Plan</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {subs.content.map((row) => (
                    <TableRow key={row.parentId}>
                      <TableCell>{row.parentId}</TableCell>
                      <TableCell>{row.email}</TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            row.status === "ACTIVE"
                              ? "default"
                              : "secondary"
                          }
                        >
                          {row.status}
                        </Badge>
                      </TableCell>
                      <TableCell>{row.plan ?? "—"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <div className="mt-4 flex items-center justify-between">
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
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={subs.last}
                    onClick={() => setPage((p) => p + 1)}
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
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="flex items-center gap-2">
            <Receipt className="h-4 w-4" />
            Invoices
          </CardTitle>
        </CardHeader>
        <CardContent>
          {!invoices ? (
            <Skeleton className="h-48 w-full" />
          ) : invoices.content.length === 0 ? (
            <EmptyState
              icon={Receipt}
              title="No invoices"
              description="Invoices will appear here."
            />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Invoice</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Plan</TableHead>
                    <TableHead>Amount</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Due / Paid</TableHead>
                    <TableHead className="w-[100px]">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {invoices.content.map((row) => (
                    <TableRow key={row.id}>
                      <TableCell className="font-mono text-xs">
                        {row.providerInvoiceId ?? String(row.id)}
                      </TableCell>
                      <TableCell>{row.email}</TableCell>
                      <TableCell>{row.plan}</TableCell>
                      <TableCell>
                        {row.currency} {row.amount.toFixed(2)}
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
              <div className="mt-4 flex items-center justify-between">
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
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={invoices.last}
                    onClick={() => setInvoicePage((p) => p + 1)}
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
