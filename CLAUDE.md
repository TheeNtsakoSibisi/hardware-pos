# Working on this repository

Point of sale for a small hardware store: walk-in sales and contractor credit accounts.
Spring Boot 3.5 on Java 21, PostgreSQL 16 with Flyway, Angular 20.

Read `docs/domain-rules.md` before changing anything in `sales`, `accounts` or `payments`.
It is the source of truth for the behaviour; the code follows it, not the other way round.

## Rules that are not negotiable

- **Money is `BigDecimal`**, scale 2, `HALF_UP`, via `za.co.ntsumo.pos.money.Money`. Never
  `double`, never a bare `new BigDecimal(...)` in a service.
- **Prices are VAT-inclusive.** VAT comes out of the total, never gets added to a net figure.
- **Sales are append-only.** Never add an update path to `Sale`. A correction is a new sale.
- **Stock is a sum of `stock_movement` rows.** Never add a quantity column to `product`.
- **Flyway owns the schema.** Add a new `V<n>__*.sql`; never edit a migration that has run,
  and never switch Hibernate off `ddl-auto: validate`.
- **Time is injected.** Use the `Clock` bean. Never `Instant.now()` or `LocalDate.now()`
  inside a service.
- **Document numbers go through `DocumentNumberService`**, which locks the sequence row.
- **Secrets come from the environment.** The Anthropic key is `POS_AI_ANTHROPIC_API_KEY` and
  is never written to a file in this repo.

## Testing

- Unit tests (`*Test`) run on `mvn test` and must not need a database.
- Integration tests (`*IT`) run on `mvn verify` against real PostgreSQL: `docker compose up -d db`.
- Any change to credit, ageing, allocation, VAT or rounding needs a test that would have
  failed before the change.
- Build test entities through `za.co.ntsumo.pos.testsupport.TestData`. Do not add public
  setters to an entity to make it testable.

## Demo data

`data/seed_generator.py` generates `V2__demo_seed.sql` from the CSVs in `data/`. Edit the
generator, never the migration. Dates must stay relative to `CURRENT_DATE`, and the output
must stay byte-identical across runs — CI checks both.

## Style

- Constructor injection, no field injection, no Lombok.
- Records for DTOs; entities keep their protected constructors and no setters.
- Comments explain *why*, not what. The reader can see what the code does.
- Angular: standalone components, signals for state, no `any`.
