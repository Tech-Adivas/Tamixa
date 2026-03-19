import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function HomePage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-6 bg-tamixa-page p-4">
      <h1 className="text-3xl font-bold tracking-tight text-foreground">Tamixa Admin</h1>
      <p className="text-muted-foreground">Listen • Learn • Shine</p>
      <Button asChild variant="primary" size="lg" className="rounded-xl">
        <Link href="/login">Admin login</Link>
      </Button>
    </div>
  );
}
