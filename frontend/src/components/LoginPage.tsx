"use client";

import React, { useState } from "react";
import { BadgeCheck, BriefcaseBusiness, Building2, LockKeyhole, ShieldCheck, UsersRound } from "lucide-react";
import { authApi, NexusApiError } from "@/lib/api";
import { groupsForUser, type WorkGroup } from "@/lib/authContext";
import type { AuthUser } from "@/lib/types";

export function LoginPage() {
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [user, setUser] = useState<AuthUser | null>(null);

  const availableGroups = user ? groupsForUser(user) : [];

  React.useEffect(() => {
    let cancelled = false;
    authApi.me()
      .then((response) => {
        if (!cancelled && response.enabled && response.authenticated && response.user) {
          setUser(response.user);
        }
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, []);

  async function login(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const response = await authApi.login({ username, password });
      if (!response.enabled) {
        window.location.href = "/";
        return;
      }
      if (response.authenticated && response.user) {
        const groups = groupsForUser(response.user);
        if (groups.length === 1) {
          chooseGroup(groups[0]);
          return;
        }
        setUser(response.user);
        return;
      }
      setError("Login failed");
    } catch (caught) {
      setError(caught instanceof NexusApiError || caught instanceof Error ? caught.message : "Login failed");
    } finally {
      setLoading(false);
    }
  }

  async function chooseGroup(group: WorkGroup) {
    setLoading(true);
    setError(null);
    try {
      await authApi.selectActiveGroup(group.code);
      window.location.href = group.landingHref;
    } catch (caught) {
      setError(caught instanceof NexusApiError || caught instanceof Error ? caught.message : "Could not select group");
      setLoading(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-shell">
        <div className="login-panel">
          <div className="login-brand">
            <span>
              <ShieldCheck size={24} />
            </span>
            <div>
              <strong>NexusXVA</strong>
              <small>Secure risk workstation</small>
            </div>
          </div>

          <div className="login-copy">
            <span className="page-eyebrow">Access control</span>
            <h1>{user ? "Choose your active desk." : "Sign in to your risk workspace."}</h1>
            <p>
              {user
                ? "Your user can belong to several groups. Pick the active context for this session and NexusXVA will show the screens that match that desk."
                : "One user can carry multiple groups at once. After login you choose which group is active for this session."}
            </p>
          </div>

          {user ? (
            <div className="desk-picker">
              {availableGroups.map((group) => (
                <button className="desk-choice" disabled={loading} key={group.code} type="button" onClick={() => chooseGroup(group)}>
                  <GroupIcon code={group.code} />
                  <span>
                    <strong>{group.code}</strong>
                    <small>{group.name}</small>
                  </span>
                  <p>{group.description}</p>
                </button>
              ))}
              {availableGroups.length === 0 ? <div className="alert">This user has no supported NexusXVA group.</div> : null}
            </div>
          ) : (
            <form className="login-form" onSubmit={login}>
              <label className="field full">
                <span>Username</span>
                <input className="input" autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)} />
              </label>
              <label className="field full">
                <span>Password</span>
                <input className="input" autoComplete="current-password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
              </label>
              {error ? <div className="alert">{error}</div> : null}
              <button className="btn login-submit" disabled={loading} type="submit">
                <LockKeyhole size={16} />
                {loading ? "Signing in" : "Sign in"}
              </button>
            </form>
          )}

          <div className="login-security-strip">
            <span><BadgeCheck size={15} /> BCrypt passwords</span>
            <span><BadgeCheck size={15} /> HttpOnly sessions</span>
            <span><BadgeCheck size={15} /> CSRF protected</span>
          </div>
        </div>

        <aside className="login-side">
          <div>
            <span className="page-eyebrow">Groups attached to the user</span>
            <h2>One identity, multiple working contexts.</h2>
            <p>FO, BO and ADMIN are memberships on the same account. The selected group controls the first screen and the menu visible in the workstation.</p>
          </div>

          <div className="group-stack">
            <GroupCard icon={<BriefcaseBusiness size={18} />} code="FO" name="Front Office" text="Trade capture, pricing, exposure and risk workflows." />
            <GroupCard icon={<Building2 size={18} />} code="BO" name="Back Office" text="Operational review, lifecycle checks and control workflows." />
            <GroupCard icon={<UsersRound size={18} />} code="ADMIN" name="Admin" text="User, group and platform administration." />
          </div>

          <div className="login-note">
            <strong>Docker dev bootstrap</strong>
            <span>Default user is `admin`; password comes from `NEXUSXVA_BOOTSTRAP_ADMIN_PASSWORD` and defaults to `admin12345` in compose.</span>
          </div>
        </aside>
      </section>
    </main>
  );
}

function GroupCard({ icon, code, name, text }: { icon: React.ReactNode; code: string; name: string; text: string }) {
  return (
    <div className="group-card">
      <span>{icon}</span>
      <div>
        <strong>{code}</strong>
        <small>{name}</small>
      </div>
      <p>{text}</p>
    </div>
  );
}

function GroupIcon({ code }: { code: string }) {
  if (code === "FO") {
    return <BriefcaseBusiness size={18} />;
  }
  if (code === "BO") {
    return <Building2 size={18} />;
  }
  return <UsersRound size={18} />;
}
