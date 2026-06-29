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
  executionPrice: number | null;
  strategyId: string | null;
  strategyType: OptionStrategyType | null;
  strategyName: string | null;
  strategyLegIndex: number | null;
  lifecycleStatus: "ACTIVE" | "CANCELLED" | "AMENDED";
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
  executionPrice?: number | null;
};

export type TradeBookingStatus = "PENDING_VALIDATION" | "CONFIRMED" | "REJECTED";
export type TradeBookingType = "SINGLE_OPTION" | "OPTION_STRATEGY";
export type OptionStrategyType = "CALL_SPREAD" | "PUT_SPREAD" | "STRADDLE" | "STRANGLE" | "RISK_REVERSAL" | "BUTTERFLY" | "CUSTOM";

export type OptionStrategyLegRequest = {
  optionType: OptionType;
  strike: number;
  maturityDate: string;
  quantity: number;
  executionPrice?: number | null;
};

export type CreateOptionStrategyBookingRequest = {
  strategyType: OptionStrategyType;
  strategyName?: string | null;
  underlyingSymbol: string;
  legs: OptionStrategyLegRequest[];
};

export type TradeBookingLeg = {
  legIndex: number;
  optionType: OptionType;
  strike: number;
  maturityDate: string;
  quantity: number;
  executionPrice: number | null;
};

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
  bookingType: TradeBookingType;
  strategyId: string | null;
  strategyType: OptionStrategyType | null;
  strategyName: string | null;
  underlyingSymbol: string;
  optionType: OptionType;
  strike: number;
  maturityDate: string;
  quantity: number;
  executionPrice: number | null;
  bookingNotional: number | null;
  legs: TradeBookingLeg[];
  status: TradeBookingStatus;
  submittedBy: BookingActor;
  submittedAt: string;
  reviewedBy: BookingActor | null;
  reviewedAt: string | null;
  rejectionReason: string | null;
  confirmedPositionId: string | null;
  confirmedPositionIds: string[];
};

export type FrontOfficeDeskBooking = TradeBooking & {
  portfolioVisible: boolean;
};

export type FrontOfficeDeskResponse = {
  user: {
    id: string | null;
    username: string;
    displayName: string;
  };
  bookingCounts: {
    pendingValidation: number;
    confirmed: number;
    rejected: number;
    total: number;
  };
  portfolios: PortfolioSummary[];
  bookings: FrontOfficeDeskBooking[];
};

