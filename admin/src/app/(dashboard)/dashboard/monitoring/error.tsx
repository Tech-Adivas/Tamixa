"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { AlertCircle } from "lucide-react";

export default function MonitoringError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="flex flex-col items-center justify-center py-12">
      <Card className="w-full max-w-md border-destructive/30">
        <CardHeader className="text-center">
          <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-destructive/10">
            <AlertCircle className="h-6 w-6 text-destructive" />
          </div>
          <CardTitle>Monitoring unavailable</CardTitle>
        </CardHeader>
        <CardContent className="text-center text-sm text-muted-foreground">
          {error.message || "Could not load monitoring data."}
        </CardContent>
        <CardFooter className="flex justify-center">
          <Button onClick={reset}>Retry</Button>
        </CardFooter>
      </Card>
    </div>
  );
}
