import { DatePipe, LowerCasePipe } from '@angular/common';
import { Component, Input } from '@angular/core';
import { SaleView } from '../core/api.models';
import { RandsPipe } from './money.pipe';
import { QtyPipe } from './qty.pipe';

/**
 * The printed document, laid out for A4.
 *
 * <p>A4 rather than a till roll: the store already owns an office printer, and
 * the same sheet has to serve as a quote, a tax invoice and a statement. A
 * walk-in slip takes half a sheet; an account invoice prints the customer copy
 * and the store's signature copy on one page.
 */
@Component({
  selector: 'app-document',
  standalone: true,
  imports: [DatePipe, LowerCasePipe, RandsPipe, QtyPipe],
  templateUrl: './document.component.html',
})
export class DocumentComponent {
  @Input({ required: true }) sale!: SaleView;
  @Input() accountName: string | null = null;

  readonly store = {
    name: 'Sibanye Hardware & Building Supplies',
    address: '14 Main Road, Florida, Roodepoort, 1709',
    phone: '011 555 0100',
    vatNo: '4012345678',
  };

  get isAccount(): boolean {
    return this.sale.saleType === 'ACCOUNT';
  }

  get exclVat(): number {
    return Math.round((this.sale.total - this.sale.vat + Number.EPSILON) * 100) / 100;
  }
}
