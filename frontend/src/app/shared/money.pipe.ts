import { Pipe, PipeTransform } from '@angular/core';

/** Rands, with a thin space between thousands, the way a South African till prints. */
@Pipe({ name: 'rands', standalone: true })
export class RandsPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '';
    }
    const sign = value < 0 ? '−R' : 'R';
    return sign + Math.abs(value).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  }
}
