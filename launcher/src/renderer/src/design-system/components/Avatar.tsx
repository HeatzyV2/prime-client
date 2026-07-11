import './Avatar.css'

type Size = 'sm' | 'md' | 'lg' | 'xl'

interface AvatarProps {
  src?: string
  alt: string
  size?: Size
  glow?: boolean
}

export function Avatar({ src, alt, size = 'md', glow }: AvatarProps) {
  const url =
    src ??
    `https://mc-heads.net/avatar/${encodeURIComponent(alt)}/${size === 'xl' ? 80 : size === 'lg' ? 64 : size === 'md' ? 48 : 32}`

  return (
    <img
      className={`prime-avatar prime-avatar--${size}${glow ? ' prime-avatar--glow' : ''}`}
      src={url}
      alt={alt}
      draggable={false}
    />
  )
}
