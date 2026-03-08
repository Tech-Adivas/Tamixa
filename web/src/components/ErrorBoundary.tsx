import { Component, ErrorInfo, ReactNode } from "react";
import { logger } from "../lib/logger";

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    logger.error("ErrorBoundary", "Unhandled React error", error, {
      componentStack: errorInfo.componentStack,
    });
  }

  render() {
    if (this.state.hasError && this.state.error) {
      if (this.props.fallback) return this.props.fallback;
      return (
        <div className="page">
          <h1>Something went wrong</h1>
          <p className="muted">
            We&apos;re sorry. Please try refreshing the page or go back to{" "}
            <a href="/">home</a>.
          </p>
          <details style={{ marginTop: "1rem", fontSize: "0.875rem" }}>
            <summary>Error details</summary>
            <pre style={{ overflow: "auto", padding: "0.5rem", background: "#f5f5f5", borderRadius: 4 }}>
              {this.state.error.message}
            </pre>
          </details>
        </div>
      );
    }
    return this.props.children;
  }
}
