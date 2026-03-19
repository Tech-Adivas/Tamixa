import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl text-sm font-semibold transition-all duration-standard ease-out-expo focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 active:scale-[0.98] [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0",
  {
    variants: {
      variant: {
        default:
          "bg-primary text-primary-foreground shadow-md hover:bg-primary/90 hover:shadow-glow active:bg-primary/95",
        primary:
          "bg-gradient-to-r from-[#4F46E5] to-[#7C3AED] text-white shadow-md hover:opacity-95 hover:shadow-glow active:opacity-100",
        secondary:
          "bg-secondary text-secondary-foreground border-2 border-primary/20 hover:bg-secondary/80 hover:border-primary/40 active:bg-secondary/90",
        accent:
          "bg-accent text-accent-foreground hover:bg-accent/90 active:bg-accent/95",
        destructive:
          "bg-destructive text-destructive-foreground shadow-sm hover:bg-destructive/90 active:bg-destructive/95",
        outline:
          "border-2 border-input bg-background hover:bg-accent hover:text-accent-foreground hover:border-primary/30 active:bg-accent/80",
        ghost: "hover:bg-accent hover:text-accent-foreground active:bg-accent/80",
        link: "text-primary underline-offset-4 hover:underline active:opacity-90",
        success:
          "bg-success text-success-foreground shadow-md hover:bg-success/90 active:bg-success/95",
      },
      size: {
        default: "h-12 min-h-touch min-w-touch px-5 py-2.5 rounded-xl",
        sm: "h-9 rounded-lg px-3 text-xs",
        lg: "h-14 min-h-touch min-w-touch rounded-2xl px-8 text-base",
        icon: "h-12 w-12 min-h-touch min-w-touch rounded-xl",
        "icon-sm": "h-9 w-9 rounded-lg",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button";
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    );
  }
);
Button.displayName = "Button";

export { Button, buttonVariants };
