"use client";

import React from "react";
import {
  Loader2,
  LockKeyhole,
  Search,
  Shield,
  UsersRound,
} from "lucide-react";
import { authApi, nexusApi, NexusApiError } from "@/lib/api";
import { formatNumber } from "@/lib/format";
import type {
  AdminPortfolioSummary,
  AdminUserAccess,
} from "@/lib/types";
import { AppShell } from "./AppShell";

const groups = ["FO", "BO", "ADMIN"];

const howTo = [
  { title: "Groups", body: "ADMIN controls which high-level group contexts a user can select after login." },
  { title: "FO checks", body: "Feature checks are user-level overrides. If no override exists, the FO group keeps its default capability." },
  { title: "Portfolio visibility", body: "Use ALL for normal users or SELECTED to restrict which portfolios they can see and run analytics on." },
  { title: "Self protection", body: "An ADMIN cannot remove their own ADMIN group; this keeps the current session from locking itself out." },
];

export function AdminPage() {
  const [users, setUsers] = React.useState<AdminUserAccess[]>([]);
  const [portfolios, setPortfolios] = React.useState<AdminPortfolioSummary[]>([]);
  const [selected, setSelected] = React.useState<AdminUserAccess | null>(null);
  const [currentUserId, setCurrentUserId] = React.useState<string | null>(null);
  const [query, setQuery] = React.useState("");
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [success, setSuccess] = React.useState<string | null>(null);

  React.useEffect(() => {
    void loadAccess();
  }, [query]);

  async function loadAccess(preferredUserId?: string) {
    setLoading(true);
    setError(null);
    try {
      const [session, userPage, portfolioList] = await Promise.all([
        authApi.me(),
        nexusApi.listAdminUsers(query, 0, 100),
        nexusApi.listAdminPortfolios(),
      ]);
      setCurrentUserId(session.user?.id ?? null);
      setUsers(userPage.items);
      setPortfolios(portfolioList);
      const next = userPage.items.find((user) => user.id === (preferredUserId ?? selected?.id)) ?? userPage.items[0] ?? null;
      setSelected(next);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function saveGroups(user: AdminUserAccess, nextGroups: string[]) {
    await save("groups", () => nexusApi.updateAdminUserGroups(user.id, nextGroups));
  }

  async function savePermission(user: AdminUserAccess, code: string, enabled: boolean) {
    await save("permissions", () => nexusApi.updateAdminUserPermissions(user.id, { [code]: enabled }));
  }

  async function savePortfolioAccess(user: AdminUserAccess, mode: "ALL" | "SELECTED", portfolioIds: string[]) {
    await save("portfolios", () => nexusApi.updateAdminPortfolioAccess(user.id, mode, portfolioIds));
  }

  async function save(label: string, operation: () => Promise<AdminUserAccess>) {
    setSaving(label);
    setError(null);
    setSuccess(null);
    try {
      const updated = await operation();
      setSelected(updated);
      setUsers((current) => current.map((user) => user.id === updated.id ? updated : user));
      setSuccess("Access updated.");
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSaving(null);
    }
  }

  return (
    <AppShell title="Access configuration" eyebrow="Administration" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}
      {success ? <div className="success">{success}</div> : null}

      <div className="admin-section-title">
        <UsersRound size={18} />
        <div>
          <h2>Users & access</h2>
          <p>Manage group memberships, FO feature checks and portfolio visibility.</p>
        </div>
      </div>

      <div className="admin-access-layout">
          <section className="panel admin-user-list-panel">
            <label className="bo-search admin-search">
              <Search size={16} />
              <input placeholder="Search users" value={query} onChange={(event) => setQuery(event.target.value)} />
            </label>
            {loading ? <div className="empty"><Loader2 className="spin" size={18} /> Loading users</div> : null}
            <div className="admin-user-list">
              {users.map((user) => (
                <button className={`admin-user-row ${selected?.id === user.id ? "selected" : ""}`} key={user.id} type="button" onClick={() => setSelected(user)}>
                  <strong>{user.displayName}</strong>
                  <span>@{user.username}</span>
                  <small>{user.groups.join(" · ") || "No groups"}</small>
                </button>
              ))}
            </div>
          </section>

          <section className="panel admin-detail-panel">
            {selected ? (
              <>
                <div className="admin-detail-head">
                  <div>
                    <span className="badge">User access</span>
                    <h2>{selected.displayName}</h2>
                    <p>@{selected.username} · last login {selected.lastLoginAt ? new Date(selected.lastLoginAt).toLocaleString() : "never"}</p>
                  </div>
                  <Shield size={24} />
                </div>

                <div className="admin-card-grid">
                  <section className="admin-card">
                    <h3>Groups</h3>
                    <div className="check-grid">
                      {groups.map((group) => {
                        const checked = selected.groups.includes(group);
                        const nextGroups = checked ? selected.groups.filter((item) => item !== group) : [...selected.groups, group];
                        const protectedOwnAdmin = selected.id === currentUserId && group === "ADMIN" && checked;
                        return (
                          <label className={`admin-check ${protectedOwnAdmin ? "disabled" : ""}`} key={group}>
                            <input
                              type="checkbox"
                              checked={checked}
                              disabled={protectedOwnAdmin}
                              onChange={() => saveGroups(selected, nextGroups)}
                            />
                            <span>{group}</span>
                            {protectedOwnAdmin ? <small>Current admin</small> : null}
                          </label>
                        );
                      })}
                    </div>
                  </section>

                  <section className="admin-card">
                    <h3>FO feature checks</h3>
                    <div className="permission-list">
                      {selected.permissions.map((permission) => (
                        <label className="admin-permission" key={permission.code}>
                          <input
                            type="checkbox"
                            checked={permission.effectiveEnabled}
                            onChange={(event) => savePermission(selected, permission.code, event.target.checked)}
                            disabled={saving === "permissions"}
                          />
                          <span>
                            <strong>{permission.name}</strong>
                            <small>{permission.description}</small>
                          </span>
                        </label>
                      ))}
                    </div>
                  </section>

                  <section className="admin-card wide">
                    <div className="admin-card-head">
                      <div>
                        <h3>Portfolio visibility</h3>
                        <p>Restricts portfolio list, details, booking, pricing, exposure and CVA.</p>
                      </div>
                      <LockKeyhole size={18} />
                    </div>
                    <div className="segmented-control admin-mode">
                      <button className={selected.portfolioAccess.accessMode === "ALL" ? "active" : ""} type="button" onClick={() => savePortfolioAccess(selected, "ALL", [])}>All portfolios</button>
                      <button className={selected.portfolioAccess.accessMode === "SELECTED" ? "active" : ""} type="button" onClick={() => savePortfolioAccess(selected, "SELECTED", selected.portfolioAccess.portfolios.map((portfolio) => portfolio.id))}>Selected</button>
                    </div>
                    <div className="portfolio-access-grid">
                      {portfolios.map((portfolio) => {
                        const selectedIds = new Set(selected.portfolioAccess.portfolios.map((item) => item.id));
                        const checked = selected.portfolioAccess.accessMode === "ALL" || selectedIds.has(portfolio.id);
                        const next = checked ? [...selectedIds].filter((id) => id !== portfolio.id) : [...selectedIds, portfolio.id];
                        return (
                          <label className="portfolio-access-card" key={portfolio.id}>
                            <input
                              type="checkbox"
                              checked={checked}
                              disabled={selected.portfolioAccess.accessMode === "ALL"}
                              onChange={() => savePortfolioAccess(selected, "SELECTED", next)}
                            />
                            <span><strong>{portfolio.name}</strong><small>{portfolio.baseCurrency} · {formatNumber(portfolio.positionCount, 0)} positions</small></span>
                          </label>
                        );
                      })}
                    </div>
                  </section>
                </div>
              </>
            ) : (
              <div className="empty">Select a user to manage groups and access.</div>
            )}
          </section>
      </div>
    </AppShell>
  );
}

function errorMessage(caught: unknown) {
  return caught instanceof NexusApiError || caught instanceof Error ? caught.message : "Unexpected admin error";
}
