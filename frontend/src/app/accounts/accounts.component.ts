import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ApiService } from '../core/api.service';
import { AccountDetail, AccountSummary, Statement } from '../core/api.models';
import { RandsPipe } from '../shared/money.pipe';
import { AgeingBarComponent } from '../shared/ageing-bar.component';
import { StatementComponent } from './statement.component';

/** Who owes what, how old it is, and the statement that proves it. */
@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [DatePipe, RandsPipe, AgeingBarComponent, StatementComponent],
  templateUrl: './accounts.component.html',
})
export class AccountsComponent {
  private readonly api = inject(ApiService);

  readonly accounts = signal<AccountSummary[]>([]);
  readonly detail = signal<AccountDetail | null>(null);
  readonly statement = signal<Statement | null>(null);
  readonly loading = signal(true);

  constructor() {
    this.api.accounts().subscribe({
      next: (list) => {
        this.accounts.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  get owedTotal(): number {
    return this.accounts().reduce((sum, a) => sum + a.balance, 0);
  }

  get over60Total(): number {
    return this.accounts().reduce((sum, a) => sum + a.ageing.days60 + a.ageing.days90Plus, 0);
  }

  get needingAttention(): number {
    return this.accounts().filter((a) => a.standing !== 'Good standing').length;
  }

  open(account: AccountSummary) {
    this.api.account(account.code).subscribe((detail) => this.detail.set(detail));
  }

  back() {
    this.detail.set(null);
    this.statement.set(null);
  }

  showStatement(code: string) {
    this.api.statement(code).subscribe((statement) => this.statement.set(statement));
  }

  closeStatement() {
    this.statement.set(null);
  }

  standingClass(standing: string): string {
    if (standing === 'Good standing') {
      return 'chip ok';
    }
    return standing === 'Near limit' ? 'chip warn' : 'chip bad';
  }
}
