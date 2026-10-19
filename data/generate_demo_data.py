"""
Demo data generator for the hardware store POS.
Deterministic (seeded) so every run produces the same data.
Outputs: products.csv, contractors.csv, authorised_buyers.csv, jobs.csv,
         account_invoices.csv, account_payments.csv, walkin_daily_sales.csv,
         seed.sql (PostgreSQL), demo_data.json (embedded in the demo app)
"""
import csv, json, random
from datetime import date, timedelta
from decimal import Decimal, ROUND_HALF_UP

random.seed(20260921)
TODAY = date(2026, 9, 21)
START = TODAY - timedelta(days=91)
VAT = Decimal("0.15")

def money(x):
    return Decimal(str(x)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

def price(x):
    """Round retail to a sensible shelf price (.99 / .49 style for small, whole rands for big)."""
    x = float(x)
    if x < 20:   return money(round(x * 2) / 2 - 0.01) if x > 1 else money(x)
    if x < 200:  return money(round(x) - 0.01)
    return money(round(x / 5) * 5 - 0.01)

# ---------------------------------------------------------------- products
# (category, name, unit, retail_incl_vat, barcoded, serialised)
P = []
def add(cat, name, unit, retail, barcoded=True, serial=False):
    P.append((cat, name, unit, price(retail), barcoded, serial))

# Building materials
add("Building", "Cement 42.5N 50kg", "BAG", 109)
add("Building", "Cement 32.5R 50kg", "BAG", 99)
add("Building", "Plaster sand", "BAG", 45, False)
add("Building", "Building sand", "BAG", 42, False)
add("Building", "River sand", "BAG", 48, False)
add("Building", "Crusher stone 19mm", "BAG", 55, False)
add("Building", "Building sand (loose)", "M3", 650, False)
add("Building", "Crusher stone 19mm (loose)", "M3", 780, False)
add("Building", "Stock brick NFP", "EACH", 2.9, False)
add("Building", "Face brick red", "EACH", 6.5, False)
add("Building", "Cement block 140mm", "EACH", 14.5, False)
add("Building", "Maxi brick", "EACH", 4.2, False)
add("Building", "Lintel 1.2m", "EACH", 89)
add("Building", "Lintel 2.4m", "EACH", 169)
add("Building", "Brickforce 150mm 20m", "ROLL", 79)
add("Building", "DPC 375mm 40m", "ROLL", 239)
add("Building", "Tile adhesive 20kg", "BAG", 139)
add("Building", "Tile grout 5kg white", "BAG", 89)
add("Building", "Rhinolite 40kg", "BAG", 159)
add("Building", "Readymix concrete 40kg", "BAG", 89)
add("Building", "Waterproofing compound 5L", "TIN", 329)
for L in (2.4, 3.0, 3.6, 4.2, 4.8, 6.0):
    add("Timber", f"SA pine 38x114 {L}m", "EACH", 26 * L)
    add("Timber", f"SA pine 38x38 {L}m", "EACH", 9.5 * L)
    add("Timber", f"SA pine 50x76 {L}m", "EACH", 22 * L)
add("Timber", "Shutterply 18mm 1220x2440", "SHEET", 689)
add("Timber", "Chipboard 16mm 1830x2750", "SHEET", 559)
add("Timber", "Hardboard 3mm 1220x2440", "SHEET", 219)
add("Timber", "Meranti PAR 19x44", "M", 38, False)
add("Timber", "Pine skirting 69mm", "M", 29, False)
# Roofing
for L in (3.0, 3.6, 4.2, 4.8, 6.0):
    add("Roofing", f"IBR sheet 0.47 {L}m", "EACH", 72 * L)
    add("Roofing", f"Corrugated sheet 0.4 {L}m", "EACH", 58 * L)
add("Roofing", "Roof screws 12x65 (box 100)", "BOX", 219)
add("Roofing", "Ridge capping 1.8m", "EACH", 159)
add("Roofing", "Sisalation 1.5x27.5m", "ROLL", 1150)
add("Roofing", "Gutter PVC 4m", "EACH", 239)
add("Roofing", "Downpipe PVC 3m", "EACH", 189)
# Plumbing
for d, p6 in ((50, 189), (110, 399), (160, 789)):
    add("Plumbing", f"PVC sewer pipe {d}mm 6m", "LENGTH", p6)
    add("Plumbing", f"PVC bend {d}mm 90deg", "EACH", p6 * 0.12)
    add("Plumbing", f"PVC junction {d}mm", "EACH", p6 * 0.16)
for d, pm in ((15, 42), (22, 69), (28, 99)):
    add("Plumbing", f"Copper pipe {d}mm", "M", pm, False)
    add("Plumbing", f"Copper elbow {d}mm", "EACH", pm * 0.45)
    add("Plumbing", f"Copper tee {d}mm", "EACH", pm * 0.65)
    add("Plumbing", f"Compression coupling {d}mm", "EACH", pm * 0.8)
for d, pm in ((15, 14), (20, 19), (25, 26)):
    add("Plumbing", f"HDPE pipe {d}mm", "M", pm, False)
add("Plumbing", "Garden hose 20m", "EACH", 299)
add("Plumbing", "Basin mixer chrome", "EACH", 699)
add("Plumbing", "Sink mixer chrome", "EACH", 899)
add("Plumbing", "Pillar tap 15mm", "EACH", 189)
add("Plumbing", "Garden tap brass 15mm", "EACH", 129)
add("Plumbing", "Stopcock 15mm", "EACH", 169)
add("Plumbing", "Tap washer kit", "PACK", 39)
add("Plumbing", "O-ring assortment", "PACK", 49)
add("Plumbing", "PTFE thread tape", "EACH", 12)
add("Plumbing", "Toilet cistern complete", "EACH", 1150)
add("Plumbing", "Toilet pan close-coupled", "EACH", 1450)
add("Plumbing", "Toilet seat white", "EACH", 229)
add("Plumbing", "Flexi connector 300mm", "EACH", 59)
add("Plumbing", "Geyser 150L 3kW", "EACH", 5999)
add("Plumbing", "Geyser element 3kW", "EACH", 289)
add("Plumbing", "Geyser thermostat", "EACH", 249)
add("Plumbing", "PVC solvent cement 250ml", "TIN", 99)
add("Plumbing", "Silicone sealant clear", "EACH", 79)
# Electrical
for mm, pm in ((1.5, 9.5), (2.5, 14.5), (4.0, 24), (6.0, 36)):
    add("Electrical", f"Surfix cable {mm}mm 3-core", "M", pm, False)
add("Electrical", "House wire 2.5mm red", "M", 6.5, False)
add("Electrical", "House wire 2.5mm black", "M", 6.5, False)
add("Electrical", "Earth wire 2.5mm", "M", 6.5, False)
add("Electrical", "Conduit PVC 20mm 4m", "EACH", 29)
add("Electrical", "Conduit bend 20mm", "EACH", 6.5)
add("Electrical", "Single plug socket", "EACH", 69)
add("Electrical", "Double plug socket", "EACH", 109)
add("Electrical", "1-lever light switch", "EACH", 45)
add("Electrical", "2-lever light switch", "EACH", 59)
add("Electrical", "3-pin plug top", "EACH", 29)
add("Electrical", "Extension cord 5m", "EACH", 199)
add("Electrical", "Multiplug 4-way", "EACH", 129)
add("Electrical", "DB board 12-way", "EACH", 549)
for a in (10, 16, 20, 32, 63):
    add("Electrical", f"Circuit breaker {a}A", "EACH", 79 if a < 63 else 249)
add("Electrical", "Earth leakage 63A", "EACH", 689)
add("Electrical", "LED downlight 6W", "EACH", 59)
add("Electrical", "LED bulb 9W E27", "EACH", 29)
add("Electrical", "LED bulb 9W B22", "EACH", 29)
add("Electrical", "Bulkhead light outdoor", "EACH", 169)
add("Electrical", "Insulation tape", "EACH", 15)
add("Electrical", "Cable ties 200mm (100)", "PACK", 49)
# Fasteners (loose by count, boxes)
for s in ("4x25", "4x40", "5x50", "6x60"):
    add("Fasteners", f"Chipboard screw {s} (loose)", "EACH", 0.45 if s < "5" else 0.85, False)
    add("Fasteners", f"Chipboard screw {s} (box 200)", "BOX", 69 if s < "5" else 129)
for s in ("50mm", "75mm", "100mm"):
    add("Fasteners", f"Wire nails {s}", "KG", 49, False)
add("Fasteners", "Roofing nails galv", "KG", 69, False)
for s in ("M8x50", "M10x75", "M12x100"):
    add("Fasteners", f"Hex bolt & nut {s}", "EACH", {"M8x50": 4.5, "M10x75": 8.5, "M12x100": 14}[s], False)
for s in ("6mm", "8mm", "10mm"):
    add("Fasteners", f"Nylon wall plug {s} (loose)", "EACH", 0.5, False)
add("Fasteners", "Anchor bolt M10x100", "EACH", 18, False)
add("Fasteners", "Coach screw 8x100", "EACH", 6.5, False)
add("Fasteners", "Washers M10 (loose)", "EACH", 0.6, False)
add("Fasteners", "Hinge butt 100mm (pair)", "PAIR", 39)
add("Fasteners", "Hasp & staple 100mm", "EACH", 49)
add("Fasteners", "Padlock 50mm brass", "EACH", 189)
add("Fasteners", "Door lock 3-lever", "EACH", 299)
add("Fasteners", "Door handle set", "EACH", 249)
# Paint
for n, p1, p5, p20 in (("PVA white", 69, 229, 799), ("Acrylic exterior white", 99, 399, 1399),
                      ("Enamel gloss black", 149, 599, None), ("Roof paint red oxide", 119, 499, 1799),
                      ("Universal undercoat", 109, 449, None)):
    add("Paint", f"{n} 1L", "TIN", p1)
    add("Paint", f"{n} 5L", "TIN", p5)
    if p20: add("Paint", f"{n} 20L", "TIN", p20)
add("Paint", "Paint roller 225mm", "EACH", 89)
add("Paint", "Paint brush 50mm", "EACH", 39)
add("Paint", "Paint brush 75mm", "EACH", 55)
add("Paint", "Paint tray", "EACH", 49)
add("Paint", "Masking tape 24mm", "EACH", 29)
add("Paint", "Mineral turps", "L", 45, False)
add("Paint", "Sandpaper 120 grit (sheet)", "EACH", 8.5, False)
add("Paint", "Crack filler 1kg", "TUB", 69)
# Hand tools
for n, p in (("Claw hammer 450g", 189), ("Brick hammer", 219), ("Club hammer 1.8kg", 249),
             ("Steel tape 8m", 159), ("Spirit level 600mm", 199), ("Spirit level 1200mm", 349),
             ("Brick trowel", 139), ("Plastering trowel", 129), ("Wood float", 79),
             ("Builders line 100m", 69), ("Chalk line", 89), ("Tile cutter 600mm", 699),
             ("Hacksaw", 149), ("Panel saw 550mm", 229), ("Screwdriver set 6pc", 169),
             ("Adjustable spanner 250mm", 199), ("Pipe wrench 350mm", 289), ("Pliers combination", 119),
             ("Wheelbarrow 65L", 899), ("Spade round nose", 229), ("Pick with handle", 349),
             ("Safety boots size 8", 499), ("Safety goggles", 49), ("Work gloves", 39), ("Dust mask (5)", 59)):
    add("Hand tools", n, "EACH", p)
# Power tools (serialised)
for n, p in (("Angle grinder 115mm 850W", 899), ("Angle grinder 230mm 2200W", 1899),
             ("Impact drill 750W", 1099), ("Cordless drill 18V kit", 2499), ("Rotary hammer SDS 800W", 2299),
             ("Circular saw 1400W", 1799), ("Jigsaw 650W", 999), ("Orbital sander", 899),
             ("Tile cutter electric 800W", 2799), ("Welding machine 160A inverter", 3499),
             ("Generator 2.8kVA petrol", 6999), ("Concrete mixer 120L", 5499)):
    add("Power tools", n, "EACH", p, True, True)
for n, p in (("Cutting disc 115mm", 12), ("Cutting disc 230mm", 29), ("Grinding disc 115mm", 25),
             ("Diamond blade 230mm", 249), ("Masonry drill bit 8mm", 29), ("Masonry drill bit 10mm", 35),
             ("HSS drill bit set 13pc", 199), ("SDS bit 12x160", 69), ("Welding rods 2.5mm 1kg", 119)):
    add("Power tools", n, "EACH", p)

UNIT_LABEL = {"EACH":"each","BAG":"bag","M3":"m³","ROLL":"roll","TIN":"tin","SHEET":"sheet","M":"metre",
              "BOX":"box","LENGTH":"6m length","PACK":"pack","KG":"kg","PAIR":"pair","L":"litre","TUB":"tub"}
DECIMAL_UNITS = {"M","KG","M3","L"}
CAT_CODE = {"Building":"BLD","Timber":"TMB","Roofing":"ROF","Plumbing":"PLB","Electrical":"ELC",
            "Fasteners":"FST","Paint":"PNT","Hand tools":"HTL","Power tools":"PWT"}

products = []
counters = {}
for cat, name, unit, retail, barcoded, serial in P:
    counters[cat] = counters.get(cat, 0) + 1
    sku = f"{CAT_CODE[cat]}-{counters[cat]:04d}"
    cost = money(float(retail) / random.uniform(1.28, 1.45))
    barcode = ("600" + "".join(str(random.randint(0, 9)) for _ in range(10))) if barcoded else ""
    reorder = random.choice([5, 10, 20, 50]) if unit not in DECIMAL_UNITS else random.choice([50, 100])
    stock = round(random.uniform(1.15, 4.0) * reorder, 1 if unit in DECIMAL_UNITS else 0)
    if random.random() < 0.07: stock = round(reorder * random.uniform(0.2, 0.9), 0)  # some low stock
    products.append({
        "sku": sku, "barcode": barcode, "name": name, "category": cat, "unit": unit,
        "unit_label": UNIT_LABEL[unit], "decimal_qty": unit in DECIMAL_UNITS,
        "retail_incl_vat": str(retail), "cost_excl_vat": str(cost),
        "stock_on_hand": stock, "reorder_level": reorder, "serialised": serial,
    })
PBY = {p["sku"]: p for p in products}
print("products:", len(products))

# quick keys: top non-barcoded counter items
QUICK = [p["sku"] for p in products if p["name"] in (
    "Building sand", "Plaster sand", "Stock brick NFP", "Chipboard screw 4x40 (loose)",
    "Wire nails 75mm", "Surfix cable 2.5mm 3-core", "Copper pipe 15mm", "HDPE pipe 20mm",
    "Nylon wall plug 8mm (loose)", "Sandpaper 120 grit (sheet)", "Mineral turps", "Hex bolt & nut M10x75")]

# ---------------------------------------------------------------- contractors
TIERS = {"A": Decimal("0.10"), "B": Decimal("0.15")}  # trade discount off retail
contractors = [
  dict(code="MOK001", name="Mokoena Builders", contact="Thabo Mokoena", phone="082 555 0141",
       email="accounts@mokoenabuilders.example", vat_no="4120287731", tier="B", limit=60000, terms=30,
       status="ACTIVE", story="Reliable builder, always pays within terms"),
  dict(code="NDL001", name="Ndlovu Plumbing & Drain", contact="Sipho Ndlovu", phone="073 555 0192",
       email="sipho@ndlovuplumbing.example", vat_no="4290115643", tier="A", limit=20000, terms=30,
       status="ACTIVE", story="Close to credit limit"),
  dict(code="SIT001", name="Sithole Construction", contact="Lerato Sithole", phone="071 555 0167",
       email="admin@sitholeconstruction.example", vat_no="4550342218", tier="B", limit=30000, terms=30,
       status="ACTIVE", story="Over credit limit, sale will be blocked"),
  dict(code="KHU001", name="Khumalo Renovations", contact="Bongani Khumalo", phone="083 555 0118",
       email="bongani@khumaloreno.example", vat_no="", tier="A", limit=25000, terms=30,
       status="ON_HOLD", story="90+ days overdue, account on hold"),
  dict(code="VWY001", name="Van Wyk Electrical", contact="Pieter van Wyk", phone="084 555 0175",
       email="office@vanwykelec.example", vat_no="4830059921", tier="A", limit=40000, terms=30,
       status="ACTIVE", story="Good payer, several authorised buyers"),
  dict(code="MAH001", name="Mahlangu Roofing", contact="Nomsa Mahlangu", phone="072 555 0133",
       email="nomsa@mahlanguroofing.example", vat_no="", tier="A", limit=10000, terms=30,
       status="ACTIVE", story="New account opened last month"),
]
buyers = [
  ("MOK001","Thabo Mokoena","Owner","1111",None), ("MOK001","Jabu Dlamini","Site foreman","2201",15000),
  ("MOK001","Kagiso Molefe","Driver","2202",5000),
  ("NDL001","Sipho Ndlovu","Owner","1112",None), ("NDL001","Themba Zulu","Plumber","2203",3000),
  ("SIT001","Lerato Sithole","Owner","1113",None), ("SIT001","Mandla Nkosi","Site foreman","2204",10000),
  ("KHU001","Bongani Khumalo","Owner","1114",None),
  ("VWY001","Pieter van Wyk","Owner","1115",None), ("VWY001","Andile Mthembu","Electrician","2205",8000),
  ("VWY001","Ruan Botha","Electrician","2206",8000), ("VWY001","Lindiwe Cele","Office buyer","2207",20000),
  ("MAH001","Nomsa Mahlangu","Owner","1116",None), ("MAH001","Sello Maseko","Roofer","2208",4000),
]
jobs = [
  ("MOK001","MOK-J01","Nkosi house extension, Soweto"), ("MOK001","MOK-J02","Ruimsig townhouses unit 4-6"),
  ("MOK001","MOK-J03","Boundary wall, Naturena"),
  ("NDL001","NDL-J01","Bathroom refit, Kensington"), ("NDL001","NDL-J02","Drainage repair, Yeoville school"),
  ("SIT001","SIT-J01","Double storey, Protea Glen"), ("SIT001","SIT-J02","Tuck shop build, Diepkloof"),
  ("KHU001","KHU-J01","Kitchen renovation, Linden"),
  ("VWY001","VWY-J01","Office rewire, Rosebank"), ("VWY001","VWY-J02","Solar prep, Northcliff"),
  ("VWY001","VWY-J03","Townhouse complex DBs, Fourways"),
  ("MAH001","MAH-J01","Reroof, Orlando East"),
]

# Category mix per contractor for invoice lines
MIX = {"MOK001":["Building","Timber","Roofing","Fasteners","Hand tools"],
       "NDL001":["Plumbing","Fasteners"], "SIT001":["Building","Timber","Fasteners","Paint"],
       "KHU001":["Paint","Timber","Plumbing","Building"], "VWY001":["Electrical","Fasteners"],
       "MAH001":["Roofing","Fasteners","Timber"]}

def trade_price(sku, tier):
    r = Decimal(PBY[sku]["retail_incl_vat"])
    return money(r * (1 - TIERS[tier]))

inv_seq = [0]
def make_invoice(c, d, target):
    """Build invoice lines from the contractor's category mix up to roughly `target` rand."""
    cats = MIX[c["code"]]
    pool = [p for p in products if p["category"] in cats and not p["serialised"]]
    lines, total = [], Decimal(0)
    while total < Decimal(target) * Decimal("0.9") and len(lines) < 14:
        p = random.choice(pool)
        if any(l["sku"] == p["sku"] for l in lines): continue
        up = trade_price(p["sku"], c["tier"])
        if p["decimal_qty"]: qty = Decimal(str(random.choice([5, 10, 15, 20, 25, 50])))
        elif up < 5: qty = Decimal(random.choice([100, 200, 500, 1000]))
        elif up < 50: qty = Decimal(random.choice([10, 20, 30, 50]))
        elif up < 300: qty = Decimal(random.randint(2, 12))
        else: qty = Decimal(random.randint(1, 3))
        lt = money(up * qty)
        if total + lt > Decimal(target) * Decimal("1.25") and lines: continue
        lines.append({"sku": p["sku"], "description": p["name"], "qty": str(qty), "unit": p["unit"],
                      "unit_price_incl_vat": str(up), "line_total": str(lt)})
        total += lt
    inv_seq[0] += 1
    cjobs = [j for j in jobs if j[0] == c["code"]]
    cbuy = [b for b in buyers if b[0] == c["code"]]
    b = random.choice(cbuy)
    return {"invoice_no": f"T1-A{inv_seq[0]:05d}", "account": c["code"], "date": d.isoformat(),
            "due_date": (d + timedelta(days=c["terms"])).isoformat(), "job": random.choice(cjobs)[1],
            "buyer": b[1], "order_ref": f"PO{random.randint(1000,9999)}", "total": str(total),
            "vat": str(money(total * VAT / (1 + VAT))), "lines": lines}

invoices, payments = [], []
pay_seq = [0]
def pay(c, d, amount, method, alloc):
    pay_seq[0] += 1
    payments.append({"payment_no": f"RCPT-{pay_seq[0]:05d}", "account": c["code"], "date": d.isoformat(),
                     "amount": str(money(amount)), "method": method,
                     "reference": f"{c['code']}-{d.strftime('%m%d')}", "allocated_to": alloc})

def days(n): return TODAY - timedelta(days=n)

def history(c, inv_days, avg, paid_rule):
    made = []
    for dd in inv_days:
        inv = make_invoice(c, days(dd), avg * random.uniform(0.6, 1.4))
        invoices.append(inv); made.append(inv)
    for inv in made:
        d_inv = date.fromisoformat(inv["date"])
        pr = paid_rule(inv, d_inv)
        if pr: pay(c, pr, Decimal(inv["total"]), "EFT", [inv["invoice_no"]])
    return made

C = {c["code"]: c for c in contractors}
# Mokoena: reliable. Invoices every ~10 days, paid ~20 days after invoice (if that date has passed)
history(C["MOK001"], [88, 79, 71, 62, 55, 47, 40, 33, 26, 19, 12, 6, 2], 6500,
        lambda inv, d: d + timedelta(days=21) if d + timedelta(days=21) <= TODAY else None)
# Ndlovu: pays, but slowly; lots of recent unpaid -> near limit (target ~R18.5k)
history(C["NDL001"], [85, 72, 60, 45, 38, 30, 21, 14, 9, 4], 3000,
        lambda inv, d: d + timedelta(days=33) if (TODAY - d).days > 40 else None)
# Sithole: over limit (target ~R32k+)
history(C["SIT001"], [90, 76, 64, 52, 44, 36, 29, 23, 16, 10, 5], 5200,
        lambda inv, d: d + timedelta(days=38) if (TODAY - d).days > 55 else None)
# Khumalo: old unpaid invoices, one small part-payment -> 90+ days
kh = history(C["KHU001"], [124, 112, 101, 95], 4500, lambda inv, d: None)
pay(C["KHU001"], days(70), Decimal("2000.00"), "CASH", [kh[0]["invoice_no"]])
# Van Wyk: good payer, bigger volume
history(C["VWY001"], [86, 80, 73, 66, 59, 52, 45, 38, 31, 24, 17, 11, 7, 3], 4200,
        lambda inv, d: d + timedelta(days=random.randint(18, 28)) if d + timedelta(days=28) <= TODAY else None)
# Mahlangu: new, two invoices
history(C["MAH001"], [24, 8], 3200, lambda inv, d: None)

def settle_to(code, target, when):
    """Add a part-payment so the account lands on a demo-friendly balance."""
    c = C[code]
    inv = sum(Decimal(i["total"]) for i in invoices if i["account"] == code)
    pd = sum(Decimal(p["amount"]) for p in payments if p["account"] == code)
    gap = inv - pd - Decimal(target)
    if gap > 0:
        paid = {n for p in payments for n in p["allocated_to"]}
        oldest = sorted([i for i in invoices if i["account"] == code and i["invoice_no"] not in paid],
                        key=lambda i: i["date"])
        pay(c, days(when), gap, "EFT", [oldest[0]["invoice_no"]] if oldest else [])

settle_to("NDL001", "18420.00", 12)   # near its R20k limit
settle_to("SIT001", "33150.00", 20)   # over its R30k limit

invoices.sort(key=lambda i: i["date"]); payments.sort(key=lambda p: p["date"])

# ---------------------------------------------------------------- walk-in daily sales (aggregated)
walkin = []
d = START
while d <= TODAY:
    wd = d.weekday()
    if wd == 6:  # closed Sundays
        d += timedelta(days=1); continue
    base = 14500 if wd == 5 else 9800
    if d.day >= 25 or d.day <= 2: base *= 1.3  # month-end payday bump
    txns = int(base / random.uniform(250, 380))
    total = money(base * random.uniform(0.8, 1.2))
    cash = money(total * Decimal(str(random.uniform(0.3, 0.45))))
    eft = money(total * Decimal(str(random.uniform(0.05, 0.12))))
    card = total - cash - eft
    walkin.append({"date": d.isoformat(), "transactions": txns, "total": str(total),
                   "cash": str(cash), "card": str(card), "eft": str(eft)})
    d += timedelta(days=1)

# ---------------------------------------------------------------- write CSVs
def wcsv(fn, rows, fields):
    with open(fn, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fields); w.writeheader()
        for r in rows: w.writerow({k: r[k] for k in fields})

wcsv("products.csv", products, ["sku","barcode","name","category","unit","decimal_qty","retail_incl_vat",
                                "cost_excl_vat","stock_on_hand","reorder_level","serialised"])
wcsv("contractors.csv", contractors, ["code","name","contact","phone","email","vat_no","tier","limit","terms","status","story"])
with open("authorised_buyers.csv","w",newline="") as f:
    w = csv.writer(f); w.writerow(["account","name","role","pin","per_sale_limit"])
    for b in buyers: w.writerow([b[0],b[1],b[2],b[3],b[4] or ""])
with open("jobs.csv","w",newline="") as f:
    w = csv.writer(f); w.writerow(["account","job_code","description"])
    for j in jobs: w.writerow(j)
with open("account_invoices.csv","w",newline="") as f:
    w = csv.writer(f); w.writerow(["invoice_no","account","date","due_date","job","buyer","order_ref","sku","description","qty","unit","unit_price_incl_vat","line_total"])
    for i in invoices:
        for l in i["lines"]:
            w.writerow([i["invoice_no"],i["account"],i["date"],i["due_date"],i["job"],i["buyer"],i["order_ref"],
                        l["sku"],l["description"],l["qty"],l["unit"],l["unit_price_incl_vat"],l["line_total"]])
with open("account_payments.csv","w",newline="") as f:
    w = csv.writer(f); w.writerow(["payment_no","account","date","amount","method","reference","allocated_to"])
    for p in payments: w.writerow([p["payment_no"],p["account"],p["date"],p["amount"],p["method"],p["reference"],";".join(p["allocated_to"])])
wcsv("walkin_daily_sales.csv", walkin, ["date","transactions","total","cash","card","eft"])

# ---------------------------------------------------------------- balances check
for c in contractors:
    inv = sum(Decimal(i["total"]) for i in invoices if i["account"] == c["code"])
    pd = sum(Decimal(p["amount"]) for p in payments if p["account"] == c["code"])
    print(f'{c["code"]}: invoiced {inv:>10} paid {pd:>10} balance {inv-pd:>10} limit {c["limit"]}')

# ---------------------------------------------------------------- JSON for the demo app
demo = {"today": TODAY.isoformat(), "vat_rate": 0.15, "tiers": {k: float(v) for k, v in TIERS.items()},
        "products": products, "quick_keys": QUICK, "contractors": contractors,
        "buyers": [dict(account=b[0], name=b[1], role=b[2], pin=b[3], per_sale_limit=b[4]) for b in buyers],
        "jobs": [dict(account=j[0], code=j[1], description=j[2]) for j in jobs],
        "invoices": invoices, "payments": payments, "walkin_daily": walkin,
        "store": {"name": "Sibanye Hardware & Building Supplies", "address": "14 Main Road, Florida, Roodepoort, 1709",
                  "phone": "011 555 0100", "vat_no": "4012345678", "reg_no": "2019/123456/07"}}
with open("demo_data.json", "w") as f: json.dump(demo, f, separators=(",", ":"))
print("invoices", len(invoices), "payments", len(payments))

# ---------------------------------------------------------------- seed.sql (PostgreSQL)
def q(v):
    if v is None or v == "": return "NULL"
    if isinstance(v, bool): return "TRUE" if v else "FALSE"
    if isinstance(v, (int, float, Decimal)): return str(v)
    return "'" + str(v).replace("'", "''") + "'"

sql = ["-- Demo seed for the hardware store POS (PostgreSQL). Generated by generate_demo_data.py",
"BEGIN;",
"""CREATE TABLE IF NOT EXISTS product (
  sku VARCHAR(20) PRIMARY KEY, barcode VARCHAR(20) UNIQUE, name VARCHAR(120) NOT NULL,
  category VARCHAR(40) NOT NULL, unit VARCHAR(10) NOT NULL, decimal_qty BOOLEAN NOT NULL,
  retail_incl_vat NUMERIC(12,2) NOT NULL, cost_excl_vat NUMERIC(12,2) NOT NULL,
  stock_on_hand NUMERIC(12,3) NOT NULL, reorder_level NUMERIC(12,3) NOT NULL, serialised BOOLEAN NOT NULL);""",
"""CREATE TABLE IF NOT EXISTS contractor_account (
  code VARCHAR(10) PRIMARY KEY, name VARCHAR(120) NOT NULL, contact VARCHAR(80), phone VARCHAR(20),
  email VARCHAR(120), vat_no VARCHAR(20), price_tier CHAR(1) NOT NULL, credit_limit NUMERIC(12,2) NOT NULL,
  terms_days INT NOT NULL, status VARCHAR(12) NOT NULL);""",
"""CREATE TABLE IF NOT EXISTS authorised_buyer (
  id SERIAL PRIMARY KEY, account_code VARCHAR(10) NOT NULL REFERENCES contractor_account(code),
  name VARCHAR(80) NOT NULL, role VARCHAR(40), pin_hash VARCHAR(100) NOT NULL, per_sale_limit NUMERIC(12,2));""",
"""CREATE TABLE IF NOT EXISTS job_site (
  code VARCHAR(12) PRIMARY KEY, account_code VARCHAR(10) NOT NULL REFERENCES contractor_account(code),
  description VARCHAR(160) NOT NULL);""",
"""CREATE TABLE IF NOT EXISTS account_invoice (
  invoice_no VARCHAR(20) PRIMARY KEY, account_code VARCHAR(10) NOT NULL REFERENCES contractor_account(code),
  invoice_date DATE NOT NULL, due_date DATE NOT NULL, job_code VARCHAR(12) REFERENCES job_site(code),
  buyer_name VARCHAR(80), order_ref VARCHAR(30), total_incl_vat NUMERIC(12,2) NOT NULL, vat NUMERIC(12,2) NOT NULL);""",
"""CREATE TABLE IF NOT EXISTS account_invoice_line (
  id SERIAL PRIMARY KEY, invoice_no VARCHAR(20) NOT NULL REFERENCES account_invoice(invoice_no),
  sku VARCHAR(20) NOT NULL REFERENCES product(sku), description VARCHAR(120) NOT NULL,
  qty NUMERIC(12,3) NOT NULL, unit VARCHAR(10) NOT NULL, unit_price_incl_vat NUMERIC(12,2) NOT NULL,
  line_total NUMERIC(12,2) NOT NULL);""",
"""CREATE TABLE IF NOT EXISTS account_payment (
  payment_no VARCHAR(20) PRIMARY KEY, account_code VARCHAR(10) NOT NULL REFERENCES contractor_account(code),
  payment_date DATE NOT NULL, amount NUMERIC(12,2) NOT NULL, method VARCHAR(10) NOT NULL, reference VARCHAR(40));""",
"""CREATE TABLE IF NOT EXISTS payment_allocation (
  payment_no VARCHAR(20) NOT NULL REFERENCES account_payment(payment_no),
  invoice_no VARCHAR(20) NOT NULL REFERENCES account_invoice(invoice_no), PRIMARY KEY (payment_no, invoice_no));""",
"""-- Demo-only: walk-in history as daily totals. The real system records individual sale events.
CREATE TABLE IF NOT EXISTS walkin_daily_summary (
  sale_date DATE PRIMARY KEY, transactions INT NOT NULL, total NUMERIC(12,2) NOT NULL,
  cash NUMERIC(12,2) NOT NULL, card NUMERIC(12,2) NOT NULL, eft NUMERIC(12,2) NOT NULL);""",
"CREATE EXTENSION IF NOT EXISTS pgcrypto;",
]
for p in products:
    sql.append("INSERT INTO product VALUES (" + ",".join(q(v) for v in [p["sku"], p["barcode"] or None, p["name"], p["category"],
        p["unit"], p["decimal_qty"], p["retail_incl_vat"], p["cost_excl_vat"], p["stock_on_hand"], p["reorder_level"], p["serialised"]]) + ");")
for c in contractors:
    sql.append("INSERT INTO contractor_account VALUES (" + ",".join(q(v) for v in [c["code"], c["name"], c["contact"], c["phone"],
        c["email"], c["vat_no"], c["tier"], c["limit"], c["terms"], c["status"]]) + ");")
for j in jobs:
    sql.append(f"INSERT INTO job_site VALUES ({q(j[1])},{q(j[0])},{q(j[2])});")
for b in buyers:
    sql.append(f"INSERT INTO authorised_buyer (account_code,name,role,pin_hash,per_sale_limit) VALUES "
               f"({q(b[0])},{q(b[1])},{q(b[2])},crypt({q(b[3])}, gen_salt('bf')),{q(b[4])});")
for i in invoices:
    sql.append("INSERT INTO account_invoice VALUES (" + ",".join(q(v) for v in [i["invoice_no"], i["account"], i["date"],
        i["due_date"], i["job"], i["buyer"], i["order_ref"], i["total"], i["vat"]]) + ");")
    for l in i["lines"]:
        sql.append("INSERT INTO account_invoice_line (invoice_no,sku,description,qty,unit,unit_price_incl_vat,line_total) VALUES (" +
                   ",".join(q(v) for v in [i["invoice_no"], l["sku"], l["description"], l["qty"], l["unit"], l["unit_price_incl_vat"], l["line_total"]]) + ");")
for p in payments:
    sql.append("INSERT INTO account_payment VALUES (" + ",".join(q(v) for v in [p["payment_no"], p["account"], p["date"],
        p["amount"], p["method"], p["reference"]]) + ");")
    for a in p["allocated_to"]:
        sql.append(f"INSERT INTO payment_allocation VALUES ({q(p['payment_no'])},{q(a)});")
for w in walkin:
    sql.append("INSERT INTO walkin_daily_summary VALUES (" + ",".join(q(v) for v in [w["date"], w["transactions"], w["total"], w["cash"], w["card"], w["eft"]]) + ");")
sql.append("COMMIT;")
with open("seed.sql", "w") as f: f.write("\n".join(sql) + "\n")
print("seed.sql lines:", len(sql))
