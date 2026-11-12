import { useMemo } from 'react'
import { deploymentFrequency, changeFailureRate, nonProductionDeploymentsPerDay } from '@/lib/dora'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { CreateDeploymentForm } from './CreateDeploymentForm'
import { DeploymentList } from './DeploymentList'
import { useAllDeployments } from '@/api/queries'

const WINDOW_DAYS = 30

type Metrics = {
  frequency: number
  failureRate: number | null
  nonProdFrequency: number
}

export function DashboardPage() {
  const { data, isPending, isError, error } = useAllDeployments()

  const metrics = useMemo<Metrics | null>(() => {
    if (!data) return null
    const now = new Date()
    return {
      frequency: deploymentFrequency(data, WINDOW_DAYS, now),
      failureRate: changeFailureRate(data, WINDOW_DAYS, now),
      nonProdFrequency: nonProductionDeploymentsPerDay(data, WINDOW_DAYS, now),
    }
  }, [data])

  return (
    <div className="flex flex-col gap-6">
      {isError ? (
        <p className="text-destructive">Could not load deployments: {error.message}</p>
      ) : isPending || !metrics ? (
        <p className="text-muted-foreground">Loading metrics…</p>
      ) : (
        <MetricCards metrics={metrics} />
      )}
      <CreateDeploymentForm />
      <DeploymentList />
    </div>
  )
}

function MetricCards({ metrics }: { metrics: Metrics }) {
  return (
    <div className="grid gap-4 md:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle>Deployment frequency</CardTitle>
          <CardDescription>Successful PROD deployments per day, last {WINDOW_DAYS} days</CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-3xl font-semibold">
            {metrics.frequency.toFixed(2)} <span className="text-base font-normal">/ day</span>
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Change failure rate</CardTitle>
          <CardDescription>Failed share of finished PROD deployments, last {WINDOW_DAYS} days</CardDescription>
        </CardHeader>
        <CardContent>
          {metrics.failureRate === null ? (
            <p className="text-muted-foreground">No finished production deployments in this window.</p>
          ) : (
            <p className="text-3xl font-semibold">{Math.round(metrics.failureRate * 100)}%</p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
            <CardTitle>Non Production Deployments per Day</CardTitle>
            <CardDescription>Non-PROD deployments per day, last {WINDOW_DAYS} days</CardDescription>
        </CardHeader>
        <CardContent>
            <p className="text-3xl font-semibold">
              {metrics.nonProdFrequency.toFixed(2)} <span className="text-base font-normal">/ day</span>
            </p>
        </CardContent>
      </Card>
    </div>
  )
}