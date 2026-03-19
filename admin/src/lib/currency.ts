/** Approx USD to INR for cost/revenue display (admin default). */
export const USD_TO_INR = 93;

/** Format amount in INR with ₹ symbol (amount is already in rupees). */
export function formatInr(amountInr: number): string {
  return `₹${amountInr.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

/** Format USD amount as INR using USD_TO_INR. */
export function formatUsdToInr(usd: number | null | undefined): string {
  if (usd == null) return "—";
  return formatInr(usd * USD_TO_INR);
}
