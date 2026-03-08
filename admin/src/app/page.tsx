import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function HomePage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-6 bg-muted/30 p-4">
      <h1 className="text-2xl font-semibold">Admin</h1>
      <p className="text-muted-foreground">Admin dashboard for Olyx.</p>
      <Button asChild>
        <Link href="/login">Admin login</Link>
      </Button>
    </div>
  );
}
