import type { AuthUser } from "./types";

export type WorkGroupCode = "FO" | "BO" | "ADMIN";

export type WorkGroup = {
  code: WorkGroupCode;
  name: string;
  description: string;
  landingHref: string;
  allowedHrefs: string[];
};

export const WORK_GROUPS: Record<WorkGroupCode, WorkGroup> = {
  FO: {
    code: "FO",
    name: "Front Office",
    description: "Book trades, monitor market data, run pricing and analyze exposure.",
    landingHref: "/fo-desk",
    allowedHrefs: ["/", "/fo-desk", "/pre-trade-analysis", "/stress-testing", "/upad", "/what-if", "/portfolios", "/pricing", "/exposure", "/cva", "/valuation-runs"],
  },
  BO: {
    code: "BO",
    name: "Back Office",
    description: "Validate pending trades and manage preventive Front Office trading limits.",
    landingHref: "/trade-validation",
    allowedHrefs: ["/trade-validation", "/trading-limits", "/eod-control", "/valuation-runs"],
  },
  ADMIN: {
    code: "ADMIN",
    name: "Admin",
    description: "Manage users, access controls and workflow monitoring.",
    landingHref: "/admin",
    allowedHrefs: ["/admin", "/workflows", "/valuation-runs"],
  },
};

export function groupsForUser(user: AuthUser): WorkGroup[] {
  return user.groups
    .filter((group): group is WorkGroupCode => group in WORK_GROUPS)
    .map((group) => WORK_GROUPS[group]);
}

export function groupForCode(user: AuthUser, code: string | null): WorkGroup | null {
  return groupsForUser(user).find((group) => group.code === code) ?? null;
}

export function isHrefAllowed(group: WorkGroup, href: string) {
  return group.allowedHrefs.some((allowedHref) => {
    if (allowedHref === "/") {
      return href === "/";
    }
    return href === allowedHref || href.startsWith(`${allowedHref}/`);
  });
}
