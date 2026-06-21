import type {
  AddEuropeanOptionPositionRequest,
  AdminPortfolioSummary,
  AdminUserAccess,
  AdminUserPage,
  AdminWorkflowMap,
  ApiError,
  AuthResponse,
  BlembergHealthResponse,
  BlembergSnapshotsResponse,
  BlembergRefreshResponse,
  CreatePortfolioRequest,
  CvaCalculationRequest,
  CvaCalculationResponse,
  ExposureSimulationRequest,
  ExposureSimulationResponse,
  FrontOfficeDeskResponse,
  FrontOfficeStressTestRequest,
  FrontOfficeStressTestResponse,
  FrontOfficeWhatIfRequest,
  FrontOfficeWhatIfResponse,
  LoginRequest,
  NotificationPage,
  Portfolio,
  PortfolioPricingResponse,
  PortfolioSummary,
  TradeBooking,
  TradeBookingPage,
  TradeBookingStatus,
  TradeLifecyclePage,
  TradeLifecycleRequest,
  TradeLifecycleRequestStatus,
  TradingLimitSnapshot,
  TradingLimitUserPage,
  UpdateTradingLimitRequest,
  UserNotification,
} from "./types";

export const API_BASE_URL = (process.env.NEXT_PUBLIC_NEXUSXVA_API_BASE_URL ?? "/nexus-api").replace(/\/$/, "");
const BLEMBERG_API_BASE_URL = "/blemberg-api";
let csrfToken: string | null = null;

export class NexusApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly body: ApiError | string | null,
  ) {
    super(message);
    this.name = "NexusApiError";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const method = init?.method?.toUpperCase() ?? "GET";
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json",
      ...(csrfToken && !["GET", "HEAD", "OPTIONS"].includes(method) ? { "X-CSRF-Token": csrfToken } : {}),
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    const body = await readErrorBody(response);
    const message = typeof body === "object" && body?.message ? body.message : `Request failed with status ${response.status}`;
    if (response.status === 401 && typeof window !== "undefined" && window.location.pathname !== "/login") {
      window.location.href = "/login";
    }
    throw new NexusApiError(message, response.status, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

async function readErrorBody(response: Response): Promise<ApiError | string | null> {
  const text = await response.text();
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text) as ApiError;
  } catch {
    return text.slice(0, 500);
  }
}

