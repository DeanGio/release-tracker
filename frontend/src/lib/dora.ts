import type { Deployment } from '@/types/deployment'

const DAY_MS = 24 * 60 * 60 * 1000

function startedWithin(d: Deployment, windowDays: number, now: Date): boolean {
    const started = new Date(d.startTime).getTime()
    const windowStart = now.getTime() - windowDays * DAY_MS
    return started >= windowStart && started <= now.getTime()
}

function assertValidWindow(windowDays: number): void {
    if (!Number.isFinite(windowDays) || windowDays <= 0) {
        throw new RangeError(`windowDays must be a positive number, got ${windowDays}`)
    }
}

export function deploymentFrequency(
  deployments: Deployment[],
  windowDays: number,
  now: Date,
): number {
  assertValidWindow(windowDays)
  const count = deployments.filter(
    (d) => d.environment === 'PROD' && d.status === 'SUCCEEDED' && startedWithin(d, windowDays, now),
  ).length
  return count / windowDays
}

export function changeFailureRate(
  deployments: Deployment[],
  windowDays: number,
  now: Date,
): number | null {
  assertValidWindow(windowDays)
  const finished = deployments.filter(
    (d) => d.environment === 'PROD' && d.status !== 'IN_PROGRESS' && startedWithin(d, windowDays, now),
  )
  if (finished.length === 0) return null
  const failed = finished.filter((d) => d.status === 'FAILED').length
  return failed / finished.length
}

export function nonProductionDeploymentsPerDay(
    deployments: Deployment[],
    windowDays: number,
    now: Date,
): number {
    assertValidWindow(windowDays)
    const count = deployments.filter(
        (d) => d.environment !== 'PROD' && startedWithin(d, windowDays, now),
    ).length
    return Math.round((count / windowDays) * 100) / 100
}