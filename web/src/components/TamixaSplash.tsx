/**
 * Tamixa splash — premium kids app intro.
 * One hero (star), depth, soft motion, clear sequence. ~3.2s.
 */
import { useEffect, useState } from "react";

const SPLASH_DURATION_MS = 3200;

export function TamixaSplash({ onDone }: { onDone: () => void }) {
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    const t = setTimeout(() => {
      setVisible(false);
      onDone();
    }, SPLASH_DURATION_MS);
    return () => clearTimeout(t);
  }, [onDone]);

  if (!visible) return null;

  return (
    <div
      className="tamixa-splash"
      role="img"
      aria-label="Tamixa – Listen, Learn, Shine"
    >
      {/* Layered background: gradient + soft center glow */}
      <div className="tamixa-splash__bg" />
      <div className="tamixa-splash__glow" />

      {/* Floating particles — slow drift for depth */}
      <div className="tamixa-splash__particle tamixa-splash__particle--1" />
      <div className="tamixa-splash__particle tamixa-splash__particle--2" />
      <div className="tamixa-splash__particle tamixa-splash__particle--3" />
      <div className="tamixa-splash__particle tamixa-splash__particle--4" />
      <div className="tamixa-splash__particle tamixa-splash__particle--5" />
      <div className="tamixa-splash__particle tamixa-splash__particle--6" />
      <div className="tamixa-splash__particle tamixa-splash__particle--7" />
      <div className="tamixa-splash__particle tamixa-splash__particle--8" />

      {/* Hero: app icon */}
      <div className="tamixa-splash__hero">
        <img
          src="/tamixa-app-icon.png"
          alt="Tamixa"
          className="tamixa-splash__logo-img"
        />
      </div>

      {/* Sparkles around logo */}
      <div className="tamixa-splash__sparkle tamixa-splash__sparkle--1" />
      <div className="tamixa-splash__sparkle tamixa-splash__sparkle--2" />
      <div className="tamixa-splash__sparkle tamixa-splash__sparkle--3" />
      <div className="tamixa-splash__sparkle tamixa-splash__sparkle--4" />
      <div className="tamixa-splash__sparkle tamixa-splash__sparkle--5" />
      <div className="tamixa-splash__sparkle tamixa-splash__sparkle--6" />
      <div className="tamixa-splash__sparkle tamixa-splash__sparkle--7" />
      <div className="tamixa-splash__sparkle tamixa-splash__sparkle--8" />

      {/* Soft ring that expands when hero lands */}
      <div className="tamixa-splash__ring" aria-hidden />

      {/* Tagline: each word animated */}
      <p className="tamixa-splash__tagline" aria-hidden>
        <span className="tamixa-splash__tagline-word">Listen</span>
        <span className="tamixa-splash__tagline-dot"> · </span>
        <span className="tamixa-splash__tagline-word">Learn</span>
        <span className="tamixa-splash__tagline-dot"> · </span>
        <span className="tamixa-splash__tagline-word">Shine</span>
      </p>
    </div>
  );
}

export default TamixaSplash;
