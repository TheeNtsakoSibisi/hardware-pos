import { Component, computed, inject, signal } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { OfflineQueueService } from '../core/offline-queue.service';
import {
  AccountSummary, ApiError, BuyerOption, CartLine, JobOption, Product, SaleView, TenderType,
} from '../core/api.models';
import { RandsPipe } from '../shared/money.pipe';
import { QtyPipe } from '../shared/qty.pipe';
import { DocumentComponent } from '../shared/document.component';

/**
 * The counter screen: one sale at a time, settled either at the till or against
 * a contractor account. Everything else in the app exists to serve this screen.
 */
@Component({
  selector: 'app-counter',
  standalone: true,
  imports: [FormsModule, LowerCasePipe, RandsPipe, QtyPipe, DocumentComponent],
  templateUrl: './counter.component.html',
})
export class CounterComponent {
  private readonly api = inject(ApiService);
  private readonly queue = inject(OfflineQueueService);
  readonly auth = inject(AuthService);

  readonly tillId = 'T1';

  readonly term = signal('');
  readonly category = signal('All');
  readonly categories = signal<string[]>([]);
  readonly results = signal<Product[]>([]);
  readonly cart = signal<CartLine[]>([]);
  readonly busy = signal(false);
  readonly error = signal<ApiError | null>(null);
  readonly pendingSync = this.queue.pending;
  readonly online = this.queue.online;

  /** Null while serving a walk-in; set once an account has been chosen. */
  readonly account = signal<AccountSummary | null>(null);
  readonly buyer = signal<BuyerOption | null>(null);
  readonly job = signal<JobOption | null>(null);
  readonly orderRef = signal('');

  readonly accounts = signal<AccountSummary[]>([]);
  readonly buyers = signal<BuyerOption[]>([]);
  readonly jobs = signal<JobOption[]>([]);
  readonly buyerPin = signal('');

  readonly dialog = signal<'none' | 'qty' | 'account' | 'buyer' | 'payment' | 'override'>('none');
  readonly pendingProduct = signal<Product | null>(null);
  readonly qtyInput = signal('');

  readonly tenderType = signal<TenderType>('CASH');
  readonly cashReceived = signal('');
  readonly tenderReference = signal('');
  readonly taxInvoiceName = signal('');
  readonly taxInvoiceVat = signal('');

  readonly overrideReason = signal('');
  readonly blockedReasons = signal<string[]>([]);

  readonly completed = signal<SaleView | null>(null);

  /** True when the last message was "we kept your sale", not "something broke". */
  readonly queuedLocally = computed(() => this.error()?.offline === true && this.pendingSync() > 0);

  readonly total = computed(() =>
    round2(this.cart().reduce((sum, line) => sum + round2(line.unitPrice * line.qty), 0)));

  readonly change = computed(() => {
    const received = Number(this.cashReceived());
    return Number.isFinite(received) ? Math.max(0, round2(received - this.total())) : 0;
  });

  constructor() {
    this.api.categories().subscribe((list) => this.categories.set(['All', ...list]));
    this.search();
    void this.queue.flush();
  }

  search() {
    this.api.products(this.term(), this.category()).subscribe({
      next: (list) => this.results.set(list),
      // Offline: the cashier keeps the lines already on the sale and can still
      // take payment; only the catalogue search goes quiet.
      error: () => this.results.set([]),
    });
  }

  chooseCategory(category: string) {
    this.category.set(category);
    this.search();
  }

  add(product: Product) {
    if (product.decimalQty) {
      this.pendingProduct.set(product);
      this.qtyInput.set('');
      this.dialog.set('qty');
      return;
    }
    this.addLine(product, 1);
  }

  confirmQty() {
    const product = this.pendingProduct();
    const qty = Number(this.qtyInput());
    if (!product || !Number.isFinite(qty) || qty <= 0) {
      return;
    }
    this.addLine(product, round3(qty));
    this.dialog.set('none');
  }

  private addLine(product: Product, qty: number) {
    const lines = [...this.cart()];
    const existing = lines.find((line) => line.product.sku === product.sku);
    if (existing) {
      existing.qty = round3(existing.qty + qty);
    } else {
      lines.push({ product, qty, unitPrice: this.priceOf(product) });
    }
    this.cart.set(lines);
  }

  changeQty(line: CartLine, qty: number) {
    const lines = this.cart()
      .map((l) => (l === line ? { ...l, qty: round3(qty) } : l))
      .filter((l) => l.qty > 0);
    this.cart.set(lines);
  }

  remove(line: CartLine) {
    this.cart.set(this.cart().filter((l) => l !== line));
  }

  clear() {
    this.cart.set([]);
    this.toWalkIn();
    this.error.set(null);
  }

  /** Trade price while an account is selected, shelf price otherwise. */
  private priceOf(product: Product): number {
    const account = this.account();
    if (!account) {
      return product.price;
    }
    const discount = account.priceTier === 'A' ? 0.1 : 0.15;
    return round2(product.price * (1 - discount));
  }

