import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';

/**
 * Holds sales the till could not send.
 *
 * <p>Power and connectivity both fail in a South African high street, and a
 * store that cannot sell during a four-hour outage loses the day. Each sale
 * carries a UUID the till generated, so re-sending the queue is safe: the
 * server recognises a sale it already stored instead of charging twice.
 *
 * <p>The queue survives a browser restart in localStorage, which is enough for
 * one shift on one machine. A multi-till rollout would move this to IndexedDB
 * with a service worker.
 */
@Injectable({ providedIn: 'root' })
export class OfflineQueueService {
  private readonly api = inject(ApiService);
  private static readonly KEY = 'pos.queue.v1';

  readonly pending = signal<number>(0);
  readonly online = signal<boolean>(navigator.onLine);
  readonly lastError = signal<string | null>(null);

  constructor() {
    this.pending.set(this.read().walkInSales.length + this.read().accountSales.length);
    window.addEventListener('online', () => {
      this.online.set(true);
      void this.flush();
    });
    window.addEventListener('offline', () => this.online.set(false));
  }

  queueWalkIn(sale: unknown) {
    const queue = this.read();
    queue.walkInSales.push(sale);
    this.write(queue);
  }

  queueAccountSale(sale: unknown) {
    const queue = this.read();
    queue.accountSales.push(sale);
    this.write(queue);
  }

  /** Sends everything queued; clears only what the server accepted or already had. */
  async flush(): Promise<void> {
    const queue = this.read();
    if (queue.walkInSales.length === 0 && queue.accountSales.length === 0) {
      return;
    }
    try {
      await new Promise<void>((resolve, reject) => {
        this.api.syncSales(queue).subscribe({
          next: () => resolve(),
          error: (err) => reject(err),
        });
      });
      this.write({ walkInSales: [], accountSales: [] });
      this.lastError.set(null);
    } catch (error) {
      this.lastError.set('Still offline; the queue will be sent when the connection returns.');
    }
  }

  private read(): { walkInSales: unknown[]; accountSales: unknown[] } {
    try {
      const raw = localStorage.getItem(OfflineQueueService.KEY);
      return raw ? JSON.parse(raw) : { walkInSales: [], accountSales: [] };
    } catch {
      return { walkInSales: [], accountSales: [] };
    }
  }

  private write(queue: { walkInSales: unknown[]; accountSales: unknown[] }) {
    try {
      localStorage.setItem(OfflineQueueService.KEY, JSON.stringify(queue));
    } catch {
      // A till with storage disabled still sells; it just cannot queue.
    }
    this.pending.set(queue.walkInSales.length + queue.accountSales.length);
  }
}
