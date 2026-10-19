# Domain rules

The rules the code is built to, written in the store's language rather than in types. If a
rule here and the code disagree, one of them is a bug.

## Money

- Every amount is rands, `BigDecimal`, scale 2, `RoundingMode.HALF_UP`. No `double` touches
  the money path.
- Prices are **VAT-inclusive**. VAT is derived out of the total (`total − total ÷ 1.15`),
  never added to a net figure, so the printed total equals what the customer paid.
- A line total is `unit price × quantity`, rounded once at the end rather than at each step.
- Quantities carry three decimals: cable is sold by the metre, sand by the cubic metre.

## Pricing

- A walk-in customer pays the shelf price.
- A contractor pays the shelf price less their tier's discount: tier A 10%, tier B 15%.
- The discount is applied per unit and then multiplied, so the price printed beside the line
  is the price the contractor can check by hand.
- Tiers are rows in `price_tier`, so the owner can add one without a release.

## Sales

- A sale is **append-only**. Nothing is edited after capture; a correction is a new sale
  that reverses the first.
- Walk-in and account sales are the same record. What differs is settlement.
- A walk-in sale must be covered by its tenders, which may be split across cash, card and
  EFT. Change is received less due, never negative.
- An account sale needs a buyer, a job and a due date. It needs no tenders.
- Stock leaves as a `stock_movement` row, never as an update to a count on the product.

## Document numbers

- Every number carries its till's prefix: `T1-000123` for a sale, `T1-A00007` for an account
  invoice, `RCPT-00042` for a payment.
- Numbers come from `document_sequence`, locked `FOR UPDATE` while one is issued.
- Two tills that have been offline all morning can both capture sales and sync without
  colliding.

## Offline

- The till generates a `clientUuid` for every sale before the server sees it.
- Re-sending a sale is safe: the server returns the sale it already stored rather than
  capturing a second one.
- A batch is processed per sale. One rejected sale is reported back with its reasons and
  skipped; it does not block the rest of the queue.
- The till keeps its own timestamp for a sale captured offline, unless that timestamp is in
  the future.

## Credit

A sale may go onto an account unless any of these hold:

1. **The account is not active** — on hold or closed.
2. **Something on it is 90 days old or more.**
3. **The sale would take the balance past the credit limit.** A sale landing exactly on the
   limit is allowed.
4. **The collector is over their own per-sale limit.** A buyer with no limit is not stopped
   by one.

All four are evaluated and reported together, so the cashier explains the problem once.

- A buyer confirms with a PIN, BCrypt hashed. The PIN identifies who collected; it is not a
  security boundary on its own.
- A manager may override a refusal. The reason and the manager's username are stored on the
  invoice and printed on it.
- The PIN is checked before the credit rules, so a wrong PIN never reveals an account's
  financial position.

## Ageing

- Buckets are by **invoice date**, not due date, because that is how a contractor reads
  their own statement: current, 30, 60, 90+.
- A bucket boundary is inclusive at the lower end: an invoice 30 days old is in the 30-day
  bucket, 89 days is in the 60-day bucket, 90 days is in 90+.
- Ageing is computed from allocations, so the buckets always add up to the balance.
- A part-paid invoice ages only by what is still owed.

## Payments

- Allocated **oldest invoice first**. Contractors pay round amounts off a statement, so the
  store has to decide the split.
- An overpayment stays unallocated on the account rather than being forced onto an invoice
  that does not exist yet.
- A payment of zero or less is refused.

## Roles

| | Cashier | Manager | Owner |
|---|---|---|---|
| Sell, charge to accounts, quote | yes | yes | yes |
| Approve a credit override | no | yes | yes |
| Record a payment on an account | no | yes | yes |
| Owner dashboard | no | yes | yes |

## Printing

- A4 throughout, because the store already owns an office printer and the same paper has to
  serve quotes, invoices and statements.
- A walk-in slip takes half a sheet.
- An account invoice prints the customer copy and the store's signature copy on one sheet.
  The signed store copy is the evidence when an invoice is queried.
- A walk-in customer can ask for a tax invoice in their own name and VAT number.

## Quote parsing

- A parsed line never becomes a sale on its own. A person confirms every line.
- Anything below high confidence comes back flagged, with a short note saying what was
  assumed.
- A line that matches nothing is returned with no product rather than with a guess.
- Headings and greetings in a pasted list are dropped, not flagged as unmatched items.
