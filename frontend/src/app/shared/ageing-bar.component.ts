import { Component, Input, computed, signal } from '@angular/core';
import { Ageing } from '../core/api.models';

/** The four ageing buckets as one bar, so a row scans in a glance. */
@Component({
  selector: 'app-ageing-bar',
  standalone: true,
  template: `
    <div class="ageing" role="img" [attr.aria-label]="label()">
      <span class="a0" [style.width.%]="share().current"></span>
      <span class="a30" [style.width.%]="share().days30"></span>
      <span class="a60" [style.width.%]="share().days60"></span>
      <span class="a90" [style.width.%]="share().days90Plus"></span>
    </div>
  `,
})
export class AgeingBarComponent {
  private readonly value = signal<Ageing | null>(null);

  @Input({ required: true }) set ageing(ageing: Ageing) {
    this.value.set(ageing);
  }

  readonly share = computed(() => {
    const a = this.value();
    const total = a?.total ?? 0;
    if (!a || total <= 0) {
      return { current: 0, days30: 0, days60: 0, days90Plus: 0 };
    }
    return {
      current: (a.current / total) * 100,
      days30: (a.days30 / total) * 100,
      days60: (a.days60 / total) * 100,
      days90Plus: (a.days90Plus / total) * 100,
    };
  });

  readonly label = computed(() => {
    const a = this.value();
    return a
      ? `Current ${a.current}, 30 days ${a.days30}, 60 days ${a.days60}, 90 plus ${a.days90Plus}`
      : 'Nothing outstanding';
  });
}
