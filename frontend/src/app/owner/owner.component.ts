import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ApiService } from '../core/api.service';
import { Dashboard } from '../core/api.models';
import { RandsPipe } from '../shared/money.pipe';
import { QtyPipe } from '../shared/qty.pipe';

/** Takings, debtors and what to reorder: the three things the owner checks. */
@Component({
  selector: 'app-owner',
  standalone: true,
  imports: [DatePipe, RandsPipe, QtyPipe],
  templateUrl: './owner.component.html',
})
export class OwnerComponent {
  private readonly api = inject(ApiService);

  readonly dashboard = signal<Dashboard | null>(null);
  readonly loading = signal(true);

  /** Bars scaled to the best day in the window, drawn as plain SVG. */
  readonly bars = computed(() => {
    const data = this.dashboard()?.dailyTakings ?? [];
    const max = Math.max(1, ...data.map((d) => d.total));
    const width = 640;
    const height = 180;
    const barWidth = data.length ? width / data.length : 0;
    return data.map((day, index) => ({
      date: day.date,
      total: day.total,
      x: index * barWidth + 2,
      y: height - (day.total / max) * height,
      width: Math.max(1, barWidth - 4),
      height: (day.total / max) * height,
      saturday: new Date(day.date).getDay() === 6,
    }));
  });

  constructor() {
    this.api.dashboard().subscribe({
      next: (data) => {
        this.dashboard.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
