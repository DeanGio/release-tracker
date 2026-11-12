import { useTransitionDeployment } from "@/api/mutations";
import { Button } from "@/components/ui/button";
import type { Deployment } from "@/types/deployment";

export function InProgressActions({ deployment }: { deployment: Deployment }) {
  const transition = useTransitionDeployment();

  if (deployment.status !== "IN_PROGRESS") return null;

  const pendingTarget = transition.isPending ? transition.variables?.transitionTo : undefined;

  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center gap-2">
        <Button
          size="sm"
          variant="outline"
          disabled={transition.isPending}
          onClick={() => transition.mutate({ id: deployment.id, transitionTo: "SUCCEEDED" })}
        >
          {pendingTarget === "SUCCEEDED" ? "Saving…" : "Mark succeeded"}
        </Button>
        <Button
          size="sm"
          variant="destructive"
          disabled={transition.isPending}
          onClick={() => transition.mutate({ id: deployment.id, transitionTo: "FAILED" })}
        >
          {pendingTarget === "FAILED" ? "Saving…" : "Mark failed"}
        </Button>
      </div>
      {transition.isError && (
        <p role="alert" className="text-sm text-destructive">
          {transition.error.message}
        </p>
      )}
    </div>
  );
}
