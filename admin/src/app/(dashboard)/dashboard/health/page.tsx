"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { HealthDto } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

export default function HealthPage() {
  const [data, setData] = useState<HealthDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    api.admin
      .getHealth()
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">System health</h1>
        <p className="text-muted-foreground">
          Service and dependency status. Full details via backend actuator.
        </p>
      </div>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle>Health</CardTitle>
          <Button variant="outline" size="sm" onClick={load} disabled={loading}>
            {loading ? "Refreshing…" : "Refresh"}
          </Button>
        </CardHeader>
        <CardContent>
          {error && (
            <p className="mb-4 text-sm text-destructive">{error}</p>
          )}
          {!error && data && (
            <>
              <div className="mb-4">
                <span className="text-sm text-muted-foreground">Overall: </span>
                <Badge
                  variant={data.status === "UP" ? "default" : "destructive"}
                >
                  {data.status}
                </Badge>
              </div>
              {data.components && (
                <ul className="space-y-2 text-sm">
                  {Object.entries(data.components).map(([name, comp]) => (
                    <li key={name} className="flex items-center gap-2">
                      <Badge
                        variant={
                          comp.status === "UP" ? "secondary" : "destructive"
                        }
                      >
                        {comp.status}
                      </Badge>
                      <span>{name}</span>
                      {comp.details &&
                        Object.entries(comp.details).map(([k, v]) => (
                          <span key={k} className="text-muted-foreground">
                            {k}: {String(v)}
                          </span>
                        ))}
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
