import os
import json
import random
import requests
import re
import decimal
import uuid
from datetime import datetime, timedelta, time as _pytime
from typing import Dict, List, Any, Optional
from io import StringIO

import pandas as pd
import streamlit as st
from pydantic import BaseModel, Field, field_validator

from faker import Faker
import psycopg2
from psycopg2 import sql as pgsql, extras

# API key is injected via environment variable in Docker; keep backward compat for local runs.
if not os.environ.get("GROQ_API_KEY"):
    os.environ["GROQ_API_KEY"] = "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"

# ──────────────────── PAGE CONFIG ────────────────────
st.set_page_config(
    page_title="Synthetic DB Agent",
    page_icon="🧪",
    layout="wide",
    initial_sidebar_state="expanded",
)

# ──────────────────── CUSTOM CSS ────────────────────
st.markdown("""
<style>
    .block-container { padding-top: 1.5rem; }
    div[data-testid="stMetric"] {
        background: #f8f9fa; border-radius: .5rem; padding: .75rem;
        border-left: 4px solid #4e8cff;
    }
    .step-header { font-size: 1.1rem; font-weight: 600; margin-bottom: .5rem; }
    .stTabs [data-baseweb="tab-list"] { gap: 2rem; }
</style>
""", unsafe_allow_html=True)

# ──────────────────── SIDEBAR ────────────────────
with st.sidebar:
    st.header("Settings")

    st.subheader("Database Connection")
    PG_HOST     = st.text_input("Host", value=os.environ.get("PG_HOST", "localhost"))
    PG_PORT     = st.number_input("Port", min_value=1, max_value=65535, value=int(os.environ.get("PG_PORT", "5432")), step=1)
    PG_DATABASE = st.text_input("Database", value=os.environ.get("PG_DATABASE", "postgres"))
    PG_USER     = st.text_input("User", value=os.environ.get("PG_USER", "postgres"))
    PG_PASSWORD = st.text_input("Password", type="password", value=os.environ.get("PG_PASSWORD", "postgres"))
    SRC_SCHEMA  = st.text_input("Source schema (read tables from)", value=os.environ.get("SRC_SCHEMA", "public"))
    TGT_SCHEMA  = st.text_input("Target schema (load synthetic data)", value=os.environ.get("TGT_SCHEMA", "synthetic"))

    st.divider()
    st.subheader("LLM Endpoint")
    BASE_URL        = st.text_input("Base URL", value="https://api.groq.com/openai/v1")
    DEPLOYMENT_NAME = st.text_input("Model", value="llama-3.3-70b-versatile")
    API_KEY         = os.environ.get("GROQ_API_KEY", "")
    LLM_VERIFY_SSL  = st.checkbox("Verify SSL certificate", value=True)
    st.caption("Disable only in restricted corporate environments with custom/intercepting TLS certificates.")

    st.divider()
    st.subheader("Generation")
    DEFAULT_ROWS_PER_TABLE = st.number_input("Default rows per table", min_value=5, max_value=100_000, value=200, step=5)
    FAKER_LOCALE = st.text_input("Faker locale", value="en_IN")
    GLOBAL_SEED  = st.number_input("Seed", min_value=1, max_value=10_000_000, value=12345, step=1)

    st.divider()
    st.subheader("Run Mode")
    DEMO_MODE = st.checkbox("Demo Mode (no external calls)", value=False)
    st.caption("Demo Mode fakes LLM & PostgreSQL — great for showing the UI without a live database.")

# ──────────────────── HEADER ────────────────────
st.title("🧪 Synthetic DB Agent")
st.caption("Schema → LLM → Faker → PostgreSQL")

# ──────────────────── HELPERS ────────────────────

def get_pg_conn():
    """Return a new psycopg2 connection using sidebar settings."""
    return psycopg2.connect(
        host=PG_HOST,
        port=int(PG_PORT),
        dbname=PG_DATABASE,
        user=PG_USER,
        password=PG_PASSWORD,
    )


# Mapping from PostgreSQL data_type to a simplified canonical type used internally.
PG_TYPE_MAP: Dict[str, str] = {
    "integer": "INTEGER",
    "bigint": "BIGINT",
    "smallint": "SMALLINT",
    "serial": "INTEGER",
    "bigserial": "BIGINT",
    "numeric": "NUMERIC",
    "decimal": "NUMERIC",
    "real": "REAL",
    "double precision": "DOUBLE PRECISION",
    "character varying": "VARCHAR",
    "character": "CHAR",
    "text": "TEXT",
    "boolean": "BOOLEAN",
    "date": "DATE",
    "timestamp without time zone": "TIMESTAMP",
    "timestamp with time zone": "TIMESTAMPTZ",
    "time without time zone": "TIME",
    "time with time zone": "TIMETZ",
    "uuid": "UUID",
    "bytea": "BYTEA",
    "json": "JSON",
    "jsonb": "JSONB",
    "inet": "INET",
    "cidr": "CIDR",
    "macaddr": "MACADDR",
    "interval": "INTERVAL",
    "money": "MONEY",
    "xml": "XML",
    "point": "POINT",
    "array": "ARRAY",
    "user-defined": "TEXT",
}


