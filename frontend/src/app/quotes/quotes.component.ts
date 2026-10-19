import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AccountSummary, ApiError, Quote } from '../core/api.models';
import { RandsPipe } from '../shared/money.pipe';
import { QtyPipe } from '../shared/qty.pipe';

/**
 * Builders send materials lists as WhatsApp text. This turns one into a priced
 * quote, flags whatever the parser was unsure about, and leaves the cashier to
 * confirm before anything is printed.
 */
@Component({
  selector: 'app-quotes',
  standalone: true,
  imports: [FormsModule, RandsPipe, QtyPipe],
  templateUrl: './quotes.component.html',
})
export class QuotesComponent {
  private readonly api = inject(ApiService);

  static readonly SAMPLE = `Mandla - Protea Glen job
10 pockets cement
2 cube sand
500 stock bricks
6 x 110 pipe
4 bends 110
3 boxes 4x40 chipboard screws
20m surfix 2.5
1 pva white 20L
2 lintels 2.4`;

  readonly shoppingList = signal('');
  readonly accountCode = signal<string>('');
  readonly accounts = signal<AccountSummary[]>([]);
  readonly quote = signal<Quote | null>(null);
  readonly busy = signal(false);
  readonly error = signal<ApiError | null>(null);

  constructor() {
    this.api.accounts().subscribe((list) => this.accounts.set(list));
  }

  useSample() {
    this.shoppingList.set(QuotesComponent.SAMPLE);
  }

  build() {
    if (!this.shoppingList().trim()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.api.quote(this.shoppingList(), this.accountCode() || null).subscribe({
      next: (quote) => {
        this.quote.set(quote);
        this.busy.set(false);
      },
      error: (err: ApiError) => {
        this.error.set(err);
        this.busy.set(false);
      },
    });
  }

  print() {
    window.print();
  }

  rowClass(line: Quote['lines'][number]): string {
    if (!line.sku) {
      return 'qline none';
    }
    return line.needsChecking ? 'qline flag' : 'qline';
  }
}
