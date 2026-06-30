"use client";

import React from "react";
import { usePathname } from "next/navigation";
import {
  Bell,
  BarChart3,
  BriefcaseBusiness,
  CalendarCheck,
  CircleDollarSign,
  FlaskConical,
  Gauge,
  GitCompareArrows,
  GitBranch,
  HelpCircle,
  History,
  LineChart,
  ListChecks,
  LogOut,
  Settings,
  Shield,
  SlidersHorizontal,
  Waves,
  SquarePen,
  Wallet,
} from "lucide-react";
import { authApi, nexusApi } from "@/lib/api";
import { groupForCode, isHrefAllowed, type WorkGroup } from "@/lib/authContext";
import type { AuthUser, UserNotification } from "@/lib/types";
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
  { href: "/fo-desk", label: "FO Desk", icon: BriefcaseBusiness },
  { href: "/pre-trade-analysis", label: "Pre-Trade Analysis", icon: FlaskConical },
  { href: "/stress-testing", label: "Stress Testing", icon: Waves },
  { href: "/delta-hedge", label: "Delta Hedge", icon: GitCompareArrows },
  { href: "/upad", label: "u-Pad", icon: SquarePen },
  { href: "/portfolios", label: "Portfolios", icon: Wallet },
  { href: "/pricing", label: "Pricing", icon: CircleDollarSign },
  { href: "/exposure", label: "Exposure", icon: LineChart },
  { href: "/cva", label: "CVA", icon: Shield },
  { href: "/valuation-runs", label: "Run History", icon: History },
  { href: "/trade-validation", label: "Trade Validation", icon: ListChecks },
  { href: "/lifecycle-reporting", label: "Lifecycle Reporting", icon: BarChart3 },
  { href: "/trading-limits", label: "Trading Limits", icon: SlidersHorizontal },
  { href: "/eod-control", label: "EOD Control", icon: CalendarCheck },
  { href: "/admin", label: "Administration", icon: Settings },
  { href: "/workflows", label: "Workflows", icon: GitBranch },
];

export function AppShell({ title, eyebrow, children, howTo = [] }: AppShellProps) {
  const pathname = usePathname() ?? "/";
  const [howToOpen, setHowToOpen] = React.useState(false);
  const [authChecked, setAuthChecked] = React.useState(false);
  const [authUser, setAuthUser] = React.useState<AuthUser | null>(null);
  const [activeGroup, setActiveGroupState] = React.useState<WorkGroup | null>(null);
  const [notificationsOpen, setNotificationsOpen] = React.useState(false);
  const [notifications, setNotifications] = React.useState<UserNotification[]>([]);
  const [unreadCount, setUnreadCount] = React.useState(0);
  const [notificationsError, setNotificationsError] = React.useState<string | null>(null);
  const howToRef = React.useRef<HTMLSpanElement | null>(null);
  const notificationsRef = React.useRef<HTMLSpanElement | null>(null);

  React.useEffect(() => {
    function closeOnOutsideClick(event: MouseEvent) {
      const target = event.target as Node;
      if (howToRef.current?.contains(target) || notificationsRef.current?.contains(target)) {
        return;
      }
      setHowToOpen(false);
      setNotificationsOpen(false);
    }

    document.addEventListener("mousedown", closeOnOutsideClick);
    return () => document.removeEventListener("mousedown", closeOnOutsideClick);
  }, []);

  React.useEffect(() => {
    if (!authUser) {
      return;
    }
    let cancelled = false;
    async function load() {
      try {
        const response = await nexusApi.listNotifications(true, 0, 20);
        if (cancelled) {
          return;
        }
        setNotifications(response.items);
        setUnreadCount(response.unreadCount);
        setNotificationsError(null);
      } catch (error) {
        if (!cancelled) {
          setNotificationsError(error instanceof Error ? error.message : "Notifications unavailable");
        }
      }
    }
    void load();
    const interval = window.setInterval(load, 30000);
    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, [authUser]);

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

  async function openNotification(notification: UserNotification) {
    try {
      if (notification.unread) {
        await nexusApi.markNotificationRead(notification.id);
        setNotifications((current) => current.filter((item) => item.id !== notification.id));
        setUnreadCount((current) => Math.max(0, current - 1));
      }
    } finally {
      if (notification.linkPath) {
        window.location.href = notification.linkPath;
      }
    }
  }

  async function markAllNotificationsRead() {
    await nexusApi.markAllNotificationsRead();
    setNotifications([]);
    setUnreadCount(0);
  }

  const visibleNavItems = activeGroup ? navItems.filter((item) => isHrefAllowed(activeGroup, item.href)) : navItems;

  return (
    <div className="app-shell">
      <aside className="app-sidebar">
        <div className="app-brand">
          <div className="brand-mark">
            <img src="/nexusxva-logo.svg" alt="" />
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
              <span className="notifications-wrap" ref={notificationsRef}>
                <button
                  className="header-action notifications-trigger"
                  type="button"
                  onClick={() => setNotificationsOpen((current) => !current)}
                  title="Notifications"
                >
                  <Bell size={16} />
                  <span>Notifications</span>
                  {unreadCount > 0 ? <strong className="notification-count">{unreadCount > 99 ? "99+" : unreadCount}</strong> : null}
                </button>
                {notificationsOpen ? (
                  <div className="notifications-panel" role="status">
                    <div className="notifications-head">
                      <strong>Notifications</strong>
                      <button type="button" onClick={markAllNotificationsRead} disabled={unreadCount === 0}>
                        Mark all read
                      </button>
                    </div>
                    {notificationsError ? <span className="panel-error">{notificationsError}</span> : null}
                    {!notificationsError && notifications.length === 0 ? (
                      <span className="empty-inline">You are all caught up.</span>
                    ) : null}
                    {notifications.map((notification) => (
                      <button
                        className={`notification-row ${notification.unread ? "unread" : ""}`}
                        key={notification.id}
                        type="button"
                        onClick={() => openNotification(notification)}
                      >
                        <span>
                          <strong>{notification.title}</strong>
                          <small>{new Date(notification.createdAt).toLocaleString()}</small>
                        </span>
                        <em>{notification.message}</em>
                      </button>
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
