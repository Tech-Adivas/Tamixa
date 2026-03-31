import { useState, useEffect } from "react";

export function useAccessibility() {
  const [fontScale, setFontScale] = useState(1);
  const [highContrast, setHighContrast] = useState(false);

  useEffect(() => {
    document.documentElement.style.setProperty("--font-size-scale", String(fontScale));
  }, [fontScale]);

  useEffect(() => {
    document.body.classList.toggle("high-contrast", highContrast);
  }, [highContrast]);

  return { fontScale, setFontScale, highContrast, setHighContrast };
}