def normalise_pg_type(raw_type: str) -> str:
    """Normalise a PostgreSQL data_type string to a canonical label."""
    return PG_TYPE_MAP.get(raw_type.lower(), raw_type.upper())


def list_schemas_with_tables() -> Dict[str, int]:
    """Return schemas that contain base tables and their table counts."""
    if DEMO_MODE:
        return {"public": 2, "synthetic": 2}

    conn = get_pg_conn()
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT table_schema, COUNT(*) AS table_count
                FROM information_schema.tables
                WHERE table_type = 'BASE TABLE'
                  AND table_schema NOT IN ('information_schema', 'pg_catalog')
                GROUP BY table_schema
                ORDER BY table_schema
                """
            )
            rows = cur.fetchall()
            return {r[0]: int(r[1]) for r in rows}
    finally:
        conn.close()


# ──────────────────── SCHEMA DISCOVERY ────────────────────

def discover_schema(src_schema: str) -> Dict[str, List[Dict[str, str]]]:
    if DEMO_MODE:
        return {
            "users": [
                {"name": "user_id", "type": "UUID"},
                {"name": "user_name", "type": "TEXT"},
                {"name": "segment", "type": "TEXT"},
                {"name": "created_at", "type": "TIMESTAMP"},
            ],
            "transactions": [
                {"name": "txn_id", "type": "UUID"},
                {"name": "user_id", "type": "UUID"},
                {"name": "txn_date", "type": "DATE"},
                {"name": "category", "type": "TEXT"},
                {"name": "amount", "type": "NUMERIC"},
                {"name": "payment_channel", "type": "TEXT"},
            ],
        }

    conn = get_pg_conn()
    try:
        with conn.cursor() as cur:
            # Get base tables
            cur.execute(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = %s AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """,
                (src_schema,),
            )
            tables = [row[0] for row in cur.fetchall()]
            if not tables:
                cur.execute(
                    """
                    SELECT table_schema, COUNT(*) AS table_count
                    FROM information_schema.tables
                    WHERE table_type = 'BASE TABLE'
                      AND table_schema NOT IN ('information_schema', 'pg_catalog')
                    GROUP BY table_schema
                    ORDER BY table_schema
                    """
                )
                available = cur.fetchall()
                if available:
                    options = ", ".join(f"{s} ({c})" for s, c in available)
                    raise RuntimeError(
                        f"No base tables found in schema '{src_schema}'. "
                        f"Try one of: {options}"
                    )
                raise RuntimeError(
                    f"No base tables found in schema '{src_schema}', and no user schemas with tables were detected."
                )

            # Get columns
            cur.execute(
                """
                SELECT table_name, column_name, data_type
                FROM information_schema.columns
                WHERE table_schema = %s
                ORDER BY table_name, ordinal_position
                """,
                (src_schema,),
            )
            all_cols = cur.fetchall()
    finally:
        conn.close()

    out: Dict[str, List[Dict[str, str]]] = {}
    for t in tables:
        out[t] = [
            {"name": c[1], "type": normalise_pg_type(c[2])}
            for c in all_cols
            if c[0] == t
        ]
    return out


# ──────────────────── SPEC TYPES ────────────────────

class ColumnSpec(BaseModel):
    strategy: str
    provider: Optional[str] = None
    choices: Optional[List[str]] = None
    min: Optional[float] = None
    max: Optional[float] = None
    decimals: Optional[int] = None
    date_start: Optional[str] = None
    date_end: Optional[str] = None
    ts_start: Optional[str] = None
    ts_end: Optional[str] = None
    template: Optional[str] = None   # bothify pattern for "template" strategy, e.g. "POL-#####"
    start: Optional[int] = None      # starting value for "sequential" strategy

    @field_validator("strategy")
    @classmethod
    def _s_ok(cls, v):
        allowed = {"faker", "categorical", "numeric", "date", "timestamp",
                    "time", "datetime", "uuid", "bytes", "boolean", "json",
                    "template", "sequential"}
        if v not in allowed:
            raise ValueError(f"strategy must be one of {allowed}")
        return v


class TableSpec(BaseModel):
    columns: Dict[str, ColumnSpec]