export const nexusApi = {
  listNotifications: (unreadOnly = false, page = 0, size = 20) => {
    const query = new URLSearchParams({ unreadOnly: String(unreadOnly), page: String(page), size: String(size) });
    return request<NotificationPage>(`/notifications?${query.toString()}`);
  },

  markNotificationRead: (notificationId: string) =>
    request<UserNotification>(`/notifications/${notificationId}/read`, {
      method: "POST",
    }),

  markAllNotificationsRead: () =>
    request<void>("/notifications/read-all", {
      method: "POST",
    }),

  listPortfolios: () => request<PortfolioSummary[]>("/portfolios"),

  getPortfolio: (portfolioId: string) => request<Portfolio>(`/portfolios/${portfolioId}`),

  createPortfolio: (body: CreatePortfolioRequest) =>
    request<Portfolio>("/portfolios", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  submitEuropeanOptionBooking: (portfolioId: string, body: AddEuropeanOptionPositionRequest) =>
    request<TradeBooking>(`/portfolios/${portfolioId}/trade-bookings/european-options`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  listMyTradeBookings: () => request<TradeBooking[]>("/trade-bookings/mine"),

  getFrontOfficeDesk: () => request<FrontOfficeDeskResponse>("/front-office/desk"),

  runFrontOfficeWhatIf: (body: FrontOfficeWhatIfRequest) =>
    request<FrontOfficeWhatIfResponse>("/front-office/what-if/european-option", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  runFrontOfficeStressTest: (body: FrontOfficeStressTestRequest) =>
    request<FrontOfficeStressTestResponse>("/front-office/stress-tests/european-options", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  requestPositionAmend: (positionId: string, body: AddEuropeanOptionPositionRequest) =>
    request<TradeLifecycleRequest>(`/front-office/lifecycle/positions/${positionId}/amend`, {
      method: "POST",
      body: JSON.stringify(body),
    }),

  requestPositionCancel: (positionId: string) =>
    request<TradeLifecycleRequest>(`/front-office/lifecycle/positions/${positionId}/cancel`, {
      method: "POST",
    }),

  listMyLifecycleRequests: () => request<TradeLifecycleRequest[]>("/front-office/lifecycle/mine"),

  listBackOfficeTradeBookings: (status?: TradeBookingStatus, page = 0, size = 50) => {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) {
      query.set("status", status);
    }
    return request<TradeBookingPage>(`/back-office/trade-bookings?${query.toString()}`);
  },

  getBackOfficeTradeBooking: (bookingId: string) =>
    request<TradeBooking>(`/back-office/trade-bookings/${bookingId}`),

  approveTradeBooking: (bookingId: string) =>
    request<TradeBooking>(`/back-office/trade-bookings/${bookingId}/approve`, {
      method: "POST",
    }),

  rejectTradeBooking: (bookingId: string, rejectionReason: string) =>
    request<TradeBooking>(`/back-office/trade-bookings/${bookingId}/reject`, {
      method: "POST",
      body: JSON.stringify({ rejectionReason }),
    }),

  listBackOfficeLifecycleRequests: (status?: TradeLifecycleRequestStatus, page = 0, size = 50) => {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) {
      query.set("status", status);
    }
    return request<TradeLifecyclePage>(`/back-office/lifecycle-requests?${query.toString()}`);
  },

  approveLifecycleRequest: (requestId: string) =>
    request<TradeLifecycleRequest>(`/back-office/lifecycle-requests/${requestId}/approve`, {
      method: "POST",
    }),

  rejectLifecycleRequest: (requestId: string, rejectionReason: string) =>
    request<TradeLifecycleRequest>(`/back-office/lifecycle-requests/${requestId}/reject`, {
      method: "POST",
      body: JSON.stringify({ rejectionReason }),
    }),

  getMyTradingLimits: () => request<TradingLimitSnapshot>("/trading-limits/me"),

  listTradingLimitUsers: (query = "", page = 0, size = 50) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (query.trim()) {
      params.set("query", query.trim());
    }
    return request<TradingLimitUserPage>(`/back-office/trading-limits/users?${params.toString()}`);
  },

  getTradingLimitUser: (userId: string) =>
    request<TradingLimitSnapshot>(`/back-office/trading-limits/users/${userId}`),

  updateTradingLimitUser: (userId: string, body: UpdateTradingLimitRequest) =>
    request<TradingLimitSnapshot>(`/back-office/trading-limits/users/${userId}`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),

  runPortfolioPricing: (portfolioId: string, valuationDate: string) =>
    request<PortfolioPricingResponse>(`/portfolios/${portfolioId}/pricing/black-scholes`, {
      method: "POST",
      body: JSON.stringify({ valuationDate }),
    }),

  runExposure: (body: ExposureSimulationRequest) =>
    request<ExposureSimulationResponse>("/simulations/exposure", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  runCva: (body: CvaCalculationRequest) =>
    request<CvaCalculationResponse>("/risk/cva", {
      method: "POST",
      body: JSON.stringify(body),
    }),

  listAdminUsers: (query = "", page = 0, size = 50) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (query.trim()) {
      params.set("query", query.trim());
    }
    return request<AdminUserPage>(`/admin/users?${params.toString()}`);
  },

  getAdminUser: (userId: string) => request<AdminUserAccess>(`/admin/users/${userId}`),

  listAdminPortfolios: () => request<AdminPortfolioSummary[]>("/admin/portfolios"),

  deleteAdminPortfolio: (portfolioId: string) =>
    request<void>(`/admin/portfolios/${portfolioId}`, {
      method: "DELETE",
    }),

  updateAdminUserGroups: (userId: string, groups: string[]) =>
    request<AdminUserAccess>(`/admin/users/${userId}/groups`, {
      method: "PUT",
      body: JSON.stringify({ groups }),
    }),

  updateAdminUserPermissions: (userId: string, permissions: Record<string, boolean>) =>
    request<AdminUserAccess>(`/admin/users/${userId}/permissions`, {
      method: "PUT",
      body: JSON.stringify({ permissions }),
    }),

  updateAdminPortfolioAccess: (userId: string, accessMode: "ALL" | "SELECTED", portfolioIds: string[]) =>
    request<AdminUserAccess>(`/admin/users/${userId}/portfolio-access`, {
      method: "PUT",
      body: JSON.stringify({ accessMode, portfolioIds }),
    }),

  getAdminWorkflowMap: (portfolioId?: string) => {
    const params = portfolioId ? `?portfolioId=${encodeURIComponent(portfolioId)}` : "";
    return request<AdminWorkflowMap>(`/admin/workflow-map${params}`);
  },
};

export const authApi = {
  me: async () => {
    const response = await request<AuthResponse>("/auth/me");
    csrfToken = response.csrfToken;
    return response;
  },

  login: async (body: LoginRequest) => {
    const response = await request<AuthResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify(body),
    });
    csrfToken = response.csrfToken;
    return response;
  },

  selectActiveGroup: async (group: string) => {
    const response = await request<AuthResponse>("/auth/active-group", {
      method: "POST",
      body: JSON.stringify({ group }),
    });
    csrfToken = response.csrfToken;
    return response;
  },

  logout: async () => {
    const response = await request<AuthResponse>("/auth/logout", {
      method: "POST",
    });
    csrfToken = null;
    return response;
  },
};

export const blembergApi = {
  health: () => externalRequest<BlembergHealthResponse>(`${BLEMBERG_API_BASE_URL}/actuator/health`),

  snapshots: (symbols: string[]) =>
    externalRequest<BlembergSnapshotsResponse>(`${BLEMBERG_API_BASE_URL}/api/market-data/snapshots?symbols=${encodeURIComponent(symbols.join(","))}`),

  refreshMarketData: (prioritySymbols?: string[]) =>
    externalRequest<BlembergRefreshResponse>(`${BLEMBERG_API_BASE_URL}/api/admin/market-data/refresh`, {
      method: "POST",
      body: prioritySymbols && prioritySymbols.length > 0 ? JSON.stringify({ prioritySymbols }) : undefined,
    }),
};

async function externalRequest<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    const body = await readErrorBody(response);
    const message = typeof body === "object" && body?.message ? body.message : `Request failed with status ${response.status}`;
    throw new NexusApiError(message, response.status, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}
