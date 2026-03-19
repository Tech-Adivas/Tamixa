import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-xl border-2 px-3 py-1 text-xs font-semibold transition-colors duration-standard focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2",
  {
    variants: {
      variant: {
        default:
          "border-transparent bg-primary text-primary-foreground shadow-sm hover:bg-primary/90",
        secondary:
          "border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80",
        reward:
          "border-transparent bg-gradient-to-r from-tamixa-yellow to-tamixa-orange text-white shadow-md animate-tamixa-bounce-in",
        success:
          "border-transparent bg-success/15 text-success border-success/40",
        destructive:
          "border-transparent bg-destructive/15 text-destructive border-destructive/40",
        outline: "border-2 border-border text-foreground bg-transparent",
        star:
          "border-transparent bg-tamixa-yellow/20 text-foreground border-tamixa-orange/30",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return (
    <div className={cn(badgeVariants({ variant }), className)} {...props} />
  );
}

export { Badge, badgeVariants };