class GenSpec(BaseModel):
    row_counts: Dict[str, int] = Field(default_factory=dict)
    tables: Dict[str, TableSpec]


SYSTEM_PROMPT = """You are a data generation planner.
Input: dataset schema (tables with columns and PostgreSQL data types).
Return a STRICT JSON spec that assigns a generator to every column:

{
  "row_counts": { "<table>": <int>, ... },
  "tables": {
    "<table>": {
      "columns": {
        "<column>": {
          "strategy": "faker|categorical|numeric|date|timestamp|time|datetime|uuid|bytes|boolean|json|template|sequential",
          "provider": "<faker.provider>",
          "choices": ["..."],
          "min": <number>, "max": <number>, "decimals": <int>,
          "date_start": "YYYY-MM-DD", "date_end": "YYYY-MM-DD",
          "ts_start": "YYYY-MM-DD", "ts_end": "YYYY-MM-DD",
          "template": "<bothify-pattern>",
          "start": <int>
        }
      }
    }
  }
}

Rules:
- VALID JSON only (no code fences).
- No real prod values; invent plausible categories.
- Use common Faker providers for text columns (names/emails/phones/addresses).
- Provide sensible row_counts (>= 5) per table.
- For UUID columns use strategy "uuid".
- For BOOLEAN columns use strategy "boolean".
- For JSON/JSONB columns use strategy "json".
- For a table's OWN primary-key column (e.g. policy_id in policies) use strategy "sequential"
  with "start": 1. This guarantees unique, gapless IDs.
- For foreign-key columns (e.g. policy_id in claims, claim_id in claim_history) use strategy
  "numeric" with min=1 and max equal to the PARENT table's row_count. This ensures every FK
  value references a valid parent row.
- For status/enum-style VARCHAR columns use strategy "categorical" with the EXACT valid enum
  values the application expects — never use random words. Examples:
    * policy status  : ["ACTIVE", "INACTIVE", "PENDING", "CANCELLED"]
    * claim status   : ["SUBMITTED", "IN_REVIEW", "APPROVED", "REJECTED"]
    * claim_type     : ["MEDICAL", "DENTAL", "VISION", "AUTO", "HOME", "DISABILITY", "LIFE"]
    * coverage status: ["ACTIVE", "INACTIVE"]
- For formatted string patterns use strategy "template" with a Python Faker bothify pattern
  (# = digit, ? = letter). Example: policy_number -> {"strategy": "template", "template": "POL-#####"}
"""


