import { DashboardPage } from "./features/deployments/DashboardPage";
import { createBrowserRouter } from "react-router";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <DashboardPage />,
  },
])