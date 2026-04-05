/**
 * Tamixa Financial Reality Engine — in-story family economy simulation (web).
 * Amounts are abstract currency units (treat as ₹ for copy).
 */

export interface ActiveLoan {
  id: string;
  label: string;
  principalRemaining: number;
  /** Annual APR as decimal, e.g. 0.24 = 24% */
  aprAnnual: number;
  monthlyEmi: number;
  monthsRemaining: number;
}

export interface FamilyEconomy {
  /** Spendable cash after the last simulated step */
  liquidCash: number;
  /** Rent, existing EMIs, school fees — deducted each simulated month */
  fixedObligationsMonthly: number;
  /** Target safety cushion (3–6 months of burn is a common goal) */
  emergencyBuffer: number;
  /** Outstanding principal on “bad” or high-cost debt (compounds monthly) */
  outstandingBadDebtPrincipal: number;
  /** Effective annual rate on bad-debt bucket (dynamic narrative hook) */
  debtInterestRateAnnual: number;
  simulatedMonth: number;
  loans: ActiveLoan[];
  /** Social validation / status-seeking (0–100) — pressures choices */
  socialValidationScore: number;
  /** Financial freedom / savings health (0–100) */
  financialFreedomScore: number;
}

export const DEFAULT_FAMILY_ECONOMY: FamilyEconomy = {
  liquidCash: 45_000,
  fixedObligationsMonthly: 18_000,
  emergencyBuffer: 60_000,
  outstandingBadDebtPrincipal: 0,
  debtInterestRateAnnual: 0.22,
  simulatedMonth: 0,
  loans: [],
  socialValidationScore: 35,
  financialFreedomScore: 55,
};

export function totalMonthlyEmi(e: FamilyEconomy): number {
  return e.loans.reduce((s, l) => s + l.monthlyEmi, 0);
}

/** Standard amortizing EMI (monthly). */
export function computeMonthlyEmi(principal: number, annualRate: number, tenureMonths: number): number {
  if (principal <= 0 || tenureMonths <= 0) return 0;
  const r = annualRate / 12;
  if (r <= 0) return principal / tenureMonths;
  const pow = Math.pow(1 + r, tenureMonths);
  return (principal * r * pow) / (pow - 1);
}

function clampScore(n: number): number {
  if (!Number.isFinite(n)) return 0;
  return Math.min(100, Math.max(0, n));
}

function recomputeFreedomScores(e: FamilyEconomy): FamilyEconomy {
  const burn = e.fixedObligationsMonthly + totalMonthlyEmi(e);
  const monthsCovered = burn > 0 ? e.liquidCash / burn : e.liquidCash > 0 ? 6 : 0;
  const bufferRatio = e.emergencyBuffer > 0 ? Math.min(1, e.liquidCash / e.emergencyBuffer) : 0.5;
  const freedom = clampScore(monthsCovered * 14 + bufferRatio * 40 - e.socialValidationScore * 0.35);
  return { ...e, financialFreedomScore: freedom };
}

/** Pay one month of fixed obligations + all EMIs; accrue story-level debt interest; amortize loans. */
export function applyMonthlyBurn(economy: FamilyEconomy): FamilyEconomy {
  const bucketRate = economy.debtInterestRateAnnual / 12;
  const bucketInterest = economy.outstandingBadDebtPrincipal * bucketRate;
  let outstanding = economy.outstandingBadDebtPrincipal + bucketInterest;

  const liquid = economy.liquidCash - economy.fixedObligationsMonthly - totalMonthlyEmi(economy);
  let principalPaidTotal = 0;
  const loans = economy.loans.map((l) => {
    if (l.monthsRemaining <= 0 || l.monthlyEmi <= 0) return l;
    const r = l.aprAnnual / 12;
    const interest = l.principalRemaining * r;
    let principalPay = l.monthlyEmi - interest;
    if (principalPay < 0) principalPay = 0;
    if (principalPay > l.principalRemaining) principalPay = l.principalRemaining;
    principalPaidTotal += principalPay;
    const pr = Math.max(0, l.principalRemaining - principalPay);
    return {
      ...l,
      principalRemaining: pr,
      monthsRemaining: Math.max(0, l.monthsRemaining - 1),
    };
  });
  outstanding = Math.max(0, outstanding - principalPaidTotal);
  const next: FamilyEconomy = {
    ...economy,
    liquidCash: liquid,
    outstandingBadDebtPrincipal: outstanding,
    loans: loans.filter((l) => l.monthsRemaining > 0 && l.principalRemaining > 0.01),
    simulatedMonth: economy.simulatedMonth + 1,
  };
  return recomputeFreedomScores(next);
}

export interface FinancialChoiceImpact {
  /** New personal loan / EMI line (wedding, gadget, etc.) */
  addLoan?: {
    label?: string;
    principal: number;
    aprAnnual: number;
    tenureMonths: number;
  };
  /** Extra EMI without full loan math (quick authoring) */
  addMonthlyEmi?: number;
  /** Push status-seeking bar */
  socialValidationDelta?: number;
  /** Direct hit to savings freedom */
  savingsDelta?: number;
  /** One-off spend (party, gift) */
  oneTimeSpend?: number;
  /** If cash goes negative after applying this choice, jump here */
  crisisChapterSegmentId?: string;
}