def call_llm_spec(schema: Dict[str, Any]) -> Dict[str, Any]:
    def local_fallback_spec() -> Dict[str, Any]:
        # ── Row counts: give parent tables fewer rows so FK ranges stay tight ──
        POLICY_ROWS   = 20
        COVERAGE_ROWS = 40
        CLAIM_ROWS    = 50
        HISTORY_ROWS  = 80
        DEFAULT_ROWS  = 15

        row_counts: Dict[str, int] = {}
        for t in schema:
            tl = t.lower()
            if "polic" in tl and "coverage" not in tl:
                row_counts[t] = POLICY_ROWS
            elif "coverage" in tl:
                row_counts[t] = COVERAGE_ROWS
            elif "claim" in tl and "history" not in tl:
                row_counts[t] = CLAIM_ROWS
            elif "history" in tl:
                row_counts[t] = HISTORY_ROWS
            else:
                row_counts[t] = DEFAULT_ROWS

        def _parent_count(keyword: str, default: int) -> int:
            """Return row_count for the first table whose name contains keyword."""
            for t in schema:
                if keyword in t.lower():
                    return row_counts.get(t, default)
            return default

        def col_spec(table_name: str, col: dict) -> dict:
            cname = col["name"].lower()
            ctype = col["type"].upper()
            tl    = table_name.lower()
            cols  = schema.get(table_name, [])
            is_first_col = cols and cols[0]["name"].lower() == cname

            # ── Type-first decisions ──────────────────────────────────────────
            if ctype == "UUID":    return {"strategy": "uuid"}
            if ctype == "BOOLEAN": return {"strategy": "boolean"}
            if ctype in ("JSON", "JSONB"): return {"strategy": "json"}
            if ctype == "BYTEA":   return {"strategy": "bytes"}

            if ctype == "DATE":
                if cname in ("effective_date", "start_date"):
                    return {"strategy": "date", "date_start": "2022-01-01", "date_end": "2025-06-30"}
                if cname in ("expiry_date",    "end_date"):
                    return {"strategy": "date", "date_start": "2025-07-01", "date_end": "2028-12-31"}
                return {"strategy": "date", "date_start": "2024-01-01", "date_end": "2026-12-31"}
            if ctype in ("TIMESTAMP", "TIMESTAMPTZ"):
                return {"strategy": "timestamp", "ts_start": "2024-01-01", "ts_end": "2026-03-31"}
            if ctype in ("TIME", "TIMETZ"):
                return {"strategy": "time"}

            # ── Integer / numeric columns ─────────────────────────────────────
            if ctype in ("INTEGER", "BIGINT", "SMALLINT"):
                # First column ending in _id → treat as this table's PK (sequential)
                if is_first_col and cname.endswith("_id"):
                    return {"strategy": "sequential", "start": 1}
                # Known FK columns → range 1..parent_count so every value hits a valid parent row
                if cname == "policy_id":
                    n = _parent_count("polic", POLICY_ROWS)
                    return {"strategy": "numeric", "min": 1, "max": n, "decimals": 0}
                if cname == "claim_id":
                    n = _parent_count("claim", CLAIM_ROWS)
                    return {"strategy": "numeric", "min": 1, "max": n, "decimals": 0}
                if cname == "customer_id":
                    return {"strategy": "numeric", "min": 1001, "max": 9999, "decimals": 0}
                return {"strategy": "numeric", "min": 1, "max": 10000, "decimals": 0}

            if ctype in ("NUMERIC", "REAL", "DOUBLE PRECISION", "MONEY"):
                if cname in ("coverage_limit", "limit_amount"):
                    return {"strategy": "numeric", "min": 10000, "max": 500000, "decimals": 2}
                if cname == "claim_amount":
                    return {"strategy": "numeric", "min": 500, "max": 100000, "decimals": 2}
                return {"strategy": "numeric", "min": 0, "max": 10000, "decimals": 2}

            # ── Domain-specific string columns ────────────────────────────────
            if cname == "policy_number":
                return {"strategy": "template", "template": "POL-#####"}
            if cname == "status":
                if "polic" in tl and "coverage" not in tl:
                    return {"strategy": "categorical", "choices": ["ACTIVE", "ACTIVE", "ACTIVE", "INACTIVE", "PENDING", "CANCELLED"]}
                if "claim" in tl or "history" in tl:
                    return {"strategy": "categorical", "choices": ["SUBMITTED", "IN_REVIEW", "APPROVED", "REJECTED"]}
                return {"strategy": "categorical", "choices": ["ACTIVE", "INACTIVE"]}
            if cname == "claim_type":
                return {"strategy": "categorical", "choices": ["MEDICAL", "DENTAL", "VISION", "AUTO", "HOME", "DISABILITY", "LIFE"]}
            if cname in ("description", "reviewer_notes", "notes", "comments"):
                return {"strategy": "faker", "provider": "sentence"}
            if cname in ("name", "full_name", "customer_name"):
                return {"strategy": "faker", "provider": "name"}
            if cname == "email":
                return {"strategy": "faker", "provider": "email"}
            if cname in ("phone", "phone_number"):
                return {"strategy": "faker", "provider": "phone_number"}
            if cname == "category":
                return {"strategy": "categorical", "choices": ["Food", "Bills", "Shopping", "Travel", "Fuel", "Entertainment", "Other"]}
            if cname == "payment_channel":
                return {"strategy": "categorical", "choices": ["UPI", "Debit", "Credit Card", "Wallet"]}
            if cname in ("user_name", "username"):
                return {"strategy": "faker", "provider": "name"}

            # Default
            return {"strategy": "faker", "provider": "word"}

        return {
            "row_counts": row_counts,
            "tables": {
                t: {"columns": {c["name"]: col_spec(t, c) for c in schema[t]}}
                for t in schema
            },
        }

    if DEMO_MODE:
        return local_fallback_spec()

    url = f"{BASE_URL}/chat/completions"
    params = {}
    headers = {"Authorization": f"Bearer {API_KEY}", "Content-Type": "application/json"}
    body = {
        "model": DEPLOYMENT_NAME,
        "temperature": 0.2,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": json.dumps({"schema": schema}, indent=2)},
        ],
    }
    try:
        resp = requests.post(
            url,
            params=params,
            headers=headers,
            json=body,
            timeout=90,
            verify=LLM_VERIFY_SSL,
        )
        resp.raise_for_status()
        data = resp.json()
        content = data["choices"][0]["message"]["content"].strip()
        # Strip possible markdown code fences
        if content.startswith("```"):
            lines = content.splitlines()
            lines = [l for l in lines if not l.strip().startswith("```")]
            content = "\n".join(lines)
        return json.loads(content)
    except requests.exceptions.SSLError as e:
        st.warning(
            "LLM SSL verification failed. Falling back to local generation plan. "
            "If your company proxy rewrites certificates, disable 'Verify SSL certificate'."
        )
        st.caption(f"SSL detail: {e}")
        return local_fallback_spec()
    except (requests.RequestException, KeyError, json.JSONDecodeError) as e:
        st.warning("LLM call failed. Falling back to local generation plan.")
        st.caption(f"Fallback reason: {e}")
        return local_fallback_spec()


