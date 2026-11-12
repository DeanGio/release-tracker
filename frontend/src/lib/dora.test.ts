// src/lib/dora.test.ts
import { describe, it, expect } from 'vitest'
import type { Deployment } from '@/types/deployment'
import { deploymentFrequency, changeFailureRate } from './dora'

const now = new Date('2026-11-08T12:00:00Z')
const DAY_MS = 24 * 60 * 60 * 1000

// One successful PROD deployment in a 7-day window.
const ONE_PER_7_DAYS = 1 / 7

function deployment(overrides: Partial<Deployment> = {}): Deployment {
  return {
    id: crypto.randomUUID(),
    serviceName: 'billing',
    version: '1.0.0',
    environment: 'PROD',
    status: 'SUCCEEDED',
    startTime: new Date(now.getTime() - 60_000).toISOString(),
    endTime: null,
    ...overrides,
  }
}

describe('deploymentFrequency', () => {
  it('counts only successful PROD deployments in the window', () => {
    const deployments = [
      deployment(),
      deployment({ status: 'FAILED' }),
      deployment({ environment: 'DEV' }),
    ]
    expect(deploymentFrequency(deployments, 7, now)).toBeCloseTo(ONE_PER_7_DAYS)
  })
})

describe('changeFailureRate', () => {
  it('calculates the failure rate for finished PROD deployments in the window', () => {
    const deployments = [
      deployment({ status: 'FAILED' }),
      deployment({ status: 'SUCCEEDED' }),
      deployment({ status: 'IN_PROGRESS' }),
      deployment({ environment: 'DEV', status: 'FAILED' }),
    ]
    expect(changeFailureRate(deployments, 7, now)).toBeCloseTo(0.5)
  })
})

describe('window boundary', () => {
  it('includes only deployments within the window', () => {
    const deployments = [
      deployment({ startTime: new Date(now.getTime() - 8 * DAY_MS).toISOString() }), // 8 days ago
      deployment({ startTime: new Date(now.getTime() - 6 * DAY_MS).toISOString() }), // 6 days ago
    ]
    expect(deploymentFrequency(deployments, 7, now)).toBeCloseTo(ONE_PER_7_DAYS)
    expect(changeFailureRate(deployments, 7, now)).toBe(0)
  })

  it('includes a deployment exactly at the window start', () => {
    const start = new Date(now.getTime() - 7 * DAY_MS)
    const deployments = [deployment({ startTime: start.toISOString() })]
    expect(deploymentFrequency(deployments, 7, now)).toBeCloseTo(ONE_PER_7_DAYS)
  })

  it('excludes a deployment one millisecond before the window start', () => {
    const justBefore = new Date(now.getTime() - 7 * DAY_MS - 1)
    const deployments = [deployment({ startTime: justBefore.toISOString() })]
    expect(deploymentFrequency(deployments, 7, now)).toBe(0)
  })
})

describe('future deployments', () => {
  it('does not include deployments that start in the future', () => {
    const deployments = [
      deployment({ startTime: new Date(now.getTime() + DAY_MS).toISOString() }), // 1 day in the future
    ]
    expect(deploymentFrequency(deployments, 7, now)).toBe(0)
    expect(changeFailureRate(deployments, 7, now)).toBeNull()
  })
})

describe('empty window', () => {
  it('returns 0 for deploymentFrequency and null for changeFailureRate when there are no deployments in the window', () => {
    const deployments: Deployment[] = []
    expect(deploymentFrequency(deployments, 7, now)).toBe(0)
    expect(changeFailureRate(deployments, 7, now)).toBeNull()
  })
})

describe('in progress only', () => {
  it('returns 0 for deploymentFrequency and null for changeFailureRate when all deployments are in progress', () => {
    const deployments = [
      deployment({ status: 'IN_PROGRESS' }),
      deployment({ status: 'IN_PROGRESS' }),
    ]
    expect(deploymentFrequency(deployments, 7, now)).toBe(0)
    expect(changeFailureRate(deployments, 7, now)).toBeNull()
  })
})

describe('invalid window', () => {
  it('throws RangeError for both metrics when the window is negative', () => {
    const deployments = [deployment()]
    expect(() => deploymentFrequency(deployments, -7, now)).toThrow(RangeError)
    expect(() => changeFailureRate(deployments, -7, now)).toThrow(RangeError)
  })

  it('throws RangeError for both metrics when the window is 0', () => {
    const deployments = [deployment()]
    expect(() => deploymentFrequency(deployments, 0, now)).toThrow(RangeError)
    expect(() => changeFailureRate(deployments, 0, now)).toThrow(RangeError)
  })
})