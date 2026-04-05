import type { FamilyEconomy } from "../lib/financialRealityEngine";
import { totalMonthlyEmi } from "../lib/financialRealityEngine";

export interface FinancialRealityHudProps {
  economy: FamilyEconomy;
  onSimulateThreeMonths: () => void;
}

function fmt(n: number): string {
  return `₹${Math.round(n).toLocaleString()}`;
}

/**
 * Status (social validation pressure) vs Savings / financial freedom — both 0–100, no exam marks.
 */
export default function FinancialRealityHud({ economy, onSimulateThreeMonths }: FinancialRealityHudProps) {
  const status = economy.socialValidationScore;
  const savings = economy.financialFreedomScore;
  const emi = totalMonthlyEmi(economy);

  return (
    <section className="financial-reality-hud" aria-label="Family economy snapshot">
      <div className="financial-reality-hud__row financial-reality-hud__row--metrics">
        <div>
          <span className="financial-reality-hud__metric-label">Liquid cash</span>
          <span className="financial-reality-hud__metric-value">{fmt(economy.liquidCash)}</span>
        </div>
        <div>
          <span className="financial-reality-hud__metric-label">Monthly burn</span>
          <span className="financial-reality-hud__metric-value">{fmt(economy.fixedObligationsMonthly + emi)}</span>
        </div>
        <div>
          <span className="financial-reality-hud__metric-label">Safety buffer target</span>
          <span className="financial-reality-hud__metric-value">{fmt(economy.emergencyBuffer)}</span>
        </div>
        <div>
          <span className="financial-reality-hud__metric-label">Debt bucket</span>
          <span className="financial-reality-hud__metric-value">{fmt(economy.outstandingBadDebtPrincipal)}</span>
        </div>
      </div>
      <div className="financial-reality-hud__dual">
        <div>
          <div className="financial-reality-hud__dual-head">
            <span>Social validation</span>
            <span className="financial-reality-hud__dual-val">{Math.round(status)}</span>
          </div>
          <p className="financial-reality-hud__dual-hint muted">Status choices can pull you toward “showing off.”</p>
          <div className="financial-reality-hud__track" aria-hidden>
            <div className="financial-reality-hud__fill financial-reality-hud__fill--status" style={{ width: `${status}%` }} />
          </div>
        </div>
        <div>
          <div className="financial-reality-hud__dual-head">
            <span>Financial freedom</span>
            <span className="financial-reality-hud__dual-val">{Math.round(savings)}</span>
          </div>
          <p className="financial-reality-hud__dual-hint muted">Room to breathe — savings, buffers, calmer trade-offs.</p>
          <div className="financial-reality-hud__track" aria-hidden>
            <div className="financial-reality-hud__fill financial-reality-hud__fill--savings" style={{ width: `${savings}%` }} />
          </div>
        </div>
      </div>
      <div className="financial-reality-hud__actions">
        <button type="button" className="btn btn-secondary financial-reality-hud__skip" onClick={onSimulateThreeMonths}>
          Skip ahead — see 3 months later
        </button>
        <span className="financial-reality-hud__apr muted" title="Story interest rate on the debt bucket">
          Story debt APR ≈ {Math.round(economy.debtInterestRateAnnual * 100)}% (simulated)
        </span>
      </div>
    </section>
  );
}
