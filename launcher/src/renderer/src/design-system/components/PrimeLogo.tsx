export function PrimeLogo({ size = 48 }: { size?: number }) {
  return (
    <svg width={size} height={size * 0.55} viewBox="0 0 1024 559" fill="none" aria-hidden>
      <rect width="1024" height="559" rx="80" fill="#0a0a0c" />
      <path
        d="M180 80 L180 479 L420 479 L420 340 L620 340 L780 479 L980 479 L760 280 L980 80 L780 80 L620 220 L420 220 L420 80 Z"
        fill="url(#primeGrad)"
      />
      <defs>
        <linearGradient id="primeGrad" x1="180" y1="80" x2="980" y2="479" gradientUnits="userSpaceOnUse">
          <stop stopColor="#ff2d42" />
          <stop offset="1" stopColor="#e11d2e" />
        </linearGradient>
      </defs>
    </svg>
  )
}
