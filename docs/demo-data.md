# Demo data reference

The store, its staff, its customers and its stock are invented. No real data appears in
this repository. The dataset is generated from a fixed seed, so these figures are the same
on every machine; the dates shift so the scenarios stay true whenever you run it.

## Staff logins

| Username | Password | Role |
|----------|----------|------|
| `thandi` | `demo1234` | Cashier |
| `johan` | `demo1234` | Manager |
| `owner` | `demo1234` | Owner |

## Contractor accounts

Each account exists to exercise one rule at the counter.

| Account | Limit | Tier | Status | What it demonstrates |
|---------|-------|------|--------|----------------------|
| **Mokoena Builders** (`MOK001`) | R60 000 | B | Active | Healthy account: the sale goes through at trade prices. |
| **Ndlovu Plumbing & Drain** (`NDL001`) | R20 000 | A | Active | Close to its limit: a large sale tips it over. |
| **Sithole Construction** (`SIT001`) | R30 000 | B | Active | Over its limit: every sale is blocked until a manager overrides. |
| **Khumalo Renovations** (`KHU001`) | R25 000 | A | On Hold | On hold with debt past 90 days: blocked on two rules at once. |
| **Van Wyk Electrical** (`VWY001`) | R40 000 | A | Active | Several authorised buyers, each with their own ceiling. |
| **Mahlangu Roofing** (`MAH001`) | R10 000 | A | Active | Opened recently: nothing paid yet, nothing overdue. |

## Buyer PINs

Every buyer confirms a sale with a PIN. A buyer with a per-sale limit cannot charge more
than that in one go, however healthy the account is.

| Account | Buyer | Role | Per-sale limit | Demo PIN |
|---------|-------|------|----------------|----------|
| Mokoena Builders | Thabo Mokoena | Owner | — | `1111` |
| Mokoena Builders | Jabu Dlamini | Site foreman | R15 000 | `2201` |
| Mokoena Builders | Kagiso Molefe | Driver | R5 000 | `2202` |
| Ndlovu Plumbing & Drain | Sipho Ndlovu | Owner | — | `1112` |
| Ndlovu Plumbing & Drain | Themba Zulu | Plumber | R3 000 | `2203` |
| Sithole Construction | Lerato Sithole | Owner | — | `1113` |
| Sithole Construction | Mandla Nkosi | Site foreman | R10 000 | `2204` |
| Khumalo Renovations | Bongani Khumalo | Owner | — | `1114` |
| Van Wyk Electrical | Pieter van Wyk | Owner | — | `1115` |
| Van Wyk Electrical | Andile Mthembu | Electrician | R8 000 | `2205` |
| Van Wyk Electrical | Ruan Botha | Electrician | R8 000 | `2206` |
| Van Wyk Electrical | Lindiwe Cele | Office buyer | R20 000 | `2207` |
| Mahlangu Roofing | Nomsa Mahlangu | Owner | — | `1116` |
| Mahlangu Roofing | Sello Maseko | Roofer | R4 000 | `2208` |

Manager and owner passwords are the same `demo1234`; there is no separate override PIN,
because the override is authorised by whoever is signed in and their username is printed on
the invoice.

## Catalogue

223 products across nine categories, chosen to include the awkward cases a generic POS
handles badly:

- **No barcode:** loose screws, wall plugs, sand, bricks. Found by name or category instead.
- **Sold by the metre:** electrical cable, copper and HDPE pipe, skirting.
- **Sold by weight:** nails, roofing nails.
- **Sold by volume:** sand and stone by the cubic metre.
- **Bulk broken down:** chipboard screws as a box of 200 and as single units.
- **Serialised:** power tools, where a serial number matters for warranty.

## Trading history

- 90 days of walk-in sales, about 2 200 transactions, with cash, card and EFT split.
- 54 account invoices across the six contractors, each tagged to a job site and a buyer.
- 28 payments, allocated oldest invoice first.
- Closed on Sundays, busier on Saturdays and at month end — so the dashboard chart looks
  like a real trading week rather than noise.

## Regenerating

```bash
pip install bcrypt
python3 data/seed_generator.py
```

This rewrites `backend/src/main/resources/db/migration/V2__demo_seed.sql`. CI regenerates
it and fails if the committed file differs.