# ──────────────────── GENERATORS ────────────────────

def gen_decimal(mn, mx, decimals=2):
    q = decimal.Decimal(10) ** -decimals
    return decimal.Decimal(str(random.uniform(mn, mx))).quantize(q, rounding=decimal.ROUND_HALF_UP)


def gen_numeric_for_type(ctype_upper, mn, mx, decimals):
    if ctype_upper in ("NUMERIC", "MONEY"):
        d = decimals if decimals is not None else 2
        return float(gen_decimal(mn if mn is not None else 0, mx if mx is not None else 1000, d))
    if ctype_upper in ("INTEGER", "BIGINT", "SMALLINT"):
        return int(round(random.uniform(mn or 0, mx or 1000)))
    if ctype_upper in ("REAL", "DOUBLE PRECISION"):
        return round(random.uniform(mn or 0.0, mx or 1000.0), decimals if decimals else 6)
    return int(round(random.uniform(mn or 0, mx or 1000)))


def gen_date(ds, de):
    d0 = datetime.strptime(ds, "%Y-%m-%d").date()
    d1 = datetime.strptime(de, "%Y-%m-%d").date()
    span = max(1, (d1 - d0).days)
    return d0 + timedelta(days=random.randint(0, span))


def gen_ts(ts, te):
    t0 = datetime.strptime(ts, "%Y-%m-%d")
    t1 = datetime.strptime(te, "%Y-%m-%d")
    seconds = max(1, int((t1 - t0).total_seconds()))
    return (t0 + timedelta(seconds=random.randint(0, seconds))).isoformat()


def gen_time():
    return _pytime(
        hour=random.randint(0, 23),
        minute=random.randint(0, 59),
        second=random.randint(0, 59),
    ).isoformat()


def gen_datetime(ds="2025-01-01", de="2026-03-31"):
    d = gen_date(ds, de)
    t = gen_time()
    return f"{d.isoformat()} {t}"


def call_faker(fake: Faker, provider: str) -> str:
    obj: Any = fake
    for part in provider.split("."):
        if not hasattr(obj, part):
            return fake.word()
        obj = getattr(obj, part)
    try:
        return str(obj())
    except TypeError:
        return str(obj)


def generate_rows(
    schema: Dict[str, List[Dict[str, str]]],
    spec_dict: Dict[str, Any],
    default_rows: int,
    locale: str,
    seed: int,
) -> Dict[str, List[Dict[str, Any]]]:
    random.seed(seed)
    Faker.seed(seed)
    fake = Faker(locale)
    spec = GenSpec.model_validate(spec_dict)

    rows_by_table: Dict[str, List[Dict[str, Any]]] = {}
    for t in schema:
        n = spec.row_counts.get(t, default_rows)
        tcols = {c["name"]: c["type"] for c in schema[t]}
        colspecs = spec.tables.get(t, TableSpec(columns={})).columns
        out: List[Dict[str, Any]] = []
        seq_counters: Dict[str, int] = {}  # per-column counter, reset for each table

        for _ in range(n):
            row: Dict[str, Any] = {}
            for cname, ctype in tcols.items():
                cu = ctype.upper()
                cs = colspecs.get(cname)
                strat = cs.strategy if cs else None

                # ----- fallback when no spec for the column -----
                if cs is None:
                    if cu in ("INTEGER", "BIGINT", "SMALLINT", "NUMERIC", "REAL", "DOUBLE PRECISION", "MONEY"):
                        row[cname] = gen_numeric_for_type(cu, 0, 1000, 2 if cu in ("NUMERIC", "REAL", "DOUBLE PRECISION", "MONEY") else 0)
                    elif cu == "DATE":
                        row[cname] = gen_date("2025-01-01", "2026-03-31").isoformat()
                    elif cu in ("TIMESTAMP", "TIMESTAMPTZ"):
                        row[cname] = gen_ts("2025-01-01", "2026-03-31")
                    elif cu in ("TIME", "TIMETZ"):
                        row[cname] = gen_time()
                    elif cu == "BOOLEAN":
                        row[cname] = random.random() < 0.5
                    elif cu == "UUID":
                        row[cname] = str(uuid.uuid4())
                    elif cu in ("JSON", "JSONB"):
                        row[cname] = json.dumps({"key": fake.word(), "value": fake.sentence()})
                    elif cu == "BYTEA":
                        row[cname] = os.urandom(16).hex()
                    else:
                        row[cname] = call_faker(fake, "word")
                    continue

                # ----- strategy-based generation -----
                if strat == "faker":
                    row[cname] = call_faker(fake, cs.provider or "word")
                elif strat == "categorical":
                    row[cname] = random.choice(cs.choices or ["A", "B", "C"])
                elif strat == "numeric":
                    mn = cs.min if cs.min is not None else 0
                    mx = cs.max if cs.max is not None else 1000
                    dec = cs.decimals if cs.decimals is not None else (2 if cu in ("NUMERIC", "REAL", "DOUBLE PRECISION", "MONEY") else 0)
                    row[cname] = gen_numeric_for_type(cu, mn, mx, dec)
                elif strat == "date":
                    ds = cs.date_start or "2025-01-01"
                    de = cs.date_end or "2026-03-31"
                    row[cname] = gen_date(ds, de).isoformat()
                elif strat == "timestamp":
                    ts = cs.ts_start or "2025-01-01"
                    te = cs.ts_end or "2026-03-31"
                    row[cname] = gen_ts(ts, te)
                elif strat == "time":
                    row[cname] = gen_time()
                elif strat == "datetime":
                    ds = cs.ts_start or "2025-01-01"
                    de = cs.ts_end or "2026-03-31"
                    row[cname] = gen_datetime(ds, de)
                elif strat == "uuid":
                    row[cname] = str(uuid.uuid4())
                elif strat == "boolean":
                    row[cname] = random.random() < 0.5
                elif strat == "json":
                    row[cname] = json.dumps({"key": fake.word(), "value": fake.sentence()})
                elif strat == "bytes":
                    row[cname] = os.urandom(16).hex()
                elif strat == "template":
                    row[cname] = fake.bothify(cs.template or "??###")
                elif strat == "sequential":
                    start = cs.start if cs.start is not None else 1
                    seq_counters[cname] = seq_counters.get(cname, start - 1) + 1
                    row[cname] = seq_counters[cname]
                else:
                    row[cname] = call_faker(fake, "word")

            out.append(row)
        rows_by_table[t] = out
    return rows_by_table


