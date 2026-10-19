import { DatePipe } from '@angular/common';
import { Component, Input } from '@angular/core';
import { Statement } from '../core/api.models';
import { RandsPipe } from '../shared/money.pipe';

/** A statement on A4, the document the contractor pays from. */
@Component({
  selector: 'app-statement',
  standalone: true,
  imports: [DatePipe, RandsPipe],
  templateUrl: './statement.component.html',
})
export class StatementComponent {
  @Input({ required: true }) statement!: Statement;

  readonly store = {
    name: 'Sibanye Hardware & Building Supplies',
    address: '14 Main Road, Florida, Roodepoort, 1709',
    phone: '011 555 0100',
    vatNo: '4012345678',
  };

  print() {
    window.print();
  }
}
