export function formatAmount(amount?: number) {
  if (!amount) {
    return "待确认";
  }

  if (amount >= 1e9) {
    return `$${(amount / 1e9).toFixed(2)}B`;
  }

  if (amount >= 1e6) {
    return `$${(amount / 1e6).toFixed(2)}M`;
  }

  return `$${amount.toLocaleString()}`;
}

export function formatPercent(value?: number) {
  if (value === undefined || Number.isNaN(value)) {
    return "待确认";
  }

  return `${Math.round(value * 100)}%`;
}

export function normalizeDirection(direction: string) {
  if (direction.includes("空")) {
    return "bad";
  }
  if (direction.includes("中")) {
    return "neutral";
  }
  return "good";
}
