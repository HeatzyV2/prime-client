import { motion } from 'framer-motion'
import { Construction } from 'lucide-react'
import { Badge } from '@renderer/design-system/components'
import './PlaceholderPage.css'

interface PlaceholderPageProps {
  title: string
  phase: number
  description: string
}

export function PlaceholderPage({ title, phase, description }: PlaceholderPageProps) {
  return (
    <motion.div
      className="placeholder"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3 }}
    >
      <div className="placeholder__icon">
        <Construction size={28} />
      </div>
      <h1 className="text-title">{title}</h1>
      <p className="text-body" style={{ color: 'var(--prime-muted)', maxWidth: 420 }}>
        {description}
      </p>
      <Badge variant="red">Phase {phase}</Badge>
      <span className="placeholder__phase">Coming soon in Prime Launcher roadmap</span>
    </motion.div>
  )
}
