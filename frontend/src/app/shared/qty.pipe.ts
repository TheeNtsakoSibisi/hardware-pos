import { Pipe, PipeTransform } from '@angular/core';

/** Shows 12.5 for cable by the metre, and 3 rather than 3.0 for whole units. */
@Pipe({ name: 'qty', standalone: true })
export class QtyPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '';
    }
    return Number.isInteger(value) ? String(value) : value.toFixed(value * 10 % 1 === 0 ? 1 : 3);
  }
}
