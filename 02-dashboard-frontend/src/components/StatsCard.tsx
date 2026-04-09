import { ReactNode } from 'react'

interface StatsCardProps {
  title: string
  value: string | number
  subtitle?: string
  icon: ReactNode
  trend?: {
    value: number
    label: string
  }
  color?: 'blue' | 'green' | 'purple' | 'orange'
}

const colorMap = {
  blue: {
    bg: 'bg-blue-50',
    icon: 'text-blue-600',
    trend: 'text-blue-600',
  },
  green: {
    bg: 'bg-emerald-50',
    icon: 'text-emerald-600',
    trend: 'text-emerald-600',
  },
  purple: {
    bg: 'bg-violet-50',
    icon: 'text-violet-600',
    trend: 'text-violet-600',
  },
  orange: {
    bg: 'bg-orange-50',
    icon: 'text-orange-600',
    trend: 'text-orange-600',
  },
}

export default function StatsCard({ title, value, subtitle, icon, trend, color = 'blue' }: StatsCardProps) {
  const colors = colorMap[color]

  return (
    <div className="card group">
      <div className="flex items-start justify-between">
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-gray-500 truncate">{title}</p>
          <p className="mt-1 text-2xl font-bold text-gray-900 tabular-nums">{value}</p>
          {subtitle && (
            <p className="mt-0.5 text-xs text-gray-400">{subtitle}</p>
          )}
        </div>
        <div className={`flex-shrink-0 p-3 rounded-xl ${colors.bg} ml-4`}>
          <div className={`h-6 w-6 ${colors.icon}`}>
            {icon}
          </div>
        </div>
      </div>
      {trend && (
        <div className="mt-4 flex items-center gap-1.5 pt-4 border-t border-gray-100">
          <span className={`text-xs font-semibold ${trend.value >= 0 ? 'text-emerald-600' : 'text-red-500'}`}>
            {trend.value >= 0 ? '▲' : '▼'} {Math.abs(trend.value)}%
          </span>
          <span className="text-xs text-gray-400">{trend.label}</span>
        </div>
      )}
    </div>
  )
}
