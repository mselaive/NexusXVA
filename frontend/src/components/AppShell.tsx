"use client";

import React from "react";
import { usePathname } from "next/navigation";
import {
  BarChart3,
  CircleDollarSign,
  Gauge,
  HelpCircle,
  LineChart,
  ListChecks,
  LogOut,
  Settings,
  Shield,
  SlidersHorizontal,
  SquarePen,
  Wallet,
} from "lucide-react";
import { authApi } from "@/lib/api";
import { groupForCode, isHrefAllowed, type WorkGroup } from "@/lib/authContext";
import type { AuthUser } from "@/lib/types";
import { MarketDataStatus } from "./MarketDataStatus";

type AppShellProps = {
  title: string;
  eyebrow: string;
  children: React.ReactNode;
  howTo?: HowToItem[];
};

export type HowToItem = {
  title: string;
  body: string;
};

const navItems = [
  { href: "/", label: "Overview", icon: Gauge },
  { href: "/upad", label: "u-Pad", icon: SquarePen },
  { href: "/portfolios", label: "Portfolios", icon: Wallet },
  { href: "/pricing", label: "Pricing", icon: CircleDollarSign },
  { href: "/exposure", label: "Exposure", icon: LineChart },
  { href: "/cva", label: "CVA", icon: Shield },
  { href: "/trade-validation", label: "Trade Validation", icon: ListChecks },
  { href: "/trading-limits", label: "Trading Limits", icon: SlidersHorizontal },
  { href: "/admin", label: "Administration", icon: Settings },
];

export function AppShell({ title, eyebrow, children, howTo = [] }: AppShellProps) {
  const pathname = usePathname() ?? "/";
  const [howToOpen, setHowToOpen] = React.useState(false);
  const [authChecked, setAuthChecked] = React.useState(false);
  const [authUser, setAuthUser] = React.useState<AuthUser | null>(null);
  const [activeGroup, setActiveGroupState] = React.useState<WorkGroup | null>(null);
  const howToRef = React.useRef<HTMLSpanElement | null>(null);

  React.useEffect(() => {
    function closeOnOutsideClick(event: MouseEvent) {
      if (!howToRef.current || howToRef.current.contains(event.target as Node)) {
        return;
      }
      setHowToOpen(false);
    }

    document.addEventListener("mousedown", closeOnOutsideClick);
    return () => document.removeEventListener("mousedown", closeOnOutsideClick);
  }, []);

  React.useEffect(() => {
    let cancelled = false;
    authApi.me()
      .then((response) => {
        if (cancelled) {
          return;
        }
        if (response.enabled && !response.authenticated) {
          window.location.href = "/login";
          return;
        }
        if (!response.enabled) {
          setAuthChecked(true);
          return;
        }
        if (!response.user) {
          window.location.href = "/login";
          return;
        }
        const group = groupForCode(response.user, response.activeGroup);
        if (!group) {
          window.location.href = "/login";
          return;
        }
        if (!isHrefAllowed(group, pathname)) {
          window.location.href = group.landingHref;
          return;
        }
        setAuthUser(response.user);
        setActiveGroupState(group);
        setAuthChecked(true);
      })
      .catch(() => {
        if (!cancelled) {
          window.location.href = "/login";
        }
      });
    return () => {
      cancelled = true;
    };
  }, [pathname]);

  async function logout() {
    await authApi.logout();
    window.location.href = "/login";
  }

  const visibleNavItems = activeGroup ? navItems.filter((item) => isHrefAllowed(activeGroup, item.href)) : navItems;

  return (
    <div className="app-shell">
      <aside className="app-sidebar">
        <div className="app-brand">
          <div className="brand-mark">
            <BarChart3 size={20} />
          </div>
          <div>
            <strong>NexusXVA</strong>
            <span>Risk workstation</span>
          </div>
        </div>

        <nav className="app-nav" aria-label="Dashboard navigation">
          {visibleNavItems.map((item) => {
            const Icon = item.icon;
            const active = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
            return (
              <a className={`nav-link ${active ? "active" : ""}`} href={item.href} key={item.href}>
                <Icon size={17} />
                <span>{item.label}</span>
              </a>
            );
          })}
        </nav>

        <div className="sidebar-note">
          <span className="badge">USD V1</span>
          <span className="muted">European options · stateless analytics</span>
        </div>
      </aside>

      <main className="app-main">
        {!authChecked ? <div className="auth-loading">Checking session...</div> : null}
        <header className="page-header">
          <div>
            <span className="page-eyebrow">{eyebrow}</span>
            <h1>{title}</h1>
          </div>
          <div className="page-actions">
            {howTo.length > 0 ? (
              <span className="howto-wrap" ref={howToRef}>
                <button className="btn secondary howto-trigger" type="button" onClick={() => setHowToOpen((current) => !current)}>
                  <HelpCircle size={16} />
                  How to
                </button>
                {howToOpen ? (
                  <div className="howto-panel" role="status">
                    {howTo.map((item) => (
                      <div key={item.title}>
                        <strong>{item.title}</strong>
                        <span>{item.body}</span>
                      </div>
                    ))}
                  </div>
                ) : null}
              </span>
            ) : null}
            {authUser ? (
              <button className="user-pill" type="button" onClick={() => { window.location.href = "/login"; }} title="Switch active group">
                <strong>{authUser.displayName}</strong>
                <span>{activeGroup ? `${activeGroup.code} · ${activeGroup.name}` : authUser.groups.join(" · ")}</span>
              </button>
            ) : null}
            {authUser ? (
              <button className="header-action header-logout" type="button" onClick={logout} title="Log out">
                <LogOut size={16} />
                <span>Log out</span>
              </button>
            ) : null}
            <MarketDataStatus />
          </div>
        </header>
        {authChecked ? children : null}
      </main>
    </div>
  );
}
