// src/components/DeploymentList.tsx
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { useDeploymentsQuery } from "@/api/queries";
import { InProgressActions } from "./InProgressActions";

const PAGE_SIZE = 20;

export function DeploymentList() {
  const [offset, setOffset] = useState(0);
  const { data, isPending, isError, error } = useDeploymentsQuery(PAGE_SIZE, offset);

  if (isPending) return <p>Loading deployments…</p>;
  if (isError) return <p role="alert" className="text-destructive">{error.message}</p>;
  if (data.items.length === 0) return <p>No deployments yet.</p>;

  const hasPrev = offset > 0;
  const hasNext = offset + PAGE_SIZE < data.total;

  return (
    <section className="flex flex-col gap-4">
      <ul className="divide-y">
        {data.items.map((d) => (
          <li key={d.id} className="flex items-center justify-between gap-4 py-3">
            <div>
              <p className="font-medium">
                {d.serviceName} {d.version}
              </p>
              <p className="text-sm text-muted-foreground">
                {d.environment} · {d.status}
              </p>
            </div>
            <InProgressActions deployment={d} />
          </li>
        ))}
      </ul>

      <div className="flex items-center justify-between">
        <Button
          variant="outline"
          size="sm"
          disabled={!hasPrev}
          onClick={() => setOffset(Math.max(0, offset - PAGE_SIZE))}
        >
          Previous
        </Button>
        <span className="text-sm text-muted-foreground">
          {offset + 1}–{Math.min(offset + PAGE_SIZE, data.total)} of {data.total}
        </span>
        <Button
          variant="outline"
          size="sm"
          disabled={!hasNext}
          onClick={() => setOffset(offset + PAGE_SIZE)}
        >
          Next
        </Button>
      </div>
    </section>
  );
}