  private reprice() {
    this.cart.set(this.cart().map((line) => ({ ...line, unitPrice: this.priceOf(line.product) })));
  }

  toWalkIn() {
    this.account.set(null);
    this.buyer.set(null);
    this.job.set(null);
    this.orderRef.set('');
    this.reprice();
  }

  openAccountPicker() {
    this.api.accounts().subscribe((list) => this.accounts.set(list));
    this.dialog.set('account');
  }

  pickAccount(account: AccountSummary) {
    this.account.set(account);
    this.buyerPin.set('');
    this.api.buyers(account.code).subscribe((list) => {
      this.buyers.set(list);
      this.buyer.set(list[0] ?? null);
    });
    this.api.jobs(account.code).subscribe((list) => {
      this.jobs.set(list);
      this.job.set(list[0] ?? null);
    });
    this.dialog.set('buyer');
  }

  confirmBuyer() {
    this.reprice();
    this.dialog.set('none');
  }

  startPayment() {
    if (this.cart().length === 0) {
      return;
    }
    this.error.set(null);
    if (this.account()) {
      this.chargeToAccount(null);
      return;
    }
    this.cashReceived.set('');
    this.tenderReference.set('');
    this.dialog.set('payment');
  }

  completeWalkIn() {
    const total = this.total();
    const type = this.tenderType();
    if (type === 'CASH' && Number(this.cashReceived() || 0) < total) {
      this.error.set({ message: 'Cash received is less than the amount due.', reasons: [] });
      return;
    }

    const sale = {
      clientUuid: crypto.randomUUID(),
      tillId: this.tillId,
      lines: this.cart().map((line) => ({ sku: line.product.sku, qty: line.qty })),
      tenders: [{
        type,
        amount: total,
        reference: this.tenderReference() || null,
        cashReceived: type === 'CASH' ? Number(this.cashReceived()) : null,
      }],
      customerName: this.taxInvoiceName() || null,
      customerVatNo: this.taxInvoiceVat() || null,
      occurredAt: new Date().toISOString(),
    };

    this.busy.set(true);
    this.api.walkInSale(sale).subscribe({
      next: (view) => this.saleCaptured(view),
      error: (err: ApiError) => this.saleFailed(err, () => this.queue.queueWalkIn(sale)),
    });
  }

  chargeToAccount(overrideReason: string | null) {
    const account = this.account();
    const buyer = this.buyer();
    const job = this.job();
    if (!account || !buyer || !job) {
      return;
    }

    const sale = {
      clientUuid: crypto.randomUUID(),
      tillId: this.tillId,
      accountCode: account.code,
      buyerId: buyer.id,
      buyerPin: this.buyerPin(),
      jobCode: job.code,
      orderRef: this.orderRef() || null,
      lines: this.cart().map((line) => ({ sku: line.product.sku, qty: line.qty })),
      overrideReason,
      overrideBy: overrideReason ? this.auth.session()?.username ?? null : null,
      occurredAt: new Date().toISOString(),
    };

    this.busy.set(true);
    this.api.accountSale(sale).subscribe({
      next: (view) => this.saleCaptured(view),
      error: (err: ApiError) => {
        this.busy.set(false);
        if (err.message === 'Credit check failed') {
          // The manager decides. The reason is printed on the invoice.
          this.blockedReasons.set(err.reasons);
          this.overrideReason.set('');
          this.dialog.set('override');
          return;
        }
        this.error.set(err);
      },
    });
  }

  approveOverride() {
    if (!this.overrideReason().trim()) {
      return;
    }
    this.dialog.set('none');
    this.chargeToAccount(this.overrideReason().trim());
  }

  private saleCaptured(view: SaleView) {
    this.busy.set(false);
    this.completed.set(view);
    this.cart.set([]);
    this.toWalkIn();
    this.taxInvoiceName.set('');
    this.taxInvoiceVat.set('');
    this.dialog.set('none');
    this.search();
  }

  /** A sale that cannot reach the server is queued, not lost. */
  private saleFailed(error: ApiError, queueIt: () => void) {
    this.busy.set(false);
    if (error.offline) {
      queueIt();
      this.dialog.set('none');
      this.cart.set([]);
      this.toWalkIn();
      this.error.set({
        message: 'Saved to the till. It will sync when the connection returns.',
        reasons: [],
        offline: true,
      });
      return;
    }
    this.error.set(error);
  }

  closeDocument() {
    this.completed.set(null);
  }

  print() {
    window.print();
  }

  /** The next round note above the total: the cashier's usual shortcut. */
  roundUpTo(note: number): number {
    return Math.ceil(this.total() / note) * note;
  }

  standingClass(standing: string): string {
    if (standing === 'Good standing') {
      return 'chip ok';
    }
    return standing === 'Near limit' ? 'chip warn' : 'chip bad';
  }
}

function round2(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

function round3(value: number): number {
  return Math.round((value + Number.EPSILON) * 1000) / 1000;
}
