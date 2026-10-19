import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import {
  AccountDetail, AccountSummary, ApiError, BuyerOption, Dashboard, JobOption, Product,
  Quote, SaleView, Session, Statement, TenderType,
} from './api.models';

/** Every call the till makes. Credentials are the session cookie, so withCredentials is on. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';
  private readonly options = { withCredentials: true };

  login(username: string, password: string): Observable<Session> {
    return this.post<Session>('/auth/login', { username, password });
  }

  session(): Observable<Session> {
    return this.get<Session>('/auth/session');
  }

  logout(): Observable<void> {
    return this.post<void>('/auth/logout', {});
  }

  products(term: string, category: string): Observable<Product[]> {
    const params = new URLSearchParams();
    if (term) {
      params.set('term', term);
    }
    if (category && category !== 'All') {
      params.set('category', category);
    }
    return this.get<Product[]>(`/products?${params.toString()}`);
  }

  categories(): Observable<string[]> {
    return this.get<string[]>('/products/categories');
  }

  accounts(): Observable<AccountSummary[]> {
    return this.get<AccountSummary[]>('/accounts');
  }

  account(code: string): Observable<AccountDetail> {
    return this.get<AccountDetail>(`/accounts/${code}`);
  }

  statement(code: string): Observable<Statement> {
    return this.get<Statement>(`/accounts/${code}/statement`);
  }

  buyers(code: string): Observable<BuyerOption[]> {
    return this.get<BuyerOption[]>(`/accounts/${code}/buyers`);
  }

  jobs(code: string): Observable<JobOption[]> {
    return this.get<JobOption[]>(`/accounts/${code}/jobs`);
  }

  recordPayment(code: string, amount: number, method: TenderType, reference: string) {
    return this.post(`/accounts/${code}/payments`, { accountCode: code, amount, method, reference });
  }

  walkInSale(body: unknown): Observable<SaleView> {
    return this.post<SaleView>('/sales/walk-in', body);
  }

  accountSale(body: unknown): Observable<SaleView> {
    return this.post<SaleView>('/sales/account', body);
  }

  syncSales(body: unknown): Observable<{ received: number; accepted: number; rejected: number }> {
    return this.post('/sync/sales', body);
  }

  quote(shoppingList: string, accountCode: string | null): Observable<Quote> {
    return this.post<Quote>('/quotes', { shoppingList, accountCode });
  }

  dashboard(): Observable<Dashboard> {
    return this.get<Dashboard>('/owner/dashboard');
  }

  private get<T>(path: string): Observable<T> {
    return this.http.get<T>(this.base + path, this.options).pipe(catchError(this.asApiError));
  }

  private post<T>(path: string, body: unknown): Observable<T> {
    return this.http.post<T>(this.base + path, body, this.options).pipe(catchError(this.asApiError));
  }

  /**
   * Turns any failure into the one shape the screens handle.
   *
   * <p>A dropped connection does not produce an ApiError body: the browser
   * hands back a ProgressEvent and status 0. That case is flagged rather than
   * detected by reading the message, because the counter queues the sale on it.
   */
  private asApiError(error: HttpErrorResponse) {
    if (error.status === 0) {
      return throwError((): ApiError => ({
        message: 'The till cannot reach the server.',
        reasons: [],
        offline: true,
      }));
    }
    const body = error.error as Partial<ApiError> | null;
    const message = typeof body?.message === 'string'
      ? body.message
      : 'Something went wrong (' + error.status + ').';
    return throwError((): ApiError => ({
      message,
      reasons: Array.isArray(body?.reasons) ? body.reasons : [],
      offline: false,
    }));
  }
}