# ──────────────────── POSTGRESQL LOAD ────────────────────

def _pg_col_type(canonical: str) -> str:
    """Map our canonical type back to a valid PostgreSQL CREATE TABLE type."""
    mapping = {
        "INTEGER": "INTEGER",
        "BIGINT": "BIGINT",
        "SMALLINT": "SMALLINT",
        "NUMERIC": "NUMERIC",
        "REAL": "REAL",
        "DOUBLE PRECISION": "DOUBLE PRECISION",
        "VARCHAR": "TEXT",
        "CHAR": "TEXT",
        "TEXT": "TEXT",
        "BOOLEAN": "BOOLEAN",
        "DATE": "DATE",
        "TIMESTAMP": "TIMESTAMP",
        "TIMESTAMPTZ": "TIMESTAMPTZ",
        "TIME": "TIME",
        "TIMETZ": "TIMETZ",
        "UUID": "UUID",
        "BYTEA": "BYTEA",
        "JSON": "JSONB",
        "JSONB": "JSONB",
        "INET": "INET",
        "CIDR": "CIDR",
        "MACADDR": "MACADDR",
        "INTERVAL": "INTERVAL",
        "MONEY": "NUMERIC(15,2)",
        "XML": "XML",
        "POINT": "POINT",
        "ARRAY": "TEXT",
    }
    return mapping.get(canonical, "TEXT")


def load_to_postgres(
    schema_info: Dict[str, List[Dict[str, str]]],
    rows_by_table: Dict[str, List[Dict[str, Any]]],
    tgt_schema: str,
) -> Dict[str, int]:
    if DEMO_MODE:
        return {t: len(rows) for t, rows in rows_by_table.items()}

    conn = get_pg_conn()
    results: Dict[str, int] = {}
    try:
        conn.autocommit = False
        with conn.cursor() as cur:
            # Ensure target schema exists
            cur.execute(
                pgsql.SQL("CREATE SCHEMA IF NOT EXISTS {}").format(pgsql.Identifier(tgt_schema))
            )

            for table_name, rows in rows_by_table.items():
                if not rows:
                    results[table_name] = 0
                    continue

                col_defs = schema_info[table_name]
                columns = [c["name"] for c in col_defs]
                col_types = [c["type"] for c in col_defs]

                # Drop + create table in target schema
                qualified = pgsql.SQL("{}.{}").format(
                    pgsql.Identifier(tgt_schema), pgsql.Identifier(table_name)
                )
                cur.execute(pgsql.SQL("DROP TABLE IF EXISTS {} CASCADE").format(qualified))

                create_cols = pgsql.SQL(", ").join(
                    pgsql.SQL("{} {}").format(pgsql.Identifier(c), pgsql.SQL(_pg_col_type(t)))
                    for c, t in zip(columns, col_types)
                )
                cur.execute(
                    pgsql.SQL("CREATE TABLE {} ({})").format(qualified, create_cols)
                )

                # Bulk insert using execute_values for performance
                col_ids = pgsql.SQL(", ").join(pgsql.Identifier(c) for c in columns)
                insert_sql = pgsql.SQL("INSERT INTO {} ({}) VALUES %s").format(qualified, col_ids)

                tuples = [
                    tuple(
                        json.dumps(row.get(c)) if col_types[i] in ("JSON", "JSONB") and isinstance(row.get(c), (dict, list))
                        else row.get(c)
                        for i, c in enumerate(columns)
                    )
                    for row in rows
                ]
                extras.execute_values(cur, insert_sql.as_string(conn), tuples, page_size=500)
                results[table_name] = len(rows)

        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

    return results