export type TradeBookingPage = {
  items: TradeBooking[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type TradeLifecycleRequestType = "AMEND" | "CANCEL";
export type TradeLifecycleRequestStatus = "PENDING_VALIDATION" | "APPROVED" | "REJECTED";

export type TradeLifecycleRequest = {
  id: string;
  portfolioId: string | null;
  portfolioName: string;
  positionId: string | null;
  requestType: TradeLifecycleRequestType;
  status: TradeLifecycleRequestStatus;
  originalUnderlyingSymbol: string;
  originalOptionType: OptionType;
  originalStrike: number;
  originalMaturityDate: string;
  originalQuantity: number;
  requestedUnderlyingSymbol: string | null;
  requestedOptionType: OptionType | null;
  requestedStrike: number | null;
  requestedMaturityDate: string | null;
  requestedQuantity: number | null;
  submittedBy: BookingActor;
  submittedAt: string;
  reviewedBy: BookingActor | null;
  reviewedAt: string | null;
  rejectionReason: string | null;
  resultingPositionId: string | null;
};

export type TradeLifecyclePage = {
  items: TradeLifecycleRequest[];
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
  totalTradeValue: number;
  totalUnrealizedPnl: number;
  positionsWithoutExecutionPrice: number;
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
  executionPrice: number | null;
  tradeValue: number | null;
  unrealizedPnl: number | null;
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

export type PositionEodSnapshot = {
  positionId: string;
  underlyingSymbol: string;
  quantity: number;
  unitPrice: number;
  marketValue: number;
  executionPrice: number | null;
  tradeValue: number | null;
  unrealizedPnl: number | null;
  marketDataAsOf: string;
  marketDataSource: string;
  stale: boolean;
};

export type PortfolioEodSnapshot = {
  id: string;
  portfolioId: string;
  businessDate: string;
  baseCurrency: string;
  totalMarketValue: number;
  totalTradeValue: number;
  totalUnrealizedPnl: number;
  positionsWithoutExecutionPrice: number;
  capturedAt: string;
  source: string;
  status: "ACTIVE" | "VOIDED" | "SUPERSEDED";
  voidedAt: string | null;
  voidedByUserId: string | null;
  voidReason: string | null;
  correctionOfRunId: string | null;
  positions: PositionEodSnapshot[];
};

export type EodBatchPortfolioResult = {
  portfolioId: string;
  portfolioName: string;
  status: "CAPTURED" | "SKIPPED" | "FAILED";
  message: string;
};

export type EodBatchResult = {
  businessDate: string;
  totalPortfolios: number;
  captured: number;
  skipped: number;
  failed: number;
  completedAt: string;
  portfolios: EodBatchPortfolioResult[];
};

export type PositionDailyPnl = {
  positionId: string;
  underlyingSymbol: string;
  currentMarketValue: number;
  referenceValue: number | null;
  dailyPnl: number | null;
  referenceMethod: "PRIOR_EOD" | "EXECUTION" | "UNAVAILABLE";
};

export type PortfolioDailyPnl = {
  portfolioId: string;
  valuationDate: string;
  previousEodDate: string | null;
  baseCurrency: string;
  currentMarketValue: number;
  dailyPnl: number;
  positionsWithoutReference: number;
  positions: PositionDailyPnl[];
};

export type FrontOfficeWhatIfRequest = {
  portfolioId: string;
  valuationDate: string;
  trade: AddEuropeanOptionPositionRequest;
};

export type WhatIfPortfolioTotals = {
  totalPrice: number;
  totalGreeks: Greeks;
};

export type WhatIfImpact = Greeks & {
  price: number;
};

export type FrontOfficeWhatIfResponse = {
  portfolioId: string;
  valuationDate: string;
  model: string;
  basePortfolio: WhatIfPortfolioTotals;
  hypotheticalTrade: PortfolioPositionPricing;
  withTradePortfolio: WhatIfPortfolioTotals;
  impact: WhatIfImpact;
};

export type StressScenarioRequest = {
  name: string;
  spotShockPercent: number;
  volatilityShockBps: number;
  riskFreeRateShockBps: number;
  dividendYieldShockBps: number;
};

export type FrontOfficeStressTestRequest = {
  portfolioId: string;
  valuationDate: string;
  hypotheticalTrade?: AddEuropeanOptionPositionRequest | null;
  scenarios: StressScenarioRequest[];
};

export type StressPortfolioTotals = {
  totalPrice: number;
  totalGreeks: Greeks;
};

export type StressImpact = Greeks & {
  price: number;
};

export type StressScenarioResult = {
  scenario: StressScenarioRequest;
  totals: StressPortfolioTotals;
  impact: StressImpact;
  positions: PortfolioPositionPricing[];
};

export type FrontOfficeStressTestResponse = {
  portfolioId: string;
  valuationDate: string;
  model: string;
  baseCurrency: string;
  basePortfolio: StressPortfolioTotals;
  hypotheticalTrade: PortfolioPositionPricing | null;
  scenarios: StressScenarioResult[];
  unpriceablePositions: UnpriceablePortfolioPosition[];
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

export type ValuationRunType = "PRICING" | "EXPOSURE" | "CVA";
export type ValuationRunStatus = "SUCCESS" | "FAILED";

export type ValuationRun = {
  id: string;
  portfolioId: string;
  portfolioName: string;
  runType: ValuationRunType;
  model: string;
  valuationDate: string | null;
  status: ValuationRunStatus;
  requestedByUserId: string | null;
  requestedByUsername: string | null;
  requestedByDisplayName: string | null;
  activeGroupCode: string | null;
  input: unknown;
  result: unknown | null;
  summary: unknown | null;
  errorMessage: string | null;
  createdAt: string;
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

export type AdminFeaturePermission = {
  code: string;
  groupCode: string;
  name: string;
  description: string;
  effectiveEnabled: boolean;
  overrideEnabled: boolean | null;
};

export type AdminPortfolioSummary = {
  id: string;
  name: string;
  baseCurrency: string;
  positionCount: number;
};

export type AdminPortfolioAccess = {
  accessMode: "ALL" | "SELECTED";
  portfolios: AdminPortfolioSummary[];
};

export type AdminUserAccess = {
  id: string;
  username: string;
  displayName: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  lastLoginAt: string | null;
  groups: string[];
  permissions: AdminFeaturePermission[];
  portfolioAccess: AdminPortfolioAccess;
};

export type AdminUserPage = {
  items: AdminUserAccess[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type AdminWorkflowBooking = {
  id: string;
  portfolioId: string | null;
  portfolioName: string;
  node: string;
  status: string;
  underlyingSymbol: string;
  optionType: OptionType;
  strike: number;
  maturityDate: string;
  quantity: number;
  submittedBy: string | null;
  submittedAt: string;
  reviewedBy: string | null;
  reviewedAt: string | null;
  rejectionReason: string | null;
  confirmedPositionId: string | null;
};

export type AdminWorkflowNode = {
  id: string;
  label: string;
  description: string;
  count: number;
  bookings: AdminWorkflowBooking[];
};

export type AdminWorkflowLink = {
  from: string;
  to: string;
  count: number;
};

export type AdminWorkflowMap = {
  portfolioId: string | null;
  nodes: AdminWorkflowNode[];
  links: AdminWorkflowLink[];
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

export type UserNotification = {
  id: string;
  notificationType: string;
  title: string;
  message: string;
  linkPath: string | null;
  unread: boolean;
  createdAt: string;
  readAt: string | null;
};

export type NotificationPage = {
  items: UserNotification[];
  unreadCount: number;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
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
  requestedSymbols?: string[];
  attemptedSymbols?: string[];
  succeededSymbols?: string[];
  skippedRateLimitSymbols?: string[];
  missingSnapshotSymbols?: string[];
  pricingNotReadySymbols?: string[];
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

export type BlembergCoverageItem = {
  symbol: string;
  pricingReady?: boolean;
  readyForPricing?: boolean;
  hasSnapshot?: boolean;
  snapshotReady?: boolean;
  hasEnoughDailyBars?: boolean;
  barsReady?: boolean;
  hasRiskFreeRate?: boolean;
  riskFreeRateReady?: boolean;
  hasDividendYield?: boolean;
  dividendYieldReady?: boolean;
  missingReasons?: string[];
  reasons?: string[];
  status?: string;
};

export type BlembergCoverageResponse = {
  symbols?: BlembergCoverageItem[];
  coverage?: BlembergCoverageItem[];
  items?: BlembergCoverageItem[];
  results?: BlembergCoverageItem[];
};
