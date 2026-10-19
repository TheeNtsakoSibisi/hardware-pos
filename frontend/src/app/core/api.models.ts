/** Shapes returned by the backend. Kept in one place so a contract change is one edit. */

export type SaleType = 'WALK_IN' | 'ACCOUNT';
export type TenderType = 'CASH' | 'CARD' | 'EFT';
export type Role = 'CASHIER' | 'MANAGER' | 'OWNER';

export interface Session {
  username: string;
  displayName: string;
  role: Role;
}

export interface Product {
  sku: string;
  barcode: string | null;
  name: string;
  category: string;
  unit: string;
  decimalQty: boolean;
  price: number;
  stockOnHand: number;
  reorderLevel: number;
}

export interface SaleLineView {
  description: string;
  qty: number;
  unit: string;
  unitPrice: number;
  lineTotal: number;
}

export interface TenderView {
  type: TenderType;
  amount: number;
  reference: string | null;
  cashReceived: number | null;
  changeGiven: number | null;
}

export interface SaleView {
  documentNo: string;
  saleType: SaleType;
  occurredAt: string;
  total: number;
  vat: number;
  dueDate: string | null;
  customerName: string | null;
  customerVatNo: string | null;
  orderRef: string | null;
  accountName: string | null;
  buyerName: string | null;
  jobDescription: string | null;
  overrideReason: string | null;
  overrideBy: string | null;
  capturedBy: string;
  lines: SaleLineView[];
  tenders: TenderView[];
}

export interface Ageing {
  current: number;
  days30: number;
  days60: number;
  days90Plus: number;
  total: number;
}

export interface AccountSummary {
  code: string;
  name: string;
  contactName: string;
  phone: string;
  email: string;
  vatNo: string | null;
  priceTier: string;
  creditLimit: number;
  termsDays: number;
  status: 'ACTIVE' | 'ON_HOLD' | 'CLOSED';
  balance: number;
  available: number;
  ageing: Ageing;
  standing: string;
}

export interface BuyerOption {
  id: number;
  name: string;
  roleTitle: string;
  perSaleLimit: number | null;
}

export interface JobOption {
  code: string;
  description: string;
}

export interface InvoiceView {
  documentNo: string;
  date: string;
  job: string;
  buyer: string;
  total: number;
  outstanding: number;
  overridden: boolean;
}

export interface PaymentView {
  paymentNo: string;
  date: string;
  amount: number;
  method: string;
  reference: string | null;
}

export interface AccountDetail {
  summary: AccountSummary;
  buyers: BuyerOption[];
  jobs: { code: string; description: string; spend: number }[];
  invoices: InvoiceView[];
  payments: PaymentView[];
}

export interface StatementLine {
  date: string;
  reference: string;
  detail: string;
  debit: number;
  credit: number;
  balance: number;
}

export interface Statement {
  account: AccountSummary;
  from: string;
  to: string;
  openingBalance: number;
  lines: StatementLine[];
  ageing: Ageing;
}

export interface QuoteLine {
  sourceText: string;
  sku: string | null;
  description: string | null;
  qty: number;
  unit: string | null;
  unitPrice: number | null;
  lineTotal: number | null;
  confidence: 'HIGH' | 'MEDIUM' | 'LOW';
  note: string;
  needsChecking: boolean;
}

export interface Quote {
  lines: QuoteLine[];
  total: number;
  linesNeedingChecks: number;
  pricedFor: string;
  parserUsed: string;
}

export interface DayTakings {
  date: string;
  transactions: number;
  total: number;
  cash: number;
  card: number;
  eft: number;
}

export interface Dashboard {
  takingsThisMonth: number;
  owedByContractors: number;
  owedOver60Days: number;
  accountsNeedingAttention: number;
  itemsBelowReorderLevel: number;
  dailyTakings: DayTakings[];
  reorderSoon: { sku: string; name: string; unit: string; onHand: number; reorderLevel: number }[];
}

export interface ApiError {
  message: string;
  reasons: string[];
}

/** A line on the sale in progress, before it is posted. */
export interface CartLine {
  product: Product;
  qty: number;
  unitPrice: number;
}