export function applyFinancialChoice(
  economy: FamilyEconomy,
  impact: FinancialChoiceImpact | null | undefined,
  choiceId: string
): FamilyEconomy {
  if (!impact) return economy;
  const e = { ...economy, loans: [...economy.loans] };

  if (impact.oneTimeSpend != null && Number.isFinite(impact.oneTimeSpend)) {
    e.liquidCash -= impact.oneTimeSpend;
  }
  if (impact.socialValidationDelta != null && Number.isFinite(impact.socialValidationDelta)) {
    e.socialValidationScore = clampScore(e.socialValidationScore + impact.socialValidationDelta);
  }
  if (impact.savingsDelta != null && Number.isFinite(impact.savingsDelta)) {
    e.financialFreedomScore = clampScore(e.financialFreedomScore + impact.savingsDelta);
  }

  if (impact.addLoan && impact.addLoan.principal > 0 && impact.addLoan.tenureMonths > 0) {
    const { principal, aprAnnual, tenureMonths } = impact.addLoan;
    const emi = computeMonthlyEmi(principal, aprAnnual, tenureMonths);
    e.loans.push({
      id: `${choiceId}-${Date.now()}`,
      label: impact.addLoan.label?.trim() || "New loan",
      principalRemaining: principal,
      aprAnnual,
      monthlyEmi: emi,
      monthsRemaining: tenureMonths,
    });
    e.outstandingBadDebtPrincipal += principal;
    e.liquidCash -= emi;
    e.debtInterestRateAnnual = Math.max(e.debtInterestRateAnnual, aprAnnual);
  } else if (impact.addMonthlyEmi != null && impact.addMonthlyEmi > 0) {
    const emi = impact.addMonthlyEmi;
    const syntheticPrincipal = emi * 24;
    e.loans.push({
      id: `${choiceId}-emi`,
      label: "Added EMI",
      principalRemaining: syntheticPrincipal,
      aprAnnual: e.debtInterestRateAnnual,
      monthlyEmi: emi,
      monthsRemaining: 24,
    });
    e.outstandingBadDebtPrincipal += syntheticPrincipal;
    e.liquidCash -= emi;
  }

  return recomputeFreedomScores(e);
}

export function isLiquidityCrisis(e: FamilyEconomy): boolean {
  return e.liquidCash < 0;
}

export interface TimePassageResult {
  economy: FamilyEconomy;
  monthsAdvanced: number;
  totalInterestAccrued: number;
  /** Plain-language lines for “3 months later” screen */
  summaryLines: string[];
}

/**
 * Advance simulated calendar; compound bad-debt bucket monthly, pay EMIs/obligations each month.
 */
export function simulateTimePassage(economy: FamilyEconomy, months: number): TimePassageResult {
  if (months <= 0) {
    return {
      economy: { ...economy },
      monthsAdvanced: 0,
      totalInterestAccrued: 0,
      summaryLines: ["No time skipped."],
    };
  }
  let e: FamilyEconomy = { ...economy, loans: economy.loans.map((l) => ({ ...l })) };
  let interestAccrued = 0;
  const startDebt = e.outstandingBadDebtPrincipal;
  const startLiquid = e.liquidCash;

  for (let m = 0; m < months; m++) {
    const beforeDebt = e.outstandingBadDebtPrincipal;
    e = applyMonthlyBurn(e);
    const r = economy.debtInterestRateAnnual / 12;
    interestAccrued += beforeDebt * r;
  }

  const lines = [
    `${months} month${months === 1 ? "" : "s"} later`,
    `Rough interest stacked on the debt bucket in this stretch: about ${Math.round(interestAccrued).toLocaleString()}.`,
    e.liquidCash < startLiquid
      ? `Cash after obligations: about ${Math.round(e.liquidCash).toLocaleString()} — tighter than before.`
      : `Cash after obligations: about ${Math.round(e.liquidCash).toLocaleString()}.`,
    e.outstandingBadDebtPrincipal > startDebt * 0.99
      ? `Debt load sits around ${Math.round(e.outstandingBadDebtPrincipal).toLocaleString()}.`
      : "Debt load shrank or held steady.",
    isLiquidityCrisis(e)
      ? "Cash is negative — a crisis chapter may already be ringing."
      : "Still room to recover if you change course.",
  ];

  return {
    economy: recomputeFreedomScores(e),
    monthsAdvanced: months,
    totalInterestAccrued: interestAccrued,
    summaryLines: lines,
  };
}

/** Enable HUD / EMI logic when the graph opts in or any choice carries money impact. */
export function graphUsesFinancialReality(graph: {
  financialRealityEnabled?: boolean | null;
  segments: Record<string, { choices?: { financialImpact?: unknown }[] }>;
}): boolean {
  if (graph.financialRealityEnabled === true) return true;
  for (const s of Object.values(graph.segments)) {
    for (const c of s.choices ?? []) {
      if (c.financialImpact != null && typeof c.financialImpact === "object") return true;
    }
  }
  return false;
}