# ──────────────────── UI FLOW ────────────────────

# Progress tracker
def _step_done(n: int) -> bool:
    return st.session_state.get(f"step{n}_done", False)

# Status bar
status_cols = st.columns(4)
for i, label in enumerate(["1. Schema", "2. LLM Spec", "3. Generate", "4. Load"], start=1):
    with status_cols[i - 1]:
        if _step_done(i):
            st.success(label, icon="✅")
        else:
            st.info(label, icon="⬜")

st.divider()

# ──────────────────── SEQUENTIAL WORKFLOW ────────────────────

# Initialize session state tracking
if "step1_done" not in st.session_state:
    st.session_state["step1_done"] = False
if "step2_done" not in st.session_state:
    st.session_state["step2_done"] = False
if "step3_done" not in st.session_state:
    st.session_state["step3_done"] = False
if "step4_done" not in st.session_state:
    st.session_state["step4_done"] = False


def render_step_header(step_num: int, title: str, is_active: bool, is_complete: bool):
    """Render a step header with visual indicators."""
    if is_complete:
        badge = "✅"
        color = "#28a745"
    elif is_active:
        badge = "⏳"
        color = "#4e8cff"
    else:
        badge = f"{step_num}"
        color = "#ccc"
    
    st.markdown(f"<h3 style='color: {color}; margin-bottom: 1rem;'>{badge} {title}</h3>", unsafe_allow_html=True)


# ── STEP 1: Discover Schema ──
render_step_header(1, "📋 Discover Schema", True, st.session_state.get("step1_done", False))

col_left, col_right = st.columns([2, 1])
with col_left:
    st.markdown("Connect to your database and discover table schemas from the source schema.")

with col_right:
    col_inspect, col_discover = st.columns(2)
    with col_inspect:
        run_inspect = st.button("📊 Inspect", key="inspect_btn", use_container_width=True)
    with col_discover:
        run_discover = st.button("🔍 Discover", type="primary", key="discover_btn", use_container_width=True)

if run_inspect:
    try:
        with st.spinner("Checking available schemas..."):
            schema_counts = list_schemas_with_tables()
        if not schema_counts:
            st.warning("No user schemas with base tables found in this database.")
        else:
            st.dataframe(
                pd.DataFrame(
                    [{"schema": s, "base_tables": c} for s, c in schema_counts.items()]
                ),
                use_container_width=True,
                hide_index=True,
            )
    except Exception as e:
        st.error(f"Schema inspection failed: {e}")

if run_discover:
    try:
        with st.spinner("Connecting and reading schema..."):
            schema = discover_schema(SRC_SCHEMA)
        st.session_state["schema"] = schema
        st.session_state["step1_done"] = True
        st.success(f"✅ Discovered **{len(schema)}** table(s).")
        for tname, cols in schema.items():
            with st.expander(f"📄 {tname}  ({len(cols)} columns)", expanded=True):
                st.dataframe(
                    pd.DataFrame(cols),
                    use_container_width=True,
                    hide_index=True,
                )
    except Exception as e:
        st.error(f"Schema discovery failed: {e}")
        try:
            schema_counts = list_schemas_with_tables()
            if schema_counts:
                st.caption("Detected schemas in this database:")
                st.dataframe(
                    pd.DataFrame(
                        [{"schema": s, "base_tables": c} for s, c in schema_counts.items()]
                    ),
                    use_container_width=True,
                    hide_index=True,
                )
        except Exception:
            pass

elif "schema" in st.session_state:
    schema = st.session_state["schema"]
    st.info(f"✅ Schema cached — **{len(schema)}** table(s)")
    for tname, cols in schema.items():
        with st.expander(f"📄 {tname}  ({len(cols)} columns)"):
            st.dataframe(pd.DataFrame(cols), use_container_width=True, hide_index=True)

