export type OptionType = "CALL" | "PUT";

export type PortfolioSummary = {
  id: string;
  name: string;
  description: string | null;
  baseCurrency: string;
  createdAt: string;
  updatedAt: string;
  positionCount: number;
};

export type Portfolio = {
  id: string;
  name: string;
  description: string | null;
  baseCurrency: string;
  createdAt: string;
  updatedAt: string;
  positions: EuropeanOptionPosition[];
};

export type EuropeanOptionPosition = {
  id: string;
  portfolioId: string;
  underlyingSymbol: string;
  optionType: OptionType;
  strike: number;
  maturityDate: string;
  quantity: number;
  createdAt: string;
  updatedAt: string;
};

export type CreatePortfolioRequest = {
  name: string;
  description?: string | null;
  baseCurrency?: string;
};

export type AddEuropeanOptionPositionRequest = {
  underlyingSymbol: string;
  optionType: OptionType;
  strike: number;
  maturityDate: string;
  quantity: number;
};

export type TradeBookingStatus = "PENDING_VALIDATION" | "CONFIRMED" | "REJECTED";

export type BookingActor = {
  userId: string | null;
  username: string;
  displayName: string;
};

export type TradeBooking = {
  id: string;
  portfolioId: string | null;
  portfolioName: string;
  instrumentType: "EUROPEAN_OPTION";
  underlyingSymbol: string;
  optionType: OptionType;
  strike: number;
  maturityDate: string;
  quantity: number;
  status: TradeBookingStatus;
  submittedBy: BookingActor;
  submittedAt: string;
  reviewedBy: BookingActor | null;
  reviewedAt: string | null;
  rejectionReason: string | null;
  confirmedPositionId: string | null;
};

export type TradeBookingPage = {
  items: TradeBooking[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type Greeks = {
  delta: number;
  gamma: number;
  vega: number;
  theta: number;
  rho: number;
};

export type PortfolioPricingResponse = {
  portfolioId: string;
  valuationDate: string;
  model: string;
  baseCurrency: string;
  totalPrice: number;
  totalGreeks: Greeks;
  positions: PortfolioPositionPricing[];
  unpriceablePositions: UnpriceablePortfolioPosition[];
};

export type PortfolioPositionPricing = {
  positionId: string;
  status: string;
  underlyingSymbol: string;
  quantity: number;
  unitPrice: number;
  positionPrice: number;
  unitGreeks: Greeks;
  positionGreeks: Greeks;
  marketData: PortfolioPositionMarketData;
};

export type PortfolioPositionMarketData = {
  spot: number;
  volatility: number;
  riskFreeRate: number;
  dividendYield: number;
  currency: string;
  asOf: string;
  source: string;
  stale: boolean;
};

export type UnpriceablePortfolioPosition = {
  positionId: string;
  status: string;
  reason: string;
};

export type ExposureSimulationRequest = {
  portfolioId: string;
  valuationDate: string;
  horizonDays: number;
  timeSteps: number;
  paths: number;
  seed: number;
  pfeConfidenceLevel: number;
};

export type ExposureSimulationResponse = {
  portfolioId: string;
  valuationDate: string;
  model: string;
  paths: number;
  timeSteps: number;
  pfeConfidenceLevel: number;
  points: ExposurePoint[];
};

export type ExposurePoint = {
  date: string;
  expectedExposure: number;
  expectedNegativeExposure: number;
  pfe: number;
};

export type CvaCalculationRequest = ExposureSimulationRequest & {
  lossGivenDefault: number;
  counterpartyHazardRate: number;
  discountRate: number;
};

export type CvaCalculationResponse = {
  portfolioId: string;
  valuationDate: string;
  model: string;
  exposureModel: string;
  paths: number;
  timeSteps: number;
  pfeConfidenceLevel: number;
  lossGivenDefault: number;
  counterpartyHazardRate?: number;
  discountRate?: number;
  creditMethod: "FLAT_HAZARD_RATE" | "CREDIT_CURVE";
  discountMethod: "FLAT_RATE" | "DISCOUNT_CURVE";
  cva: number;
  points: CvaPoint[];
};

export type CvaPoint = {
  date: string;
  expectedExposure: number;
  discountFactor: number;
  survivalProbability: number;
  defaultProbabilityIncrement: number;
  discountedExpectedExposure: number;
  cvaContribution: number;
};

export type ApiError = {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  violations?: Array<{ field: string; message: string }>;
  metadata?: Record<string, unknown>;
};

export type TradingLimitStatus = "ACTIVE" | "DISABLED" | "UNLIMITED";

export type TradingLimitPolicy = {
  maxTradesPerHour: number | null;
  maxTradesPerDay: number | null;
  maxNotionalPerHour: number | null;
  maxNotionalPerDay: number | null;
  notionalCurrency: "USD";
  active: boolean;
  createdAt: string;
  updatedAt: string;
  updatedByUserId: string | null;
  updatedByUsername: string;
  updatedByDisplayName: string;
  version: number;
};

export type TradingLimitUsage = {
  tradesThisHour: number;
  tradesToday: number;
  notionalThisHour: number;
  notionalToday: number;
  hourEndsAt: string;
  dayEndsAt: string;
};

export type TradingLimitRemaining = {
  tradesThisHour: number | null;
  tradesToday: number | null;
  notionalThisHour: number | null;
  notionalToday: number | null;
};

export type TradingLimitSnapshot = {
  userId: string | null;
  username: string;
  displayName: string;
  status: TradingLimitStatus;
  policy: TradingLimitPolicy | null;
  usage: TradingLimitUsage;
  remaining: TradingLimitRemaining;
};

export type TradingLimitUserPage = {
  items: TradingLimitSnapshot[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type UpdateTradingLimitRequest = {
  maxTradesPerHour: number | null;
  maxTradesPerDay: number | null;
  maxNotionalPerHour: number | null;
  maxNotionalPerDay: number | null;
  active: boolean;
  version: number | null;
};

export type AuthUser = {
  id: string;
  username: string;
  displayName: string;
  groups: string[];
};

export type AuthResponse = {
  enabled: boolean;
  authenticated: boolean;
  user: AuthUser | null;
  activeGroup: string | null;
  csrfToken: string | null;
};

export type LoginRequest = {
  username: string;
  password: string;
};

export type BlembergHealthResponse = {
  status?: string;
};

export type BlembergRefreshResponse = {
  runId?: string;
  status?: string;
  symbolsRequested?: number;
  symbolsSucceeded?: number;
  symbolsFailed?: number;
  jobSummaries?: BlembergRefreshJobSummary[];
  errors?: BlembergRefreshError[];
};

export type BlembergRefreshJobSummary = {
  jobName?: string;
  requested?: number;
  succeeded?: number;
  failed?: number;
  skippedRateLimit?: number;
};

export type BlembergRefreshError = {
  jobName?: string;
  provider?: string;
  symbol?: string;
  status?: string;
  errorCode?: string;
  message?: string;
};

export type BlembergMarketSnapshot = {
  symbol: string;
  lastPrice?: number | null;
  open?: number | null;
  high?: number | null;
  low?: number | null;
  previousClose?: number | null;
  volume?: number | null;
  currency?: string | null;
  asOf?: string | null;
  source?: string | null;
};

export type BlembergSnapshotsResponse = {
  snapshots: BlembergMarketSnapshot[];
  missingSymbols: string[];
};
