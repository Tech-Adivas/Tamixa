"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { VoiceUploadLog, PagedResponse } from "@/types/api";
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
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { Mic } from "lucide-react";

const PAGE_SIZE = 20;

export default function VoiceLogsPage() {
  const [data, setData] = useState<PagedResponse<VoiceUploadLog> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    api.admin
      .getVoiceLogs(page, PAGE_SIZE)
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [page]);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Voice upload logs"
        description="Monitor voice profile uploads by parent."
        breadcrumbs
      />
      <Card>
        <CardHeader>
          <CardTitle>Voice uploads</CardTitle>
        </CardHeader>
        <CardContent>
          {error && (
            <p className="mb-4 text-sm text-destructive">{error}</p>
          )}
          {loading ? (
            <div className="space-y-3">
              <Skeleton className="h-10 w-full" />
              {[1, 2, 3, 4, 5].map((i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : data ? (
            <>
              {data.content.length === 0 ? (
                <EmptyState
                  icon={Mic}
                  title="No voice uploads yet"
                  description="Voice profile uploads will appear here when parents complete voice setup."
                />
              ) : (
              <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>ID</TableHead>
                    <TableHead>Parent ID</TableHead>
                    <TableHead>Parent email</TableHead>
                    <TableHead>Created</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.content.map((row) => (
                    <TableRow key={row.id}>
                      <TableCell>{row.id}</TableCell>
                      <TableCell>{row.parentId}</TableCell>
                      <TableCell>{row.parentEmail ?? "—"}</TableCell>
                      <TableCell>
                        {new Date(row.createdAt).toLocaleString()}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <div className="mt-4 flex items-center justify-between">
                <p className="text-sm text-muted-foreground">
                  {data.totalElements} total · page {data.page + 1} of{" "}
                  {data.totalPages || 1}
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={data.first}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={data.last}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
              </>
              )}
            </>
          ) : null}
        </CardContent>
      </Card>
    </div>
  );
}