# ── STEP 2: LLM Spec ──
if st.session_state.get("step1_done"):
    st.divider()
    render_step_header(2, "🤖 Generate LLM Spec", True, st.session_state.get("step2_done", False))
    
    col_left, col_right = st.columns([2, 1])
    with col_left:
        st.markdown("Send the schema to the LLM to receive a synthetic-data generation plan.")
    with col_right:
        run_llm = st.button("⚡ Get Spec", type="primary", key="llm_btn", use_container_width=True)
    
    if run_llm:
        schema = st.session_state.get("schema")
        try:
            with st.spinner("Calling LLM for generation spec..."):
                spec_dict = call_llm_spec(schema)
                GenSpec.model_validate(spec_dict)
            st.session_state["spec"] = spec_dict
            st.session_state["step2_done"] = True
            st.success("✅ LLM spec ready.")
            with st.expander("📋 View Specification", expanded=False):
                st.json(spec_dict)
        except Exception as e:
            st.error(f"LLM spec failed: {e}")
    
    elif "spec" in st.session_state:
        st.info("✅ Spec cached from previous run")
        with st.expander("📋 View Specification", expanded=False):
            st.json(st.session_state["spec"])

# ── STEP 3: Generate Data ──
if st.session_state.get("step2_done"):
    st.divider()
    render_step_header(3, "🔧 Generate Synthetic Data", True, st.session_state.get("step3_done", False))
    
    col_left, col_right = st.columns([2, 1])
    with col_left:
        st.markdown("Generate synthetic rows using Faker according to the LLM specification.")
    with col_right:
        run_gen = st.button("🎲 Generate", type="primary", key="gen_btn", use_container_width=True)
    
    if run_gen:
        schema = st.session_state.get("schema")
        spec = st.session_state.get("spec")
        try:
            with st.spinner("Generating synthetic data..."):
                rows = generate_rows(schema, spec, DEFAULT_ROWS_PER_TABLE, FAKER_LOCALE, GLOBAL_SEED)
            st.session_state["rows"] = rows
            st.session_state["step3_done"] = True
            st.success("✅ Synthetic data generated!")

            # Metrics
            mcols = st.columns(len(rows))
            for idx, (tname, trows) in enumerate(rows.items()):
                with mcols[idx % len(mcols)]:
                    st.metric(tname, f"{len(trows):,} rows")

            # Preview
            for tname, trows in rows.items():
                with st.expander(f"👁️ Preview: {tname} (first 10 rows)", expanded=False):
                    st.dataframe(pd.DataFrame(trows[:10]), use_container_width=True, hide_index=True)
        except Exception as e:
            st.error(f"Generation failed: {e}")

    elif "rows" in st.session_state:
        rows = st.session_state["rows"]
        st.info("✅ Generated data cached")
        mcols = st.columns(max(len(rows), 1))
        for idx, (tname, trows) in enumerate(rows.items()):
            with mcols[idx % len(mcols)]:
                st.metric(tname, f"{len(trows):,} rows")
        for tname, trows in rows.items():
            with st.expander(f"👁️ Preview: {tname} (first 10 rows)"):
                st.dataframe(pd.DataFrame(trows[:10]), use_container_width=True, hide_index=True)

# ── STEP 4: Load to Database ──
if st.session_state.get("step3_done"):
    st.divider()
    render_step_header(4, "🚀 Load to Database", True, st.session_state.get("step4_done", False))
    
    col_left, col_right = st.columns([2, 1])
    with col_left:
        st.markdown(f"Create tables in schema **`{TGT_SCHEMA}`** and bulk-insert the generated rows.")
    with col_right:
        run_load = st.button("💾 Load Now", type="primary", key="load_btn", use_container_width=True)
    
    if run_load:
        schema = st.session_state.get("schema")
        spec = st.session_state.get("spec")
        rows = st.session_state.get("rows")
        try:
            with st.spinner("Creating tables and inserting data..."):
                loaded = load_to_postgres(schema, rows, TGT_SCHEMA)
            st.session_state["step4_done"] = True
            st.success("✅ Load complete!")
            st.dataframe(
                pd.DataFrame([{"Table": k, "Rows Loaded": v} for k, v in loaded.items()]),
                use_container_width=True,
                hide_index=True,
            )
            st.balloons()
        except Exception as e:
            st.error(f"Load failed: {e}")

# ──────────────────── FOOTER ────────────────────
st.divider()
st.caption("💡 Tip: Enable Demo Mode in the sidebar to preview the workflow without a live database or LLM.")